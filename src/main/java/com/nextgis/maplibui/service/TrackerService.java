/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2018 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplib.util.PermissionUtil;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.NotificationHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_TRACKS;
import static com.nextgis.maplib.util.Constants.ONE_MINUTE;
import static com.nextgis.maplib.util.Constants.TAG;

public class TrackerService
        extends Service
        implements LocationListener, GpsStatus.Listener
{
    public static final  String TEMP_PREFERENCES      = "tracks_temp";
    private static final String TRACK_URI             = "track_uri";
    public static final String ACTION_STOP            = "com.nextgis.maplibui.TRACK_STOP";
    private static final String ACTION_SPLIT          = "com.nextgis.maplibui.TRACK_SPLIT";
    private static final int    TRACK_NOTIFICATION_ID = 1;
    public static final String URL = "http://track.nextgis.com/ng-mobile";

    private boolean         mIsRunning;
    private LocationManager mLocationManager;
    private Thread          mLocationSenderThread;

    private SharedPreferences mSharedPreferencesTemp;
    private String            mTrackId;
    private Uri mContentUriTracks, mContentUriTrackPoints;
    private ContentValues mValues;
    private GeoPoint mPoint;

    private NotificationManager mNotificationManager;
    private AlarmManager        mAlarmManager;
    private PendingIntent       mSplitService, mOpenActivity;
    private String mTicker;
    private int    mSmallIcon, mSatellitesCount;
    private boolean             mHasGPSFix;
    private long                mLastSync = 0;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mHasGPSFix = false;

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        IGISApplication application = (IGISApplication) getApplication();
        String authority = application.getAuthority();
        mContentUriTracks = Uri.parse("content://" + authority + "/" + TrackLayer.TABLE_TRACKS);
        mContentUriTrackPoints =
                Uri.parse("content://" + authority + "/" + TrackLayer.TABLE_TRACKPOINTS);

        mPoint = new GeoPoint();
        mValues = new ContentValues();

        SharedPreferences sharedPreferences =
                getSharedPreferences(getPackageName() + "_preferences", Constants.MODE_MULTI_PROCESS);
        mSharedPreferencesTemp = getSharedPreferences(TEMP_PREFERENCES, MODE_PRIVATE);

        String minTimeStr =
                sharedPreferences.getString(SettingsConstants.KEY_PREF_TRACKS_MIN_TIME, "2");
        String minDistanceStr =
                sharedPreferences.getString(SettingsConstants.KEY_PREF_TRACKS_MIN_DISTANCE, "10");
        long minTime = Long.parseLong(minTimeStr) * 1000;
        float minDistance = Float.parseFloat(minDistanceStr);

        mTicker = getString(R.string.tracks_running);
        mSmallIcon = R.drawable.ic_action_maps_directions_walk;

        Intent intentSplit = new Intent(this, TrackerService.class);
        intentSplit.setAction(ACTION_SPLIT);
        mSplitService =
                PendingIntent.getService(this, 0, intentSplit, PendingIntent.FLAG_UPDATE_CURRENT);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if(!PermissionUtil.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                || !PermissionUtil.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION))
            return;

        mLocationManager.addGpsStatusListener(this);

        String provider = LocationManager.GPS_PROVIDER;
        if (mLocationManager.getAllProviders().contains(provider)) {
            mLocationManager.requestLocationUpdates(provider, minTime, minDistance, this);

            if(Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "Tracker service request location updates for " + provider);
        }

        provider = LocationManager.NETWORK_PROVIDER;
        if (mLocationManager.getAllProviders().contains(provider)) {
            mLocationManager.requestLocationUpdates(provider, minTime, minDistance, this);

            if(Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "Tracker service request location updates for " + provider);
        }

        NotificationHelper.showLocationInfo(this);

        mLocationSenderThread = createLocationSenderThread();
        mLocationSenderThread.start();
    }


    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId)
    {

        String targetActivity = "";

        if (intent != null) {
            targetActivity = intent.getStringExtra(ConstantsUI.TARGET_CLASS);
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_STOP:
                        stopSelf();
                        return START_NOT_STICKY;
                    case ACTION_SPLIT:
                        stopTrack();
                        startTrack();
                        addNotification();
                        return START_STICKY;
                }
            }
        }

        if (!mIsRunning) {
            // there are no tracks or last track correctly ended
            if (mSharedPreferencesTemp.getString(TRACK_URI, null) == null) {
                startTrack();
                mSharedPreferencesTemp.edit().putString(ConstantsUI.TARGET_CLASS, targetActivity).apply();
            } else {
                // looks like service was killed, restore data
                restoreData();
                targetActivity = mSharedPreferencesTemp.getString(ConstantsUI.TARGET_CLASS, "");
            }

            initTargetIntent(targetActivity);
            addNotification();
        }

        return START_STICKY;
    }


    private void restoreData()
    {
        Uri mNewTrack = Uri.parse(mSharedPreferencesTemp.getString(TRACK_URI, ""));
        mTrackId = mNewTrack.getLastPathSegment();
        mIsRunning = true;
        addSplitter();
    }


    private void startTrack()
    {
        // get track name date unique appendix
        final SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss", Locale.getDefault());

        // insert DB row
        final long started = System.currentTimeMillis();
        String mTrackName = simpleDateFormat.format(started);
        mValues.clear();
        mValues.put(TrackLayer.FIELD_NAME, mTrackName);
        mValues.put(TrackLayer.FIELD_START, started);
        mValues.put(TrackLayer.FIELD_VISIBLE, true);
        Uri newTrack = getContentResolver().insert(mContentUriTracks, mValues);
        if(null != newTrack) {
            // save vars
            mTrackId = newTrack.getLastPathSegment();
            mSharedPreferencesTemp.edit().putString(TRACK_URI, newTrack.toString()).apply();
        }

        mIsRunning = true;
        addSplitter();
    }


    private void stopTrack()
    {
        // update unclosed tracks in DB
        closeTracks(this, (IGISApplication) getApplication());
        sync((IGISApplication) getApplication());

        mIsRunning = false;

        // cancel midnight splitter
        mAlarmManager.cancel(mSplitService);

        mSharedPreferencesTemp.edit().remove(ConstantsUI.TARGET_CLASS).apply();
        mSharedPreferencesTemp.edit().remove(TRACK_URI).apply();
    }


    public static void closeTracks(Context context, IGISApplication app) {
        ContentValues cv = new ContentValues();
        cv.put(TrackLayer.FIELD_END, System.currentTimeMillis());
        String selection = TrackLayer.FIELD_END + " IS NULL OR " + TrackLayer.FIELD_END + " = ''";
        Uri tracksUri = Uri.parse("content://" + app.getAuthority() + "/" + TrackLayer.TABLE_TRACKS);
        context.getContentResolver().update(tracksUri, cv, selection, null);
    }


    private void addSplitter() {
        // set midnight track splitter
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        today.add(Calendar.DATE, 1);
        mAlarmManager.set(AlarmManager.RTC, today.getTimeInMillis(), mSplitService);
    }


    private void addNotification()
    {
        String name = "";
        String selection = TrackLayer.FIELD_ID + " = ?";
        Cursor currentTrack = getContentResolver().query(mContentUriTracks, new String[]{TrackLayer.FIELD_NAME}, selection, new String[]{mTrackId}, null);
        if (null != currentTrack) {
            if (currentTrack.moveToFirst())
                name = currentTrack.getString(0);
            currentTrack.close();
        }

        String title = String.format(getString(R.string.tracks_title), name);
        Bitmap largeIcon = NotificationHelper.getLargeIcon(
                R.drawable.ic_action_maps_directions_walk, getResources());

        Intent intentStop = new Intent(this, TrackerService.class);
        intentStop.setAction(ACTION_STOP);
        PendingIntent stopService = PendingIntent.getService(
                this, 0, intentStop, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentIntent(mOpenActivity)
                .setSmallIcon(mSmallIcon)
                .setLargeIcon(largeIcon)
                .setTicker(mTicker)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setContentTitle(title)
                .setContentText(mTicker)
                .setOngoing(true);

        builder.addAction(
                R.drawable.ic_location, getString(R.string.tracks_open),
                mOpenActivity);
        builder.addAction(
                R.drawable.ic_action_cancel_dark, getString(R.string.tracks_stop),
                stopService);

        mNotificationManager.notify(TRACK_NOTIFICATION_ID, builder.build());
        startForeground(TRACK_NOTIFICATION_ID, builder.build());

        Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
    }


    private void removeNotification()
    {
        mNotificationManager.cancel(TRACK_NOTIFICATION_ID);
    }


    // intent to open on notification click
    private void initTargetIntent(String targetActivity)
    {
        Intent intentActivity = new Intent();

        if (!TextUtils.isEmpty(targetActivity)) {
            Class<?> targetClass = null;

            try {
                targetClass = Class.forName(targetActivity);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (targetClass != null) {
                intentActivity = new Intent(this, targetClass);
            }
        }

        intentActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mOpenActivity = PendingIntent.getActivity(
                this, 0, intentActivity, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public void onDestroy()
    {
        stopTrack();
        removeNotification();
        stopSelf();

        if(PermissionUtil.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                && PermissionUtil.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            mLocationManager.removeUpdates(this);
            mLocationManager.removeGpsStatusListener(this);
        }

        if (mLocationSenderThread != null) {
            mLocationSenderThread.interrupt();
        }

        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    @Override
    public void onLocationChanged(Location location)
    {
        boolean update = LocationUtil.isProviderEnabled(this, location.getProvider(), true);
        if (!mIsRunning || !update)
            return;

        if(mHasGPSFix && !location.getProvider().equals(LocationManager.GPS_PROVIDER))
            return;

        String fixType = location.hasAltitude() ? "3d" : "2d";

        mValues.clear();
        mValues.put(TrackLayer.FIELD_SESSION, mTrackId);

        mPoint.setCoordinates(location.getLongitude(), location.getLatitude());
        mPoint.setCRS(GeoConstants.CRS_WGS84);
        mPoint.project(GeoConstants.CRS_WEB_MERCATOR);
        mValues.put(TrackLayer.FIELD_LON, mPoint.getX());
        mValues.put(TrackLayer.FIELD_LAT, mPoint.getY());
        mValues.put(TrackLayer.FIELD_ELE, location.getAltitude());
        mValues.put(TrackLayer.FIELD_FIX, fixType);
        mValues.put(TrackLayer.FIELD_SAT, mSatellitesCount);
        mValues.put(TrackLayer.FIELD_SPEED, location.getSpeed());
        mValues.put(TrackLayer.FIELD_ACCURACY, location.getAccuracy());
        mValues.put(TrackLayer.FIELD_SENT, 0);
        mValues.put(TrackLayer.FIELD_TIMESTAMP, location.getTime());
        getContentResolver().insert(mContentUriTrackPoints, mValues);

        IGISApplication app = (IGISApplication) getApplication();
        mValues.remove(TrackLayer.FIELD_LAT);
        mValues.remove(TrackLayer.FIELD_LON);
        try {
            mValues.put(FIELD_GEOM, mPoint.toBlob());
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<ILayer> tracks = getTrackLayers();
        for (ILayer layer : tracks) {
            Uri uri = Uri.parse("content://" + app.getAuthority() + "/" + layer.getPath().getName());
            getContentResolver().insert(uri, mValues);
        }
    }

    private List<ILayer> getTrackLayers() {
        MapBase map = MapBase.getInstance();
        List<ILayer> tracks = new ArrayList<>();
        LayerGroup.getLayersByType(map, LAYERTYPE_NGW_TRACKS, tracks);
        return tracks;
    }

    private void sync(IGISApplication app) {
        if (System.currentTimeMillis() - mLastSync > ONE_MINUTE) {
            List<ILayer> tracks = getTrackLayers();
            for (ILayer layer : tracks) {
                if (layer instanceof INGWLayer) {
                    INGWLayer ngwLayer = (INGWLayer) layer;
                    Pair<Integer, Integer> ver = NGWUtil.getNgwVersion(getApplicationContext(), ngwLayer.getAccountName());
                    ngwLayer.sync(app.getAuthority(), ver, new SyncResult());
                }
            }
            mLastSync = System.currentTimeMillis();
        }
    }

    @Override
    public void onStatusChanged(
            String provider,
            int status,
            Bundle extras)
    {

    }


    @Override
    public void onProviderEnabled(String provider)
    {

    }


    @Override
    public void onProviderDisabled(String provider)
    {

    }


    @Override
    public void onGpsStatusChanged(int event)
    {
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
            case GpsStatus.GPS_EVENT_STOPPED:
                mHasGPSFix = false;
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                mHasGPSFix = true;
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                mSatellitesCount = 0;

                for (GpsSatellite sat : mLocationManager.getGpsStatus(null).getSatellites()) {
                    if (sat.usedInFix()) {
                        mSatellitesCount++;
                    }
                }
                break;
        }
    }


    public static boolean hasUnfinishedTracks(Context context) {
        IGISApplication app = (IGISApplication) context.getApplicationContext();
        Uri tracksUri = Uri.parse("content://" + app.getAuthority() + "/" + TrackLayer.TABLE_TRACKS);
        String selection = TrackLayer.FIELD_END + " IS NULL OR " + TrackLayer.FIELD_END + " = ''";
        Cursor data = context.getContentResolver().query(tracksUri, new String[]{TrackLayer.FIELD_ID}, selection, null, null);
        boolean hasUnfinishedTracks = false;
        if(data != null) {
            hasUnfinishedTracks = data.moveToFirst();
            data.close();
        }
        return hasUnfinishedTracks;
    }


    public static boolean isTrackerServiceRunning(Context context) {
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (TrackerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    private Thread createLocationSenderThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferences = getSharedPreferences(getPackageName() + "_preferences", Constants.MODE_MULTI_PROCESS);
                String minTimeStr = preferences.getString(SettingsConstants.KEY_PREF_TRACKS_MIN_TIME, "2");
                long minTime = Long.parseLong(minTimeStr) * 1000;

                IGISApplication app = (IGISApplication) getApplication();
                ContentResolver resolver = getContentResolver();
                String selection = TrackLayer.FIELD_SENT + " = 0";
                String sort = TrackLayer.FIELD_TIMESTAMP + " ASC";
                Cursor mPoints;

                while (!Thread.currentThread().isInterrupted()) {
                    if (preferences.getBoolean(SettingsConstants.KEY_PREF_TRACK_SEND, false)) {
                        mPoints = resolver.query(mContentUriTrackPoints, null, selection, null, sort);
                        if (mPoints != null) {
                            List<String> ids = new ArrayList<>();
                            if (mPoints.moveToFirst()) {
                                GeoPoint point = new GeoPoint();
                                int lon = mPoints.getColumnIndex(TrackLayer.FIELD_LON);
                                int lat = mPoints.getColumnIndex(TrackLayer.FIELD_LAT);
                                int ele = mPoints.getColumnIndex(TrackLayer.FIELD_ELE);
                                int fix = mPoints.getColumnIndex(TrackLayer.FIELD_FIX);
                                int sat = mPoints.getColumnIndex(TrackLayer.FIELD_SAT);
                                int acc = mPoints.getColumnIndex(TrackLayer.FIELD_ACCURACY);
                                int speed = mPoints.getColumnIndex(TrackLayer.FIELD_SPEED);
                                int time = mPoints.getColumnIndex(TrackLayer.FIELD_TIMESTAMP);
                                JSONArray payload = new JSONArray();

                                int counter = 0;
                                do {
                                    JSONObject item = new JSONObject();
                                    try {
                                        point.setCoordinates(mPoints.getDouble(lon), mPoints.getDouble(lat));
                                        point.setCRS(GeoConstants.CRS_WEB_MERCATOR);
                                        point.project(GeoConstants.CRS_WGS84);
                                        item.put("lt", point.getY());
                                        item.put("ln", point.getX());
                                        item.put("ts", mPoints.getLong(time));
                                        item.put("a", mPoints.getDouble(ele));
                                        item.put("s", mPoints.getInt(sat));
                                        item.put("ft", mPoints.getString(fix).equals("3d") ? 3 : 2);
                                        item.put("sp", mPoints.getDouble(speed));
                                        item.put("ha", mPoints.getDouble(acc));
                                        payload.put(item);
                                        ids.add(mPoints.getString(time));
                                        counter++;

                                        if (counter >= 100) {
                                            post(payload.toString(), TrackerService.this, ids);
                                            payload = new JSONArray();
                                            ids.clear();
                                            counter = 0;
                                        }
                                    } catch (Exception ignored) { }
                                } while (mPoints.moveToNext());

                                if (counter > 0) {
                                    try {
                                        post(payload.toString(), TrackerService.this, ids);
                                    } catch (Exception ignored) { }
                                }
                            }
                            mPoints.close();
                        }
                    }

                    sync(app);

                    try {
                        Thread.sleep(minTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
    }

    private void post(String payload, Context context, List<String> ids) throws IOException {
        String url = String.format("%s/%s/packet", URL, getUid(context));
        NetworkUtil.post(url, payload, null, null, false);
        ContentValues cv = new ContentValues();
        cv.put(TrackLayer.FIELD_SENT, 1);
        String where = TrackLayer.FIELD_TIMESTAMP + " in (" + MapUtil.makePlaceholders(ids.size()) + ")";
        String[] timestamps = ids.toArray(new String[0]);
        context.getContentResolver().update(mContentUriTrackPoints, cv, where, timestamps);
    }

    @SuppressLint("HardwareIds")
    public static String getUid(Context context) {
        String uuid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return String.format("%X", uuid.hashCode());
    }
}

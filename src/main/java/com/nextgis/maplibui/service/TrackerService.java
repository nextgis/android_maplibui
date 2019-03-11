/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2019 NextGIS, info@nextgis.com
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

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplib.util.MapUtil;
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

import static com.nextgis.maplibui.util.NotificationHelper.createBuilder;

@SuppressLint("MissingPermission")
public class TrackerService extends Service implements LocationListener, GpsStatus.Listener {
    public static final  String TEMP_PREFERENCES      = "tracks_temp";
    private static final String TRACK_URI             = "track_uri";
    public static final String ACTION_SYNC            = "com.nextgis.maplibui.TRACK_SYNC";
    public static final String ACTION_STOP            = "com.nextgis.maplibui.TRACK_STOP";
    private static final String ACTION_SPLIT          = "com.nextgis.maplibui.TRACK_SPLIT";
    private static final int    TRACK_NOTIFICATION_ID = 1;
    public static final String SCHEME = "http";
//    public static final String HOST = "dev.nextgis.com/tracker-dev1-hub";
    public static final String HOST = "track.nextgis.com";
    public static final String URL = SCHEME + "://" + HOST + "/ng-mobile";

    private boolean         mIsRunning;
    private LocationManager mLocationManager;
    private Thread          mLocationSenderThread;

    private SharedPreferences mSharedPreferencesTemp;
    private SharedPreferences mSharedPreferences;
    private String            mTrackId;
    private Uri mContentUriTracks, mContentUriTrackPoints;
    private ContentValues mValues;
    private GeoPoint mPoint;

    private NotificationManager mNotificationManager;
    private AlarmManager        mAlarmManager;
    private PendingIntent       mSplitService, mOpenActivity;
    private String              mTicker;
    private int                 mSmallIcon, mSatellitesCount;
    private Bitmap              mLargeIcon;
    private boolean             mHasGPSFix;

    @Override
    public void onCreate() {
        super.onCreate();

        mHasGPSFix = false;

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        IGISApplication application = (IGISApplication) getApplication();
        String authority = application.getAuthority();
        String tracks = TrackLayer.TABLE_TRACKS;
        mContentUriTracks = Uri.parse("content://" + authority + "/" + tracks);
        String points = TrackLayer.TABLE_TRACKPOINTS;
        mContentUriTrackPoints = Uri.parse("content://" + authority + "/" + points);

        mPoint = new GeoPoint();
        mValues = new ContentValues();

        String name = getPackageName() + "_preferences";
        mSharedPreferences = getSharedPreferences(name, MODE_MULTI_PROCESS);
        mSharedPreferencesTemp = getSharedPreferences(TEMP_PREFERENCES, MODE_PRIVATE);

        mTicker = getString(R.string.tracks_running);
        mSmallIcon = R.drawable.ic_action_maps_directions_walk;
        mLargeIcon = NotificationHelper.getLargeIcon(mSmallIcon, getResources());

        Intent intentSplit = new Intent(this, TrackerService.class);
        intentSplit.setAction(ACTION_SPLIT);
        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        mSplitService = PendingIntent.getService(this, 0, intentSplit, flag);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String targetActivity = "";

        if (intent != null) {
            targetActivity = intent.getStringExtra(ConstantsUI.TARGET_CLASS);
            String action = intent.getAction();

            if (action != null && !TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_SYNC:
                        if (mIsRunning || mLocationSenderThread != null)
                            return START_STICKY;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            int res = R.string.sync_started;
                            String title = getString(res);
                            NotificationCompat.Builder builder = createBuilder(this, res);
                            builder.setSmallIcon(mSmallIcon)
                                    .setLargeIcon(mLargeIcon)
                                    .setTicker(title)
                                    .setWhen(System.currentTimeMillis())
                                    .setAutoCancel(false)
                                    .setContentTitle(title)
                                    .setContentText(title)
                                    .setOngoing(true);

                            startForeground(TRACK_NOTIFICATION_ID, builder.build());
                        }

                        mLocationSenderThread = createLocationSenderThread(500L);
                        mLocationSenderThread.start();
                        return START_NOT_STICKY;
                    case ACTION_STOP:
                        removeNotification();
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
            if (!PermissionUtil.hasLocationPermissions(this)) {
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }

            mLocationManager.addGpsStatusListener(this);

            String time = SettingsConstants.KEY_PREF_TRACKS_MIN_TIME;
            String distance = SettingsConstants.KEY_PREF_TRACKS_MIN_DISTANCE;
            String minTimeStr = mSharedPreferences.getString(time, "2");
            String minDistanceStr = mSharedPreferences.getString(distance, "10");
            long minTime = Long.parseLong(minTimeStr) * 1000;
            float minDistance = Float.parseFloat(minDistanceStr);

            String provider = LocationManager.GPS_PROVIDER;
            if (mLocationManager.getAllProviders().contains(provider)) {
                mLocationManager.requestLocationUpdates(provider, minTime, minDistance, this);

                if (Constants.DEBUG_MODE)
                    Log.d(Constants.TAG, "Tracker service request location updates for " + provider);
            }

            provider = LocationManager.NETWORK_PROVIDER;
            if (mLocationManager.getAllProviders().contains(provider)) {
                mLocationManager.requestLocationUpdates(provider, minTime, minDistance, this);

                if (Constants.DEBUG_MODE)
                    Log.d(Constants.TAG, "Tracker service request location updates for " + provider);
            }

            NotificationHelper.showLocationInfo(this);

            // there are no tracks or last track correctly ended
            if (mSharedPreferencesTemp.getString(TRACK_URI, null) == null) {
                startTrack();
                mSharedPreferencesTemp.edit().putString(ConstantsUI.TARGET_CLASS, targetActivity).apply();
            } else {
                // looks like service was killed, restore data
                restoreData();
                targetActivity = mSharedPreferencesTemp.getString(ConstantsUI.TARGET_CLASS, "");
            }

            mLocationSenderThread = createLocationSenderThread(minTime);
            mLocationSenderThread.start();

            initTargetIntent(targetActivity);
            addNotification();
        }

        return START_STICKY;
    }


    private void restoreData() {
        Uri mNewTrack = Uri.parse(mSharedPreferencesTemp.getString(TRACK_URI, ""));
        mTrackId = mNewTrack.getLastPathSegment();
        mIsRunning = true;
        addSplitter();
    }


    private void startTrack() {
        // get track name date unique appendix
        String pattern = "yyyy-MM-dd--HH-mm-ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.getDefault());

        // insert DB row
        final long started = System.currentTimeMillis();
        String mTrackName = simpleDateFormat.format(started);
        mValues.clear();
        mValues.put(TrackLayer.FIELD_NAME, mTrackName);
        mValues.put(TrackLayer.FIELD_START, started);
        mValues.put(TrackLayer.FIELD_VISIBLE, true);
        try {
            Uri newTrack = getContentResolver().insert(mContentUriTracks, mValues);
            if (null != newTrack) {
                // save vars
                mTrackId = newTrack.getLastPathSegment();
                mSharedPreferencesTemp.edit().putString(TRACK_URI, newTrack.toString()).apply();
            }

            mIsRunning = true;
            addSplitter();
        } catch (SQLiteException ignored) {
        }
    }


    private void stopTrack() {
        // update unclosed tracks in DB
        closeTracks(this, (IGISApplication) getApplication());

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
        try {
            context.getContentResolver().update(tracksUri, cv, selection, null);
        } catch (IllegalArgumentException | SQLiteException ignore) {
        }
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


    private void addNotification() {
        String name = "";
        String selection = TrackLayer.FIELD_ID + " = ?";
        String[] proj = new String[]{TrackLayer.FIELD_NAME};
        String[] args = new String[]{mTrackId};
        try {
            Cursor currentTrack = getContentResolver().query(mContentUriTracks, proj, selection, args, null);
            if (null != currentTrack) {
                if (currentTrack.moveToFirst())
                    name = currentTrack.getString(0);
                currentTrack.close();
            }
        } catch (SQLiteException ignored){
        }

        String title = String.format(getString(R.string.tracks_title), name);
        Intent intentStop = new Intent(this, TrackerService.class);
        intentStop.setAction(ACTION_STOP);
        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent stopService = PendingIntent.getService(this, 0, intentStop, flag);

        NotificationCompat.Builder builder = createBuilder(this, R.string.title_edit_by_walk);
        builder.setContentIntent(mOpenActivity)
                .setSmallIcon(mSmallIcon)
                .setLargeIcon(mLargeIcon)
                .setTicker(mTicker)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setContentTitle(title)
                .setContentText(mTicker)
                .setOngoing(true);

        int resource = R.drawable.ic_location;
        builder.addAction(resource, getString(R.string.tracks_open), mOpenActivity);
        resource = R.drawable.ic_action_cancel_dark;
        builder.addAction(resource, getString(R.string.tracks_stop), stopService);

        mNotificationManager.notify(TRACK_NOTIFICATION_ID, builder.build());
        startForeground(TRACK_NOTIFICATION_ID, builder.build());
        Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
    }


    private void removeNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            stopForeground(true);
        else
            mNotificationManager.cancel(TRACK_NOTIFICATION_ID);
    }


    // intent to open on notification click
    private void initTargetIntent(String targetActivity) {
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
        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        mOpenActivity = PendingIntent.getActivity(this, 0, intentActivity, flag);
    }


    public void onDestroy() {
        stopTrack();
        stopSelf();

        if (PermissionUtil.hasLocationPermissions(this)) {
            mLocationManager.removeUpdates(this);
            mLocationManager.removeGpsStatusListener(this);
        }

        if (mLocationSenderThread != null)
            mLocationSenderThread.interrupt();

        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onLocationChanged(Location location) {
        boolean update = LocationUtil.isProviderEnabled(this, location.getProvider(), true);
        if (!mIsRunning || !update)
            return;

        if (mHasGPSFix && !location.getProvider().equals(LocationManager.GPS_PROVIDER))
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
        try {
            getContentResolver().insert(mContentUriTrackPoints, mValues);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }


    @Override
    public void onProviderEnabled(String provider) {
    }


    @Override
    public void onProviderDisabled(String provider) {
    }


    @Override
    public void onGpsStatusChanged(int event) {
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
        String[] projection = new String[]{TrackLayer.FIELD_ID};
        boolean hasUnfinishedTracks = false;
        try {
            Cursor data = context.getContentResolver().query(tracksUri, projection, selection, null, null);
            if (data != null) {
                hasUnfinishedTracks = data.moveToFirst();
                data.close();
            }
        } catch (SQLiteException ignored) {}
        return hasUnfinishedTracks;
    }


    public static boolean isTrackerServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (TrackerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    private Thread createLocationSenderThread(final Long delay) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
//                Log.d(Constants.TAG, "Entering sync thread");
                while (!Thread.currentThread().isInterrupted()) {
                    try {
//                        Log.d(Constants.TAG, "Sleep sync thread");
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    try {
                        sync();
                    } catch (SQLiteException ignored) {
                    }

                    if (!mIsRunning) {
                        removeNotification();
                        stopSelf();
                    }
                }
            }
        });
    }

    private void sync() throws SQLiteException {
//        Log.d(Constants.TAG, "Syncing trackpoints");
        if (mSharedPreferences.getBoolean(SettingsConstants.KEY_PREF_TRACK_SEND, false)) {
            ContentResolver resolver = getContentResolver();
            String selection = TrackLayer.FIELD_SENT + " = 0";
            String sort = TrackLayer.FIELD_TIMESTAMP + " ASC";
            Cursor points = resolver.query(mContentUriTrackPoints, null, selection, null, sort);
            if (points != null) {
                List<String> ids = new ArrayList<>();
                if (points.moveToFirst()) {
                    GeoPoint point = new GeoPoint();
                    int lon = points.getColumnIndex(TrackLayer.FIELD_LON);
                    int lat = points.getColumnIndex(TrackLayer.FIELD_LAT);
                    int ele = points.getColumnIndex(TrackLayer.FIELD_ELE);
                    int fix = points.getColumnIndex(TrackLayer.FIELD_FIX);
                    int sat = points.getColumnIndex(TrackLayer.FIELD_SAT);
                    int acc = points.getColumnIndex(TrackLayer.FIELD_ACCURACY);
                    int speed = points.getColumnIndex(TrackLayer.FIELD_SPEED);
                    int time = points.getColumnIndex(TrackLayer.FIELD_TIMESTAMP);
                    JSONArray payload = new JSONArray();

                    int counter = 0;
                    do {
                        JSONObject item = new JSONObject();
                        try {
                            point.setCoordinates(points.getDouble(lon), points.getDouble(lat));
                            point.setCRS(GeoConstants.CRS_WEB_MERCATOR);
                            point.project(GeoConstants.CRS_WGS84);
                            item.put("lt", point.getY());
                            item.put("ln", point.getX());
                            item.put("ts", points.getLong(time)/1000);
                            item.put("a", points.getDouble(ele));
                            item.put("s", points.getInt(sat));
                            item.put("ft", points.getString(fix).equals("3d") ? 3 : 2);
                            item.put("sp", points.getDouble(speed) * 18 / 5);
                            item.put("ha", points.getDouble(acc));
                            payload.put(item);
                            ids.add(points.getString(time));
                            counter++;

                            if (counter >= 100) {
                                post(payload.toString(), this, ids);
                                payload = new JSONArray();
                                ids.clear();
                                counter = 0;
                            }
                        } catch (Exception ignored) { }
                    } while (points.moveToNext());

                    if (counter > 0) {
                        try {
                            post(payload.toString(), this, ids);
                        } catch (Exception ignored) { }
                    }
                }
                points.close();
            }
        }
    }

    private void post(String payload, Context context, List<String> ids) throws IOException {
        String url = String.format("%s/%s/packet", URL, getUid(context));
//        Log.d(Constants.TAG, "Post to " + url);
        HttpResponse response = NetworkUtil.post(url, payload, null, null, false);
//        Log.d(Constants.TAG, "Response is " + response.getResponseCode());
        if (!response.isOk())
            return;

        ContentValues cv = new ContentValues();
        cv.put(TrackLayer.FIELD_SENT, 1);
        String where = TrackLayer.FIELD_TIMESTAMP + " in (" + MapUtil.makePlaceholders(ids.size()) + ")";
        String[] timestamps = ids.toArray(new String[0]);
        try {
            context.getContentResolver().update(mContentUriTrackPoints, cv, where, timestamps);
        } catch (SQLiteException ignored) {
        }
    }

    @SuppressLint("HardwareIds")
    public static String getUid(Context context) {
        String uuid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return String.format("%X", uuid.hashCode());
    }
}

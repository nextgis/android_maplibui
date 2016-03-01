/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplib.util.PermissionUtil;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class TrackerService
        extends Service
        implements LocationListener, GpsStatus.Listener
{
    public static final  String TEMP_PREFERENCES      = "tracks_temp";
    private static final String TRACK_URI             = "track_uri";
    private static final String TRACK_DAILY_ID        = "track_daily_id";
    private static final String ACTION_STOP           = "com.nextgis.maplibui.TRACK_STOP";
    private static final String ACTION_SPLIT          = "com.nextgis.maplibui.TRACK_SPLIT";
    private static final int    TRACK_NOTIFICATION_ID = 1;

    private boolean         mIsRunning;
    private LocationManager mLocationManager;

    private SharedPreferences mSharedPreferencesTemp;
    private String            mTrackId;
    private Uri mContentUriTracks, mContentUriTrackPoints;
    private ContentValues mValues;

    private NotificationManager mNotificationManager;
    private AlarmManager        mAlarmManager;
    private PendingIntent       mSplitService, mOpenActivity;
    private String mTicker;
    private int    mSmallIcon, mSatellitesCount;
    private boolean             mHasGPSFix;

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
        if (LocationUtil.isProviderEnabled(this, provider, true) &&
            mLocationManager.getAllProviders().contains(provider)) {
            mLocationManager.requestLocationUpdates(provider, minTime, minDistance, this);

            if(Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "Tracker service request location updates for " + provider);
        }

        provider = LocationManager.NETWORK_PROVIDER;
        if (LocationUtil.isProviderEnabled(this, provider, true) &&
            mLocationManager.getAllProviders().contains(provider)) {
            mLocationManager.requestLocationUpdates(provider, minTime, minDistance, this);

            if(Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "Tracker service request location updates for " + provider);
        }
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
                mSharedPreferencesTemp.edit().putString(ConstantsUI.TARGET_CLASS, targetActivity).commit();
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
    }


    private void startTrack()
    {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // get track name date unique appendix
        final SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("yyyy-MM-dd-", Locale.getDefault());
        int append = getAppendix(today.getTimeInMillis());

        // insert DB row
        final long started = System.currentTimeMillis();
        String mTrackName = simpleDateFormat.format(started) + append;
        mValues.clear();
        mValues.put(TrackLayer.FIELD_NAME, mTrackName);
        mValues.put(TrackLayer.FIELD_START, started);
        mValues.put(TrackLayer.FIELD_VISIBLE, true);
        Uri newTrack = getContentResolver().insert(mContentUriTracks, mValues);
        if(null != newTrack) {
            // save vars
            mTrackId = newTrack.getLastPathSegment();
            mSharedPreferencesTemp.edit().putString(TRACK_URI, newTrack.toString()).commit();
        }
        mSharedPreferencesTemp.edit().putString(TRACK_DAILY_ID, today.getTimeInMillis() + "-" + append).commit();
        mIsRunning = true;

        // set midnight track splitter
        today.add(Calendar.DATE, 1);
        mAlarmManager.set(AlarmManager.RTC, today.getTimeInMillis(), mSplitService);
    }


    private void stopTrack()
    {
        // update unclosed tracks in DB
        closeTracks(this, (IGISApplication) getApplication());

        mIsRunning = false;

        // cancel midnight splitter
        mAlarmManager.cancel(mSplitService);

        mSharedPreferencesTemp.edit().remove(ConstantsUI.TARGET_CLASS).commit();
        mSharedPreferencesTemp.edit().remove(TRACK_URI).commit();
    }


    private int getAppendix(long today) {
        int result = 0;
        String previousAppendix = mSharedPreferencesTemp.getString(TRACK_DAILY_ID, "0-1");
        if (Long.parseLong(previousAppendix.split("-")[0]) >= today)
            result = Integer.parseInt(previousAppendix.split("-")[1]);

        return ++result;
    }


    public static void closeTracks(Context context, IGISApplication app) {
        ContentValues cv = new ContentValues();
        cv.put(TrackLayer.FIELD_END, System.currentTimeMillis());
        String selection = TrackLayer.FIELD_END + " IS NULL OR " + TrackLayer.FIELD_END + " = ''";
        Uri tracksUri = Uri.parse("content://" + app.getAuthority() + "/" + TrackLayer.TABLE_TRACKS);
        context.getContentResolver().update(tracksUri, cv, selection, null);
    }


    private void addNotification()
    {
        String name = "";

        String selection = TrackLayer.FIELD_ID + " = ?";
        Cursor currentTrack = getContentResolver().query(mContentUriTracks, new String[]{TrackLayer.FIELD_NAME}, selection, new String[]{mTrackId}, null);
        if(null != currentTrack) {
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
        if (!mIsRunning)
            return;

        if(mHasGPSFix && !location.getProvider().equals(LocationManager.GPS_PROVIDER))
            return;

        String fixType = location.hasAltitude() ? "3d" : "2d";

        mValues.clear();
        mValues.put(TrackLayer.FIELD_SESSION, mTrackId);
        mValues.put(TrackLayer.FIELD_LON, location.getLongitude());
        mValues.put(TrackLayer.FIELD_LAT, location.getLatitude());
        mValues.put(TrackLayer.FIELD_ELE, location.getAltitude());
        mValues.put(TrackLayer.FIELD_FIX, fixType);
        mValues.put(TrackLayer.FIELD_SAT, mSatellitesCount);
        mValues.put(TrackLayer.FIELD_TIMESTAMP, location.getTime());
        getContentResolver().insert(mContentUriTrackPoints, mValues);
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
        IGISApplication app = (IGISApplication) ((Activity) context).getApplication();
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

}

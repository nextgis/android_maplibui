/*
 *  Project:  NextGIS Mobile
 *  Purpose:  Mobile GIS for Android.
 *  Author:   Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *  Author:   Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 *  Copyright (c) 2012-2016 NextGIS, info@nextgis.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.service;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplib.util.PermissionUtil;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.NotificationHelper;

/**
 * Service to gather position data during walking
 */
public class WalkEditService
        extends Service
        implements LocationListener, GpsStatus.Listener{

    private static final int    WALK_NOTIFICATION_ID = 7;
    public static final String TEMP_PREFERENCES      = "walkedit_temp";
    public static final String ACTION_STOP           = "com.nextgis.maplibui.WALKEDIT_STOP";
    public static final String ACTION_START          = "com.nextgis.maplibui.WALKEDIT_START";
    public static final String WALKEDIT_CHANGE       = "com.nextgis.maplibui.WALKEDIT_CHANGE";

    private SharedPreferences mSharedPreferencesTemp;
    private LocationManager mLocationManager;
    private NotificationManager mNotificationManager;
    private String mTicker;
    private int    mSmallIcon;
    private PendingIntent mOpenActivity;

    protected String mTargetActivity;
    protected GeoGeometry mGeometry;
    protected int mLayerId;

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mSharedPreferencesTemp = getSharedPreferences(TEMP_PREFERENCES, Constants.MODE_MULTI_PROCESS);

        mTicker = getString(R.string.tracks_running);
        mSmallIcon = R.drawable.ic_action_maps_directions_walk;

        mLayerId = Constants.NOT_FOUND;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_STOP:
                        mGeometry = null;
                        mLayerId = Constants.NOT_FOUND;
                        stopSelf();
                        break;
                    case ACTION_START:
                        int layerId = intent.getIntExtra(ConstantsUI.KEY_LAYER_ID, Constants.NOT_FOUND);
                        if(mLayerId == layerId) { // we are already running track record
                            sendGeometryBroadcast();
                        } else {
                            mLayerId = layerId;
                            mGeometry = (GeoGeometry) intent.getSerializableExtra(ConstantsUI.KEY_GEOMETRY);
                            mTargetActivity = intent.getStringExtra(ConstantsUI.TARGET_CLASS);
                            startWalkEdit();

                            SharedPreferences.Editor edit = mSharedPreferencesTemp.edit();
                            edit.putInt(ConstantsUI.KEY_LAYER_ID, mLayerId);
                            edit.putString(ConstantsUI.KEY_GEOMETRY, mGeometry.toWKT(true));
                            edit.putString(ConstantsUI.TARGET_CLASS, mTargetActivity);
                            edit.commit();
                        }
                        break;
                }
            }
        } else {
            mLayerId = mSharedPreferencesTemp.getInt(ConstantsUI.KEY_LAYER_ID, Constants.NOT_FOUND);
            mGeometry = GeoGeometryFactory.fromWKT(mSharedPreferencesTemp.getString(ConstantsUI.KEY_GEOMETRY, ""));
            mTargetActivity = mSharedPreferencesTemp.getString(ConstantsUI.TARGET_CLASS, "");
            startWalkEdit();
        }

        return START_STICKY;

    }

    private void startWalkEdit() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(getPackageName() + "_preferences", Constants.MODE_MULTI_PROCESS);

        String minTimeStr =
                sharedPreferences.getString(SettingsConstants.KEY_PREF_LOCATION_MIN_TIME, "2");
        String minDistanceStr =
                sharedPreferences.getString(SettingsConstants.KEY_PREF_LOCATION_MIN_DISTANCE, "10");
        long minTime = Long.parseLong(minTimeStr) * 1000;
        float minDistance = Float.parseFloat(minDistanceStr);

        if(!PermissionUtil.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                || !PermissionUtil.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION))
            return;

        mLocationManager.addGpsStatusListener(this);

        String provider = LocationManager.GPS_PROVIDER;
        if (mLocationManager.getAllProviders().contains(provider)) {
            mLocationManager.requestLocationUpdates(provider, minTime, minDistance, this);
        }

        provider = LocationManager.NETWORK_PROVIDER;
        if (mLocationManager.getAllProviders().contains(provider)) {
            mLocationManager.requestLocationUpdates(provider, minTime, minDistance, this);
        }

        NotificationHelper.showLocationInfo(this);
        initTargetIntent(mTargetActivity);
        addNotification();
    }

    private void sendGeometryBroadcast() {
        Intent broadcastIntent = new Intent(WALKEDIT_CHANGE);
        broadcastIntent.putExtra(ConstantsUI.KEY_GEOMETRY, mGeometry);
        sendBroadcast(broadcastIntent);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy() {
        mSharedPreferencesTemp.edit().clear().commit();
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
    public void onLocationChanged(Location location)
    {
        GeoPoint point;
        point = new GeoPoint(location.getLongitude(), location.getLatitude());
        point.setCRS(GeoConstants.CRS_WGS84);
        point.project(GeoConstants.CRS_WEB_MERCATOR);

        switch (mGeometry.getType()) {
            case GeoConstants.GTLineString:
                GeoLineString line = (GeoLineString) mGeometry;
                line.add(point);
                break;
            case GeoConstants.GTPolygon:
                GeoPolygon polygon = (GeoPolygon) mGeometry;
                polygon.add(point);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported geometry type");
        }

        SharedPreferences.Editor edit = mSharedPreferencesTemp.edit();
        edit.putString(ConstantsUI.KEY_GEOMETRY, mGeometry.toWKT(true)).commit();

        sendGeometryBroadcast();
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
    }


    private void addNotification()
    {
        MapBase map = MapBase.getInstance();
        ILayer layer = map.getLayerById(mLayerId);
        String name = "";
        if(null != layer)
            name = layer.getName();

        String title = String.format(getString(R.string.walkedit_title), name);
        Bitmap largeIcon = NotificationHelper.getLargeIcon(
                R.drawable.ic_action_maps_directions_walk, getResources());

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

        mNotificationManager.notify(WALK_NOTIFICATION_ID, builder.build());
        startForeground(WALK_NOTIFICATION_ID, builder.build());
    }


    private void removeNotification()
    {
        mNotificationManager.cancel(WALK_NOTIFICATION_ID);
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

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            if (WalkEditService.class.getName().equals(service.service.getClassName()))
                return true;

        return false;
    }
}

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016, 2018 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;

import com.nextgis.maplibui.BuildConfig;
import com.nextgis.maplibui.R;

public final class NotificationHelper {
    private static final int NOTIFICATION_GPS_ID = 695;

    public static Bitmap getLargeIcon(int iconResourceId, Resources resources) {
        Bitmap icon = BitmapFactory.decodeResource(resources, iconResourceId);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return icon;

        int iconSize = ControlHelper.dpToPx(40, resources);
        int innerIconSize = ControlHelper.dpToPx(24, resources);
        icon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, false);
        Bitmap largeIcon = icon.copy(Bitmap.Config.ARGB_8888, true);
        icon = Bitmap.createScaledBitmap(icon, innerIconSize, innerIconSize, false);

        Canvas canvas = new Canvas(largeIcon);
        int center = canvas.getHeight() / 2;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(resources.getColor(R.color.accent));
        canvas.drawCircle(center, center, center, paint);
        paint.setColor(Color.WHITE);
        canvas.drawBitmap(icon, center - icon.getWidth() / 2, center - icon.getWidth() / 2, paint);

        return largeIcon;
    }

    public static AlertDialog showLocationInfo(final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.getBoolean(SettingsConstantsUI.KEY_PREF_SHOW_GEO_DIALOG, true))
            return null;

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        final boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled || !isNetworkEnabled) {
            String title, info;

            if (!isGPSEnabled && !isNetworkEnabled) {
                title = context.getString(R.string.location_disabled);
                info = context.getString(R.string.location_disabled_msg);
            } else {
                String network = "", gps = "";

                if (!isNetworkEnabled)
                    network = "\r\n- " + context.getString(R.string.location_network);

                if(!isGPSEnabled)
                    gps = "\r\n- " + context.getString(R.string.location_gps);

                title = context.getString(R.string.location_accuracy);
                info = context.getString(R.string.location_inaccuracy) + network + gps;
            }

            if (context instanceof Activity)
                return showLocationDialog(context, title, info);
            else
                showLocationNotification(context, title, info);
        }

        return null;
    }

    public static NotificationCompat.Builder createBuilder(Context context, @StringRes int channelName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            return createBuilderO(context, channelName);
        else
            return new NotificationCompat.Builder(context);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static NotificationCompat.Builder createBuilderO(Context context, int channelNameRes) {
        String NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID;
        String channelName = context.getString(channelNameRes);
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.CYAN);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);
        return new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
    }

    private static AlertDialog showLocationDialog(final Context context, String title, String info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setMessage(info)
                .setPositiveButton(R.string.action_settings,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.do_not_ask_again,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                                editor.putBoolean(SettingsConstantsUI.KEY_PREF_SHOW_GEO_DIALOG, false).apply();
                            }
                        });
        builder.create();
        return builder.show();
    }

    private static void showLocationNotification(Context context, String title, String info) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent locationSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        PendingIntent intentNotify = PendingIntent.getActivity(context, 0, locationSettings, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = createBuilder(context, R.string.location_accuracy)
                .setContentIntent(intentNotify)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_location)
                .setContentTitle(title)
                .setContentText(info)
                .setTicker(title);

        manager.notify(NOTIFICATION_GPS_ID, builder.build());
    }
}

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016, 2019 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;

import com.nextgis.maplib.util.SettingsConstants;

public class BootLoader extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String boot = Intent.ACTION_BOOT_COMPLETED;
        if (intent != null && intent.getAction() != null && intent.getAction().equals(boot))
            checkTrackerService(context);
    }

    public static void checkTrackerService(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean restoreTrack = preferences.getBoolean(SettingsConstants.KEY_PREF_TRACK_RESTORE, false);

        if (TrackerService.hasUnfinishedTracks(context)) {
            Intent trackerService = new Intent(context, TrackerService.class);
            if (!restoreTrack)
                trackerService.setAction(TrackerService.ACTION_STOP);

            ContextCompat.startForegroundService(context, trackerService);
        }
    }
}
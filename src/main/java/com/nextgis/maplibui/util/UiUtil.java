/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2018, 2021 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.PopupMenu;
import android.widget.Toast;

import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplibui.R;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class UiUtil {

    // Thanks to https://stackoverflow.com/a/41562794
    public static void setForceShowIcon(PopupMenu popupMenu) {
        try {
            Field[] mFields = popupMenu.getClass().getDeclaredFields();
            for (Field field : mFields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> popupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method mMethods = popupHelper.getMethod("setForceShowIcon", boolean.class);
                    mMethods.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void notFound(Activity activity) {
        Toast.makeText(activity, R.string.no_activity_found, Toast.LENGTH_SHORT).show();
    }

    public static void share(File file, String mimeType, Activity activity) {
        Intent shareIntent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = activity.getPackageName() + FileUtil.AUTHORITY;
            Uri uri = FileProvider.getUriForFile(activity, authority, file);
            shareIntent = ShareCompat.IntentBuilder.from(activity)
                    .setStream(uri)
                    .setType(mimeType)
                    .getIntent()
                    .setAction(Intent.ACTION_SEND)
                    .setDataAndType(uri, mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            shareIntent = Intent.createChooser(shareIntent, activity.getString(R.string.menu_share));
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            shareIntent.setType(mimeType);
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
//        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisArray); // multiple data
        try {
            activity.startActivity(shareIntent);
        } catch (ActivityNotFoundException e) {
            notFound(activity);
        }
    }
}

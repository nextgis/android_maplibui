/*
 * Project:  NextGIS Collector
 * Purpose:  Light mobile GIS for collecting data
 * Author:   Stanisl av Petriakov, becomeglory@gmail.com
 * ********************************************************************
 * Copyright (c) 2021 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.util;

import static com.nextgis.maplib.map.TrackLayer.TABLE_TRACKPOINTS;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplibui.R;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Currency;
import java.util.List;

public class CheckDirSizeTask extends AsyncTask<Void, Long, Boolean> {
    private Activity mActivity;
    private List<VectorLayer> mLayers;
    private long mFree, mNeeded;
    boolean useTracksToCalc;

    public CheckDirSizeTask(Activity activity, List<VectorLayer> layers, boolean useTracksToCalc) {
        mActivity = activity;
        mLayers = layers;
        this.useTracksToCalc = useTracksToCalc;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        for (VectorLayer layer : mLayers) {
            mNeeded += layer.getFields().size() * layer.getCount() * 64; // 64 is a approximately length of each feature in geojson with 4-chars field values
            mNeeded += FileUtil.getDirectorySize(layer.getPath());
        }


        if (useTracksToCalc) {
            String authority = ((IGISApplication)mActivity.getApplication()).getAuthority();
            String  projection = null ;//arrayOf(TrackLayer.FIELD_ID, TrackLayer.FIELD_NAME, TrackLayer.FIELD_VISIBLE)
            String  selection = null; // TrackLayer.FIELD_ID + " = ?"
            Uri mContentUriTrackpoints = Uri.parse("content://" + authority + "/" + TABLE_TRACKPOINTS);


            Cursor  tracksPoints =  mActivity.getContentResolver().query(
                    mContentUriTrackpoints,null,
                    null,
                    null,
                    null);
            if (tracksPoints != null && tracksPoints.moveToFirst() ) {
                long count = tracksPoints.getCount();
                mNeeded += count * 200; // about 200 bytes per one point

            }

        }


        if (mLayers.size() > 1)
            mNeeded *= 2;

        // https://stackoverflow.com/a/64600129/2088273
        long total = 0, used = 0;
        File[] files = ContextCompat.getExternalFilesDirs(mActivity, null);
        if (files[0] == null) {
            // ROM
            total = getTotalStorageInfo(Environment.getDataDirectory().getPath());
            used = getUsedStorageInfo(Environment.getDataDirectory().getPath());
        } else {
            // External Storage (SD Card)
            if (files.length == 1) {
                total = getTotalStorageInfo(files[0].getPath());
                used = getUsedStorageInfo(files[0].getPath());
            }
        }
        mFree = total - used;
        return mFree >= mNeeded || total == 0; // last clause is needed if storage info fails
    }

    public static long getTotalStorageInfo(String path) {
        StatFs statFs = new StatFs(path);
        long t;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            t = statFs.getTotalBytes();
        } else {
            t = statFs.getBlockCount() * statFs.getBlockCount();
        }
        return t;    // remember to convert in GB,MB or KB.
    }

    public static long getUsedStorageInfo(String path) {
        StatFs statFs = new StatFs(path);
        long u;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            u = statFs.getTotalBytes() - statFs.getAvailableBytes();
        } else {
            u = statFs.getBlockCount() * statFs.getBlockSize() - statFs.getAvailableBlocks() * statFs.getBlockSize();
        }
        return u;  // remember to convert in GB,MB or KB.
    }

    public static String getFileSize(long size) {
        if (size <= 0)
            return "0";

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    protected void onPostExecute(Boolean enoughSpace) {
        super.onPostExecute(enoughSpace);
        if (enoughSpace != Boolean.valueOf(true)) {
            String free = getFileSize(mFree);
            String needed = getFileSize(mNeeded);
            String message = mActivity.getString(R.string.free_space_message, needed, free);
            AlertDialog builder = new AlertDialog.Builder(mActivity).setTitle(R.string.not_enough_space).setMessage(message)
                    .setPositiveButton(R.string.ok, null).create();
            builder.show();
        }
    }
}

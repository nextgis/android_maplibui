/*
 * Project:  NextGIS Collector
 * Purpose:  Light mobile GIS for collecting data
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * ********************************************************************
 * Copyright (c) 2020-2021 NextGIS, info@nextgis.com
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

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.hypertrack.hyperlog.HyperLog;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplibui.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.nextgis.maplib.util.LayerUtil.normalizeLayerName;

public class ExportGeoJSONBatchTask extends ExportGeoJSONTask {
    private List<VectorLayer> mLayers;
    private String mName;

    public ExportGeoJSONBatchTask(Activity activity, List<VectorLayer> layers, boolean proceedAttaches, String name) {
        super(activity, null, proceedAttaches, false, false, null);
        mLayers = layers;
        mName = name;
    }

    @Override
    protected Object doInBackground(Void... voids) {
        Context context = null;
        HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: start doInBackground");
        for (ILayer layer : mLayers)
            if (layer != null) {
                HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: found context from layer " + layer.getName());
                context = layer.getContext();
                break;
            }

        if (context != null) {
            try {
                Boolean enoughSpace = null;
                try {
                    CheckDirSizeTask sizeTask = new CheckDirSizeTask(mActivity, mLayers);
                    enoughSpace = sizeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
                } catch (ExecutionException | InterruptedException ignored) {

                }

                if (enoughSpace != Boolean.valueOf(true))
                    return R.string.not_enough_space;

                if (mName.trim().isEmpty())
                    mName = Long.toString(System.currentTimeMillis());
                File temp = MapUtil.prepareTempDir(context, "exported_projects", false);
                String fileName = normalizeLayerName(mName) + ".zip";
                HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: result fileName is " + fileName);
                if (temp == null) {
                    HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: error creating file due to temp = null");
                    return R.string.error_file_create;
                }

                temp = new File(temp, fileName);
                temp.createNewFile();
                FileOutputStream fos = new FileOutputStream(temp, false);
                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

                HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: Temp file created, start zipping");
                if (mIsCanceled)
                    return R.string.canceled;

                byte[] buffer = new byte[1024];
                int length;

                ArrayList<String> existingFiles = new ArrayList<>();
                for (VectorLayer layer : mLayers) {
                    HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: process " + layer.getName());
                    try {
                        HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: executeOnExecutor ExportGeoJSONTask");
                        ExportGeoJSONTask exportTask = new ExportGeoJSONTask(mActivity, layer, mProceedAttaches, true, false, null);
                        Object result = exportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();

                        if (result instanceof File) {
                            File file = (File) result;
                            HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: result is File");
                            String name = file.getName();
                            name = getUniqueName(name, existingFiles, ZIP_EXT);
                            existingFiles.add(name);
                            FileInputStream fis = new FileInputStream(file);
                            zos.putNextEntry(new ZipEntry(name));

                            HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: put result to zip");
                            while ((length = fis.read(buffer)) > 0)
                                zos.write(buffer, 0, length);

                            HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: close entry");
                            zos.closeEntry();
                            fis.close();
                        } else {
                            HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: result is NOT File: " + result);
                            if (result == null)
                                result = R.string.error_export_geojson;
                            publishProgress((int) result);
                        }
                    } catch (ExecutionException | InterruptedException ex) {
                        HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: zip error: " + ex.getMessage());
                        publishProgress(R.string.sync_error_io);
                    }
                }

                HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: close streams and return a result");
                zos.close();
                fos.close();

                return temp;
            } catch (IOException e) {
                HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: error: " + e.getMessage());
                e.printStackTrace();
                return R.string.error_file_create;
            }
        }
        HyperLog.v(Constants.TAG, "ExportGeoJSONBatchTask: error creating file due to context = null");
        return R.string.error_file_create;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        Toast.makeText(mActivity, values[0], Toast.LENGTH_SHORT).show();
    }
}
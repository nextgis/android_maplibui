/*
 * Project:  NextGIS Collector
 * Purpose:  Light mobile GIS for collecting data
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * ********************************************************************
 * Copyright (c) 2020 NextGIS, info@nextgis.com
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

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplibui.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.nextgis.maplib.util.LayerUtil.normalizeLayerName;

public class ExportGeoJSONBatchTask extends ExportGeoJSONTask {
    private List<VectorLayer> mLayers;
    private String mName;

    public ExportGeoJSONBatchTask(Activity activity, List<VectorLayer> layers, boolean proceedAttaches, String name) {
        super(activity, null, proceedAttaches, false);
        mLayers = layers;
        mName = name;
    }

    @Override
    protected Object doInBackground(Void... voids) {
        Context context = null;
        for (ILayer layer : mLayers)
            if (layer != null)
                context = layer.getContext();

        if (context != null) {
            try {
                if (mName.trim().isEmpty())
                    mName = Long.toString(System.currentTimeMillis());
                File temp = MapUtil.prepareTempDir(context, "shared_layers");
                String fileName = normalizeLayerName(mName) + ".zip";
                if (temp == null)
                    return R.string.error_file_create;

                temp = new File(temp, fileName);
                temp.createNewFile();
                FileOutputStream fos = new FileOutputStream(temp, false);
                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

                if (mIsCanceled)
                    return R.string.canceled;

                byte[] buffer = new byte[1024];
                int length;

                for (VectorLayer layer : mLayers) {
                    try {
                        ExportGeoJSONTask exportTask = new ExportGeoJSONTask(mActivity, layer, mProceedAttaches, true);
                        Object result = exportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();

                        if (result instanceof File) {
                            FileInputStream fis = new FileInputStream((File) result);
                            zos.putNextEntry(new ZipEntry(((File) result).getName()));

                            while ((length = fis.read(buffer)) > 0)
                                zos.write(buffer, 0, length);

                            zos.closeEntry();
                            fis.close();
                        } else {
                            if (result == null)
                                result = R.string.error_file_create;
                            Toast.makeText(mActivity, (int) result, Toast.LENGTH_SHORT).show();
                        }
                    } catch (ExecutionException | InterruptedException ex) {
                        Toast.makeText(mActivity, R.string.error_file_create, Toast.LENGTH_SHORT).show();
                    }
                }

                zos.close();
                fos.close();

                return temp;
            } catch (IOException e) {
                e.printStackTrace();
                return R.string.error_file_create;
            }
        }
        return R.string.error_file_create;
    }
}
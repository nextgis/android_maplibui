/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2018-2021 NextGIS, info@nextgis.com
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

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import com.hypertrack.hyperlog.HyperLog;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.AttachItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoJSONUtil;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.PermissionUtil;
import com.nextgis.maplibui.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.nextgis.maplib.util.GeoConstants.FTDate;
import static com.nextgis.maplib.util.GeoConstants.FTDateTime;
import static com.nextgis.maplib.util.GeoConstants.FTTime;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_ATTACHES;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_CRS;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_CRS_EPSG_3857;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_GEOMETRY;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_NAME;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_PROPERTIES;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_FEATURES;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_Feature;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_FeatureCollection;
import static com.nextgis.maplib.util.LayerUtil.normalizeLayerName;

public class ExportGeoJSONTask extends AsyncTask<Void, Integer, Object> {
    protected static String ZIP_EXT = ".zip";
    Activity mActivity;
    private VectorLayer mLayer;
    private ProgressDialog mProgress;
    boolean mIsCanceled;
    boolean mProceedAttaches;
    private boolean mResultOnly;

    ExportGeoJSONTask(Activity activity, VectorLayer layer, boolean proceedAttaches, boolean resultOnly) {
        mActivity = activity;
        mLayer = layer;
        mProceedAttaches = proceedAttaches;
        mResultOnly = resultOnly;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mResultOnly)
            return;

        mProgress = new ProgressDialog(mActivity);
        mProgress.setTitle(R.string.export);
        mProgress.setMessage(mActivity.getString(R.string.preparing));
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mIsCanceled = true;
            }
        });
        mProgress.show();
        ControlHelper.lockScreenOrientation(mActivity);
    }

    @Override
    protected Object doInBackground(Void... voids) {
        HyperLog.v(Constants.TAG, "ExportGeoJSONTask: start doInBackground");
        try {
            if (!PermissionUtil.hasPermission(mLayer.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                HyperLog.v(Constants.TAG, "ExportGeoJSONTask: no write permission granted");
                return R.string.no_permission;
            }

            File temp = MapUtil.prepareTempDir(mLayer.getContext(), "shared_layers");
            String fileName = normalizeLayerName(mLayer.getName()) + ZIP_EXT;
            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: result fileName is " + fileName);
            if (temp == null) {
                HyperLog.v(Constants.TAG, "ExportGeoJSONTask: prepare temp directory is null");
                return R.string.error_file_create;
            }

            temp = new File(temp, fileName);
            temp.createNewFile();
            FileOutputStream fos = new FileOutputStream(temp, false);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: temp file created");
            JSONObject obj = new JSONObject();
            JSONArray geoJSONFeatures = new JSONArray();

            if (mIsCanceled)
                return R.string.canceled;

            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: create json with crs");
            JSONObject crs = new JSONObject();
            crs.put(GEOJSON_TYPE, GEOJSON_NAME);
            JSONObject crsName = new JSONObject();
            crsName.put(GEOJSON_NAME, GEOJSON_CRS_EPSG_3857);
            crs.put(GEOJSON_PROPERTIES, crsName);
            obj.put(GEOJSON_CRS, crs);
            obj.put(GEOJSON_TYPE, GEOJSON_TYPE_FeatureCollection);

            if (mIsCanceled)
                return R.string.canceled;

            Feature feature;
            byte[] buffer = new byte[1024];
            int length;

            Cursor featuresCursor = mLayer.query(null, null, null, null, null);
            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: cursor fetched from db");
            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: iterate over all features");
            if (featuresCursor != null && featuresCursor.moveToFirst()) {
                HyperLog.v(Constants.TAG, "ExportGeoJSONTask: move to first");
                do {
                    if (mIsCanceled)
                        break;

                    JSONObject featureJSON = new JSONObject();
                    featureJSON.put(GEOJSON_TYPE, GEOJSON_TYPE_Feature);

                    feature = mLayer.cursorToFeature(featuresCursor);
                    JSONObject properties = new JSONObject();
                    properties.put(Constants.FIELD_ID, feature.getId());
                    for (Field field : feature.getFields()) {
                        Object value = feature.getFieldValue(field.getName());
                        if (value != null) {
                            if (field.getType() == FTDateTime || field.getType() == FTDate || field.getType() == FTTime)
                                value = GeoJSONUtil.formatDateTime((Long) value, field.getType());
                        }

                        boolean isNaN = value instanceof Double && ((Double) value).isNaN();
                        properties.put(field.getName(), value == null || isNaN ? JSONObject.NULL : value);
                    }

                    if (mProceedAttaches) {
                        File attachFile, featureDir = new File(mLayer.getPath(), feature.getId() + "");
                        JSONArray attaches = new JSONArray();
                        for (Map.Entry<String, AttachItem> attach : feature.getAttachments().entrySet()) {
                            attachFile = new File(featureDir, attach.getKey());
                            String displayName = attach.getValue().getDisplayName();
                            if (TextUtils.isEmpty(displayName))
                                displayName = attach.getKey();
                            attaches.put(displayName);

                            if (attachFile.exists()) {
                                FileInputStream fis = new FileInputStream(attachFile);
                                zos.putNextEntry(new ZipEntry(feature.getId() + "/" + displayName));

                                while ((length = fis.read(buffer)) > 0)
                                    zos.write(buffer, 0, length);

                                zos.closeEntry();
                                fis.close();
                            }
                        }
                        properties.put(GEOJSON_ATTACHES, attaches);
                    }

                    featureJSON.put(GEOJSON_PROPERTIES, properties);
                    featureJSON.put(GEOJSON_GEOMETRY, feature.getGeometry().toJSON());
                    geoJSONFeatures.put(featureJSON);
                } while (featuresCursor.moveToNext());

                HyperLog.v(Constants.TAG, "ExportGeoJSONTask: close cursor");
                featuresCursor.close();
            } else {
                publishProgress();
            }

            if (mIsCanceled)
                return R.string.canceled;

            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: put features to json");
            obj.put(GEOJSON_TYPE_FEATURES, geoJSONFeatures);

            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: put json to zip");
            buffer = obj.toString().getBytes();
            zos.putNextEntry(new ZipEntry(mLayer.getName() + ".geojson"));
            zos.write(buffer);

            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: close entry and streams");
            zos.closeEntry();
            zos.close();
            fos.close();

            return temp;
        } catch (JSONException e) {
            e.printStackTrace();
            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: JSON error: " + e.getMessage());
            return R.string.error_export_geojson;
        } catch (IOException e) {
            e.printStackTrace();
            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: IOException: " + e.getMessage());
            return R.string.error_file_create;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        HyperLog.v(Constants.TAG, "ExportGeoJSONTask: layer has no features");
        Toast.makeText(mActivity, R.string.no_features, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPostExecute(Object result) {
        super.onPostExecute(result);
        HyperLog.v(Constants.TAG, "ExportGeoJSONTask: result is: " + result);
        if (mResultOnly)
            return;

        HyperLog.v(Constants.TAG, "ExportGeoJSONTask: unlock orientation and close dialog");
        ControlHelper.unlockScreenOrientation(mActivity);
        if (mProgress != null)
            mProgress.dismiss();

        if (mIsCanceled) {
            Toast.makeText(mActivity, R.string.canceled, Toast.LENGTH_SHORT).show();
            return;
        }

        if (result instanceof File) {
            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: show share dialog");
            share((File) result);
        } else {
            HyperLog.v(Constants.TAG, "ExportGeoJSONTask: error: result is null");
            if (result == null)
                result = R.string.error_file_create;
            Toast.makeText(mActivity, (int) result, Toast.LENGTH_SHORT).show();
        }
    }

    private void share(File path) {
        if (path == null || !path.exists()) {
            Toast.makeText(mActivity, R.string.error_create_feature, Toast.LENGTH_SHORT).show();
            return;
        }

        String type = "application/json,application/vnd.geo+json,application/zip";
        UiUtil.share(path, type, mActivity);
    }
}
/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2019 NextGIS, info@nextgis.com
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
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.widget.Toast;

import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.AttachItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoJSONUtil;
import com.nextgis.maplib.util.MapUtil;
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
import static com.nextgis.maplibui.util.LayerUtil.AUTHORITY;
import static com.nextgis.maplibui.util.LayerUtil.notFound;

public class ExportGeoJSONTask extends AsyncTask<Void, Void, File> {
    private Activity mActivity;
    private VectorLayer mLayer;
    private ProgressDialog mProgress;
    private boolean mIsCanceled;
    private boolean mProceedAttaches;

    ExportGeoJSONTask(Activity activity, VectorLayer layer, boolean proceedAttaches) {
        mActivity = activity;
        mLayer = layer;
        mProceedAttaches = proceedAttaches;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

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
    protected File doInBackground(Void... voids) {
        try {
            File temp = MapUtil.prepareTempDir(mLayer.getContext(), "shared_layers");
            String fileName = com.nextgis.maplib.util.LayerUtil.normalizeLayerName(mLayer.getName()) + ".zip";
            temp = new File(temp, fileName);
            FileOutputStream fos = new FileOutputStream(temp);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

            JSONObject obj = new JSONObject();
            JSONArray geoJSONFeatures = new JSONArray();
            Cursor featuresCursor = mLayer.query(null, null, null, null, null);

            if (mIsCanceled)
                return null;

            JSONObject crs = new JSONObject();
            crs.put(GEOJSON_TYPE, GEOJSON_NAME);
            JSONObject crsName = new JSONObject();
            crsName.put(GEOJSON_NAME, GEOJSON_CRS_EPSG_3857);
            crs.put(GEOJSON_PROPERTIES, crsName);
            obj.put(GEOJSON_CRS, crs);
            obj.put(GEOJSON_TYPE, GEOJSON_TYPE_FeatureCollection);

            Feature feature;
            byte[] buffer = new byte[1024];
            int length;

            if (featuresCursor != null && featuresCursor.moveToFirst()) {
                do {
                    if (mIsCanceled)
                        return null;

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

                        properties.put(field.getName(), value == null ? JSONObject.NULL : value);
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

                featuresCursor.close();
            } else {
                publishProgress();
                return null;
            }

            obj.put(GEOJSON_TYPE_FEATURES, geoJSONFeatures);

            buffer = obj.toString().getBytes();
            zos.putNextEntry(new ZipEntry(mLayer.getName() + ".geojson"));
            zos.write(buffer);
            zos.closeEntry();
            zos.close();

            return temp;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        Toast.makeText(mActivity, R.string.no_features, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPostExecute(File path) {
        super.onPostExecute(path);

        ControlHelper.unlockScreenOrientation(mActivity);
        if (mProgress != null)
            mProgress.dismiss();

        if (mIsCanceled && path == null) {
            Toast.makeText(mActivity, R.string.canceled, Toast.LENGTH_SHORT).show();
            return;
        }

        if (path == null || !path.exists()) {
            Toast.makeText(mActivity, R.string.error_create_feature, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent();
        String type = "application/json,application/vnd.geo+json,application/zip";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = mActivity.getPackageName() + AUTHORITY;
            Uri uri = FileProvider.getUriForFile(mActivity, authority, path);
            shareIntent = ShareCompat.IntentBuilder.from(mActivity)
                                                   .setStream(uri)
                                                   .setType(type)
                                                   .getIntent()
                                                   .setAction(Intent.ACTION_SEND)
                                                   .setDataAndType(uri, type)
                                                   .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            shareIntent = Intent.createChooser(shareIntent, mActivity.getString(R.string.menu_share));
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(path));
            shareIntent.setType(type);
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
//        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisArray); // multiple data
        try {
            mActivity.startActivity(shareIntent);
        } catch (ActivityNotFoundException e) {
            notFound(mActivity);
        }
    }
}
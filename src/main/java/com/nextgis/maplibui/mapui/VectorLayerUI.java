/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.mapui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.Toast;

import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.VectorCacheItem;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.FormBuilderModifyAttributesActivity;
import com.nextgis.maplibui.activity.ModifyAttributesActivity;
import com.nextgis.maplibui.activity.VectorLayerSettingsActivity;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.util.ConstantsUI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_CRS;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_GEOMETRY;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_NAME;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_PROPERTIES;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_FEATURES;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_Feature;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_FeatureCollection;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_FEATURE_ID;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_FORM_PATH;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_GEOMETRY;


/**
 * A UI for vector layer
 */
public class VectorLayerUI
        extends VectorLayer
        implements IVectorLayerUI
{
    private static final long MAX_INTERNAL_CACHE_SIZE = 1048576; // 1MB
    private static final long MAX_EXTERNAL_CACHE_SIZE = 5242880; // 5MB


    public VectorLayerUI(
            Context context,
            File path)
    {
        super(context, path);
    }


    @Override
    public Drawable getIcon(Context context)
    {
        return mContext.getResources().getDrawable(R.drawable.ic_local_vector);
    }


    @Override
    public void changeProperties(Context context)
    {
        Intent settings = new Intent(context, VectorLayerSettingsActivity.class);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settings.putExtra(ConstantsUI.KEY_LAYER_ID, getId());
        context.startActivity(settings);
    }


    @Override
    public void showEditForm(
            Context context,
            long featureId,
            GeoGeometry geometry)
    {
        if (!mIsInitialized) {
            Toast.makeText(
                    context, context.getString(R.string.error_layer_not_inited), Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        //get geometry
        if (geometry == null && featureId != Constants.NOT_FOUND) {
            for (VectorCacheItem item : mVectorCacheItems) {
                if (item.getId() == featureId) {
                    geometry = item.getGeoGeometry();
                }
            }
        }

        //check custom form
        File form = new File(mPath, ConstantsUI.FILE_FORM);
        if (form.exists()) {
            //show custom form
            Intent intent = new Intent(context, FormBuilderModifyAttributesActivity.class);
            intent.putExtra(ConstantsUI.KEY_LAYER_ID, getId());
            intent.putExtra(KEY_FEATURE_ID, featureId);
            intent.putExtra(KEY_FORM_PATH, form);
            if (null != geometry) {
                intent.putExtra(KEY_GEOMETRY, geometry);
            }
            context.startActivity(intent);
        } else {
            //if not exist show standard form
            Intent intent = new Intent(context, ModifyAttributesActivity.class);
            intent.putExtra(ConstantsUI.KEY_LAYER_ID, getId());
            intent.putExtra(KEY_FEATURE_ID, featureId);
            if (null != geometry) {
                intent.putExtra(KEY_GEOMETRY, geometry);
            }
            context.startActivity(intent);
        }
    }

    @Override
    public boolean delete() throws SQLiteException {
        File preference = new File(mContext.getApplicationInfo().dataDir, "shared_prefs");

        if (preference.exists() && preference.isDirectory()) {
            preference = new File(preference, getPath().getName() + ".xml");
            preference.delete();
        }

        return super.delete();
    }

    public void shareGeoJSON()
    {
        try {
            boolean clearCached;
            File temp = mContext.getExternalCacheDir();

            if (temp == null) {
                temp = mContext.getCacheDir();
                clearCached = FileUtil.getDirectorySize(temp) > MAX_INTERNAL_CACHE_SIZE;
            } else {
                clearCached = FileUtil.getDirectorySize(temp) > MAX_EXTERNAL_CACHE_SIZE;
            }

            temp = new File(temp, "shared_layers");
            if (clearCached) {
                FileUtil.deleteRecursive(temp);
            }

            FileUtil.createDir(temp);

            temp = new File(temp, getName() + ".geojson");
            FileWriter fw = new FileWriter(temp);

            JSONObject obj = new JSONObject();
            obj.put(GEOJSON_TYPE, GEOJSON_TYPE_FeatureCollection);

            JSONObject crs = new JSONObject();
            crs.put(GEOJSON_TYPE, GEOJSON_NAME);
            JSONObject crsName = new JSONObject();
            crsName.put(GEOJSON_NAME, "urn:ogc:def:crs:EPSG::3857");
            crs.put(GEOJSON_PROPERTIES, crsName);
            obj.put(GEOJSON_CRS, crs);

            JSONArray geoJSONFeatures = new JSONArray();
            Cursor featuresCursor = query(null, null, null, null, null);

            if (null == featuresCursor) {
                fw.close();
                return;
            }

            Feature feature;

            if (featuresCursor.moveToFirst()) {
                do {
                    JSONObject featureJSON = new JSONObject();
                    featureJSON.put(GEOJSON_TYPE, GEOJSON_TYPE_Feature);

                    feature = new Feature((long) NOT_FOUND, getFields());
                    feature.fromCursor(featuresCursor);

                    JSONObject properties = new JSONObject();
                    for (Field field : feature.getFields()) {
                        properties.put(field.getName(), feature.getFieldValue(field.getName()));
                    }

                    featureJSON.put(GEOJSON_PROPERTIES, properties);
                    featureJSON.put(GEOJSON_GEOMETRY, feature.getGeometry().toJSON());
                    geoJSONFeatures.put(featureJSON);
                } while (featuresCursor.moveToNext());
            }

            featuresCursor.close();

            obj.put(GEOJSON_TYPE_FEATURES, geoJSONFeatures);

            fw.write(obj.toString());
            fw.flush();
            fw.close();

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("application/json,application/vnd.geo+json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(temp));
//            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisArray); // multiple data

            shareIntent = Intent.createChooser(
                    shareIntent, mContext.getString(R.string.abc_shareactionprovider_share_with));
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(shareIntent);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }
}

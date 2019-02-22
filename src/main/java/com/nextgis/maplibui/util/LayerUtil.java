/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.BuildConfig;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.FormBuilderModifyAttributesActivity;
import com.nextgis.maplibui.activity.ModifyAttributesActivity;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.service.LayerFillService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.nextgis.maplibui.util.ConstantsUI.KEY_FEATURE_ID;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_FORM_PATH;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_GEOMETRY;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_GEOMETRY_CHANGED;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_LAYER_ID;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_META_PATH;

/**
 * Raster and vector layer utilities
 */
public final class LayerUtil {
    public static ArrayList<String> fillLookupTableIds(File path) throws IOException, JSONException {
        String formText = FileUtil.readFromFile(path);
        JSONArray formJson = new JSONArray(formText);
        ArrayList<String> lookupTableIds = new ArrayList<>();

        for (int i = 0; i < formJson.length(); i++) {
            JSONObject element = formJson.getJSONObject(i);
            if (ConstantsUI.JSON_COMBOBOX_VALUE.equals(element.optString(Constants.JSON_TYPE_KEY))) {
                element = element.getJSONObject(ConstantsUI.JSON_ATTRIBUTES_KEY);
                if (element.has(ConstantsUI.JSON_NGW_ID_KEY))
                    if (element.getLong(ConstantsUI.JSON_NGW_ID_KEY) != -1)
                        lookupTableIds.add(element.getLong(ConstantsUI.JSON_NGW_ID_KEY) + "");
            }
        }

        return lookupTableIds;
    }

    public static void showEditForm(VectorLayer layer, Context context, long featureId, GeoGeometry geometry) {
        if (!layer.isFieldsInitialized()) {
            Toast.makeText(context, context.getString(R.string.error_layer_not_inited), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isGeometryChanged = geometry != null;
        //get geometry
        if (geometry == null && featureId != Constants.NOT_FOUND) {
            geometry = layer.getGeometryForId(featureId);
        }

        Intent intent;
        //check custom form
        File form = new File(layer.getPath(), ConstantsUI.FILE_FORM);
        if (form.exists()) {
            //show custom form
            intent = new Intent(context, FormBuilderModifyAttributesActivity.class);
            intent.putExtra(KEY_FORM_PATH, form);
            File meta = new File(layer.getPath(), LayerFillService.NGFP_META);
            if (meta.exists())
                intent.putExtra(KEY_META_PATH, meta);
        } else {
            //if not exist show standard form
            intent = new Intent(context, ModifyAttributesActivity.class);
        }

        intent.putExtra(KEY_LAYER_ID, layer.getId());
        intent.putExtra(KEY_FEATURE_ID, featureId);
        intent.putExtra(KEY_GEOMETRY_CHANGED, isGeometryChanged);
        if (null != geometry)
            intent.putExtra(KEY_GEOMETRY, geometry);

        ((Activity) context).startActivityForResult(intent, IVectorLayerUI.MODIFY_REQUEST);
    }

    final static String AUTHORITY = ".easypicker.provider";
    static void notFound(Activity activity) {
        Toast.makeText(activity, R.string.no_activity_found, Toast.LENGTH_SHORT).show();
    }

    public static void shareTrackAsGPX(NGActivity activity, String creator, String[] tracksId) {
        ExportGPXTask exportTask = new ExportGPXTask(activity, creator, tracksId);
        exportTask.execute();
    }

    public static void shareLayerAsGeoJSON(Activity activity, VectorLayer layer, boolean proceedAttaches) {
        ExportGeoJSONTask exportTask = new ExportGeoJSONTask(activity, layer, proceedAttaches);
        exportTask.execute();
    }

    public static String getGeometryName(Context context, int geometryType) {
        switch (geometryType) {
            case GeoConstants.GTPoint:
                return context.getString(R.string.point);
            case GeoConstants.GTMultiPoint:
                return context.getString(R.string.multi_point);
            case GeoConstants.GTLineString:
                return context.getString(R.string.linestring);
            case GeoConstants.GTMultiLineString:
                return context.getString(R.string.multi_linestring);
            case GeoConstants.GTPolygon:
                return context.getString(R.string.polygon);
            case GeoConstants.GTMultiPolygon:
                return context.getString(R.string.multi_polygon);
            default:
                return context.getString(R.string.n_a);
        }
    }

}

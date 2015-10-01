package com.nextgis.maplibui.util;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplibui.R;

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

/**
 * Raster and vector layer utilities
 */
public class LayerUtil {
    private static final long MAX_INTERNAL_CACHE_SIZE = 1048576; // 1MB
    private static final long MAX_EXTERNAL_CACHE_SIZE = 5242880; // 5MB

    public static void shareLayerAsGeoJSON(VectorLayer layer)
    {
        try {
            boolean clearCached;
            File temp = layer.getContext().getExternalCacheDir();

            if (temp == null) {
                temp = layer.getContext().getCacheDir();
                clearCached = FileUtil.getDirectorySize(temp) > MAX_INTERNAL_CACHE_SIZE;
            } else {
                clearCached = FileUtil.getDirectorySize(temp) > MAX_EXTERNAL_CACHE_SIZE;
            }

            temp = new File(temp, "shared_layers");
            if (clearCached) {
                FileUtil.deleteRecursive(temp);
            }

            FileUtil.createDir(temp);

            temp = new File(temp, layer.getName() + ".geojson");
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
            Cursor featuresCursor = layer.query(null, null, null, null, null);

            if (null == featuresCursor) {
                fw.close();
                return;
            }

            Feature feature;

            if (featuresCursor.moveToFirst()) {
                do {
                    JSONObject featureJSON = new JSONObject();
                    featureJSON.put(GEOJSON_TYPE, GEOJSON_TYPE_Feature);

                    feature = new Feature((long) NOT_FOUND, layer.getFields());
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
                    shareIntent, layer.getContext().getString(R.string.abc_shareactionprovider_share_with));
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            layer.getContext().startActivity(shareIntent);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }
}

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2017 NextGIS, info@nextgis.com
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.AttachItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.GeoJSONUtil;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.FormBuilderModifyAttributesActivity;
import com.nextgis.maplibui.activity.ModifyAttributesActivity;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.service.LayerFillService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;
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

    public static void shareTrackAsGPX(NGActivity activity, String creator, String[] tracksId) {
        ExportGPXTask exportTask = new ExportGPXTask(activity, creator, tracksId);
        exportTask.execute();
    }

    public static void shareLayerAsGeoJSON(Activity activity, VectorLayer layer, boolean proceedAttaches) {
        ExportGeoJSONTask exportTask = new ExportGeoJSONTask(activity, layer, proceedAttaches);
        exportTask.execute();
    }

    private static class ExportGeoJSONTask extends AsyncTask<Void, Void, File> {
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
            Toast.makeText(mLayer.getContext(), R.string.no_features, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(File s) {
            super.onPostExecute(s);

            ControlHelper.unlockScreenOrientation(mActivity);
            if (mProgress != null)
                mProgress.dismiss();

            if (mIsCanceled && s == null) {
                Toast.makeText(mActivity, R.string.canceled, Toast.LENGTH_SHORT).show();
                return;
            }

            if (s == null || !s.exists()) {
                Toast.makeText(mActivity, R.string.error_create_feature, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("application/json,application/vnd.geo+json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(s));
//            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisArray); // multiple data

            shareIntent = Intent.createChooser(
                    shareIntent, mLayer.getContext().getString(R.string.menu_share));
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mLayer.getContext().startActivity(shareIntent);
        }
    }

    private static class ExportGPXTask extends AsyncTask<Void, Void, Void> implements DialogInterface.OnClickListener {
        private static final String XML_VERSION = "<?xml version=\"1.0\"?>";
        private static final String GPX_VERSION = "1.1";
        private static final String GPX_TAG = "<gpx version=\""
                + GPX_VERSION
                + "\" creator=\"%s\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">";
        private static final String GPX_TAG_CLOSE = "</gpx>";
        private static final String GPX_TAG_NAME = "<name>";
        private static final String GPX_TAG_NAME_CLOSE = "</name>";
        private static final String GPX_TAG_TRACK = "<trk>";
        private static final String GPX_TAG_TRACK_CLOSE = "</trk>";
        private static final String GPX_TAG_TRACK_SEGMENT = "<trkseg>";
        private static final String GPX_TAG_TRACK_SEGMENT_CLOSE = "</trkseg>";
        static final String GPX_TAG_TRACK_SEGMENT_POINT = "<trkpt lat=\"%s\" lon=\"%s\">";
        static final String GPX_TAG_TRACK_SEGMENT_POINT_CLOSE = "</trkpt>";
        static final String GPX_TAG_TRACK_SEGMENT_POINT_TIME = "<time>%s</time>";
        static final String GPX_TAG_TRACK_SEGMENT_POINT_SAT = "<sat>%s</sat>";
        static final String GPX_TAG_TRACK_SEGMENT_POINT_ELE = "<ele>%s</ele>";
        static final String GPX_TAG_TRACK_SEGMENT_POINT_FIX = "<fix>%s</fix>";

        protected NGActivity mActivity;
        AlertDialog.Builder mDialog;
        ProgressDialog mProgress;
        String[] mTracksId;
        boolean mIsCanceled, mIsChosen = true, mSeparateFiles = true;
        int mNoPoints = 0;
        String mHeader;
        ArrayList<Uri> mUris;

        ExportGPXTask(NGActivity activity, String creator, String[] tracksId) {
            mTracksId = tracksId;
            mActivity = activity;
            mHeader = XML_VERSION + "\r\n" + String.format(GPX_TAG, creator) + "\r\n";
            mUris = new ArrayList<>();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mTracksId.length > 1) {
                mIsChosen = false;
                mDialog = new AlertDialog.Builder(mActivity);
                mDialog.setTitle(R.string.menu_share).setMessage(R.string.share_gpx_multiple)
                        .setPositiveButton(R.string.share_gpx_together, this)
                        .setNeutralButton(android.R.string.cancel, this)
                        .setNegativeButton(R.string.share_gpx_separate, this).show();
                ControlHelper.lockScreenOrientation(mActivity);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (!mIsChosen)
                SystemClock.sleep(500);

            if (mIsCanceled)
                return null;

            publishProgress();
            File temp = null, parent = MapUtil.prepareTempDir(mActivity, "exported_tracks");
            try {
                IGISApplication application = (IGISApplication) mActivity.getApplication();
                Uri mContentUriTracks = Uri.parse("content://" + application.getAuthority() + "/" + TrackLayer.TABLE_TRACKS);
                Cursor track, trackpoints;
                final StringBuilder sb = new StringBuilder();
                final Formatter f = new Formatter(sb);
                if (!mSeparateFiles) {
                    temp = new File(parent, "tracks.gpx");
                    FileUtil.writeToFile(temp, mHeader);
                }

                for (String trackId : mTracksId) {
                    if (mIsCanceled)
                        return null;

                    track = mActivity.getContentResolver().query(mContentUriTracks,
                            new String[]{TrackLayer.FIELD_NAME}, TrackLayer.FIELD_ID + " = ?", new String[]{trackId}, null);
                    trackpoints = mActivity.getContentResolver().query(Uri.withAppendedPath(mContentUriTracks,
                            trackId), null, null, null, TrackLayer.FIELD_TIMESTAMP + " ASC");

                    if (track != null && track.moveToFirst()) {
                        if (mSeparateFiles) {
                            temp = new File(parent, track.getString(0) + ".gpx");
                            FileUtil.writeToFile(temp, mHeader);
                        }

                        if (trackpoints != null && trackpoints.moveToFirst()) {
                            if (mSeparateFiles) {
                                appendTrack(temp, track.getString(0), sb, f, trackpoints);
                                FileUtil.writeToFile(temp, GPX_TAG_CLOSE, true);
                                mUris.add(Uri.fromFile(temp));
                            } else
                                appendTrack(temp, track.getString(0), sb, f, trackpoints);

                            trackpoints.close();
                        } else
                            mNoPoints++;

                        track.close();
                    }
                }

                if (!mSeparateFiles) {
                    FileUtil.writeToFile(temp, GPX_TAG_CLOSE, true);
                    mUris.add(Uri.fromFile(temp));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        void appendTrack(File temp, String name, StringBuilder sb, Formatter f, Cursor trackpoints) throws IOException {
            GeoPoint point = new GeoPoint();
            int latId = trackpoints.getColumnIndex(TrackLayer.FIELD_LAT);
            int lonId = trackpoints.getColumnIndex(TrackLayer.FIELD_LON);
            int timeId = trackpoints.getColumnIndex(TrackLayer.FIELD_TIMESTAMP);
            int eleId = trackpoints.getColumnIndex(TrackLayer.FIELD_ELE);
            int satId = trackpoints.getColumnIndex(TrackLayer.FIELD_SAT);
            int fixId = trackpoints.getColumnIndex(TrackLayer.FIELD_FIX);

            DecimalFormat df = new DecimalFormat("0", new DecimalFormatSymbols(Locale.ENGLISH));
            df.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS

            sb.setLength(0);
            sb.append(GPX_TAG_TRACK);

            if (name != null) {
                sb.append(GPX_TAG_NAME);
                sb.append(name);
                sb.append(GPX_TAG_NAME_CLOSE);
            }

            sb.append(GPX_TAG_TRACK_SEGMENT);
            FileUtil.writeToFile(temp, sb.toString(), true);

            do {
                sb.setLength(0);
                point.setCoordinates(trackpoints.getDouble(lonId), trackpoints.getDouble(latId));
                point.setCRS(CRS_WEB_MERCATOR);
                point.project(CRS_WGS84);
                String sLon = df.format(point.getX());
                String sLat = df.format(point.getY());
                f.format(GPX_TAG_TRACK_SEGMENT_POINT, sLat, sLon);
                f.format(GPX_TAG_TRACK_SEGMENT_POINT_TIME, getTimeStampAsString(trackpoints.getLong(timeId)));
                f.format(GPX_TAG_TRACK_SEGMENT_POINT_ELE, df.format(trackpoints.getDouble(eleId)));
                f.format(GPX_TAG_TRACK_SEGMENT_POINT_SAT, trackpoints.getString(satId));
                f.format(GPX_TAG_TRACK_SEGMENT_POINT_FIX, trackpoints.getString(fixId));
                sb.append(GPX_TAG_TRACK_SEGMENT_POINT_CLOSE);
                FileUtil.writeToFile(temp, sb.toString(), true);
            } while (trackpoints.moveToNext());

            sb.setLength(0);
            sb.append(GPX_TAG_TRACK_SEGMENT_CLOSE);
            sb.append(GPX_TAG_TRACK_CLOSE);
            FileUtil.writeToFile(temp, sb.toString(), true);
        }

        String getTimeStampAsString(long nTimeStamp) {
            final SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return utcFormat.format(new Date(nTimeStamp));
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:
                    mSeparateFiles = false;
                    break;
                case AlertDialog.BUTTON_NEUTRAL:
                    mIsCanceled = true;
                    break;
            }
            mIsChosen = true;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

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
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            ControlHelper.unlockScreenOrientation(mActivity);
            if (mProgress != null)
                mProgress.dismiss();

            if (mIsCanceled)
                return;

            String text = mActivity.getString(R.string.not_enough_points);
            if (mNoPoints > 0)
                if (mUris.size() > 0)
                    Toast.makeText(mActivity, text + " (" + mNoPoints + ")", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(mActivity, text, Toast.LENGTH_LONG).show();

            if (mUris.size() == 0)
                return;

            Intent shareIntent = new Intent();
            shareIntent.setType("application/gpx+xml");

            if (mUris.size() > 1) {
                shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mUris);
            } else {
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, mUris.get(0));
            }

            shareIntent = Intent.createChooser(shareIntent, mActivity.getString(R.string.menu_share));
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivity(shareIntent);
        }
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

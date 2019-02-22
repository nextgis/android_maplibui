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

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGActivity;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;
import static com.nextgis.maplibui.util.LayerUtil.AUTHORITY;
import static com.nextgis.maplibui.util.LayerUtil.notFound;

public class ExportGPXTask extends AsyncTask<Void, Void, Void> implements DialogInterface.OnClickListener {
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
    private static final String GPX_TAG_TRACK_SEGMENT_POINT = "<trkpt lat=\"%s\" lon=\"%s\">";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT_CLOSE = "</trkpt>";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT_TIME = "<time>%s</time>";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT_SAT = "<sat>%s</sat>";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT_ELE = "<ele>%s</ele>";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT_FIX = "<fix>%s</fix>";

    protected NGActivity mActivity;
    private ProgressDialog mProgress;
    private String[] mTracksId;
    private boolean mIsCanceled, mIsChosen = true, mSeparateFiles = true;
    private int mNoPoints = 0;
    private String mHeader;
    private ArrayList<Uri> mUris;

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
            AlertDialog.Builder mDialog = new AlertDialog.Builder(mActivity);
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

            Context app = mActivity.getApplicationContext();
            String authority = mActivity.getPackageName() + AUTHORITY;
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
                            Uri uri = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                if (temp != null)
                                    uri = FileProvider.getUriForFile(app, authority, temp);
                            } else
                                uri = Uri.fromFile(temp);
                            if (uri != null)
                                mUris.add(uri);
                        } else
                            appendTrack(temp, track.getString(0), sb, f, trackpoints);

                        trackpoints.close();
                    } else
                        mNoPoints++;

                    track.close();
                }
            }

            Uri uri = null;
            if (!mSeparateFiles) {
                FileUtil.writeToFile(temp, GPX_TAG_CLOSE, true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (temp != null)
                        uri = FileProvider.getUriForFile(app, authority, temp);
                } else
                    uri = Uri.fromFile(temp);
                if (uri != null)
                    mUris.add(uri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void appendTrack(File temp, String name, StringBuilder sb, Formatter f, Cursor trackpoints) throws IOException {
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

    private String getTimeStampAsString(long nTimeStamp) {
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
        String type = "application/gpx+xml";
        String action = Intent.ACTION_SEND;

        if (mUris.size() > 1)
            action = Intent.ACTION_SEND_MULTIPLE;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(mActivity);
            for (Uri uri : mUris)
                builder.addStream(uri);
            shareIntent = builder.setType(type).getIntent().setAction(action).setType(type).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            shareIntent = Intent.createChooser(shareIntent, mActivity.getString(R.string.menu_share));
            shareIntent.setType(type);
            shareIntent.setAction(action);
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (mUris.size() > 1)
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mUris);
            else
                shareIntent.putExtra(Intent.EXTRA_STREAM, mUris.get(0));
        }

        try {
            mActivity.startActivity(shareIntent);
        } catch (ActivityNotFoundException e) {
            notFound(mActivity);
        }
    }
}
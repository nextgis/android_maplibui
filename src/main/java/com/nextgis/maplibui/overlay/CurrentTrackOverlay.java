/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.overlay;

import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Handler;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.util.ControlHelper;

import java.util.ArrayList;
import java.util.List;


public class CurrentTrackOverlay
        extends Overlay
{
    private       Cursor         mCursor;
    private final Uri            mContentUriTracks;
    private       Paint          mPaint;
    private       List<GeoPoint> mTrackpoints;

    String[] mProjection = new String[] {TrackLayer.FIELD_ID};
    String   mSelection  = TrackLayer.FIELD_VISIBLE + " = 1 AND (" + TrackLayer.FIELD_END +
                           " IS NULL OR " + TrackLayer.FIELD_END +
                           " = '')";


    public CurrentTrackOverlay(
            Context context,
            MapViewOverlays mapViewOverlays)
    {
        super(context, mapViewOverlays);

        Activity parent = (Activity) context;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(ControlHelper.getColor(mContext, R.attr.colorAccent));
        mPaint.setStrokeWidth(4);

        mTrackpoints = new ArrayList<>();
        IGISApplication app = (IGISApplication) parent.getApplication();
        String authority = app.getAuthority();
        mContentUriTracks = Uri.parse("content://" + authority + "/" + TrackLayer.TABLE_TRACKS);
        mCursor = mContext.getContentResolver()
                .query(mContentUriTracks, mProjection, mSelection, null, null);

        if (mCursor == null)
            return;

        mCursor.setNotificationUri(mContext.getContentResolver(), mContentUriTracks);
        ContentObserver test = new TrackObserver(new Handler());
        mCursor.registerContentObserver(test);
    }


    @Override
    public void drawOnPanning(
            Canvas canvas,
            PointF currentMouseOffset)
    {
        if (mMapViewOverlays.isLockMap()) {
            return;
        }

        if (mTrackpoints.size() < 2) {
            return;
        }

        float x0, y0, x1, y1;

        for (int i = 1; i < mTrackpoints.size(); i++) {
            x0 = (float) mTrackpoints.get(i - 1).getX() - currentMouseOffset.x;
            y0 = (float) mTrackpoints.get(i - 1).getY() - currentMouseOffset.y;
            x1 = (float) mTrackpoints.get(i).getX() - currentMouseOffset.x;
            y1 = (float) mTrackpoints.get(i).getY() - currentMouseOffset.y;
            canvas.drawLine(x0, y0, x1, y1, mPaint);
        }
    }


    @Override
    public void drawOnZooming(
            Canvas canvas,
            PointF currentFocusLocation,
            float scale)
    {
        if (mMapViewOverlays.isLockMap()) {
            return;
        }

        float x0, y0, x1, y1;
        double dx, dy;

        if (mTrackpoints.size() < 2) {
            return;
        } else {
            dx = mTrackpoints.get(0).getX() + currentFocusLocation.x;
            dy = mTrackpoints.get(0).getY() + currentFocusLocation.y;
            x0 = (float) (mTrackpoints.get(0).getX() - (1 - scale) * dx);
            y0 = (float) (mTrackpoints.get(0).getY() - (1 - scale) * dy);
        }

        for (int i = 1; i < mTrackpoints.size(); i++) {
            dx = mTrackpoints.get(i).getX() + currentFocusLocation.x;
            dy = mTrackpoints.get(i).getY() + currentFocusLocation.y;
            x1 = (float) (mTrackpoints.get(i).getX() - (1 - scale) * dx);
            y1 = (float) (mTrackpoints.get(i).getY() - (1 - scale) * dy);

            canvas.drawLine(x0, y0, x1, y1, mPaint);
            x0 = x1;
            y0 = y1;
        }
    }


    @Override
    public void draw(
            Canvas canvas,
            MapDrawable mapDrawable)
    {
        mTrackpoints.clear();

        if (mCursor == null || mCursor.getCount() == 0 || !mCursor.moveToFirst()) {
            return;
        }

        String id = mCursor.getString(0);
        String[] proj = new String[] {TrackLayer.FIELD_LON, TrackLayer.FIELD_LAT};

        Cursor track = mContext.getContentResolver().query(
                Uri.withAppendedPath(mContentUriTracks, id), proj, null, null, null);

        if (track == null) {
            return;
        }

        if (track.moveToFirst()) {
            float x0 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LON)),
                    y0 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LAT)), x1, y1;
            GeoPoint point;
            point = new GeoPoint(x0, y0);
            point.setCRS(GeoConstants.CRS_WEB_MERCATOR);

            GeoPoint mts = mapDrawable.mapToScreen(point);
            x0 = (float) (mts.getX());
            y0 = (float) (mts.getY());

            while (track.moveToNext()) {
                x1 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LON));
                y1 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LAT));

                point = new GeoPoint(x1, y1);
                point.setCRS(GeoConstants.CRS_WEB_MERCATOR);

                mts = mapDrawable.mapToScreen(point);

                canvas.drawLine(x0, y0, (float) mts.getX(), (float) mts.getY(), mPaint);

                mTrackpoints.add(new GeoPoint(x0, y0));
                x0 = (float) (mts.getX());
                y0 = (float) (mts.getY());
            }

            mTrackpoints.add(new GeoPoint(x0, y0));
        }

        track.close();
    }


    public void setLineColor(int color)
    {
        mPaint.setColor(color);
    }


    public void setLineWidth(float width)
    {
        mPaint.setStrokeWidth(width);
    }


    private class TrackObserver
            extends ContentObserver
    {

        public TrackObserver(Handler handler)
        {
            super(handler);
        }


        @Override
        public void onChange(boolean selfChange)
        {
            super.onChange(selfChange);

            mCursor.requery();
            mMapViewOverlays.postInvalidate();
        }

    }
}

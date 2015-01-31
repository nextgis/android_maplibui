/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Authors:  Stanislav Petriakov
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

package com.nextgis.maplibui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.location.Location;
import android.location.LocationManager;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.api.OverlayItem;


public class CurrentLocationOverlay
        extends Overlay
        implements GpsEventListener
{
    private Context        mContext;
    private GpsEventSource mGpsEventSource;
    private Location       mCurrentLocation;
    private boolean        mIsInBounds;
    private boolean mIsAccuracyEnabled = true;
    private boolean mIsAccuracyMarkerBiggest;
    private Bitmap  mStandingMarker, mMovingMarker;
    private int         mMarkerColor;
    private OverlayItem mMarker, mAccuracy;


    public CurrentLocationOverlay(
            Context context,
            MapDrawable mapDrawable)
    {
        mContext = context;
        Activity parent = (Activity) context;
        mGpsEventSource = ((IGISApplication) parent.getApplication()).getGpsEventSource();

        double longitude = 0, latitude = 0;
        Location location = mGpsEventSource.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location == null) {
            location = mGpsEventSource.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (location != null) {
            mCurrentLocation = location;
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        mStandingMarker = getDefaultMarker(true);
        mMovingMarker = getDefaultMarker(false);

        mMarker = new OverlayItem(mapDrawable, longitude, latitude, mStandingMarker);
        mAccuracy = new OverlayItem(mapDrawable, longitude, latitude, null);
    }


    @Override
    public void drawOnPanning(
            Canvas canvas,
            PointF mCurrentMouseOffset)
    {
        if (mIsInBounds) {
            if (mIsAccuracyEnabled && mIsAccuracyMarkerBiggest) {
                drawOnPanning(canvas, mCurrentMouseOffset, mAccuracy);
            }

            drawOnPanning(canvas, mCurrentMouseOffset, mMarker);
        }
    }


    @Override
    public void drawOnZooming(
            Canvas canvas,
            PointF mCurrentFocusLocation,
            float scale)
    {
        if (mIsInBounds) {
            if (mIsAccuracyEnabled && mIsAccuracyMarkerBiggest) {
                drawOnZooming(canvas, mCurrentFocusLocation, scale, mAccuracy, true);
            }

            drawOnZooming(canvas, mCurrentFocusLocation, scale, mMarker, false);
        }
    }


    @Override
    public void draw(
            Canvas canvas,
            MapDrawable mapDrawable)
    {
        if (mCurrentLocation != null) {
            double lat = mCurrentLocation.getLatitude();
            double lon = mCurrentLocation.getLongitude();
            mMarker.setCoordinates(lon, lat);

            double accuracy = mCurrentLocation.getAccuracy();
            accuracy = getAccuracyRadius(lat, accuracy);

            GeoPoint accuracyEdgePoint = new GeoPoint(lon, accuracy);
            accuracyEdgePoint.setCRS(GeoConstants.CRS_WGS84);
            accuracyEdgePoint.project(GeoConstants.CRS_WEB_MERCATOR);
            accuracyEdgePoint = mapDrawable.mapToScreen(accuracyEdgePoint);

            int radius = (int) Math.abs(accuracyEdgePoint.getY() - mMarker.getScreenY());
            mAccuracy.setMarker(getAccuracyMarker(radius));
            mAccuracy.setCoordinates(lon, lat);

            mIsAccuracyMarkerBiggest = compareMarkers();

            GeoEnvelope bounds = mapDrawable.getCurrentBounds();
            mIsInBounds = bounds.contains(mMarker.getCoordinates(GeoConstants.CRS_WEB_MERCATOR));

            if (mIsInBounds) {
                if (mIsAccuracyEnabled && mIsAccuracyMarkerBiggest) {
                    draw(canvas, mAccuracy);
                }

                draw(canvas, mMarker);
            }
        }
    }

    private boolean compareMarkers()
    {
        int accuracySize = mAccuracy.getMarker().getWidth();
        int markerSize = Math.min(mMarker.getMarker().getWidth(), mMarker.getMarker().getHeight());

        return accuracySize > markerSize;
    }


    private double getAccuracyRadius(
            double lat,
            double accuracy)
    {
        int R = 6378137;
        double dxLat = accuracy / R;
//        double dxLon = offsetLon / (R * Math.cos(Math.PI * lat / 180));

        return lat + dxLat * 180 / Math.PI;
    }


    public void startShowingCurrentLocation()
    {
        mGpsEventSource.addListener(this);
    }


    public void stopShowingCurrentLocation()
    {
        mGpsEventSource.removeListener(this);
    }


    public void setShowAccuracy(boolean isEnabled)
    {
        mIsAccuracyEnabled = isEnabled;
    }


    public void setStandingMarker(Bitmap standingMarker)
    {
        mStandingMarker = standingMarker;
    }


    public void setMovingMarker(Bitmap movingMarker)
    {
        mMovingMarker = movingMarker;
    }


    @Override
    public void onLocationChanged(Location location)
    {
//        if (location.getProvider()
//                    .equals(LocationManager.GPS_PROVIDER)) {
// TODO update accuracy / proper provider / invalidate rect
        if (location.getAccuracy() < mCurrentLocation.getAccuracy() ||
            location.getTime() > mCurrentLocation.getTime()) {
            mCurrentLocation = location;
        }
    }


    @Override
    public void onGpsStatusChanged(int event)
    {

    }


    private Bitmap getDefaultMarker(boolean isStanding)
    {
        int resource = isStanding
                       ? R.drawable.abc_btn_switch_to_on_mtrl_00001
                       : R.drawable.abc_ic_ab_back_mtrl_am_alpha;

        Bitmap marker = BitmapFactory.decodeResource(mContext.getResources(), resource);
        marker = marker.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(marker);

        mMarkerColor = mContext.getResources().getColor(R.color.accent);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorFilter filter = new PorterDuffColorFilter(mMarkerColor, PorterDuff.Mode.SRC_ATOP);
        paint.setColorFilter(filter);

        canvas.drawBitmap(marker, 0, 0, paint);

        if (!isStanding) {  // FIXME remove on default marker set
            Matrix matrix = new Matrix();
            matrix.setRotate(90, canvas.getWidth() / 2, canvas.getHeight() / 2);
            return Bitmap.createBitmap(marker, 0, 0, marker.getWidth(), marker.getHeight(), matrix,
                                       true);
        }

        return marker;
    }


    private Bitmap getAccuracyMarker(int accuracy)
    {
//        int max = 2 * Math.max(mContext.getResources().getDisplayMetrics().widthPixels,
//                               mContext.getResources().getDisplayMetrics().heightPixels);
//          // TODO huge radius > possible out of memory
//        if (accuracy > max) {
//            accuracy = (int) (max * Constants.OFFSCREEN_EXTRASIZE_RATIO);
//        }

        Bitmap result = Bitmap.createBitmap(accuracy * 2, accuracy * 2, Bitmap.Config.ARGB_8888);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Canvas canvas = new Canvas(result);
        paint.setColor(mMarkerColor);
        paint.setAlpha(64);
        canvas.drawCircle(accuracy, accuracy, accuracy, paint);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawCircle(accuracy, accuracy, accuracy - 2, paint);

        return result;
    }
}

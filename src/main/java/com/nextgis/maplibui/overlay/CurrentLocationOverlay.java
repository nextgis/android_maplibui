/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.overlay;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.MapViewOverlays;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.api.OverlayItem;
import com.nextgis.maplibui.util.SettingsConstantsUI;


public class CurrentLocationOverlay
        extends Overlay
        implements GpsEventListener
{
    public static final long LOCATION_FORCE_UPDATE_TIMEOUT = 15000;

    public static final int WITH_MARKER   = 1;
    public static final int WITH_ACCURACY = 1 << 1;

    private GpsEventSource mGpsEventSource;
    private Location       mCurrentLocation;
    private boolean        mIsInBounds;
    private boolean mIsAccuracyEnabled = true;
    private boolean mIsAccuracyMarkerBiggest;
    private boolean mIsStandingMarkerCustom, mIsMovingMarkerCustom;
    private int mStandingMarkerRes = R.drawable.abc_btn_switch_to_on_mtrl_00001, mMovingMarkerRes =
            R.drawable.abc_ic_ab_back_mtrl_am_alpha;
    private int         mMarkerColor;
    private OverlayItem mMarker, mAccuracy;
    private int mShowMode;


    public CurrentLocationOverlay(
            Context context,
            MapViewOverlays mapViewOverlays)
    {
        super(context, mapViewOverlays);
        Activity parent = (Activity) context;
        mGpsEventSource = ((IGISApplication) parent.getApplication()).getGpsEventSource();
        mMarkerColor = mContext.getResources().getColor(R.color.accent);

        double longitude = 0, latitude = 0;
//        Location location = mGpsEventSource.getLastKnownLocation();
//
//        if (location != null) {
//            mCurrentLocation = location;
//            longitude = location.getLongitude();
//            latitude = location.getLatitude();
//        }

        mMarker =
                new OverlayItem(mapViewOverlays.getMap(), longitude, latitude, getDefaultMarker());
        mAccuracy = new OverlayItem(mapViewOverlays.getMap(), longitude, latitude, null);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        mShowMode = Integer.parseInt(
                preferences.getString(SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC, "3"));
        setShowAccuracy(0 != (mShowMode & WITH_ACCURACY));
    }


    @Override
    public void drawOnPanning(
            Canvas canvas,
            PointF currentMouseOffset)
    {
        if (mMapViewOverlays.isLockMap()) {
            draw(canvas, null);
            return;
        }

        if (mIsInBounds && isMarkerEnabled() && mCurrentLocation != null) {
            if (mIsAccuracyEnabled && mIsAccuracyMarkerBiggest) {
                drawOnPanning(canvas, currentMouseOffset, mAccuracy);
            }

            drawOnPanning(canvas, currentMouseOffset, mMarker);
        }
    }


    @Override
    public void drawOnZooming(
            Canvas canvas,
            PointF currentFocusLocation,
            float scale)
    {
        if (mMapViewOverlays.isLockMap()) {
            draw(canvas, null);
            return;
        }

        if (mIsInBounds && isMarkerEnabled() && mCurrentLocation != null) {
            if (mIsAccuracyEnabled && mIsAccuracyMarkerBiggest) {
                drawOnZooming(canvas, currentFocusLocation, scale, mAccuracy, true);
            }

            drawOnZooming(canvas, currentFocusLocation, scale, mMarker, false);
        }
    }


    @Override
    public void draw(
            Canvas canvas,
            MapDrawable mapDrawable)
    {
        if (mCurrentLocation != null && isMarkerEnabled()) {
            double lat = mCurrentLocation.getLatitude();
            double lon = mCurrentLocation.getLongitude();
            mMarker.setMarker(getDefaultMarker());
            mMarker.setCoordinates(lon, lat);

            if (null != mapDrawable) {

                double accuracy = mCurrentLocation.getAccuracy();
                accuracy = getAccuracyRadius(lat, accuracy);

                GeoPoint accuracyEdgePoint = new GeoPoint(lon, accuracy);
                accuracyEdgePoint.setCRS(GeoConstants.CRS_WGS84);
                accuracyEdgePoint.project(GeoConstants.CRS_WEB_MERCATOR);
                accuracyEdgePoint = mapDrawable.mapToScreen(accuracyEdgePoint);

                int radius = (int) (mMarker.getScreenY() - accuracyEdgePoint.getY());
                mAccuracy.setMarker(getAccuracyMarker(radius));
                mAccuracy.setCoordinates(lon, lat);

                mIsAccuracyMarkerBiggest = compareMarkers();

                GeoEnvelope bounds = mapDrawable.getCurrentBounds();
                mIsInBounds =
                        bounds.contains(mMarker.getCoordinates(GeoConstants.CRS_WEB_MERCATOR));

//            Paint p = new Paint();
//            p.setColor(mMarkerColor);
//            p.setAlpha(60);
//            GeoPoint c = mAccuracy.getCoordinates(GeoConstants.CRS_WEB_MERCATOR);
//            mapDrawable.getDisplay().drawCircle((float) c.getX(), (float) c.getY(), radius, p);
            } else {
                mIsAccuracyMarkerBiggest = false;
            }

            if (mIsInBounds) {
                if (mIsAccuracyEnabled && mIsAccuracyMarkerBiggest) {
                    drawOverlayItem(canvas, mAccuracy);
                }

                drawOverlayItem(canvas, mMarker);
            }
        }
    }


    private boolean compareMarkers()
    {
        if (mAccuracy.getMarker() == null) {
            return false;
        }

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
        mCurrentLocation = null;
        mGpsEventSource.addListener(this);
    }


    public void stopShowingCurrentLocation()
    {
        mGpsEventSource.removeListener(this);
    }


    public void updateMode(String newMode)
    {
        mShowMode = Integer.parseInt(newMode);
        setShowAccuracy(0 != (mShowMode & WITH_ACCURACY));
    }


    private boolean isMarkerEnabled()
    {
        return 0 != (mShowMode & WITH_MARKER);
    }


    public void setShowAccuracy(boolean isEnabled)
    {
        mIsAccuracyEnabled = isEnabled;
    }


    public void setStandingMarker(int standingMarkerResource)
    {
        mStandingMarkerRes = standingMarkerResource;
        mIsStandingMarkerCustom = true;
    }


    public void setMovingMarker(int movingMarkerResource)
    {
        mMovingMarkerRes = movingMarkerResource;
        mIsMovingMarkerCustom = true;
    }


    /**
     * Set default markers overlay color and accuracy marker color
     *
     * @param color
     *         new color
     */
    public void setColor(int color)
    {
        mMarkerColor = color;
    }


    // TODO invalidate rect
    @Override
    public void onLocationChanged(Location location)
    {
        boolean update = true;

        if (location != null) {
            String provider = location.getProvider();
            update = LocationUtil.isProviderEnabled(mContext, provider, false);

//            if (!update) {
//                update = mCurrentLocation.getProvider().equals(provider) ||
//                         location.getAccuracy() < mCurrentLocation.getAccuracy() ||
//                         location.getTime() - mCurrentLocation.getTime() > LOCATION_FORCE_UPDATE_TIMEOUT;
//            }
        }

        if (update) {
            mCurrentLocation = location;
            mMapViewOverlays.postInvalidate();
        }
    }


    @Override
    public void onGpsStatusChanged(int event)
    {

    }


    private Bitmap getDefaultMarker()
    {
        boolean isStanding = mCurrentLocation == null || !mCurrentLocation.hasBearing() ||
                             !mCurrentLocation.hasSpeed() || mCurrentLocation.getSpeed() == 0;

        int resource = isStanding ? mStandingMarkerRes : mMovingMarkerRes;
        Bitmap marker = BitmapFactory.decodeResource(mContext.getResources(), resource);
        marker = marker.copy(Bitmap.Config.ARGB_8888, true);

        if (isStanding) {
            if (!mIsStandingMarkerCustom) {
                applyColorFilter(marker);
            }
        } else {
            Matrix matrix = new Matrix();
            int arrowRotate = 0;

            if (!mIsMovingMarkerCustom) {
                applyColorFilter(marker);
                arrowRotate += 90;
            }

            if (mCurrentLocation.hasBearing()) {
                arrowRotate += mCurrentLocation.getBearing();
            }

            matrix.setRotate(arrowRotate);

            int w = marker.getWidth();
            int h = marker.getHeight();
            marker = Bitmap.createBitmap(marker, 0, 0, w, h, matrix, true);
        }

        return marker;
    }


    private Bitmap applyColorFilter(Bitmap marker)
    {
        Canvas canvas = new Canvas(marker);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorFilter filter = new PorterDuffColorFilter(mMarkerColor, PorterDuff.Mode.SRC_ATOP);
        paint.setColorFilter(filter);

        canvas.drawBitmap(marker, 0, 0, paint);

        return marker;
    }


    // TODO huge radius > possible out of memory
    private Bitmap getAccuracyMarker(int accuracy)
    {
        int max = Math.max(
                mContext.getResources().getDisplayMetrics().widthPixels,
                mContext.getResources().getDisplayMetrics().heightPixels);

        if (accuracy * 2 > max) {
            accuracy = max / 2; // temp fix
        }

        if (accuracy <= 0) {
            accuracy = 1;
        }

        Bitmap result = Bitmap.createBitmap(accuracy * 2, accuracy * 2, Bitmap.Config.ARGB_4444);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Canvas canvas = new Canvas(result);
        paint.setColor(mMarkerColor);
        paint.setAlpha(64);
        canvas.drawCircle(accuracy, accuracy, accuracy, paint);
//        canvas.drawArc(0, 0, accuracy*2, accuracy*2, 0, 180, false, paint);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawCircle(accuracy, accuracy, accuracy - 2, paint);

        return result;
    }
}

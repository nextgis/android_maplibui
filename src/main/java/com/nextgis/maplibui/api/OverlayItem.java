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

package com.nextgis.maplibui.api;

import android.graphics.Bitmap;
import android.graphics.PointF;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.GeoConstants;


public class OverlayItem
{
    // TODO different gravities
    public enum Gravity
    {
        CENTER
    }


    private GeoPoint    mCoordinates;
    private PointF      mScreenCoordinates;
    private Bitmap      mMarker;
    private MapDrawable mMapDrawable;
    private float       mOffsetX, mOffsetY;
    private Gravity     mMarkerGravity;
    private boolean     mIsVisible = true;


    public OverlayItem(
            MapDrawable map,
            GeoPoint coordinates,
            Bitmap marker)
    {
        mMapDrawable = map;
        mScreenCoordinates = new PointF();

        mCoordinates = new GeoPoint();
        setCoordinates(coordinates);

        mMarker = marker;
        setMarkerGravity(Gravity.CENTER);
    }


    public OverlayItem(
            MapDrawable map,
            double longitude,
            double latitude,
            Bitmap marker)
    {
        this(map, new GeoPoint(longitude, latitude), marker);
    }


    public void setCoordinatesFromWGS(
            double longitude,
            double latitude)
    {
        mCoordinates.setCoordinates(longitude, latitude);
        mCoordinates.setCRS(GeoConstants.CRS_WGS84);
        mCoordinates.project(GeoConstants.CRS_WEB_MERCATOR);

        updateScreenCoordinates();
    }


    public void setCoordinates(GeoPoint point)
    {
        if (point != null) {
            switch (point.getCRS()) {
                case GeoConstants.CRS_WGS84:
                    setCoordinatesFromWGS(point.getX(), point.getY());
                    break;
                case GeoConstants.CRS_WEB_MERCATOR:
                    mCoordinates.setCoordinates(point.getX(), point.getY());
                    break;
            }

            mCoordinates.setCRS(point.getCRS());
        }

        updateScreenCoordinates();
    }


    public void updateScreenCoordinates() {
        GeoPoint mts = mMapDrawable.mapToScreen((GeoPoint) mCoordinates.copy());
        mScreenCoordinates.x = (float) (mts.getX() - mOffsetX);
        mScreenCoordinates.y = (float) (mts.getY() - mOffsetY);
    }


    public GeoPoint getCoordinates(int CRS)
    {
        if (CRS == GeoConstants.CRS_WGS84) {
            GeoPoint wgs = new GeoPoint(mCoordinates.getX(), mCoordinates.getY());
            wgs.setCRS(GeoConstants.CRS_WEB_MERCATOR);
            wgs.project(GeoConstants.CRS_WGS84);
            return wgs;
        }

        return mCoordinates;
    }


    public PointF getScreenCoordinates()
    {
        return mScreenCoordinates;
    }


    public float getScreenX()
    {
        return mScreenCoordinates.x;
    }


    public float getScreenY()
    {
        return mScreenCoordinates.y;
    }


    public Bitmap getMarker()
    {
        return mMarker;
    }


    public void setMarker(Bitmap newMarker)
    {
        mMarker = newMarker;
        updateMarkerOffsets();
    }


    public void setMarkerGravity(Gravity newGravity)
    {
        mMarkerGravity = newGravity;
        updateMarkerOffsets();
    }


    private void updateMarkerOffsets()
    {
        if (mMarker != null) {
            switch (mMarkerGravity) {
                case CENTER:
                    mOffsetX = mMarker.getWidth() / 2f;
                    mOffsetY = mMarker.getHeight() / 2f;
                    break;
            }
        }
    }


    public float getOffsetX()
    {
        return mOffsetX;
    }


    public float getOffsetY()
    {
        return mOffsetY;
    }

    public void setVisible(boolean isVisible) {
        mIsVisible = isVisible;
    }

    public boolean isVisible() {
        return mIsVisible;
    }
}

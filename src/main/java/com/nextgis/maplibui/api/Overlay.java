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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplibui.mapui.MapViewOverlays;


public abstract class Overlay
{
    protected boolean mIsVisible = true;

    protected static final String BUNDLE_KEY_TYPE    = "type";
    protected static final String BUNDLE_KEY_VISIBLE = "is_visible";

    protected Context         mContext;
    protected MapViewOverlays mMapViewOverlays;


    public Overlay(
            Context context,
            MapViewOverlays mapViewOverlays)
    {
        mContext = context;
        mMapViewOverlays = mapViewOverlays;
    }


    public abstract void draw(
            Canvas canvas,
            MapDrawable mapDrawable);

    public abstract void drawOnPanning(
            Canvas canvas,
            PointF currentMouseOffset);

    public abstract void drawOnZooming(
            Canvas canvas,
            PointF currentFocusLocation,
            float scale);


    protected void drawOverlayItem(
            Canvas canvas,
            OverlayItem overlayItem)
    {
        if(null == canvas || null == overlayItem || null == overlayItem.getMarker())
            return;

        overlayItem.updateScreenCoordinates();

        if (!isVisible())
            return;

        canvas.drawBitmap(
                overlayItem.getMarker(), overlayItem.getScreenX(), overlayItem.getScreenY(), null);
    }


    public void drawOnPanning(
            Canvas canvas,
            PointF currentMouseOffset,
            OverlayItem overlayItem)
    {
        if(null == canvas || null == overlayItem || null == overlayItem.getMarker())
            return;

        if (!isVisible())
            return;

        canvas.drawBitmap(
                overlayItem.getMarker(), overlayItem.getScreenX() - currentMouseOffset.x,
                overlayItem.getScreenY() - currentMouseOffset.y, null);
    }


    protected void drawOnZooming(
            Canvas canvas,
            PointF currentFocusLocation,
            float scale,
            OverlayItem overlayItem,
            boolean scaleMarker)
    {
        if(null == canvas || null == overlayItem || null == overlayItem.getMarker())
            return;

        if (!isVisible())
            return;

        GeoPoint offset = getScaledOffset(currentFocusLocation, overlayItem, scale, scaleMarker);
        float zoomedX = (float) (overlayItem.getScreenX() - offset.getX());
        float zoomedY = (float) (overlayItem.getScreenY() - offset.getY());

        Matrix matrix = new Matrix();

        if (scaleMarker) {
            matrix.postScale(scale, scale);
        }

        matrix.postTranslate(zoomedX, zoomedY);
        canvas.drawBitmap(overlayItem.getMarker(), matrix, null);
    }


    public GeoPoint getScaledOffset(
            PointF currentFocusLocation,
            OverlayItem overlayItem,
            float scale,
            boolean scaleMarker)
    {
        if(null == overlayItem || null == overlayItem.getMarker())
            return new GeoPoint();

        double x = overlayItem.getScreenCoordinates().x;
        double y = overlayItem.getScreenCoordinates().y;
        int markerWidth = overlayItem.getMarker().getWidth();
        int markerHeight = overlayItem.getMarker().getHeight();

        double dx = x + markerWidth / 2 + currentFocusLocation.x;
        double dy = y + markerHeight / 2 + currentFocusLocation.y;

        GeoPoint offset = new GeoPoint();

        if (!scaleMarker) {
            markerHeight = markerWidth = 0;
        }

        float scaledWidth = markerWidth * scale;
        float scaledHeight = markerHeight * scale;

        offset.setX((scaledWidth - markerWidth) / 2 + (1 - scale) * dx);
        offset.setY((scaledHeight - markerHeight) / 2 + (1 - scale) * dy);

        return offset;
    }


    public void setVisibility(boolean isVisible)
    {
        mIsVisible = isVisible;
    }


    public boolean isVisible()
    {
        return mIsVisible;
    }


    public Bundle onSaveState()
    {
        Bundle bundle = new Bundle();
        bundle.putBoolean(BUNDLE_KEY_VISIBLE, mIsVisible);
        return bundle;
    }


    public void onRestoreState(Bundle bundle)
    {
        mIsVisible = bundle.getBoolean(BUNDLE_KEY_VISIBLE);
    }
}

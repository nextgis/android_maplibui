/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.VectorCacheItem;
import com.nextgis.maplibui.api.Overlay;

import java.util.List;


/**
 * The class for edit vector features
 */
public class EditLayerOverlay
        extends Overlay
{
    public final static int MODE_HIGHLIGHT = 1;
    public final static int MODE_EDIT = 2;
    protected VectorLayer mLayer;
    protected VectorCacheItem mItem;
    protected int mMode;
    protected Paint mPaint;

    public EditLayerOverlay(
            Context context,
            MapViewOverlays mapViewOverlays)
    {
        super(context, mapViewOverlays);
        mLayer = null;
        mItem = null;
        mMode = MODE_HIGHLIGHT;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(mContext.getResources().getColor(R.color.accent));
        mPaint.setAlpha(190);
        mPaint.setStrokeWidth(8);
    }


    public void setMode(int mode)
    {
        mMode = mode;
    }


    public void setFeature(VectorLayer layer, VectorCacheItem item){
        mLayer = layer;
        mItem = item;

        mMapViewOverlays.postInvalidate();
    }

    @Override
    public void draw(
            Canvas canvas,
            MapDrawable mapDrawable)
    {
        if(null == mItem)
            return;
        GeoGeometry geom = mItem.getGeoGeometry();
        if(null == geom)
            return;

        GeoPoint[] geoPoints = null;
        float[] points = null;
        switch (geom.getType()){
            case GeoConstants.GTPoint:
                geoPoints = new GeoPoint[1];
                geoPoints[0] = (GeoPoint) geom;
                points = mapDrawable.mapToScreen(geoPoints);
                mPaint.setStrokeWidth(30);
                canvas.drawPoints(points, mPaint);
                break;
            case GeoConstants.GTMultiPoint:
                GeoMultiPoint geoMultiPoint = (GeoMultiPoint) geom;
                geoPoints = new GeoPoint[geoMultiPoint.size()];
                for(int i = 0; i < geoMultiPoint.size(); i++){
                    geoPoints[i] = geoMultiPoint.get(i);
                }
                points = mapDrawable.mapToScreen(geoPoints);
                mPaint.setStrokeWidth(30);
                canvas.drawPoints(points, mPaint);
                break;
            case GeoConstants.GTLineString:
                GeoLineString lineString = (GeoLineString)geom;
                geoPoints = lineString.getPoints().toArray(new GeoPoint[lineString.getPoints().size()]);
                points = mapDrawable.mapToScreen(geoPoints);
                mPaint.setStrokeWidth(10);
                canvas.drawLines(points, mPaint);
                break;
            case GeoConstants.GTMultiLineString:
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
            case GeoConstants.GTGeometryCollection:
                break;
        }


    }


    @Override
    public void drawOnPanning(
            Canvas canvas,
            PointF mCurrentMouseOffset)
    {
        if(null == mItem)
            return;
        GeoGeometry geom = mItem.getGeoGeometry();
        if(null == geom)
            return;
    }


    @Override
    public void drawOnZooming(
            Canvas canvas,
            PointF mCurrentFocusLocation,
            float scale)
    {
        if(null == mItem)
            return;
        GeoGeometry geom = mItem.getGeoGeometry();
        if(null == geom)
            return;
    }

}

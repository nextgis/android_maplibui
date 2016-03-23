/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.view.MotionEvent;

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplibui.api.DrawItem;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.util.ConstantsUI;

public class RulerOverlay extends Overlay implements MapViewEventListener {
    protected final float mTolerancePX;
    protected PointF mTempPointOffset;

    protected boolean mMeasuring, mIsMoving;
    protected DrawItem mRulerItem;
    protected GeoLineString mRulerString;
    protected OnRulerChanged mListener;

    public interface OnRulerChanged {
        void onLengthChanged(double length);
    }

    public RulerOverlay(Context context, MapViewOverlays mapViewOverlays) {
        super(context, mapViewOverlays);

        mTolerancePX = context.getResources().getDisplayMetrics().density * ConstantsUI.TOLERANCE_DP;
    }

    public void startMeasuring(OnRulerChanged listener) {
        mMeasuring = true;
        mRulerItem = new DrawItem();
        mRulerString = new GeoLineString();
        mListener = listener;
        mMapViewOverlays.addListener(this);
    }

    public void stopMeasuring() {
        mMeasuring = false;
        mRulerItem = null;
        mRulerString = null;
        mListener = null;
        mMapViewOverlays.removeListener(this);
    }

    public boolean isMeasuring() {
        return mMeasuring;
    }

    public double getLength() {
        if (mMeasuring)
            return mRulerString.getLength();

        return 0;
    }

    protected void fillDrawItem() {
        GeoPoint[] geoPoints = mRulerString.getPoints().toArray(new GeoPoint[mRulerString.getPointCount()]);
        float[] points = mMapViewOverlays.getMap().mapToScreen(geoPoints);

        int selectedPoint = 0;
        if (mRulerItem != null)
            selectedPoint = mRulerItem.getSelectedPointId();

        mRulerItem = new DrawItem(DrawItem.TYPE_VERTEX, points);
        mRulerItem.addVertices(points);

        mRulerItem.setSelectedPoint(selectedPoint);
    }

    protected void fillGeometry() {
        mRulerString.clear();
        float[] points = mRulerItem.getRing(0);

        if (points != null) {
            GeoPoint[] geoPoints = mMapViewOverlays.getMap().screenToMap(points);
            for (GeoPoint geoPoint : geoPoints)
                mRulerString.add(geoPoint);

            if (mListener != null)
                mListener.onLengthChanged(getLength());
        }
    }

    @Override
    public void draw(Canvas canvas, MapDrawable mapDrawable) {
        if (isMeasuring()) {
            fillDrawItem();
            mRulerItem.drawLines(canvas, true, true, false, false);
        }
    }

    @Override
    public void drawOnPanning(Canvas canvas, PointF currentMouseOffset) {
        if (isMeasuring()) {
            DrawItem draw = mRulerItem;

            if (!mIsMoving)
                draw = mRulerItem.pan(currentMouseOffset);

            draw.drawLines(canvas, true, true, false, false);
        }
    }

    @Override
    public void drawOnZooming(Canvas canvas, PointF currentFocusLocation, float scale) {
        if (isMeasuring())
            mRulerItem.zoom(currentFocusLocation, scale).drawLines(canvas, true, true, false, false);
    }

    @Override
    public void onLongPress(MotionEvent event) {

    }

    @Override
    public void onSingleTapUp(MotionEvent event) {
        if (isMeasuring()) {
            double dMinX = event.getX() - mTolerancePX;
            double dMaxX = event.getX() + mTolerancePX;
            double dMinY = event.getY() - mTolerancePX;
            double dMaxY = event.getY() + mTolerancePX;
            GeoEnvelope screenEnv = new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY);

            if (mRulerItem.intersectsVertices(screenEnv)) {
                mMapViewOverlays.buffer();
                mMapViewOverlays.postInvalidate();
                return;
            }

            if (mRulerItem.getSelectedRing() == null)
                mRulerItem.addVertices(new float[] {event.getX(), event.getY()});
            else {
                mRulerItem.addNewPoint(event.getX(), event.getY());
                mRulerItem.setSelectedPoint(mRulerItem.getSelectedRing().length - 2);
            }

            fillGeometry();
            mMapViewOverlays.buffer();
            mMapViewOverlays.postInvalidate();
        }
    }

    @Override
    public void panStart(MotionEvent event) {
        double dMinX = event.getX() - mTolerancePX * 2 - DrawItem.mAnchorTolerancePX;
        double dMaxX = event.getX() + mTolerancePX;
        double dMinY = event.getY() - mTolerancePX * 2 - DrawItem.mAnchorTolerancePX;
        double dMaxY = event.getY() + mTolerancePX;
        GeoEnvelope screenEnv = new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY);

        if (mRulerItem.isTapNearSelectedPoint(screenEnv)) {
            PointF tempPoint = mRulerItem.getSelectedPoint();
            mTempPointOffset = new PointF(tempPoint.x - event.getX(), tempPoint.y - event.getY());
            mMapViewOverlays.setLockMap(true);
            mIsMoving = true;
        }
    }

    @Override
    public void panMoveTo(MotionEvent e) {
        if (mIsMoving)
            mRulerItem.setSelectedPointCoordinates(e.getX() + mTempPointOffset.x, e.getY() + mTempPointOffset.y);
    }

    @Override
    public void panStop() {
        if (mIsMoving) {
            mMapViewOverlays.setLockMap(false);
            mIsMoving = false;
            fillGeometry();
            mMapViewOverlays.buffer();
            mMapViewOverlays.postInvalidate();
        }
    }

    @Override
    public void onLayerAdded(int id) {

    }

    @Override
    public void onLayerDeleted(int id) {

    }

    @Override
    public void onLayerChanged(int id) {

    }

    @Override
    public void onExtentChanged(float zoom, GeoPoint center) {

    }

    @Override
    public void onLayersReordered() {

    }

    @Override
    public void onLayerDrawFinished(int id, float percent) {

    }

    @Override
    public void onLayerDrawStarted() {

    }
}

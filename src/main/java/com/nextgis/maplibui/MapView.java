/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplibui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Scroller;
import com.nextgis.maplib.api.MapEventListener;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;

import static com.nextgis.maplibui.util.Constants.*;

public class MapView
        extends MapViewBase
        implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,  ScaleGestureDetector.OnScaleGestureListener,
                   MapEventListener
{
    protected final GestureDetector      mGestureDetector;
    protected final ScaleGestureDetector mScaleGestureDetector;
    protected       PointF               mStartMouseLocation;
    protected       PointF               mCurrentMouseOffset;
    protected       PointF               mCurrentFocusLocation;
    protected       int                  mDrawingState;
    protected       double               mScaleFactor;
    protected       double               mCurrentSpan;
    protected       Scroller             mScroller;
    protected       long                 mStartDrawTime;

    //display redraw timeout ms
    public static final int DISPLAY_REDRAW_TIMEOUT = 850;


    public MapView(
            Context context,
            MapDrawable map)
    {
        super(context, map);

        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setOnDoubleTapListener(this);

        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);

        mScroller = new Scroller(context);

        mStartMouseLocation = new PointF();
        mCurrentMouseOffset = new PointF();
        mCurrentFocusLocation = new PointF();

        mDrawingState = DRAW_SATE_drawing;
    }


    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if (mMap != null) {
            mMap.addListener(this);
        }
    }


    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        if (mMap != null) {
            mMap.removeListener(this);
        }
    }


    @Override
    protected void onDraw(Canvas canvas)
    {
        //Log.d(TAG, "state: " + mDrawingState + ", current loc: " +  mCurrentMouseOffset.toString() + " current focus: " + mCurrentFocusLocation.toString() + " scale: "  + mScaleFactor);

        if (mMap != null) {

            switch (mDrawingState) {

                case DRAW_SATE_panning:
                case DRAW_SATE_panning_fling:
                    canvas.drawBitmap(
                            mMap.getView(-mCurrentMouseOffset.x, -mCurrentMouseOffset.y, true), 0,
                            0, null);
                    break;

                case DRAW_SATE_zooming:
                    canvas.drawBitmap(
                            mMap.getView(-mCurrentFocusLocation.x, -mCurrentFocusLocation.y,
                                         (float) mScaleFactor), 0, 0, null);
                    break;

                case DRAW_SATE_drawing_noclearbk:
                    canvas.drawBitmap(mMap.getView(false), 0, 0, null);
                    break;

                case DRAW_SATE_drawing:
                    canvas.drawBitmap(mMap.getView(true), 0, 0, null);
                    break;

                default: // mDrawingState == DRAW_SATE_none
                    break;
            }

        } else {
            super.onDraw(canvas);
        }
    }

    protected void zoomStart(ScaleGestureDetector scaleGestureDetector){
        if(mDrawingState == DRAW_SATE_zooming)
            return;

        mMap.cancelDraw();
        mDrawingState = DRAW_SATE_zooming;
        mCurrentSpan = scaleGestureDetector.getCurrentSpan();
        mCurrentFocusLocation.set(-scaleGestureDetector.getFocusX(), -scaleGestureDetector.getFocusY());
        mScaleFactor = 1.f;
    }

    protected void zoom(ScaleGestureDetector scaleGestureDetector){
        if(mDrawingState == DRAW_SATE_zooming && mMap != null) {
            double scaleFactor = scaleGestureDetector.getCurrentSpan() / mCurrentSpan;
            double zoom = getZoomForScaleFactor(scaleFactor);
            if(zoom < mMap.getMinZoom() || zoom > mMap.getMaxZoom())
                return;

            mScaleFactor = scaleFactor;
            invalidate();
        }
    }

    protected float getZoomForScaleFactor(double scale){
        if(mMap == null)
            return 1;
        float zoom = mMap.getZoomLevel();

        if(scale > 1){
            zoom = (float) (mMap.getZoomLevel() + lg(scale));
        }
        else if(scale < 1){
            zoom = (float) (mMap.getZoomLevel() - lg( 1 / scale));
        }
        return zoom;
    }

    protected void zoomStop(){
        if(mDrawingState == DRAW_SATE_zooming && mMap != null) {
            //mDrawingState = DRAW_SATE_none;

            float zoom = getZoomForScaleFactor(mScaleFactor);

            GeoEnvelope env = mMap.getFullBounds();
            GeoPoint focusPt = new GeoPoint(-mCurrentFocusLocation.x, -mCurrentFocusLocation.y);

            double invertScale = 1 / mScaleFactor;

            double offX = (1 - invertScale) * focusPt.getX();
            double offY = (1 - invertScale) * focusPt.getY();
            env.scale(invertScale);
            env.offset(offX, offY);

            GeoPoint newCenterPt = env.getCenter();
            GeoPoint newCenterPtMap = mMap.screenToMap(newCenterPt);

            mMap.setZoomAndCenter(zoom, newCenterPtMap);
        }
    }

    protected void panStart(final MotionEvent e){
        if (mDrawingState == DRAW_SATE_zooming
            || mDrawingState == DRAW_SATE_panning)
            return;

        mStartMouseLocation.set(e.getX(), e.getY());
        mCurrentMouseOffset.set(0, 0);
        mMap.cancelDraw();
        mDrawingState = DRAW_SATE_panning;
    }

    protected void panMoveTo(final MotionEvent e){

        if(mDrawingState == DRAW_SATE_panning && mMap != null){
            float x = mStartMouseLocation.x - e.getX();
            float y = mStartMouseLocation.y - e.getY();

            //Log.d(TAG, "panMoveTo x - " + x + " y - " + y);

            GeoEnvelope bounds = mMap.getFullBounds();
            bounds.offset(x, y);

            GeoEnvelope limits = mMap.getLimits();

            if (bounds.getMinY() <= limits.getMinY() || bounds.getMaxY() >= limits.getMaxY()) {
                y = mCurrentMouseOffset.y;
            }

            if (bounds.getMinX() <= limits.getMinX() || bounds.getMaxX() >= limits.getMaxX()) {
                x = mCurrentMouseOffset.x;
            }

            mCurrentMouseOffset.set(x, y);
            invalidate();
        }
    }

    protected void panStop(){
        if(mDrawingState == DRAW_SATE_panning && mMap != null) {
            //mDrawingState = DRAW_SATE_none;

            float x = mCurrentMouseOffset.x;
            float y = mCurrentMouseOffset.y;

            //Log.d(TAG, "panStop x - " + x + " y - " + y);

            GeoEnvelope bounds = mMap.getFullBounds();
            bounds.offset(x, y);
            GeoEnvelope mapBounds = mMap.screenToMap(bounds);

            GeoPoint pt = mapBounds.getCenter();
            //GeoPoint screenPt = mDisplay.mapToScreen(new GeoPoint(mapBounds.getMinX(), mapBounds.getMinY()));
            //Log.d(TAG, "panStop. x: " + x + ", y:" + y + ", sx:" + screenPt.getX() + ", sy:" + screenPt.getY());
            //mDisplay.panStop((float) screenPt.getX(), (float) screenPt.getY());

            mMap.setZoomAndCenter(mMap.getZoomLevel(), pt);
        }
    }



    // delegate the event to the gesture detector
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);

        if (!mGestureDetector.onTouchEvent(event)) {
            switch (event.getAction()) { //TODO: get action can be more complicated: if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN)
                case MotionEvent.ACTION_DOWN:
                    if (!mScroller.isFinished()){
                        mScroller.abortAnimation();
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    break;

                case MotionEvent.ACTION_UP:
                    panStop();
                    break;

                default:
                    break;
            }
        }

        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        //Log.d(TAG, "onDown: " + e.toString());
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if(mMap == null)
            return false;
        float x = mCurrentMouseOffset.x;
        float y = mCurrentMouseOffset.y;
        GeoEnvelope bounds = mMap.getLimits();
        mDrawingState = DRAW_SATE_panning_fling;

        mScroller.fling((int)x, (int)y, -(int)velocityX, -(int)velocityY, (int)bounds.getMinX(), (int)bounds.getMaxX(), (int)bounds.getMinY(), (int)bounds.getMaxY());
        return true;
    }

    @Override
    public void computeScroll()
    {
        if (mScroller.computeScrollOffset() && mMap != null)
        {
            float x = mScroller.getCurrX();
            float y = mScroller.getCurrY();

            GeoEnvelope bounds = mMap.getFullBounds();
            bounds.offset(x, y);

            GeoEnvelope limits = mMap.getLimits();

            if (bounds.getMinY() <= limits.getMinY() || bounds.getMaxY() >= limits.getMaxY()) {
                y = mCurrentMouseOffset.y;
            }

            if (bounds.getMinX() <= limits.getMinX() || bounds.getMaxX() >= limits.getMaxX()) {
                x = mCurrentMouseOffset.x;
            }

            mCurrentMouseOffset.set(x, y);

            postInvalidate();
        }
        else if(mDrawingState == DRAW_SATE_panning_fling && mScroller.isFinished()){
            mDrawingState = DRAW_SATE_panning;
            panStop();
        }
    }

    @Override
    public void onLongPress(MotionEvent event) {

    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        //Log.d(TAG, "onScroll: " + event1.toString() + ", " + event2.toString() + ", "
        //        + distanceX + ", " + distanceY);

        panStart(event1);
        panMoveTo(event2);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
//        Log.d(TAG, "onSingleTapUp: " + e.toString());
        return false;
    }

    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        if(mMap == null)
            return false;

        mDrawingState = DRAW_SATE_zooming;
        mScaleFactor = 2;
        mCurrentFocusLocation.set(-e.getX(), -e.getY());
        invalidate();

        GeoEnvelope env = mMap.getFullBounds();
        GeoPoint focusPt = new GeoPoint(-mCurrentFocusLocation.x, -mCurrentFocusLocation.y);

        double invertScale = 1 / mScaleFactor;

        double offX = (1 - invertScale) * focusPt.getX();
        double offY = (1 - invertScale) * focusPt.getY();
        env.scale(invertScale);
        env.offset(offX, offY);

        GeoPoint newCenterPt = env.getCenter();
        GeoPoint newCenterPtMap = mMap.screenToMap(newCenterPt);

        setZoomAndCenter((float)Math.ceil(getZoomLevel() + 0.5), newCenterPtMap);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(final MotionEvent e) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        return false;
    }


    @Override
    public void zoomIn() {
        mDrawingState = DRAW_SATE_zooming;
        mScaleFactor = 2;
        mCurrentFocusLocation.set(-getWidth() / 2, -getHeight() / 2);
        invalidate();
        super.zoomIn();
    }

    @Override
    public void zoomOut() {
        mDrawingState = DRAW_SATE_zooming;
        mScaleFactor = 0.5;
        mCurrentFocusLocation.set(-getWidth() / 2, -getHeight() / 2);
        invalidate();
        super.zoomOut();
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        zoom(scaleGestureDetector);
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        zoomStart(scaleGestureDetector);
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        zoomStop();
    }

    public static double lg(double x) {
        return Math.log(x)/Math.log(2.0);
    }


    @Override
    public void onLayerAdded(int id)
    {
        if(mMap != null) {
            mMap.runDraw(null);
            mStartDrawTime = System.currentTimeMillis();
        }
    }


    @Override
    public void onLayerDeleted(int id)
    {
        if(mMap != null) {
            mMap.runDraw(null);
            mStartDrawTime = System.currentTimeMillis();
        }
    }


    @Override
    public void onLayerChanged(int id)
    {
        if(mMap != null) {
            mMap.runDraw(null);
            mStartDrawTime = System.currentTimeMillis();
        }
    }


    @Override
    public void onExtentChanged(
            float zoom,
            GeoPoint center)
    {
        if(mMap != null) {
            mMap.runDraw(null);
            mStartDrawTime = System.currentTimeMillis();
        }
    }


    @Override
    public void onLayersReordered()
    {
        if(mMap != null) {
            mMap.runDraw(null);
            mStartDrawTime = System.currentTimeMillis();
        }
    }


    @Override
    public void onLayerDrawFinished(
            int id,
            float percent)
    {
        mDrawingState = DRAW_SATE_drawing_noclearbk;
        if(System.currentTimeMillis() - mStartDrawTime > DISPLAY_REDRAW_TIMEOUT){
            mStartDrawTime = System.currentTimeMillis();
            invalidate();
        }
        else if(percent >= 1){
            invalidate();
        }
    }
}

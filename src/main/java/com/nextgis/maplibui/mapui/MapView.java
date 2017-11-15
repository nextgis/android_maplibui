/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.mapui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import com.nextgis.maplib.api.MapEventListener;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplibui.api.MapViewEventListener;

import java.util.Timer;
import java.util.TimerTask;

import static com.nextgis.maplib.util.Constants.DRAW_FINISH_ID;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplibui.util.ConstantsUI.DRAW_STATE_drawing;
import static com.nextgis.maplibui.util.ConstantsUI.DRAW_STATE_drawing_noclearbk;
import static com.nextgis.maplibui.util.ConstantsUI.DRAW_STATE_panning;
import static com.nextgis.maplibui.util.ConstantsUI.DRAW_STATE_panning_fling;
import static com.nextgis.maplibui.util.ConstantsUI.DRAW_STATE_zooming;


public class MapView
        extends MapViewBase
        implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,
                   ScaleGestureDetector.OnScaleGestureListener, MapEventListener
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
    private Timer mTimer;
    private InvalidateTask mInvalidateTask;
    final Handler uiHandler = new Handler();

    //display redraw timeout ms
    public static final int DISPLAY_REDRAW_TIMEOUT = 750;

    class InvalidateTask extends TimerTask {

        @Override
        public void run() {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDrawingState = DRAW_STATE_drawing;
                    setZoomAndCenter(getZoomLevel(), getMapCenter());
                }
            });
        }
    }

    public void scheduleInvalidate() {
        if (mTimer != null) {
            mTimer.cancel();
        }

        mTimer = new Timer();
        mInvalidateTask = new InvalidateTask();
        mTimer.schedule(mInvalidateTask, DISPLAY_REDRAW_TIMEOUT);
    }

    public MapView(
            Context context,
            MapDrawable map)
    {
        super(context, map);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setOnDoubleTapListener(this);

        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);

        mScroller = new Scroller(context);

        mStartMouseLocation = new PointF();
        mCurrentMouseOffset = new PointF();
        mCurrentFocusLocation = new PointF();

        mDrawingState = DRAW_STATE_drawing_noclearbk;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility == VISIBLE && mMap.isDirty()) {
            mMap.setDirty(false);
            drawMapDrawable();
        }
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if (mMap != null) {
            mMap.addListener(this);
        }

        scheduleInvalidate();
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
    protected synchronized void onDraw(Canvas canvas)
    {
        //Log.d(TAG, "state: " + mDrawingState + ", current loc: " +  mCurrentMouseOffset.toString() + " current focus: " + mCurrentFocusLocation.toString() + " scale: "  + mScaleFactor);

        if (mMap != null) {

            switch (mDrawingState) {

                case DRAW_STATE_panning:
                case DRAW_STATE_panning_fling:
                    mMap.draw(canvas, -mCurrentMouseOffset.x, -mCurrentMouseOffset.y, true);
                    break;

                case DRAW_STATE_zooming:
                    mMap.draw(
                            canvas, -mCurrentFocusLocation.x, -mCurrentFocusLocation.y,
                            (float) mScaleFactor);
                    break;
//TODO: add invalidate rect to prevent flicker
                case DRAW_STATE_drawing_noclearbk:
                    mMap.draw(canvas, 0, 0, false);
                    break;

                case DRAW_STATE_drawing:
                    mMap.draw(canvas, 0, 0, true);
                    break;

                //case DRAW_STATE_none:
                //    break;

                default:
                    break;
            }

        } else {
            super.onDraw(canvas);
        }
    }


    protected void zoomStart(ScaleGestureDetector scaleGestureDetector)
    {

        if (mDrawingState == DRAW_STATE_zooming) {
            return;
        }

        mDrawingState = DRAW_STATE_zooming;
        mCurrentSpan = scaleGestureDetector.getCurrentSpan();
        mCurrentFocusLocation.set(
                -scaleGestureDetector.getFocusX(), -scaleGestureDetector.getFocusY());
        mScaleFactor = 1.f;

        mMap.buffer(0, 0, 1);
    }


    protected void zoom(ScaleGestureDetector scaleGestureDetector)
    {
        if (mDrawingState != DRAW_STATE_zooming) {
            zoomStart(scaleGestureDetector);
        }


        if (mDrawingState == DRAW_STATE_zooming && mMap != null) {
            double scaleFactor =
                    scaleGestureDetector.getScaleFactor() * scaleGestureDetector.getCurrentSpan() /
                    mCurrentSpan;
            double zoom = MapUtil.getZoomForScaleFactor(scaleFactor, mMap.getZoomLevel());
            if (zoom < mMap.getMinZoom() || zoom > mMap.getMaxZoom()) {
                return;
            }

            mScaleFactor = scaleFactor;
            mMap.buffer(0, 0, 1);
            invalidate();
        }
    }

    protected void zoomStop()
    {
        if (mDrawingState == DRAW_STATE_zooming && mMap != null) {

            float zoom = MapUtil.getZoomForScaleFactor(mScaleFactor, mMap.getZoomLevel());

            GeoEnvelope env = mMap.getFullScreenBounds();
            GeoPoint focusPt = new GeoPoint(-mCurrentFocusLocation.x, -mCurrentFocusLocation.y);

            double invertScale = 1 / mScaleFactor;

            double offX = (1 - invertScale) * focusPt.getX();
            double offY = (1 - invertScale) * focusPt.getY();
            env.scale(invertScale);
            env.offset(offX, offY);

            GeoPoint newCenterPt = env.getCenter();
            GeoPoint newCenterPtMap = mMap.screenToMap(newCenterPt);

            if(Constants.DEBUG_MODE) {
                Log.d(TAG, "zoomStop: setZoomAndCenter");
            }

            setZoomAndCenter(zoom, newCenterPtMap);
        }
    }


    protected void panStart(final MotionEvent e)
    {

        if (mDrawingState == DRAW_STATE_zooming || mDrawingState == DRAW_STATE_panning ||
            mDrawingState == DRAW_STATE_panning_fling) {
            return;
        }

        //Log.d(TAG, "panStart");
        for (MapViewEventListener listener : mListeners) {
            if (null != listener) {
                listener.panStart(e);
            }
        }

        mDrawingState = DRAW_STATE_panning;
        mStartMouseLocation.set(e.getX(), e.getY());
        mCurrentMouseOffset.set(0, 0);

        mMap.buffer(0, 0, 1);
    }


    protected void panMoveTo(final MotionEvent e)
    {

        if (mDrawingState == DRAW_STATE_zooming || mDrawingState == DRAW_STATE_drawing_noclearbk ||
            mDrawingState == DRAW_STATE_drawing) {
            return;
        }

        if (mDrawingState == DRAW_STATE_panning && mMap != null) {
            for (MapViewEventListener listener : mListeners) {
                if (null != listener) {
                    listener.panMoveTo(e);
                }
            }

            float x = mStartMouseLocation.x - e.getX();
            float y = mStartMouseLocation.y - e.getY();

            //Log.d(TAG, "panMoveTo x - " + x + " y - " + y);

            GeoEnvelope bounds = mMap.getFullScreenBounds();
            bounds.offset(x, y);

            GeoEnvelope limits = mMap.getLimits();

            if (bounds.getMinY() <= limits.getMinY() || bounds.getMaxY() >= limits.getMaxY()) {
                y = mCurrentMouseOffset.y;
            }

            if (bounds.getMinX() <= limits.getMinX() || bounds.getMaxX() >= limits.getMaxX()) {
                x = mCurrentMouseOffset.x;
            }

            mCurrentMouseOffset.set(x, y);
            mMap.buffer(0, 0, 1);
            invalidate();
        }
    }


    protected void panStop()
    {
        //Log.d(Constants.TAG, "panStop state: " + mDrawingState);

        if (mDrawingState == DRAW_STATE_panning && mMap != null) {

            float x = mCurrentMouseOffset.x;
            float y = mCurrentMouseOffset.y;

            if(Constants.DEBUG_MODE) {
                Log.d(TAG, "panStop x - " + x + " y - " + y);
            }

            GeoEnvelope bounds = mMap.getFullScreenBounds();
            bounds.offset(x, y);
            GeoEnvelope mapBounds = mMap.screenToMap(bounds);

            GeoPoint pt = mapBounds.getCenter();

            //Log.d(TAG, "panStop: setZoomAndCenter");

            setZoomAndCenter(getZoomLevel(), pt);

            for (MapViewEventListener listener : mListeners) {
                if (null != listener) {
                    listener.panStop();
                }
            }

        }
    }


    // delegate the event to the gesture detector
    @Override
    public boolean onTouchEvent(
            @NonNull
            MotionEvent event)
    {
        mScaleGestureDetector.onTouchEvent(event);

        if (!mGestureDetector.onTouchEvent(event)) {
            switch (event.getAction()) { //TODO: get action can be more complicated: if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN)
                case MotionEvent.ACTION_DOWN:
                    if (!mScroller.isFinished()) {
                        mScroller.forceFinished(true);
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
    public boolean onDown(MotionEvent e)
    {
        //Log.d(TAG, "onDown: " + e.toString());
        return true;
    }


    @Override
    public boolean onFling(
            MotionEvent e1,
            MotionEvent e2,
            float velocityX,
            float velocityY)
    {
        if (mMap == null) //fling not always exec panStop
        {
            return false;
        }

        if (mDrawingState == DRAW_STATE_zooming || mDrawingState == DRAW_STATE_drawing_noclearbk ||
            mDrawingState == DRAW_STATE_drawing) {
            return false;
        }

        float x = mCurrentMouseOffset.x;
        float y = mCurrentMouseOffset.y;
        GeoEnvelope bounds = mMap.getLimits();
        mDrawingState = DRAW_STATE_panning_fling;

        mScroller.forceFinished(true);

        mScroller.fling(
                (int) x, (int) y, -(int) velocityX, -(int) velocityY, (int) bounds.getMinX(),
                (int) bounds.getMaxX(), (int) bounds.getMinY(), (int) bounds.getMaxY());

        //Log.d(Constants.TAG, "Fling");

        postInvalidate();

        return true;
    }


    @Override
    public void computeScroll()
    {
        super.computeScroll();
        if (mDrawingState == DRAW_STATE_panning_fling && mMap != null) {
            if (mScroller.computeScrollOffset()) {
                if (mScroller.isFinished()) {
                    mDrawingState = DRAW_STATE_panning;
                    panStop();
                } else {
                    float x = mScroller.getCurrX();
                    float y = mScroller.getCurrY();

                    GeoEnvelope bounds = mMap.getFullScreenBounds();
                    bounds.offset(x, y);

                    GeoEnvelope limits = mMap.getLimits();

                    if (bounds.getMinY() <= limits.getMinY() ||
                        bounds.getMaxY() >= limits.getMaxY()) {
                        y = mCurrentMouseOffset.y;
                    }

                    if (bounds.getMinX() <= limits.getMinX() ||
                        bounds.getMaxX() >= limits.getMaxX()) {
                        x = mCurrentMouseOffset.x;
                    }

                    mCurrentMouseOffset.set(x, y);

                    postInvalidate();
                }
            } else if (mScroller.isFinished()) {
                mDrawingState = DRAW_STATE_panning;
                panStop();
            }
        }
    }


    @Override
    public void onLongPress(MotionEvent event)
    {
        for (MapViewEventListener listener : mListeners) {
            if (null != listener) {
                listener.onLongPress(event);
            }
        }
    }


    @Override
    public boolean onScroll(
            MotionEvent event1,
            MotionEvent event2,
            float distanceX,
            float distanceY)
    {
        if (event2.getPointerCount() > 1) {
            return false;
        }
        //Log.d(TAG, "onScroll: " + event1.toString() + ", " + event2.toString() + ", "
        //           + distanceX + ", " + distanceY);

        panStart(event1);
        panMoveTo(event2);
        return true;
    }


    @Override
    public void onShowPress(MotionEvent e)
    {
    }


    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        return true;
    }


    @Override
    public boolean onDoubleTap(final MotionEvent e)
    {
        if (mMap == null) {
            return false;
        }

        mDrawingState = DRAW_STATE_zooming;
        mScaleFactor = 2;
        mCurrentFocusLocation.set(-e.getX(), -e.getY());
        //invalidate();

        GeoEnvelope env = mMap.getFullScreenBounds();
        GeoPoint focusPt = new GeoPoint(-mCurrentFocusLocation.x, -mCurrentFocusLocation.y);

        double invertScale = 1 / mScaleFactor;

        double offX = (1 - invertScale) * focusPt.getX();
        double offY = (1 - invertScale) * focusPt.getY();
        env.scale(invertScale);
        env.offset(offX, offY);

        GeoPoint newCenterPt = env.getCenter();
        GeoPoint newCenterPtMap = mMap.screenToMap(newCenterPt);

        //Log.d(TAG, "onDoubleTap: setZoomAndCenter");

        mMap.buffer(0, 0, 1);
        setZoomAndCenter((float) Math.ceil(getZoomLevel() + 0.5), newCenterPtMap);

        postInvalidate();

        return true;
    }


    @Override
    public boolean onDoubleTapEvent(final MotionEvent e)
    {
        return false;
    }


    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e)
    {
        //Log.d(Constants.TAG, "onSingleTapUp: " + e.toString());
        for (MapViewEventListener listener : mListeners) {
            if (null != listener) {
                listener.onSingleTapUp(e);
            }
        }
        return false;
    }


    @Override
    public void zoomIn()
    {
        mDrawingState = DRAW_STATE_zooming;
        mScaleFactor = 2;
        mCurrentFocusLocation.set(-getWidth() / 2, -getHeight() / 2);

        mMap.buffer(0, 0, 1); //TODO: zoom the buffer and just draw it, not draw with scale


//        scheduleInvalidate();

        super.zoomIn();

        postInvalidate();
    }


    @Override
    public void zoomOut()
    {
        mDrawingState = DRAW_STATE_zooming;
        mScaleFactor = 0.5;
        mCurrentFocusLocation.set(-getWidth() / 2, -getHeight() / 2);

        mMap.buffer(0, 0, 1); //TODO: zoom the buffer and just draw it, not draw with scale

//        scheduleInvalidate();

        super.zoomOut();

        postInvalidate();
    }


    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector)
    {
        //Log.d(TAG, "onScale");
        zoom(scaleGestureDetector);
        return true;
    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector)
    {
        zoomStart(scaleGestureDetector);
        return true;
    }


    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector)
    {
        zoomStop();
    }

    @Override
    public void onLayerAdded(int id)
    {
        drawMapDrawable();
    }


    @Override
    public void onLayerDeleted(int id)
    {
        drawMapDrawable();
    }


    @Override
    public void onLayerChanged(int id)
    {
        drawMapDrawable();
    }


    @Override
    public void onExtentChanged(
            float zoom,
            GeoPoint center)
    {
        drawMapDrawable();
    }


    @Override
    public void onLayersReordered()
    {
        drawMapDrawable();
    }


    public void drawMapDrawable()
    {
        if (mMap != null) {
            mDrawingState = DRAW_STATE_drawing;
            mStartDrawTime = System.currentTimeMillis();
            mMap.runDraw(null);
        }
    }


    @Override
    public synchronized void onLayerDrawFinished(int id, float percent)
    {
        if(Constants.DEBUG_MODE) {
            Log.d(TAG, "onLayerDrawFinished: " + id + " percent " + percent + " | draw state: " + mDrawingState);
        }

        if (mDrawingState > DRAW_STATE_drawing_noclearbk) {
            return;
        }

        if (System.currentTimeMillis() - mStartDrawTime > DISPLAY_REDRAW_TIMEOUT) {
            mStartDrawTime = System.currentTimeMillis();
            mMap.buffer(0, 0, 1);
            postInvalidate();

        } else if (id == DRAW_FINISH_ID && percent >= 1.0) {
            //Log.d(TAG, "LayerDrawFinished: id - " + id + ", percent - " + percent);

            mMap.buffer(0, 0, 1);
            postInvalidate();
        }
    }


    @Override
    public void onLayerDrawStarted()
    {

    }


    public void panTo(GeoPoint center)
    {
        Log.d(TAG, "panTo: setZoomAndCenter");
        setZoomAndCenter(getZoomLevel(), center);
    }
}

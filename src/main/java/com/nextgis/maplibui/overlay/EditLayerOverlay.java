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

package com.nextgis.maplibui.overlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.internal.widget.ThemeUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.IGeometryCacheItem;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryCollection;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoLinearRing;
import com.nextgis.maplib.datasource.GeoMultiLineString;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoMultiPolygon;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.EditEventListener;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.api.OverlayItem;
import com.nextgis.maplibui.fragment.BottomToolbar;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.util.ConstantsUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * The class for edit vector features
 */
public class EditLayerOverlay
        extends Overlay
        implements MapViewEventListener, GpsEventListener
{

    /**
     * overlay mode constants
     */
    public final static int MODE_NONE         = 0;
    public final static int MODE_HIGHLIGHT    = 1;
    public final static int MODE_EDIT         = 2;
    public final static int MODE_CHANGE       = 3;
    public final static int MODE_EDIT_BY_WALK = 4;

    /**
     * edit feature style
     */
    protected final static int VERTEX_RADIUS = 20;
    protected final static int EDGE_RADIUS   = 12;
    protected final static int LINE_WIDTH    = 4;

    protected Paint           mPaint;
    protected int             mFillColor;
    protected int             mOutlineColor;
    protected int             mSelectColor;
    protected final Bitmap          mAnchor;
    protected final float           mAnchorRectOffsetX, mAnchorRectOffsetY;
    protected final float mAnchorCenterX, mAnchorCenterY;


    protected VectorLayer     mLayer;
    protected EditLayerCacheItem mItem;
    protected int             mMode;

    protected DrawItems mDrawItems;

    protected List<EditEventListener> mListeners;

    protected static final int mType = 3;

    /**
     * Store keys
     */
    protected static final String BUNDLE_KEY_MODE          = "mode";
    protected static final String BUNDLE_KEY_LAYER         = "layer";
    protected static final String BUNDLE_KEY_FEATURE_ID    = "feature";
    protected static final String BUNDLE_KEY_SAVED_FEATURE = "feature_blob";
    protected static final String BUNDLE_KEY_HAS_EDITS     = "has_edits";
    protected static final String BUNDLE_KEY_OVERLAY_POINT = "overlay_point";

    protected final float mTolerancePX;
    protected final float mAnchorTolerancePX;

    protected PointF      mTempPointOffset;
    protected boolean     mHasEdits;
    protected OverlayItem mOverlayPoint;

    protected BottomToolbar mCurrentToolbar;

    protected float mCanvasCenterX, mCanvasCenterY;


    public EditLayerOverlay(
            Context context,
            MapViewOverlays mapViewOverlays)
    {
        super(context, mapViewOverlays);
        mLayer = null;
        mItem = null;
        mMode = MODE_NONE;

        mTolerancePX =
                context.getResources().getDisplayMetrics().density * ConstantsUI.TOLERANCE_DP;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mFillColor = ThemeUtils.getThemeAttrColor(mContext, R.attr.colorAccent);
        mOutlineColor = Color.BLACK;
        mSelectColor = Color.RED;

        mAnchor =
                BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_anchor);
        mAnchorRectOffsetX = -mAnchor.getWidth() * 0.05f;
        mAnchorRectOffsetY = -mAnchor.getHeight() * 0.05f;
        mAnchorCenterX = mAnchor.getWidth() * 0.75f;
        mAnchorCenterY = mAnchor.getHeight() * 0.75f;
        mAnchorTolerancePX = mAnchor.getScaledWidth(context.getResources().getDisplayMetrics());

        mDrawItems = new DrawItems();

        mListeners = new LinkedList<>();

        mMapViewOverlays.addListener(this);
        mOverlayPoint = new OverlayItem(mMapViewOverlays.getMap(), 0, 0, getMarker());
        mHasEdits = false;
    }


    public boolean setMode(int mode)
    {
        if((mode == MODE_EDIT || mode == MODE_EDIT_BY_WALK) && null != mLayer &&
            (mLayer.getGeometryType() == GeoConstants.GTGeometryCollection ||
            mLayer.getGeometryType() == GeoConstants.GTMultiPolygon ||
            mLayer.getGeometryType() == GeoConstants.GTMultiLineString)){
            return false;
        }

        if (mode == MODE_EDIT) {
            for (EditEventListener listener : mListeners) {
                listener.onStartEditSession();
            }
            mDrawItems.setSelectedPointIndex(0);
            mDrawItems.setSelectedRing(0);
            if(null != mLayer && null != mItem) {
                mLayer.hideFeature(mItem.getFeatureId());
            }
            mMapViewOverlays.postInvalidate();
        } else if (mode == MODE_NONE) {
            mDrawItems.setSelectedPointIndex(Constants.NOT_FOUND);
            mDrawItems.setSelectedRing(Constants.NOT_FOUND);
            if(null != mLayer && null != mItem) {
                mLayer.showAllFeatures();
            }
            mLayer = null;
            mItem = null;
            mMapViewOverlays.postInvalidate();
        } else if (mode == MODE_HIGHLIGHT) {
            if(null != mLayer && null != mItem) {
                mLayer.showFeature(mItem.getFeatureId());
            }
        } else if (mode == MODE_EDIT_BY_WALK) {
            for (EditEventListener listener : mListeners) {
                listener.onStartEditSession();
            }

            mDrawItems.setSelectedPointIndex(0);
            mDrawItems.setSelectedRing(0);
            if(null != mLayer && null != mItem) {
                mLayer.hideFeature(mItem.getFeatureId());
            }
        }

        mMode = mode;
        hideOverlayPoint();

        return true;
    }


    public void setFeature(
            VectorLayer layer,
            long featureId)
    {

        mLayer = layer;
        if(featureId == Constants.NOT_FOUND){
            mItem = null;
            if(null == mLayer)
                setMode(MODE_NONE);
        }
        else {
            mItem = new EditLayerCacheItem(featureId);
            fillDrawItems(mItem.getGeometry());
            setMode(MODE_HIGHLIGHT);
        }
    }


    public void setOverlayPoint(MotionEvent event) {
        GeoPoint mapPoint = mMapViewOverlays.getMap().screenToMap(new GeoPoint(event.getX(), event.getY()));
        mapPoint.setCRS(GeoConstants.CRS_WEB_MERCATOR);
        mapPoint.project(GeoConstants.CRS_WGS84);
        mOverlayPoint.setCoordinates(mapPoint);
        mOverlayPoint.setVisible(true);
    }


    public void createPointFromOverlay() {
        if (mOverlayPoint != null && mOverlayPoint.isVisible() && mLayer != null) {
            setHasEdits(true);


            GeoPoint point = mOverlayPoint.getCoordinates(GeoConstants.CRS_WEB_MERCATOR);
            if (mLayer.getGeometryType() == GeoConstants.GTMultiPoint) {
                GeoMultiPoint mpt = new GeoMultiPoint();
                mpt.add(point);
                mItem = new EditLayerCacheItem(Constants.NOT_FOUND, mpt);
            } else {
                mItem = new EditLayerCacheItem(Constants.NOT_FOUND, point);
            }

            mDrawItems.clear();
            GeoPoint[] geoPoints = new GeoPoint[1];
            geoPoints[0] = point;
            mDrawItems.addItems(0, mapToScreen(geoPoints), DrawItems.TYPE_VERTEX);
            mDrawItems.setSelectedRing(0);
            mDrawItems.setSelectedPointIndex(0);
            mDrawItems.fillGeometry(0, mItem.getGeometry(), mMapViewOverlays.getMap());

            updateMap();
        }
    }


    public void hideOverlayPoint() {
        mOverlayPoint.setVisible(false);
    }


    protected Bitmap getMarker() {
        float scaledDensity = mContext.getResources().getDisplayMetrics().scaledDensity;
        int size = (int) (12 * scaledDensity);
        Bitmap marker = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(marker);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        //noinspection deprecation
        p.setColor(mContext.getResources().getColor(R.color.accent));
        p.setAlpha(128);
        c.drawOval(new RectF(0, 0, size * 3 / 4, size * 3 / 4), p);
        return marker;
    }


    public long getSelectedItemId()
    {
        if (mItem == null) {
            return Constants.NOT_FOUND;
        }

        return mItem.getFeatureId();
    }


    public VectorLayer getSelectedLayer()
    {
        return mLayer;
    }


    @Override
    public void draw(
            Canvas canvas,
            MapDrawable mapDrawable)
    {
        if (mOverlayPoint.isVisible())
            drawOverlayItem(canvas, mOverlayPoint);

        if (null == mItem || mMode == MODE_CHANGE) {
            return;
        }

        GeoGeometry geom = mItem.getGeometry();
        if (null == geom) {
            return;
        }

        fillDrawItems(geom);

        switch (mItem.getGeometryType()) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                mDrawItems.drawPoints(canvas);
                break;
            case GeoConstants.GTLineString:
            case GeoConstants.GTMultiLineString:
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
                mDrawItems.drawLines(canvas);
                break;
            default:
                break;
        }

        drawCross(canvas);
    }

    protected float[] mapToScreen(GeoPoint[] geoPoints){
        return mMapViewOverlays.getMap().mapToScreen(geoPoints);
    }

    protected void fillDrawItems(GeoGeometry geom)
    {
        mDrawItems.clear();

        if(null == geom){
            Log.w(Constants.TAG, "the geometry is null in fillDrawItems method");
            return;
        }

        GeoPoint[] geoPoints;
        float[] points;
        GeoLineString lineString;
        switch (geom.getType()) {
            case GeoConstants.GTPoint:
                geoPoints = new GeoPoint[1];
                geoPoints[0] = (GeoPoint) geom;
                points = mapToScreen(geoPoints);
                mDrawItems.addItems(0, points, DrawItems.TYPE_VERTEX);
                break;
            case GeoConstants.GTMultiPoint:
                GeoMultiPoint geoMultiPoint = (GeoMultiPoint) geom;
                geoPoints = new GeoPoint[geoMultiPoint.size()];
                for (int i = 0; i < geoMultiPoint.size(); i++) {
                    geoPoints[i] = geoMultiPoint.get(i);
                }
                points = mapToScreen(geoPoints);
                mDrawItems.addItems(0, points, DrawItems.TYPE_VERTEX);
                break;
            case GeoConstants.GTLineString:
                lineString = (GeoLineString) geom;
                fillDrawLine(0, lineString);
                break;
            case GeoConstants.GTMultiLineString:
                GeoMultiLineString multiLineString = (GeoMultiLineString)geom;
                for(int i = 0; i < multiLineString.size(); i++){
                    fillDrawLine(i, multiLineString.get(i));
                }
                break;
            case GeoConstants.GTPolygon:
                GeoPolygon polygon = (GeoPolygon) geom;
                fillDrawPolygon(polygon);
                break;
            case GeoConstants.GTMultiPolygon:
                GeoMultiPolygon multiPolygon = (GeoMultiPolygon)geom;
                for(int i = 0; i < multiPolygon.size(); i++){
                    fillDrawPolygon(multiPolygon.get(i));
                }
                break;
            case GeoConstants.GTGeometryCollection:
                GeoGeometryCollection collection = (GeoGeometryCollection)geom;
                for(int i = 0; i < collection.size(); i++){
                    GeoGeometry geoGeometry = collection.get(i);
                    fillDrawItems(geoGeometry);
                }
                break;
            default:
                break;
        }
    }


    protected void fillDrawPolygon(GeoPolygon polygon)
    {
        fillDrawRing(0, polygon.getOuterRing());
        for (int i = 0; i < polygon.getInnerRingCount(); i++) {
            fillDrawRing(i + 1, polygon.getInnerRing(i));
        }
    }


    protected void fillDrawLine(int ring, GeoLineString lineString)
    {
        GeoPoint[] geoPoints =
                lineString.getPoints().toArray(new GeoPoint[lineString.getPointCount()]);
        float[] points = mapToScreen(geoPoints);

        if (points.length < 2) {
            return;
        }

        mDrawItems.addItems(ring, points, DrawItems.TYPE_VERTEX);
        float[] edgePoints = new float[points.length - 2];
        for (int i = 0; i < points.length - 2; i++) {
            edgePoints[i] = (points[i] + points[i + 2]) * .5f;
        }
        mDrawItems.addItems(ring, edgePoints, DrawItems.TYPE_EDGE);
    }


    protected void fillDrawRing(int ring, GeoLinearRing geoLinearRing)
    {
        GeoPoint[] geoPoints =
                geoLinearRing.getPoints().toArray(new GeoPoint[geoLinearRing.getPointCount()]);
        float[] points = mapToScreen(geoPoints);
        mDrawItems.addItems(ring, points, DrawItems.TYPE_VERTEX);
        float[] edgePoints = new float[points.length];

        if (points.length == 0 || edgePoints.length < 2) {
            return;
        }

        for (int i = 0; i < points.length - 2; i++) {
            edgePoints[i] = (points[i] + points[i + 2]) * .5f;
        }
        edgePoints[edgePoints.length - 2] = (points[0] + points[points.length - 2]) * .5f;
        edgePoints[edgePoints.length - 1] = (points[1] + points[points.length - 1]) * .5f;
        mDrawItems.addItems(ring, edgePoints, DrawItems.TYPE_EDGE);
    }


    @Override
    public void drawOnPanning(
            Canvas canvas,
            PointF currentMouseOffset)
    {
        if (mOverlayPoint.isVisible())
            drawOnPanning(canvas, currentMouseOffset, mOverlayPoint);

        if (null == mItem) {
            return;
        }

        DrawItems drawItems = mDrawItems;
        if (mMode != MODE_CHANGE) {
            drawItems = mDrawItems.pan(currentMouseOffset);
        }

        switch (mItem.getGeometryType()) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                drawItems.drawPoints(canvas);
                break;
            case GeoConstants.GTLineString:
            case GeoConstants.GTMultiLineString:
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
                drawItems.drawLines(canvas);
                break;
            default:
                break;
        }

        drawCross(canvas);
    }


    @Override
    public void drawOnZooming(
            Canvas canvas,
            PointF currentFocusLocation,
            float scale)
    {
        if (mOverlayPoint.isVisible())
            drawOnZooming(canvas, currentFocusLocation, scale, mOverlayPoint, false);

        if (null == mItem) {
            return;
        }

        DrawItems drawItems = mDrawItems.zoom(currentFocusLocation, scale);

        switch (mItem.getGeometryType()) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                drawItems.drawPoints(canvas);
                break;
            case GeoConstants.GTLineString:
            case GeoConstants.GTMultiLineString:
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
                drawItems.drawLines(canvas);
                break;
            default:
                break;
        }

        drawCross(canvas);
    }


    protected void drawCross(Canvas canvas)
    {
        if (mMode != MODE_EDIT) {
            return;
        }
        mCanvasCenterX = canvas.getWidth() / 2;
        mCanvasCenterY = canvas.getHeight() / 2;

        mPaint.setColor(mSelectColor);
        mPaint.setStrokeWidth(LINE_WIDTH / 2);
        canvas.drawLine(
                mCanvasCenterX - mTolerancePX, mCanvasCenterY, mCanvasCenterX + mTolerancePX,
                mCanvasCenterY, mPaint);
        canvas.drawLine(
                mCanvasCenterX, mCanvasCenterY - mTolerancePX, mCanvasCenterX,
                mCanvasCenterY + mTolerancePX, mPaint);
    }


    public void addListener(EditEventListener listener)
    {
        if (mListeners != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }


    public void removeListener(EditEventListener listener)
    {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }


    protected void setToolbarSaveState(boolean save)
    {
        if(null == mCurrentToolbar)
            return;

        if (save) {
            mCurrentToolbar.setNavigationIcon(R.drawable.ic_action_save);
            mCurrentToolbar.setNavigationContentDescription(R.string.save);
        } else {
            mCurrentToolbar.setNavigationIcon(R.drawable.ic_action_apply_dark);
            mCurrentToolbar.setNavigationContentDescription(R.string.apply);
        }

        //hide cancel button as it has no sense at this state
        Menu toolbarMenu = mCurrentToolbar.getMenu();
        if (null != toolbarMenu) {
            for (int i = 0; i < toolbarMenu.size(); i++) {
                MenuItem item = toolbarMenu.getItem(i);
                if (item.getItemId() == R.id.menu_cancel) {
                    item.setVisible(save);
                    break;
                }
            }
        }
    }


    protected float[] getNewGeometry(
            int geometryType,
            GeoPoint screenCenter)
    {
        float[] geoPoints;
        float add = mTolerancePX * 2;
        switch (geometryType) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                geoPoints = new float[2];
                geoPoints[0] = (float) screenCenter.getX();
                geoPoints[1] = (float) screenCenter.getY();
                return geoPoints;
            case GeoConstants.GTLineString:
                geoPoints = new float[4];
                geoPoints[0] = (float) screenCenter.getX() - add;
                geoPoints[1] = (float) screenCenter.getY() - add;
                geoPoints[2] = (float) screenCenter.getX() + add;
                geoPoints[3] = (float) screenCenter.getY() + add;
                return geoPoints;
            case GeoConstants.GTMultiLineString:
                break;
            case GeoConstants.GTPolygon:
                geoPoints = new float[6];
                geoPoints[0] = (float) screenCenter.getX() - add;
                geoPoints[1] = (float) screenCenter.getY() - add;
                geoPoints[2] = (float) screenCenter.getX() - add;
                geoPoints[3] = (float) screenCenter.getY() + add;
                geoPoints[4] = (float) screenCenter.getX() + add;
                geoPoints[5] = (float) screenCenter.getY() + add;
                return geoPoints;
            case GeoConstants.GTMultiPolygon:
                break;
            default:
                break;
        }
        return null;
    }

    protected void setHasEdits(boolean hasEdits){
        mHasEdits = hasEdits;
        setToolbarSaveState(hasEdits);
    }

    protected void moveSelectedPoint(float x, float y){
        setHasEdits(true);

        mDrawItems.setSelectedPoint(x, y);
        mDrawItems.fillGeometry(0, mItem.getGeometry(), mMapViewOverlays.getMap());

        updateMap();
    }

    protected void fillGeometry(){
        MapDrawable mapDrawable = mMapViewOverlays.getMap();
        mDrawItems.fillGeometry(0, mItem.getGeometry(), mapDrawable);
    }

    public void setToolbar(final BottomToolbar toolbar)
    {
        if (null == toolbar || null == mLayer) {
            return;
        }

        mCurrentToolbar = toolbar;

        switch (mMode) {
            case MODE_EDIT:
                toolbar.setNavigationOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                if (mHasEdits) {
                                    saveEdits();
                                } else {
                                    for (EditEventListener listener : mListeners) {
                                        listener.onFinishEditSession();
                                    }

                                    setMode(MODE_NONE);
                                }
                            }
                        });

                if (toolbar.getMenu() != null) {
                    toolbar.getMenu().clear();
                }

                switch (mLayer.getGeometryType()) {
                    case GeoConstants.GTPoint:
                        toolbar.inflateMenu(R.menu.edit_point);
                        break;
                    case GeoConstants.GTMultiPoint:
                        toolbar.inflateMenu(R.menu.edit_multipoint);
                        break;
                    case GeoConstants.GTLineString:
                        toolbar.inflateMenu(R.menu.edit_line);
                        break;
                    case GeoConstants.GTMultiLineString:
                        //toolbar.inflateMenu(R.menu.edit_multiline);
                        break;
                    case GeoConstants.GTPolygon:
                        toolbar.inflateMenu(R.menu.edit_polygon);
                        break;
                    case GeoConstants.GTMultiPolygon:
                        //toolbar.inflateMenu(R.menu.edit_multipolygon);
                        break;
                    case GeoConstants.GTGeometryCollection:
                    default:
                        break;
                }

                if (isCurrentGeometryValid() && mItem.getFeatureId() == Constants.NOT_FOUND) {
                    setToolbarSaveState(true);
                } else {
                    setToolbarSaveState(false);
                }

                toolbar.setOnMenuItemClickListener(
                        new BottomToolbar.OnMenuItemClickListener()
                        {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem)
                            {
                                if (null == mLayer) {
                                    return false;
                                }
                                /**
                                 * Edit attributes
                                 */
                                if (menuItem.getItemId() == R.id.menu_edit_attributes) {
                                    if (!isItemValid() || mHasEdits) {
                                        return false;
                                    }
                                    if (mLayer instanceof IVectorLayerUI) {
                                        // fill geometry with edits
                                        fillGeometry();
                                        IVectorLayerUI vectorLayerUI = (IVectorLayerUI) mLayer;
                                        vectorLayerUI.showEditForm(mContext, mItem.getFeatureId(), mItem.getGeometry());
                                    }
                                /**
                                 * Cancel edits
                                 */
                                } else if (menuItem.getItemId() == R.id.menu_cancel) {
                                    cancelEdits();
                                /**
                                 * Move point to center
                                 */
                                } else if (menuItem.getItemId() ==
                                           R.id.menu_edit_move_point_to_center) {
                                    if (!isItemValid()) {
                                        return false;
                                    }

                                    moveSelectedPoint(mCanvasCenterX, mCanvasCenterY);
                                /**
                                 * Move point to current location
                                 */
                                } else if (menuItem.getItemId() ==
                                           R.id.menu_edit_move_point_to_current_location) {
                                    if (!isItemValid()) {
                                        return false;
                                    }

                                    Activity parent = (Activity) mContext;
                                    GpsEventSource gpsEventSource =
                                            ((IGISApplication) parent.getApplication()).getGpsEventSource();
                                    Location location = gpsEventSource.getLastKnownLocation();
                                    if (null != location) {
                                        //change to screen coordinates
                                        GeoPoint pt = new GeoPoint(
                                                location.getLongitude(), location.getLatitude());
                                        pt.setCRS(GeoConstants.CRS_WGS84);
                                        pt.project(GeoConstants.CRS_WEB_MERCATOR);
                                        MapDrawable mapDrawable = mMapViewOverlays.getMap();
                                        GeoPoint screenPt = mapDrawable.mapToScreen(pt);
                                        moveSelectedPoint((float) screenPt.getX(), (float) screenPt.getY());
                                    }
                                /**
                                 * Add new point
                                 */
                                } else if (menuItem.getItemId() == R.id.menu_edit_add_new_point) {
                                    addGeometry(GeoConstants.GTPoint);
                                /**
                                 * Add new multipoint
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_add_new_multipoint) {
                                    addGeometry(GeoConstants.GTMultiPoint);
                                /**
                                 * Add new line
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_add_new_line) {
                                    addGeometry(GeoConstants.GTLineString);
                                /**
                                 * Add new polygon
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_add_new_polygon) {
                                    addGeometry(GeoConstants.GTPolygon);
                                /**
                                 * Delete geometry
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_delete_multipoint ||
                                        menuItem.getItemId() == R.id.menu_edit_delete_line ||
                                        menuItem.getItemId() == R.id.menu_edit_delete_polygon) {
                                    if (!isItemValid()) {
                                        return false;
                                    }

                                    setHasEdits(true);

                                    mDrawItems.clear();
                                    mItem.setGeometry(null);

                                    updateMap();
                                } else if (menuItem.getItemId() == R.id.menu_edit_delete_point) {
                                    if (!isItemValid()) {
                                        return false;
                                    }

                                    mDrawItems.deleteSelectedPoint();

                                    boolean hasEdits = mItem.getFeatureId() != Constants.NOT_FOUND;
                                    GeoGeometry geom = mDrawItems.fillGeometry(
                                            0, mItem.getGeometry(), mMapViewOverlays.getMap());
                                    hasEdits = hasEdits || geom != null;
                                    setHasEdits(hasEdits);

                                    if (null == geom) {
                                        mItem.setGeometry(null);
                                    }

                                    updateMap();
                                }
                        /*else if(menuItem.getItemId() == R.id.menu_edit_track){
                            if(mLayer.getGeometryType() == GeoConstants.GTPoint ||
                               mLayer.getGeometryType() == GeoConstants.GTMultiPoint)
                                return false;
                            //only support for lines and polygons

                            //TODO: release this
                        }*/
                                return true;
                            }
                        });
                break;
            case MODE_EDIT_BY_WALK:
                toolbar.setNavigationOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                stopGeometryByWalk();

                                if (isCurrentGeometryValid()) {
                                    saveEdits();
                                } else {
                                    Toast.makeText(
                                            mContext, R.string.geometry_by_walk_no_points,
                                            Toast.LENGTH_SHORT).show();
                                }

                                setMode(MODE_EDIT);
                                setToolbar(toolbar);

                            }
                        });

                setToolbarSaveState(false);
                startGeometryByWalk();
                break;
        }
    }


    protected boolean isItemValid() {
        return null != mItem && null != mItem.getGeometry();
    }


    protected void addGeometry(int geometryType) {
        boolean isGeometryTypesIdentical = mLayer.getGeometryType() == geometryType;
        if (!isItemValid())
            if (!isGeometryTypesIdentical)
                return;

        setHasEdits(true);

        MapDrawable mapDrawable = mMapViewOverlays.getMap();
        if (null == mapDrawable)
            return;

        GeoPoint center = mapDrawable.getFullScreenBounds().getCenter();
        if (isGeometryTypesIdentical)
            createNewGeometry(geometryType, center);
        else
            addGeometryToExistent(geometryType, center);

        //set new coordinates to GeoPoint from screen coordinates
        mDrawItems.fillGeometry(0, mItem.getGeometry(), mapDrawable);
        updateMap();
    }


    protected void addGeometryToExistent(int geometryType, GeoPoint center) {
        switch (geometryType) {
            case GeoConstants.GTPoint:
                mDrawItems.addNewPoint((float) center.getX(), (float) center.getY());
                break;
            case GeoConstants.GTLineString:
                break;
            case GeoConstants.GTPolygon:
                break;
        }
        //insert geometry in appropriate position
        int lastIndex = mDrawItems.getLastPointIndex();
        mDrawItems.setSelectedPointIndex(lastIndex);
    }


    protected void createNewGeometry(int geometryType, GeoPoint center) {
        switch (geometryType) {
            case GeoConstants.GTPoint:
                mItem = new EditLayerCacheItem(Constants.NOT_FOUND, new GeoPoint());
                break;
            case GeoConstants.GTMultiPoint:
                mItem = new EditLayerCacheItem(Constants.NOT_FOUND, new GeoMultiPoint());
                break;
            case GeoConstants.GTLineString:
                mItem = new EditLayerCacheItem(Constants.NOT_FOUND, new GeoLineString());
                break;
            case GeoConstants.GTMultiLineString:
                mItem = new EditLayerCacheItem(Constants.NOT_FOUND, new GeoMultiLineString());
                break;
            case GeoConstants.GTPolygon:
                mItem = new EditLayerCacheItem(Constants.NOT_FOUND, new GeoPolygon());
                break;
            case GeoConstants.GTMultiPolygon:
                mItem = new EditLayerCacheItem(Constants.NOT_FOUND, new GeoMultiPoint());
                break;
        }

        mDrawItems.clear();
        float[] geoPoints = getNewGeometry(geometryType, center);
        mDrawItems.addItems(0, geoPoints, DrawItems.TYPE_VERTEX);
        mDrawItems.setSelectedRing(0);
        mDrawItems.setSelectedPointIndex(0);
    }


    protected void updateMap()
    {
        mMapViewOverlays.buffer();
        mMapViewOverlays.postInvalidate();
    }


    protected void cancelEdits()
    {
        if (!mHasEdits) {
            return;
        }

        // restore
        setHasEdits(false);
        mMode = MODE_EDIT;
        if (mItem.getFeatureId() == Constants.NOT_FOUND) {
            mItem = null;
            mDrawItems.clear();
            mMapViewOverlays.postInvalidate();
        } else {
            mItem.restoreOriginalGeometry();
            fillDrawItems(mItem.getGeometry());
            updateMap();
        }
    }


    protected void saveEdits()
    {
        setHasEdits(false);
        mMode = MODE_EDIT;

        if (mItem == null) {
            return;
        }

        if (mItem.getGeometry() == null && mItem.getFeatureId() != Constants.NOT_FOUND) {
            mLayer.deleteAddChanges(mItem.getFeatureId());
        } else {
            //show attributes edit activity
            IVectorLayerUI vectorLayerUI = (IVectorLayerUI) mLayer;
            if (null != vectorLayerUI) {
                vectorLayerUI.showEditForm(mContext, mItem.getFeatureId(), mItem.getGeometry());
            }
            mLayer.showFeature(mItem.getFeatureId());
        }

        mDrawItems.clear();
        mItem = null;
    }


    @Override
    public Bundle onSaveState()
    {
        Bundle bundle = super.onSaveState();
        bundle.putInt(BUNDLE_KEY_TYPE, mType);
        bundle.putInt(BUNDLE_KEY_MODE, mMode);
        if (null == mLayer) {
            bundle.putInt(BUNDLE_KEY_LAYER, Constants.NOT_FOUND);
        } else {
            bundle.putInt(BUNDLE_KEY_LAYER, mLayer.getId());
        }

        if (null == mItem) {
            bundle.putLong(BUNDLE_KEY_FEATURE_ID, Constants.NOT_FOUND);
        } else {
            if (mItem.getFeatureId() == Constants.NOT_FOUND && mItem.getGeometry() != null) {
                try {
                    fillGeometry();
                    bundle.putByteArray(BUNDLE_KEY_SAVED_FEATURE, mItem.getGeometry().toBlob());
                    bundle.putBoolean(BUNDLE_KEY_HAS_EDITS, mHasEdits);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            bundle.putLong(BUNDLE_KEY_FEATURE_ID, mItem.getFeatureId());
        }

        if (mOverlayPoint.isVisible())
            bundle.putSerializable(BUNDLE_KEY_OVERLAY_POINT, mOverlayPoint.getCoordinates(GeoConstants.CRS_WGS84));

        return bundle;
    }


    @Override
    public void onRestoreState(Bundle bundle)
    {
        if (null != bundle) {
            int type = bundle.getInt(BUNDLE_KEY_TYPE);
            if (mType == type) {
                mMode = bundle.getInt(BUNDLE_KEY_MODE);
                int layerId = bundle.getInt(BUNDLE_KEY_LAYER);
                ILayer layer = mMapViewOverlays.getLayerById(layerId);
                mLayer = null;
                if (null != layer && layer instanceof VectorLayer) {
                    mLayer = (VectorLayer) layer;

                    long featureId = bundle.getLong(BUNDLE_KEY_FEATURE_ID);
                    if (featureId == Constants.NOT_FOUND) {
                        try {
                            mItem = new EditLayerCacheItem(Constants.NOT_FOUND,
                                    GeoGeometryFactory.fromBlob(
                                            bundle.getByteArray(BUNDLE_KEY_SAVED_FEATURE)));
                            mHasEdits = bundle.getBoolean(BUNDLE_KEY_HAS_EDITS);
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        mItem = new EditLayerCacheItem(featureId);
                    }
                } else {
                    mItem = null;
                }
            }

            if (bundle.containsKey(BUNDLE_KEY_OVERLAY_POINT)) {
                GeoPoint point = (GeoPoint) bundle.getSerializable(BUNDLE_KEY_OVERLAY_POINT);

                if (point != null) {
                    point.setCRS(GeoConstants.CRS_WGS84);
                    mOverlayPoint.setCoordinates(point);
                    mOverlayPoint.setVisible(true);
                }
            }
        }
        super.onRestoreState(bundle);
    }


    public void deleteItem()
    {
        if (null == mItem) {
            Log.d(Constants.TAG, "delete null item");
            return;
        }
        final long itemId = mItem.getFeatureId();

        if (null == mLayer) {
            Log.d(Constants.TAG, "delete from null layer");
            return;
        }

        setFeature(mLayer, Constants.NOT_FOUND);
        mLayer.hideFeature(itemId);

        new UndoBarController.UndoBar((android.app.Activity) mContext).message(
                mContext.getString(R.string.delete_done)).listener(
                new UndoBarController.AdvancedUndoListener()
                {
                    @Override
                    public void onHide(
                            @Nullable
                            Parcelable parcelable)
                    {
                        mLayer.deleteAddChanges(itemId);
                    }


                    @Override
                    public void onClear(
                            @NonNull
                            Parcelable[] parcelables)
                    {

                    }


                    @Override
                    public void onUndo(
                            @Nullable
                            Parcelable parcelable)
                    {
                        mLayer.showFeature(itemId);
                    }
                }).show();
    }


    @Override
    public void onLongPress(MotionEvent event)
    {
        //TODO: do we need some actions on long press on point or geometry?
    }


    /**
     * Select point in current geometry or new geometry from current layer
     *
     * @param event
     *         Motion event
     */
    @Override
    public void onSingleTapUp(MotionEvent event)
    {
        if (null == mLayer) {
            return;
        }

        selectGeometryInScreenCoordinates(event.getX(), event.getY());
    }


    protected void selectGeometryInScreenCoordinates(
            float x,
            float y)
    {
        double dMinX = x - mTolerancePX;
        double dMaxX = x + mTolerancePX;
        double dMinY = y - mTolerancePX;
        double dMaxY = y + mTolerancePX;
        GeoEnvelope screenEnv = new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY);

        if(null != mItem && null != mItem.getGeometry()) {
            //1. search current geometry point
            if (mDrawItems.intersects(screenEnv, mItem.getGeometry(), mMapViewOverlays.getMap())) {
                if (mMode == MODE_HIGHLIGHT) { // highlight same geometry
                    mDrawItems.setSelectedPointIndex(Constants.NOT_FOUND);
                    mDrawItems.setSelectedRing(Constants.NOT_FOUND);
                }
                else {
                    mMapViewOverlays.invalidate();
                }
                return;
            }

            if (mHasEdits) // prevent select another geometry before saving current edited one.
                return;
        }

        //2 select another geometry
        GeoEnvelope mapEnv = mMapViewOverlays.screenToMap(screenEnv);
        if (null == mapEnv) {
            return;
        }
        List<Long> items = mLayer.query(mapEnv);
        if (items.isEmpty()) {
            return;
        }

        long previousFeatureId = Constants.NOT_FOUND;
        if(null != mItem)
            previousFeatureId = mItem.getFeatureId();
        mItem = new EditLayerCacheItem(items.get(0));
        fillDrawItems(mItem.getGeometry());

        if (mMode == MODE_HIGHLIGHT) {
            mMapViewOverlays.invalidate();
            return;
        }

        // this part should execute only in edit mode
        mDrawItems.setSelectedPointIndex(0);
        mDrawItems.setSelectedRing(0);

        if(previousFeatureId == Constants.NOT_FOUND) {
            mLayer.hideFeature(mItem.getFeatureId());
        }
        else {
            mLayer.swapFeaturesVisibility(previousFeatureId, mItem.getFeatureId());
        }
    }


    @Override
    public void panStart(MotionEvent event)
    {
        if (mMode == MODE_EDIT) {

            if (null != mItem && null != mItem.getGeometry()) {

                //check if we are near selected point
                double dMinX = event.getX() - mTolerancePX * 2 - mAnchorTolerancePX;
                double dMaxX = event.getX() + mTolerancePX;
                double dMinY = event.getY() - mTolerancePX * 2 - mAnchorTolerancePX;
                double dMaxY = event.getY() + mTolerancePX;
                GeoEnvelope screenEnv = new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY);

                if (mDrawItems.isTapNearSelectedPoint(screenEnv)) {
                    PointF tempPoint = mDrawItems.getSelectedPoint();
                    mTempPointOffset =
                            new PointF(tempPoint.x - event.getX(), tempPoint.y - event.getY());
                    mMapViewOverlays.setLockMap(true);
                    mMode = MODE_CHANGE;
                }
            }
        }
    }


    @Override
    public void panMoveTo(MotionEvent e)
    {
        if (mMode == MODE_CHANGE) {
            mDrawItems.setSelectedPoint(
                    e.getX() + mTempPointOffset.x, e.getY() + mTempPointOffset.y);
        }
    }


    @Override
    public void panStop()
    {
        if (mMode == MODE_CHANGE) {
            mMapViewOverlays.setLockMap(false);

            setHasEdits(true);
            mMode = MODE_EDIT;
            mDrawItems.fillGeometry(0, mItem.getGeometry(), mMapViewOverlays.getMap());
            updateMap(); // redraw the map
        }
    }


    @Override
    public void onLayerAdded(int id)
    {

    }


    @Override
    public void onLayerDeleted(int id)
    {
        //if delete edited layer cancel edit session
        if (null != mLayer && mLayer.getId() == id) {
            setHasEdits(false);
            setMode(MODE_NONE);
        }
    }


    @Override
    public void onLayerChanged(int id)
    {

    }


    @Override
    public void onExtentChanged(
            float zoom,
            GeoPoint center)
    {

    }


    @Override
    public void onLayersReordered()
    {

    }


    @Override
    public void onLayerDrawFinished(
            int id,
            float percent)
    {

    }


    @Override
    public void onLayerDrawStarted()
    {

    }


    protected void startGeometryByWalk()
    {
        GeoGeometry geometry;

        if (mItem == null) {
            switch (mLayer.getGeometryType()) {
                case GeoConstants.GTLineString:
                    geometry = new GeoLineString();
                    break;
                case GeoConstants.GTPolygon:
                    geometry = new GeoPolygon();
                    break;
                default:
                    return;
            }
            mItem = new EditLayerCacheItem(Constants.NOT_FOUND, geometry);
        } else {
            if (isCurrentGeometryValid()) {
                setToolbarSaveState(true);
            }
        }

        Activity parent = (Activity) mContext;
        GpsEventSource gpsEventSource =
                ((IGISApplication) parent.getApplication()).getGpsEventSource();
        gpsEventSource.addListener(this);
    }


    public void stopGeometryByWalk()
    {
        Activity parent = (Activity) mContext;
        GpsEventSource gpsEventSource =
                ((IGISApplication) parent.getApplication()).getGpsEventSource();
        gpsEventSource.removeListener(this);
    }


    @Override
    public void onLocationChanged(Location location)
    {
        if (mItem != null && location != null) {
            GeoPoint currentPoint = new GeoPoint(location.getLongitude(), location.getLatitude());
            currentPoint.setCRS(GeoConstants.CRS_WGS84);
            currentPoint.project(GeoConstants.CRS_WEB_MERCATOR);

            //add point to mItem
            switch (mItem.getGeometry().getType()) {
                case GeoConstants.GTLineString:
                    GeoLineString line = (GeoLineString) mItem.getGeometry();
                    line.add(currentPoint);
                    break;
                case GeoConstants.GTPolygon:
                    GeoPolygon polygon = (GeoPolygon) mItem.getGeometry();
                    polygon.add(currentPoint);
                    break;
                default:
                    return;
            }

            GeoPoint screenPoint = mMapViewOverlays.getMap().mapToScreen(currentPoint);

            //add point drawItem
            mDrawItems.addNewPoint((float) screenPoint.getX(), (float) screenPoint.getY());

            if (isCurrentGeometryValid()) {
                setToolbarSaveState(true);
            } else {
                setToolbarSaveState(false);
            }
        }
    }


    @Override
    public void onBestLocationChanged(Location location)
    {

    }


    protected int getMinPointCount()
    {
        switch (mLayer.getGeometryType()) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                return 1;
            case GeoConstants.GTLineString:
            case GeoConstants.GTMultiLineString:
                return 2;
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
                return 3;
            default:
                return 1;
        }
    }


    protected boolean isCurrentGeometryValid()
    {
        if (null == mItem || null == mItem.getGeometry() ||
            mLayer.getGeometryType() != mItem.getGeometry().getType()) {
            return false;
        }

        switch (mLayer.getGeometryType()) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                return true;
            case GeoConstants.GTLineString:
                GeoLineString line = (GeoLineString) mItem.getGeometry();
                return line.getPointCount() > 1;
            case GeoConstants.GTMultiLineString:
                GeoMultiLineString multiLine = (GeoMultiLineString) mItem.getGeometry();
                for (int i = 0; i < multiLine.size(); i++) {
                    GeoLineString subLine = multiLine.get(i);
                    if (subLine.getPointCount() > 1) {
                        return true;
                    }
                }
                return false;
            case GeoConstants.GTPolygon:
                GeoPolygon polygon = (GeoPolygon) mItem.getGeometry();
                GeoLinearRing ring = polygon.getOuterRing();
                return ring.getPointCount() > 2;
            case GeoConstants.GTMultiPolygon:
                GeoMultiPolygon multiPolygon = (GeoMultiPolygon) mItem.getGeometry();
                for (int i = 0; i < multiPolygon.size(); i++) {
                    GeoPolygon subPolygon = multiPolygon.get(i);
                    if (subPolygon.getOuterRing().getPointCount() > 2) {
                        return true;
                    }
                }
                return false;
            default:
                return false;
        }
    }


    @Override
    public void onGpsStatusChanged(int event)
    {

    }


    protected class DrawItems
    {
        List<float[]> mDrawItemsVertex;
        List<float[]> mDrawItemsEdge;

        public static final int TYPE_VERTEX = 1;
        public static final int TYPE_EDGE   = 2;

        protected int mSelectedRing, mSelectedPointIndex;


        public DrawItems()
        {
            mDrawItemsVertex = new ArrayList<>();
            mDrawItemsEdge = new ArrayList<>();

            mSelectedRing = Constants.NOT_FOUND;
            mSelectedPointIndex = Constants.NOT_FOUND;
        }


        public void setSelectedRing(int selectedRing)
        {
            if (selectedRing >= mDrawItemsVertex.size()) {
                return;
            }
            mSelectedRing = selectedRing;
        }


        public void setSelectedPointIndex(int selectedPoint)
        {
            mSelectedPointIndex = selectedPoint;
        }


        public int getSelectedPointIndex()
        {
            return mSelectedPointIndex;
        }


        public void addItems(
                int ring,
                float[] points,
                int type)
        {
            if (type == TYPE_VERTEX) {
                mDrawItemsVertex.add(ring, points);
            } else if (type == TYPE_EDGE) {
                mDrawItemsEdge.add(ring, points);
            }
        }


        public void clear()
        {
            mDrawItemsVertex.clear();
            mDrawItemsEdge.clear();
        }


        public DrawItems zoom(
                PointF location,
                float scale)
        {
            DrawItems drawItems = new DrawItems();
            drawItems.setSelectedRing(mSelectedRing);
            drawItems.setSelectedPointIndex(mSelectedPointIndex);

            int count = 0;
            for (float[] items : mDrawItemsVertex) {
                float[] newItems = new float[items.length];
                for (int i = 0; i < items.length - 1; i += 2) {
                    newItems[i] = items[i] - (1 - scale) * (items[i] + location.x);
                    newItems[i + 1] = items[i + 1] - (1 - scale) * (items[i + 1] + location.y);
                }
                drawItems.addItems(count++, newItems, TYPE_VERTEX);
            }

            count = 0;
            for (float[] items : mDrawItemsEdge) {
                float[] newItems = new float[items.length];
                for (int i = 0; i < items.length - 1; i += 2) {
                    newItems[i] = items[i] - (1 - scale) * (items[i] + location.x);
                    newItems[i + 1] = items[i + 1] - (1 - scale) * (items[i + 1] + location.y);
                }
                drawItems.addItems(count++, newItems, TYPE_EDGE);
            }

            return drawItems;
        }


        public DrawItems pan(PointF offset)
        {
            DrawItems drawItems = new DrawItems();
            drawItems.setSelectedRing(mSelectedRing);
            drawItems.setSelectedPointIndex(mSelectedPointIndex);

            int count = 0;
            for (float[] items : mDrawItemsVertex) {
                float[] newItems = new float[items.length];
                for (int i = 0; i < items.length - 1; i += 2) {
                    newItems[i] = items[i] - offset.x;
                    newItems[i + 1] = items[i + 1] - offset.y;
                }
                drawItems.addItems(count++, newItems, TYPE_VERTEX);
            }

            count = 0;
            for (float[] items : mDrawItemsEdge) {
                float[] newItems = new float[items.length];
                for (int i = 0; i < items.length - 1; i += 2) {
                    newItems[i] = items[i] - offset.x;
                    newItems[i + 1] = items[i + 1] - offset.y;
                }
                drawItems.addItems(count++, newItems, TYPE_EDGE);
            }

            return drawItems;
        }


        public void drawPoints(Canvas canvas)
        {
            for (float[] items : mDrawItemsVertex) {

                mPaint.setColor(mOutlineColor);
                mPaint.setStrokeWidth(VERTEX_RADIUS + 2);
                canvas.drawPoints(items, mPaint);

                mPaint.setColor(mFillColor);
                mPaint.setStrokeWidth(VERTEX_RADIUS);
                canvas.drawPoints(items, mPaint);
            }

            //draw selected point
            if (mSelectedPointIndex != Constants.NOT_FOUND) {

                float[] items = getSelectedRing();
                if (null != items) {
                    mPaint.setColor(mSelectColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS);

                    canvas.drawPoint(
                            items[mSelectedPointIndex], items[mSelectedPointIndex + 1], mPaint);

                    float anchorX = items[mSelectedPointIndex] + mAnchorRectOffsetX;
                    float anchorY = items[mSelectedPointIndex + 1] + mAnchorRectOffsetY;
                    canvas.drawBitmap(mAnchor, anchorX, anchorY, null);
                }
            }
        }


        public void drawLines(Canvas canvas)
        {
            for (float[] items : mDrawItemsVertex) {

                mPaint.setColor(mFillColor);
                mPaint.setStrokeWidth(LINE_WIDTH);

                for (int i = 0; i < items.length - 3; i += 2) {
                    canvas.drawLine(items[i], items[i + 1], items[i + 2], items[i + 3], mPaint);
                }

                if (mItem.getGeometry().getType() == GeoConstants.GTPolygon ||
                    mItem.getGeometry().getType() == GeoConstants.GTMultiPolygon) {
                    if (items.length >= 2) {
                        canvas.drawLine(
                                items[0], items[1], items[items.length - 2],
                                items[items.length - 1], mPaint);
                    }
                }

                if (mMode == MODE_EDIT || mMode == MODE_CHANGE) {
                    mPaint.setColor(mOutlineColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS + 2);
                    canvas.drawPoints(items, mPaint);

                    mPaint.setColor(mFillColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS);
                    canvas.drawPoints(items, mPaint);
                }
            }

            if (mMode == MODE_EDIT) {
                for (float[] items : mDrawItemsEdge) {

                    mPaint.setColor(mOutlineColor);
                    mPaint.setStrokeWidth(EDGE_RADIUS + 2);
                    canvas.drawPoints(items, mPaint);

                    mPaint.setColor(mFillColor);
                    mPaint.setStrokeWidth(EDGE_RADIUS);
                    canvas.drawPoints(items, mPaint);
                }
            }

            //draw selected point
            if (mSelectedPointIndex != Constants.NOT_FOUND) {
                float[] items = getSelectedRing();
                if (null != items && mSelectedPointIndex + 1 < items.length) {
                    mPaint.setColor(mSelectColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS);

                    canvas.drawPoint(
                            items[mSelectedPointIndex], items[mSelectedPointIndex + 1], mPaint);

                    float anchorX = items[mSelectedPointIndex] + mAnchorRectOffsetX;
                    float anchorY = items[mSelectedPointIndex + 1] + mAnchorRectOffsetY;
                    canvas.drawBitmap(mAnchor, anchorX, anchorY, null);
                }
            }
        }


        public boolean intersects(GeoEnvelope screenEnv,
                                  GeoGeometry geometry,
                                  MapDrawable mapDrawable)
        {
            int point;
            for (int ring = 0; ring < mDrawItemsVertex.size(); ring++) {
                point = 0;
                float[] items = mDrawItemsVertex.get(ring);
                for (int i = 0; i < items.length - 1; i += 2) {
                    if (screenEnv.contains(new GeoPoint(items[i], items[i + 1]))) {
                        mSelectedRing = ring;
                        mSelectedPointIndex = point;
                        return true;
                    }
                    point += 2;

                }
            }

            if (mMode == MODE_EDIT) {
                for (int ring = 0; ring < mDrawItemsEdge.size(); ring++) {
                    point = 0;
                    float[] items = mDrawItemsEdge.get(ring);
                    for (int i = 0; i < items.length - 1; i += 2) {
                        if (screenEnv.contains(new GeoPoint(items[i], items[i + 1]))) {
                            mSelectedPointIndex = i + 2;
                            mSelectedRing = ring;
                            insertNewPoint(mSelectedPointIndex, items[i], items[i + 1]);

                            //fill geometry
                            fillGeometry(0, geometry, mapDrawable);

                            return true;
                        }
                        point++;
                    }
                }
            }
            return false;
        }


        public PointF getSelectedPoint()
        {
            float[] points = getSelectedRing();
            if (null == points || mSelectedPointIndex < 0 || points.length <= mSelectedPointIndex) {
                return null;
            }
            return new PointF(points[mSelectedPointIndex], points[mSelectedPointIndex + 1]);
        }


        public void addNewPoint(
                float x,
                float y)
        {
            float[] points = getSelectedRing();
            if (null == points) {
                return;
            }
            float[] newPoints = new float[points.length + 2];
            System.arraycopy(points, 0, newPoints, 0, points.length);
            newPoints[points.length] = x;
            newPoints[points.length + 1] = y;

            mDrawItemsVertex.set(mSelectedRing, newPoints);
        }


        public void insertNewPoint(
                int insertPosition,
                float x,
                float y)
        {
            float[] points = getSelectedRing();
            if (null == points) {
                return;
            }
            float[] newPoints = new float[points.length + 2];
            int count = 0;
            for (int i = 0; i < newPoints.length - 1; i += 2) {
                if (i == insertPosition) {
                    newPoints[i] = x;
                    newPoints[i + 1] = y;
                } else {
                    newPoints[i] = points[count++];
                    newPoints[i + 1] = points[count++];
                }
            }

            mDrawItemsVertex.set(mSelectedRing, newPoints);
        }


        protected float[] getSelectedRing()
        {
            if (mDrawItemsVertex.isEmpty() || mSelectedRing < 0 ||
                mSelectedRing >= mDrawItemsVertex.size()) {
                return null;
            }
            return mDrawItemsVertex.get(mSelectedRing);
        }


        public void deleteSelectedPoint()
        {
            float[] points = getSelectedRing();
            if (null == points || mSelectedPointIndex < 0) {
                return;
            }
            if (points.length <= getMinPointCount() * 2) {
                mDrawItemsVertex.remove(mSelectedRing);
                mSelectedRing--;
                mSelectedPointIndex = Constants.NOT_FOUND;
                return;
            }
            float[] newPoints = new float[points.length - 2];
            int counter = 0;
            for (int i = 0; i < points.length; i++) {
                if (i == mSelectedPointIndex || i == mSelectedPointIndex + 1) {
                    continue;
                }
                newPoints[counter++] = points[i];
            }

            if (mSelectedPointIndex >= newPoints.length) {
                mSelectedPointIndex = 0;
            }

            mDrawItemsVertex.set(mSelectedRing, newPoints);
        }


        public void setSelectedPoint(
                float x,
                float y)
        {
            float[] points = getSelectedRing();
            if (null != points && mSelectedPointIndex > Constants.NOT_FOUND) {
                points[mSelectedPointIndex] = x;
                points[mSelectedPointIndex + 1] = y;
            }
        }


        public GeoGeometry fillGeometry(
                int ring,
                GeoGeometry geometry,
                MapDrawable mapDrawable)
        {
            if (null == geometry || null == mapDrawable || ring < 0 ||
                ring >= mDrawItemsVertex.size()) {
                return null;
            }

            if (mDrawItemsVertex.isEmpty()) {
                return null;
            }

            GeoPoint[] points;
            switch (geometry.getType()) {
                case GeoConstants.GTPoint:
                    points = mapDrawable.screenToMap(mDrawItemsVertex.get(ring));
                    GeoPoint point = (GeoPoint) geometry;
                    point.setCoordinates(points[0].getX(), points[0].getY());
                    break;
                case GeoConstants.GTMultiPoint:
                    points = mapDrawable.screenToMap(mDrawItemsVertex.get(ring));
                    GeoMultiPoint multiPoint = (GeoMultiPoint) geometry;
                    multiPoint.clear();
                    for (GeoPoint geoPoint : points) {
                        multiPoint.add(geoPoint);
                    }
                    break;
                case GeoConstants.GTLineString:
                    points = mapDrawable.screenToMap(mDrawItemsVertex.get(ring));
                    GeoLineString lineString = (GeoLineString) geometry;
                    lineString.clear();
                    for (GeoPoint geoPoint : points) {
                        if (null == geoPoint) {
                            continue;
                        }
                        lineString.add(geoPoint);
                    }
                    break;
                case GeoConstants.GTMultiLineString:
                    GeoMultiLineString multiLineString = (GeoMultiLineString) geometry;

                    //  the geometry should be correspond the vertex list

                    for (int currentRing = 0; currentRing < multiLineString.size(); currentRing++) {
                        fillGeometry(
                                ring + currentRing, multiLineString.get(currentRing), mapDrawable);
                    }
                    break;
                case GeoConstants.GTPolygon:
                    points = mapDrawable.screenToMap(mDrawItemsVertex.get(ring));
                    GeoPolygon polygon = (GeoPolygon) geometry;
                    polygon.clear();
                    for (GeoPoint geoPoint : points) {
                        if (null == geoPoint) {
                            continue;
                        }
                        polygon.add(geoPoint);
                    }

                    fillGeometry(ring, polygon.getOuterRing(), mapDrawable);

                    //  the geometry should be correspond the vertex list

                    for (int currentRing = 0;
                         currentRing < polygon.getInnerRingCount();
                         currentRing++) {
                        fillGeometry(
                                ring + currentRing + 1, polygon.getInnerRing(currentRing),
                                mapDrawable);
                    }
                    break;
                case GeoConstants.GTMultiPolygon:
                    GeoMultiPolygon multiPolygon = (GeoMultiPolygon) geometry;

                    //  the geometry should be correspond the vertex list

                    int currentRing = 0;
                    for (int i = 0; i < multiPolygon.size(); i++) {
                        GeoPolygon geoPolygon = multiPolygon.get(i);
                        fillGeometry(ring + currentRing, geoPolygon, mapDrawable);
                        currentRing += 1 + geoPolygon.getInnerRingCount();
                    }
                    break;
                default:
                    break;
            }

            return geometry;
        }


        public int getLastPointIndex()
        {
            float[] points = getSelectedRing();
            if (null == points) {
                return Constants.NOT_FOUND;
            }
            if (points.length < 2) {
                return Constants.NOT_FOUND;
            }
            return points.length - 2;
        }


        public boolean isTapNearSelectedPoint(GeoEnvelope screenEnv)
        {
            float[] points = getSelectedRing();
            if (null != points && mSelectedPointIndex > Constants.NOT_FOUND &&
                points.length > mSelectedPointIndex + 1) {
                if (screenEnv.contains(
                        new GeoPoint(
                                points[mSelectedPointIndex], points[mSelectedPointIndex + 1]))) {
                    return true;
                }
            }
            return false;
        }
    }

    protected class EditLayerCacheItem implements IGeometryCacheItem{
        protected long mFeatureId;
        protected GeoGeometry mGeometry, mOriginalGeometry;

        public EditLayerCacheItem(long featureId){
            mFeatureId = featureId;
            mGeometry = mLayer.getGeometryForId(mFeatureId);
            saveOriginalGeometry();
        }

        public EditLayerCacheItem(long featureId, GeoGeometry geometry) {
            mFeatureId = featureId;
            mGeometry = geometry;
            saveOriginalGeometry();
        }

        public GeoGeometry getGeometry() {
            return mGeometry;
        }

        @Override
        public GeoEnvelope getEnvelope() {
            if(null != mGeometry)
                return mGeometry.getEnvelope();
            return null;
        }

        @Override
        public long getFeatureId() {
            return mFeatureId;
        }

        @Override
        public void setFeatureId(long id) {
            mFeatureId = id;
        }

        public void setGeometry(GeoGeometry geometry){
            mGeometry = geometry;

            if (geometry != null)
                saveOriginalGeometry();
        }

        private void saveOriginalGeometry() {
            if(null != mGeometry)
                mOriginalGeometry = mGeometry.copy();
        }

        public void restoreOriginalGeometry() {
            if(null != mOriginalGeometry)
                mGeometry = mOriginalGeometry.copy();
        }

        public int getGeometryType() {
            if(null != mGeometry)
                return mGeometry.getType();
            return GeoConstants.GTNone;
        }
    }
}

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.nextgis.maplibui.api.DrawItem;
import com.nextgis.maplibui.api.EditEventListener;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.api.OverlayItem;
import com.nextgis.maplibui.fragment.BottomToolbar;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.service.WalkEditService;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * The class for edit vector features
 */
public class EditLayerOverlay extends Overlay implements MapViewEventListener
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

    protected List<DrawItem> mDrawItems;
    protected DrawItem mSelectedItem;

    protected List<EditEventListener> mListeners;
    protected WalkEditReceiver mReceiver;

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

        mDrawItems = new ArrayList<>();
        mListeners = new LinkedList<>();

        mMapViewOverlays.addListener(this);
        mOverlayPoint = new OverlayItem(mMapViewOverlays.getMap(), 0, 0, getMarker());
        mHasEdits = false;
    }


    public boolean setMode(int mode)
    {
        if ((mode == MODE_EDIT || mode == MODE_EDIT_BY_WALK) && null != mLayer &&
            mLayer.getGeometryType() == GeoConstants.GTGeometryCollection)
            return false;

        if (mode == MODE_EDIT) {
            for (EditEventListener listener : mListeners)
                listener.onStartEditSession();

            if (null != mLayer && null != mItem)
                mLayer.hideFeature(mItem.getFeatureId());

            mMapViewOverlays.postInvalidate();
        } else if (mode == MODE_NONE) {
            if (null != mLayer && null != mItem)
                mLayer.showAllFeatures();

            mSelectedItem = null;
            mDrawItems.clear();
            mLayer = null;
            mItem = null;
            mMapViewOverlays.postInvalidate();
        } else if (mode == MODE_HIGHLIGHT) {
            if (null != mLayer && null != mItem)
                mLayer.showFeature(mItem.getFeatureId());
        } else if (mode == MODE_EDIT_BY_WALK) {
            for (EditEventListener listener : mListeners)
                listener.onStartEditSession();

            if (null != mLayer && null != mItem)
                mLayer.hideFeature(mItem.getFeatureId());
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
        if (featureId == Constants.NOT_FOUND) {
            mItem = null;
            if(null == mLayer)
                setMode(MODE_NONE);
        } else {
            mItem = new EditLayerCacheItem(featureId);
            fillDrawItems(mItem.getGeometry());
            setMode(MODE_HIGHLIGHT);
        }

        mMapViewOverlays.postInvalidate();
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
            mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, mapToScreen(geoPoints));
            mDrawItems.add(mSelectedItem);
            fillGeometry();

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
    public void draw(Canvas canvas, MapDrawable mapDrawable) {
        if (mOverlayPoint.isVisible())
            drawOverlayItem(canvas, mOverlayPoint);

        if (null == mItem || mMode == MODE_CHANGE)
            return;

        GeoGeometry geom = mItem.getGeometry();
        if (null == geom)
            return;

        fillDrawItems(geom);

        for (DrawItem drawItem : mDrawItems) {
            boolean isSelected = mSelectedItem == drawItem;
            drawItem(drawItem, canvas, isSelected);
        }

        drawCross(canvas);
    }

    protected float[] mapToScreen(GeoPoint[] geoPoints){
        return mMapViewOverlays.getMap().mapToScreen(geoPoints);
    }

    protected void fillDrawItems(GeoGeometry geom) {
        int lastItemsCount = mDrawItems.size();
        int lastSelectedItemPosition = mDrawItems.indexOf(mSelectedItem);
        DrawItem lastSelectedItem = mSelectedItem;
        mDrawItems.clear();

        if (null == geom) {
            Log.w(Constants.TAG, "the geometry is null in fillDrawItems method");
            return;
        }

        GeoPoint[] geoPoints;
        switch (geom.getType()) {
            case GeoConstants.GTPoint:
                geoPoints = new GeoPoint[1];
                geoPoints[0] = (GeoPoint) geom;
                mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, mapToScreen(geoPoints));
                mDrawItems.add(mSelectedItem);
                break;
            case GeoConstants.GTMultiPoint:
                GeoMultiPoint geoMultiPoint = (GeoMultiPoint) geom;
                geoPoints = new GeoPoint[geoMultiPoint.size()];
                for (int i = 0; i < geoMultiPoint.size(); i++)
                    geoPoints[i] = geoMultiPoint.get(i);

                mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, mapToScreen(geoPoints));
                mDrawItems.add(mSelectedItem);
                break;
            case GeoConstants.GTLineString:
                GeoLineString lineString = (GeoLineString) geom;
                fillDrawLine(lineString);
                break;
            case GeoConstants.GTMultiLineString:
                GeoMultiLineString multiLineString = (GeoMultiLineString) geom;
                for (int i = 0; i < multiLineString.size(); i++)
                    fillDrawLine(multiLineString.get(i));
                break;
            case GeoConstants.GTPolygon:
                GeoPolygon polygon = (GeoPolygon) geom;
                fillDrawPolygon(polygon);
                break;
            case GeoConstants.GTMultiPolygon:
                GeoMultiPolygon multiPolygon = (GeoMultiPolygon) geom;
                for (int i = 0; i < multiPolygon.size(); i++)
                    fillDrawPolygon(multiPolygon.get(i));
                break;
            case GeoConstants.GTGeometryCollection:
                GeoGeometryCollection collection = (GeoGeometryCollection) geom;
                for (int i = 0; i < collection.size(); i++) {
                    GeoGeometry geoGeometry = collection.get(i);
                    fillDrawItems(geoGeometry);
                }
                break;
            default:
                break;
        }

        if (mDrawItems.size() == lastItemsCount && lastSelectedItem != null &&
                lastSelectedItemPosition != Constants.NOT_FOUND) {
            mSelectedItem = mDrawItems.get(lastSelectedItemPosition);
            mSelectedItem.setSelectedRing(lastSelectedItem.getSelectedRingId());
            mSelectedItem.setSelectedPoint(lastSelectedItem.getSelectedPointId());
        }
    }


    protected void fillDrawPolygon(GeoPolygon polygon) {
        mSelectedItem = new DrawItem();

        fillDrawRing(polygon.getOuterRing());
        for (int i = 0; i < polygon.getInnerRingCount(); i++)
            fillDrawRing(polygon.getInnerRing(i));

        mDrawItems.add(mSelectedItem);
    }


    protected void fillDrawLine(GeoLineString lineString) {
        GeoPoint[] geoPoints =
                lineString.getPoints().toArray(new GeoPoint[lineString.getPointCount()]);
        float[] points = mapToScreen(geoPoints);

        if (points.length < 2)
            return;

        mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, points);
        mDrawItems.add(mSelectedItem);

        float[] edgePoints = new float[points.length - 2];
        for (int i = 0; i < points.length - 2; i++)
            edgePoints[i] = (points[i] + points[i + 2]) * .5f;

        mSelectedItem.addEdges(edgePoints);
    }


    protected void fillDrawRing(GeoLinearRing geoLinearRing) {
        GeoPoint[] geoPoints =
                geoLinearRing.getPoints().toArray(new GeoPoint[geoLinearRing.getPointCount()]);
        float[] points = mapToScreen(geoPoints);
        float[] edgePoints = new float[points.length];

        if (points.length == 0 || edgePoints.length < 2)
            return;

        for (int i = 0; i < points.length - 2; i++)
            edgePoints[i] = (points[i] + points[i + 2]) * .5f;

        edgePoints[edgePoints.length - 2] = (points[0] + points[points.length - 2]) * .5f;
        edgePoints[edgePoints.length - 1] = (points[1] + points[points.length - 1]) * .5f;

        mSelectedItem.addVertices(points);
        mSelectedItem.addEdges(edgePoints);
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

        List<DrawItem> drawItems = mDrawItems;
        for (DrawItem drawItem : drawItems) {
            boolean isSelected = mSelectedItem == drawItem;

            if (mMode != MODE_CHANGE)
                drawItem = drawItem.pan(currentMouseOffset);

            drawItem(drawItem, canvas, isSelected);
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

        List<DrawItem> drawItems = mDrawItems;
        for (DrawItem drawItem : drawItems) {
            boolean isSelected = mSelectedItem == drawItem;
            drawItem = drawItem.zoom(currentFocusLocation, scale);
            drawItem(drawItem, canvas, isSelected);
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
            case GeoConstants.GTMultiLineString:
                geoPoints = new float[4];
                geoPoints[0] = (float) screenCenter.getX() - add;
                geoPoints[1] = (float) screenCenter.getY() - add;
                geoPoints[2] = (float) screenCenter.getX() + add;
                geoPoints[3] = (float) screenCenter.getY() + add;
                return geoPoints;
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
                geoPoints = new float[6];
                geoPoints[0] = (float) screenCenter.getX() - add;
                geoPoints[1] = (float) screenCenter.getY() - add;
                geoPoints[2] = (float) screenCenter.getX() - add;
                geoPoints[3] = (float) screenCenter.getY() + add;
                geoPoints[4] = (float) screenCenter.getX() + add;
                geoPoints[5] = (float) screenCenter.getY() + add;
                return geoPoints;
            case GeoConstants.GTLinearRing:
                geoPoints = new float[6];
                geoPoints[0] = (float) screenCenter.getX() + add;
                geoPoints[1] = (float) screenCenter.getY() + add;
                geoPoints[2] = (float) screenCenter.getX() - add;
                geoPoints[3] = (float) screenCenter.getY() + add;
                geoPoints[4] = (float) screenCenter.getX() - add;
                geoPoints[5] = (float) screenCenter.getY() - add;
                return geoPoints;
            default:
                break;
        }
        return null;
    }

    protected void setHasEdits(boolean hasEdits){
        mHasEdits = hasEdits;
        setToolbarSaveState(hasEdits);
    }

    protected void moveSelectedPoint(float x, float y) {
        setHasEdits(true);

        mSelectedItem.setSelectedPointCoordinates(x, y);
        fillGeometry();

        updateMap();
    }


    protected void movePointToLocation() {
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
    }


    protected GeoGeometry fillGeometry() {
        if (mItem == null)
            return null;

        GeoGeometry geometry = mItem.getGeometry();
        MapDrawable mapDrawable = mMapViewOverlays.getMap();
        if (null == geometry || null == mapDrawable ||
                mDrawItems.isEmpty() || mSelectedItem == null)
            return null;

        float[] points;
        GeoPoint[] geoPoints;
        switch (geometry.getType()) {
            case GeoConstants.GTPoint:
                geoPoints = mapDrawable.screenToMap(mSelectedItem.getSelectedRing());
                GeoPoint point = (GeoPoint) geometry;
                point.setCoordinates(geoPoints[0].getX(), geoPoints[0].getY());
                break;
            case GeoConstants.GTMultiPoint:
                GeoMultiPoint multiPoint = (GeoMultiPoint) geometry;
                multiPoint.clear();

                for (DrawItem drawItem : mDrawItems) {
                    points = drawItem.getRing(0);
                    geoPoints = mapDrawable.screenToMap(points);
                    multiPoint.add(geoPoints[0]);
                }
                break;
            case GeoConstants.GTLineString:
                geoPoints = mapDrawable.screenToMap(mSelectedItem.getSelectedRing());
                GeoLineString lineString = (GeoLineString) geometry;
                lineString.clear();

                for (GeoPoint geoPoint : geoPoints) {
                    if (null == geoPoint)
                        continue;

                    lineString.add(geoPoint);
                }
                break;
            case GeoConstants.GTMultiLineString:
                GeoMultiLineString multiLineString = (GeoMultiLineString) geometry;
                GeoLineString line;
                multiLineString.clear();

                for (DrawItem drawItem : mDrawItems) {
                    geoPoints = mapDrawable.screenToMap(drawItem.getRing(0));
                    line = new GeoLineString();

                    for (GeoPoint geoPoint : geoPoints) {
                        if (null == geoPoint)
                            continue;

                        line.add(geoPoint);
                    }

                    multiLineString.add(line);
                }
                break;
            case GeoConstants.GTPolygon:
                if (mSelectedItem.getRingCount() == 0 || mSelectedItem.getRing(0) == null)
                    return null;

                geoPoints = mapDrawable.screenToMap(mSelectedItem.getRing(0));
                GeoPolygon polygon = (GeoPolygon) geometry;
                polygon.clear();

                for (GeoPoint geoPoint : geoPoints) {
                    if (null == geoPoint)
                        continue;

                    polygon.add(geoPoint);
                }

                for (int i = 1; i < mSelectedItem.getRingCount(); i++) {
                    geoPoints = mapDrawable.screenToMap(mSelectedItem.getRing(i));
                    GeoLinearRing ring = new GeoLinearRing();

                    for (GeoPoint geoPoint : geoPoints) {
                        if (null == geoPoint)
                            continue;

                        ring.add(geoPoint);
                    }

                    polygon.addInnerRing(ring);
                }
                break;
            case GeoConstants.GTMultiPolygon:
                GeoMultiPolygon multiPolygon = (GeoMultiPolygon) geometry;
                GeoPolygon geoPolygon;
                multiPolygon.clear();

                for (DrawItem drawItem : mDrawItems) {
                    geoPoints = mapDrawable.screenToMap(drawItem.getRing(0));
                    geoPolygon = new GeoPolygon();

                    for (GeoPoint geoPoint : geoPoints) {
                        if (null == geoPoint)
                            continue;

                        geoPolygon.add(geoPoint);
                    }

                    for (int i = 1; i < drawItem.getRingCount(); i++) {
                        geoPoints = mapDrawable.screenToMap(mSelectedItem.getRing(i));
                        GeoLinearRing ring = new GeoLinearRing();

                        for (GeoPoint geoPoint : geoPoints) {
                            if (null == geoPoint)
                                continue;

                            ring.add(geoPoint);
                        }

                        geoPolygon.addInnerRing(ring);
                    }

                    multiPolygon.add(geoPolygon);
                }
                break;
            default:
                break;
        }

        return geometry;
    }


    protected void drawItem(DrawItem drawItem, Canvas canvas, boolean isSelected) {
        isSelected = isSelected && mMode == MODE_EDIT;
        switch (mItem.getGeometryType()) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                drawPoints(drawItem, canvas, isSelected);
                break;
            case GeoConstants.GTLineString:
            case GeoConstants.GTMultiLineString:
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
                drawLines(drawItem, canvas, isSelected);
                break;
            default:
                break;
        }
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
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
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
                        toolbar.inflateMenu(R.menu.edit_multiline);
                        break;
                    case GeoConstants.GTPolygon:
                        toolbar.inflateMenu(R.menu.edit_polygon);
                        break;
                    case GeoConstants.GTMultiPolygon:
                        toolbar.inflateMenu(R.menu.edit_multipolygon);
                        break;
                    case GeoConstants.GTGeometryCollection:
                    default:
                        break;
                }

                setToolbarSaveState((isCurrentGeometryValid() && mItem.getFeatureId() == Constants.NOT_FOUND) || mHasEdits);

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

                                    movePointToLocation();
                                /**
                                 * Add new point
                                 */
                                } else if (menuItem.getItemId() == R.id.menu_edit_add_new_point) {
                                    return addGeometry(GeoConstants.GTPoint);
                                /**
                                 * Add new multipoint
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_add_new_multipoint) {
                                    return addGeometry(GeoConstants.GTMultiPoint);
                                /**
                                 * Add new line
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_add_new_line) {
                                    return addGeometry(GeoConstants.GTLineString);
                                /**
                                 * Add new multiline
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_add_new_multiline) {
                                    return addGeometry(GeoConstants.GTMultiLineString);
                                /**
                                 * Add new polygon
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_add_new_polygon) {
                                    return addGeometry(GeoConstants.GTPolygon);
                                /**
                                 * Add new multipolygon
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_add_new_multipolygon) {
                                    return addGeometry(GeoConstants.GTMultiPolygon);
                                /**
                                 * Add inner ring
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_add_new_inner_ring) {
                                    return addInnerRing();
                                /**
                                 * Delete inner ring
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_delete_inner_ring) {
                                    return deleteInnerRing();
                                /**
                                 * Delete geometry
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_delete_multipoint ||
                                        menuItem.getItemId() == R.id.menu_edit_delete_multiline ||
                                        menuItem.getItemId() == R.id.menu_edit_delete_multipolygon) {
                                    return deleteEntireGeometry();
                                /**
                                 * Delete geometry
                                 */
                                } else if (
                                        menuItem.getItemId() == R.id.menu_edit_delete_line ||
                                        menuItem.getItemId() == R.id.menu_edit_delete_polygon) {
                                    return deleteGeometry();
                                } else if (menuItem.getItemId() == R.id.menu_edit_delete_point) {
                                    return deletePoint();
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
                if (toolbar.getMenu() != null) {
                    toolbar.getMenu().clear();
                }

                toolbar.inflateMenu(R.menu.edit_by_walk);

                toolbar.setNavigationOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
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

                toolbar.setOnMenuItemClickListener(
                        new BottomToolbar.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                if (null == mLayer) {
                                    return false;
                                }

                                /**
                                 * create a new geometry during walk edit
                                 */
                                if (menuItem.getItemId() == R.id.menu_add_geometry) {
                                    Toast.makeText(mContext, mContext.getString(R.string.not_implemented), Toast.LENGTH_SHORT).show();
                                }
                                /**
                                 * cancel edit by walk
                                 */
                                else if (menuItem.getItemId() == R.id.menu_cancel) {
                                    stopGeometryByWalk();
                                    //mEditLayerOverlay.setFeature(
                                    //        mEditLayerOverlay.getSelectedLayer(), Constants.NOT_FOUND);
                                    setMode(MODE_EDIT);
                                    setToolbar(toolbar);
                                }
                                /**
                                 * show setting dialog
                                 */
                                else if (menuItem.getItemId() == R.id.menu_settings) {
                                    Activity parent = (Activity) mContext;
                                    IGISApplication app = (IGISApplication) parent.getApplication();
                                    app.showSettings(SettingsConstantsUI.ACTION_PREFS_LOCATION);
                                }

                                return true;
                            }
                        }
                );
                setToolbarSaveState(false);
                startGeometryByWalk();
                break;
        }
    }


    protected boolean isItemValid() {
        return null != mItem && null != mItem.getGeometry();
    }


    protected boolean deleteEntireGeometry() {
        if (!isItemValid())
            return false;

        mDrawItems.clear();
        mSelectedItem = null;
        mItem.setGeometry(fillGeometry());

        setHasEdits(true);
        updateMap();
        return true;
    }


    protected boolean deleteGeometry() {
        if (!isItemValid())
            return false;

        mDrawItems.remove(mSelectedItem);
        GeoGeometry geom = fillGeometry();
        if (null == geom)
            mItem.setGeometry(null);

        setHasEdits(true);
        updateMap();
        return true;
    }


    protected boolean deletePoint() {
        if (!isItemValid())
            return false;

        mSelectedItem.deleteSelectedPoint(mLayer);
        if (mSelectedItem.getRingCount() == 0)
            mDrawItems.remove(mSelectedItem);

        boolean hasEdits = mItem.getFeatureId() != Constants.NOT_FOUND;
        GeoGeometry geom = fillGeometry();
        hasEdits = hasEdits || geom != null;
        setHasEdits(hasEdits);

        if (null == geom)
            mItem.setGeometry(null);

        updateMap();
        return true;
    }


    protected boolean deleteInnerRing() {
        if (mSelectedItem != null && mSelectedItem.getSelectedRingId() != 0) {
            mSelectedItem.deleteSelectedRing(mLayer);
            setHasEdits(true);
            fillGeometry();
            updateMap();
            return true;
        }

        return false;
    }



    protected boolean addInnerRing() {
        MapDrawable mapDrawable = mMapViewOverlays.getMap();
        if (null == mapDrawable || mSelectedItem == null)
            return false;

        GeoPoint center = mapDrawable.getFullScreenBounds().getCenter();
        mSelectedItem.addVertices(getNewGeometry(GeoConstants.GTLinearRing, center));
        mSelectedItem.setSelectedRing(mSelectedItem.getRingCount() - 1);
        mSelectedItem.setSelectedPoint(0);
        setHasEdits(true);
        fillGeometry();
        updateMap();
        return true;
    }


    protected boolean addGeometry(int geometryType) {
        boolean isGeometryTypesIdentical = mLayer.getGeometryType() == geometryType;
        if (!isItemValid())
            if (!isGeometryTypesIdentical)
                return false;

        MapDrawable mapDrawable = mMapViewOverlays.getMap();
        if (null == mapDrawable)
            return false;

        GeoPoint center = mapDrawable.getFullScreenBounds().getCenter();
        if (isGeometryTypesIdentical)
            createNewGeometry(geometryType, center);
        else
            addGeometryToExistent(geometryType, center);

        setHasEdits(true);

        //set new coordinates to GeoPoint from screen coordinates
        fillGeometry();
        updateMap();
        return true;
    }


    protected void addGeometryToExistent(int geometryType, GeoPoint center) {
        //insert geometry in appropriate position
        switch (geometryType) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTLineString:
            case GeoConstants.GTPolygon:
                float[] geoPoints = getNewGeometry(geometryType, center);
                mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, geoPoints);
                mDrawItems.add(mSelectedItem);
                break;
        }
    }


    protected void createNewGeometry(int geometryType, GeoPoint center) {
        GeoGeometry geometry;
        switch (geometryType) {
            default:
            case GeoConstants.GTPoint:
                geometry = new GeoPoint();
                break;
            case GeoConstants.GTMultiPoint:
                geometry = new GeoMultiPoint();
                break;
            case GeoConstants.GTLineString:
                geometry = new GeoLineString();
                break;
            case GeoConstants.GTMultiLineString:
                geometry = new GeoMultiLineString();
                break;
            case GeoConstants.GTPolygon:
                geometry = new GeoPolygon();
                break;
            case GeoConstants.GTMultiPolygon:
                geometry = new GeoMultiPolygon();
                break;
        }

        if (!mHasEdits)
            mItem = new EditLayerCacheItem(Constants.NOT_FOUND, geometry);
        else
            mItem.setGeometry(geometry);

        mDrawItems.clear();
        float[] geoPoints = getNewGeometry(geometryType, center);
        mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, geoPoints);
        mDrawItems.add(mSelectedItem);
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

        if (mItem == null)
            return;

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

        mSelectedItem = null;
        mDrawItems.clear();
        mItem = null;
    }


    @Override
    public Bundle onSaveState()
    {
        Bundle bundle = super.onSaveState();
        bundle.putInt(BUNDLE_KEY_TYPE, mType);
        bundle.putInt(BUNDLE_KEY_MODE, mMode);
        bundle.putBoolean(BUNDLE_KEY_HAS_EDITS, mHasEdits);

        if (null == mLayer) {
            bundle.putInt(BUNDLE_KEY_LAYER, Constants.NOT_FOUND);
        } else {
            bundle.putInt(BUNDLE_KEY_LAYER, mLayer.getId());
        }

        if (null == mItem) {
            bundle.putLong(BUNDLE_KEY_FEATURE_ID, Constants.NOT_FOUND);
        } else {
            if (mItem.getGeometry() != null) {
                try {
                    fillGeometry();
                    bundle.putByteArray(BUNDLE_KEY_SAVED_FEATURE, mItem.getGeometry().toBlob());
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
                mHasEdits = bundle.getBoolean(BUNDLE_KEY_HAS_EDITS);
                int layerId = bundle.getInt(BUNDLE_KEY_LAYER);
                ILayer layer = mMapViewOverlays.getLayerById(layerId);
                mLayer = null;
                if (null != layer && layer instanceof VectorLayer) {
                    mLayer = (VectorLayer) layer;
                    GeoGeometry geometry = null;
                    try {
                        geometry = GeoGeometryFactory.fromBlob(bundle.getByteArray(BUNDLE_KEY_SAVED_FEATURE));
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    long featureId = bundle.getLong(BUNDLE_KEY_FEATURE_ID);
                    if (featureId == Constants.NOT_FOUND) {
                        mItem = new EditLayerCacheItem(Constants.NOT_FOUND, geometry);
                    } else {
                        mItem = new EditLayerCacheItem(featureId);
                        mItem.mGeometry = geometry;
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
                mContext.getString(R.string.delete_item_done)).listener(
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
    public void onSingleTapUp(MotionEvent event) {
        if (null == mLayer)
            return;

        selectGeometryInScreenCoordinates(event.getX(), event.getY());
    }


    protected void selectGeometryInScreenCoordinates(float x, float y) {
        double dMinX = x - mTolerancePX;
        double dMaxX = x + mTolerancePX;
        double dMinY = y - mTolerancePX;
        double dMaxY = y + mTolerancePX;
        GeoEnvelope screenEnv = new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY);

        if (null != mItem && null != mItem.getGeometry()) {
            //1. search current geometry point
            if (intersects(screenEnv)) {
                if (mMode == MODE_HIGHLIGHT) { // highlight same geometry
                    mSelectedItem = null;
                } else {
                    fillGeometry();
                    mMapViewOverlays.invalidate();
                }
                return;
            }

            if (mHasEdits) // prevent select another geometry before saving current edited one.
                return;
        }

        //2 select another geometry
        GeoEnvelope mapEnv = mMapViewOverlays.screenToMap(screenEnv);
        if (null == mapEnv)
            return;

        List<Long> items = mLayer.query(mapEnv);
        if (items.isEmpty())
            return;

        long previousFeatureId = Constants.NOT_FOUND;
        if (null != mItem)
            previousFeatureId = mItem.getFeatureId();

        mItem = new EditLayerCacheItem(items.get(0));
        fillDrawItems(mItem.getGeometry());

        if (mMode == MODE_HIGHLIGHT) {
            mMapViewOverlays.invalidate();
            return;
        }

        // this part should execute only in edit mode
        if (previousFeatureId == Constants.NOT_FOUND)
            mLayer.hideFeature(mItem.getFeatureId());
        else
            mLayer.swapFeaturesVisibility(previousFeatureId, mItem.getFeatureId());
    }


    protected boolean intersects(GeoEnvelope screenEnv) {
        for (DrawItem drawItem : mDrawItems) {
            if (drawItem.intersects(screenEnv, mMode == MODE_EDIT)) {
                mSelectedItem = drawItem;
                return true;
            }
        }

        return false;
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

                if (mSelectedItem.isTapNearSelectedPoint(screenEnv)) {
                    PointF tempPoint = mSelectedItem.getSelectedPoint();
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
            mSelectedItem.setSelectedPointCoordinates(
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
            fillGeometry();
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
                case GeoConstants.GTMultiLineString:
                    geometry = new GeoMultiLineString();
                    break;
                case GeoConstants.GTMultiPolygon:
                    geometry = new GeoMultiPolygon();
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

        mSelectedItem = new DrawItem();
        mDrawItems.add(mSelectedItem);

        // register broadcast events
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WalkEditService.WALKEDIT_CHANGE);
        mReceiver = new WalkEditReceiver();
        mContext.registerReceiver(mReceiver, intentFilter);

        // start service
        Intent trackerService = new Intent(mContext, WalkEditService.class);
        trackerService.setAction(WalkEditService.ACTION_START);
        trackerService.putExtra(ConstantsUI.KEY_GEOMETRY_TYPE, mLayer.getGeometryType());
        if(null != mLayer)
            trackerService.putExtra(ConstantsUI.KEY_LAYER_ID, mLayer.getId());
        trackerService.putExtra(ConstantsUI.KEY_GEOMETRY, mItem.getGeometry());
        trackerService.putExtra(ConstantsUI.TARGET_CLASS, mContext.getClass().getName());
        mContext.startService(trackerService);
    }


    public void stopGeometryByWalk()
    {
        // stop service
        Intent trackerService = new Intent(mContext, WalkEditService.class);
        trackerService.setAction(WalkEditService.ACTION_STOP);
        mContext.stopService(trackerService);

        // unregister events
        mContext.unregisterReceiver(mReceiver);
    }


    public class WalkEditReceiver
            extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent) {
            GeoGeometry geometry = (GeoGeometry) intent.getSerializableExtra(ConstantsUI.KEY_GEOMETRY);
            switch (mLayer.getGeometryType()) {
                case GeoConstants.GTLineString:
                case GeoConstants.GTPolygon:
                    mItem.setGeometry(geometry);
                    break;
                case GeoConstants.GTMultiLineString:
                case GeoConstants.GTMultiPolygon:
                    GeoGeometryCollection collection = (GeoGeometryCollection) mItem.getGeometry();
                    // TODO: 13.12.15 Select gometry index to update via walk
                    collection.set(0, geometry);
                    mItem.setGeometry(collection);
                    break;
                default:
                    return;
            }

            if (isCurrentGeometryValid()) {
                setToolbarSaveState(true);
            } else {
                setToolbarSaveState(false);
            }

            mMapViewOverlays.postInvalidate();
        }
    }

    protected boolean isCurrentGeometryValid() {
        return null != mItem && isGeometryValid(mItem.getGeometry()) &&
                mLayer.getGeometryType() == mItem.getGeometry().getType();
    }


    public boolean isGeometryValid(GeoGeometry geometry) {
        if (null == geometry)
            return false;

        switch (geometry.getType()) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                return true;
            case GeoConstants.GTLineString:
                GeoLineString line = (GeoLineString) geometry;
                return line.getPointCount() > 1;
            case GeoConstants.GTMultiLineString:
                GeoMultiLineString multiLine = (GeoMultiLineString) geometry;
                for (int i = 0; i < multiLine.size(); i++) {
                    GeoLineString subLine = multiLine.get(i);
                    if (subLine.getPointCount() > 1) {
                        return true;
                    }
                }
                return false;
            case GeoConstants.GTPolygon:
                GeoPolygon polygon = (GeoPolygon) geometry;
                GeoLinearRing ring = polygon.getOuterRing();
                return ring.getPointCount() > 2;
            case GeoConstants.GTMultiPolygon:
                GeoMultiPolygon multiPolygon = (GeoMultiPolygon) geometry;
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

    public void drawPoints(DrawItem drawItem, Canvas canvas, boolean isSelected) {
        for (int i = 0; i < drawItem.getRingCount(); i++) {
            float[] items = drawItem.getRing(i);

            if (items == null)
                continue;

            mPaint.setColor(mOutlineColor);
            mPaint.setStrokeWidth(VERTEX_RADIUS + 2);
            canvas.drawPoints(items, mPaint);

            mPaint.setColor(mFillColor);
            mPaint.setStrokeWidth(VERTEX_RADIUS);
            canvas.drawPoints(items, mPaint);
        }

        //draw selected point
        if (isSelected && drawItem.getSelectedRingId() != Constants.NOT_FOUND
                && drawItem.getSelectedPointId() != Constants.NOT_FOUND) {
            float[] items = drawItem.getSelectedRing();
            if (null != items) {
                mPaint.setColor(mSelectColor);
                mPaint.setStrokeWidth(VERTEX_RADIUS);

                canvas.drawPoint(
                        items[drawItem.getSelectedPointId()], items[drawItem.getSelectedPointId() + 1], mPaint);

                float anchorX = items[drawItem.getSelectedPointId()] + mAnchorRectOffsetX;
                float anchorY = items[drawItem.getSelectedPointId() + 1] + mAnchorRectOffsetY;
                canvas.drawBitmap(mAnchor, anchorX, anchorY, null);
            }
        }
    }

    public void drawLines(DrawItem drawItem, Canvas canvas, boolean isSelected) {
        for (int j = 0; j < drawItem.getRingCount(); j++) {
            float[] itemsVertex = drawItem.getRing(j);

            if (itemsVertex == null)
                continue;

            if (isSelected && drawItem.getSelectedRingId() == j)
                mPaint.setColor(mSelectColor);
            else
                mPaint.setColor(mFillColor);

            mPaint.setStrokeWidth(LINE_WIDTH);

            for (int i = 0; i < itemsVertex.length - 3; i += 2) {
                canvas.drawLine(itemsVertex[i], itemsVertex[i + 1],
                        itemsVertex[i + 2], itemsVertex[i + 3], mPaint);
            }

            if (mItem.getGeometry().getType() == GeoConstants.GTPolygon ||
                    mItem.getGeometry().getType() == GeoConstants.GTMultiPolygon) {
                if (itemsVertex.length >= 2) {
                    canvas.drawLine(
                            itemsVertex[0], itemsVertex[1], itemsVertex[itemsVertex.length - 2],
                            itemsVertex[itemsVertex.length - 1], mPaint);
                }
            }

            if (mMode == MODE_EDIT || mMode == MODE_CHANGE) {
                mPaint.setColor(mOutlineColor);
                mPaint.setStrokeWidth(VERTEX_RADIUS + 2);
                canvas.drawPoints(itemsVertex, mPaint);

                mPaint.setColor(mFillColor);
                mPaint.setStrokeWidth(VERTEX_RADIUS);
                canvas.drawPoints(itemsVertex, mPaint);
            }
        }

        if (mMode == MODE_EDIT) {
            for (float[] items : drawItem.getEdges()) {

                mPaint.setColor(mOutlineColor);
                mPaint.setStrokeWidth(EDGE_RADIUS + 2);
                canvas.drawPoints(items, mPaint);

                mPaint.setColor(mFillColor);
                mPaint.setStrokeWidth(EDGE_RADIUS);
                canvas.drawPoints(items, mPaint);
            }
        }

        //draw selected point
        if (isSelected && drawItem.getSelectedPointId() != Constants.NOT_FOUND) {
            float[] items = drawItem.getSelectedRing();
            if (null != items && drawItem.getSelectedPointId() + 1 < items.length) {
                mPaint.setColor(mSelectColor);
                mPaint.setStrokeWidth(VERTEX_RADIUS);

                canvas.drawPoint(
                        items[drawItem.getSelectedPointId()], items[drawItem.getSelectedPointId() + 1], mPaint);

                float anchorX = items[drawItem.getSelectedPointId()] + mAnchorRectOffsetX;
                float anchorY = items[drawItem.getSelectedPointId() + 1] + mAnchorRectOffsetY;
                canvas.drawBitmap(mAnchor, anchorX, anchorY, null);
            }
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

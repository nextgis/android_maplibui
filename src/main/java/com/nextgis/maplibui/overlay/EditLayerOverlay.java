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
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryCollection;
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
import com.nextgis.maplibui.api.EditStyle;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.api.OverlayItem;
import com.nextgis.maplibui.api.VertexStyle;
import com.nextgis.maplibui.fragment.BottomToolbar;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.service.WalkEditService;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplibui.api.DrawItem.EDGE_RADIUS;
import static com.nextgis.maplibui.api.DrawItem.LINE_WIDTH;

/**
 * The class for edit vector features
 */
public class EditLayerOverlay extends Overlay implements MapViewEventListener {
    /**
     * overlay mode constants
     */
    public final static int MODE_NONE = 0;
    public final static int MODE_HIGHLIGHT = 1;
    public final static int MODE_EDIT = 2;
    public final static int MODE_CHANGE = 3;
    public final static int MODE_EDIT_BY_WALK = 4;
    public final static int MODE_EDIT_BY_TOUCH = 5;

    /**
     * edit feature style
     */

    protected static final int mType = 3;

    /**
     * Store keys
     */
    protected static final String BUNDLE_KEY_MODE = "mode";
    protected static final String BUNDLE_KEY_HAS_EDITS = "has_edits";
    protected static final String BUNDLE_KEY_OVERLAY_POINT = "overlay_point";

    protected Paint mPaint;

    protected final float mTolerancePX;
    protected float mCanvasCenterX, mCanvasCenterY;

    protected MapDrawable mMap;
    protected Toolbar mTopToolbar;
    protected BottomToolbar mBottomToolbar;

    protected VectorLayer mLayer;
    protected Feature mFeature;

    protected List<DrawItem> mDrawItems;
    protected DrawItem mSelectedItem;

    protected int mMode;
    protected boolean mHasEdits;

    protected PointF mTempPointOffset;
    protected OverlayItem mOverlayPoint;

    protected List<EditEventListener> mListeners;
    protected WalkEditReceiver mReceiver;


    public EditLayerOverlay(
            Context context,
            MapViewOverlays mapViewOverlays) {
        super(context, mapViewOverlays);
        mLayer = null;
        mMode = MODE_NONE;

        mTolerancePX =
                context.getResources().getDisplayMetrics().density * ConstantsUI.TOLERANCE_DP;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(LINE_WIDTH / 2);

        mDrawItems = new ArrayList<>();
        mListeners = new ArrayList<>();

        mMap = mMapViewOverlays.getMap();
        mMapViewOverlays.addListener(this);
        mOverlayPoint = new OverlayItem(mMap, 0, 0, getMarker());

        Bitmap anchor = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_anchor);
        DrawItem.setAnchor(context, anchor);

        int outlineColor = Color.BLACK;
        int fillColor = ControlHelper.getColor(mContext, com.nextgis.maplibui.R.attr.colorAccent);
        int selectColor = Color.RED;
        VertexStyle vertexStyle = new VertexStyle(mContext, 255, fillColor, 5, 2.6f, selectColor, 5, 2.6f, outlineColor, 6, 3);
        VertexStyle edgeStyle = new VertexStyle(mContext, 255, fillColor, 3, 1.6f, selectColor, 3, 1.6f, outlineColor, 4, 1.8f);
        EditStyle lineStyle = new EditStyle(mContext, 255, fillColor, 2, selectColor, 2);
        EditStyle polygonStyle = new EditStyle(mContext, 0, Color.TRANSPARENT, 2, Color.TRANSPARENT, 2);
        DrawItem.setVertexStyle(vertexStyle);
        DrawItem.setEdgeStyle(edgeStyle);
        DrawItem.setLineStyle(lineStyle);
        DrawItem.setPolygonStyle(polygonStyle);
    }


    public void setTopToolbar(final Toolbar toolbar) {
        mTopToolbar = toolbar;
    }


    public void setBottomToolbar(final BottomToolbar toolbar) {
        mBottomToolbar = toolbar;
    }


    public void addListener(EditEventListener listener) {
        if (mListeners != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }


    public void removeListener(EditEventListener listener) {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }


    public void setSelectedLayer(VectorLayer layer) {
        clearDrawItems();
        clearGeometry();
        mLayer = layer;
    }


    public void setSelectedFeature(long featureId) {
        clearDrawItems();

        if (mLayer != null && featureId > Constants.NOT_FOUND) {
            mFeature = new Feature(featureId, mLayer.getFields());
            mFeature.setGeometry(mLayer.getGeometryForId(featureId));
        } else
            mFeature = null;

        updateMap();
    }


    public void setSelectedFeature(Feature feature) {
        clearDrawItems();
        mFeature = feature;
        updateMap();
    }


    public Feature getSelectedFeature() {
        return mFeature;
    }


    public long getSelectedFeatureId() {
        return mFeature == null ? Constants.NOT_FOUND : mFeature.getId();
    }


    public GeoGeometry getSelectedFeatureGeometry() {
        return mFeature == null ? null : mFeature.getGeometry();
    }


    public void showAllFeatures() {
        if (mLayer != null)
            mLayer.showAllFeatures();
    }


    protected void hideNavigationButton() {
        mBottomToolbar.setNavigationIcon(null);
        mBottomToolbar.setNavigationContentDescription(null);
    }


    protected void clearDrawItems() {
        mDrawItems.clear();
        mSelectedItem = null;
    }


    protected void clearGeometry() {
        mLayer = null;
        mFeature = null;
    }


    protected void clearAll() {
        clearGeometry();
        clearDrawItems();
    }


    protected void update() {
        setHasEdits(true);
        fillGeometry();
        updateMap();
    }


    protected void updateMap() {
        mMapViewOverlays.buffer();
        mMapViewOverlays.postInvalidate();
    }


    public boolean hasEdits() {
        return mHasEdits;
    }


    public void setHasEdits(boolean hasEdits) {
        mHasEdits = hasEdits;

        MenuItem item;
        if (mTopToolbar != null) {
            item = mTopToolbar.getMenu().findItem(R.id.menu_edit_save);
            if (item != null)
                ControlHelper.setEnabled(item, hasEdits);
        }

        if (mBottomToolbar != null && mSelectedItem != null) {
            // polygon rings
            boolean isOuterRingSelected = mSelectedItem.getSelectedRingId() == 0;
            item = mBottomToolbar.getMenu().findItem(R.id.menu_edit_add_new_inner_ring);
            if (item != null)
                ControlHelper.setEnabled(item, isOuterRingSelected);

            item = mBottomToolbar.getMenu().findItem(R.id.menu_edit_delete_inner_ring);
            if (item != null)
                ControlHelper.setEnabled(item, !isOuterRingSelected);

            // delete buttons
            boolean onlyOneItem = mDrawItems.size() > 1;
            item = mBottomToolbar.getMenu().findItem(R.id.menu_edit_delete_line);
            if (item != null)
                ControlHelper.setEnabled(item, onlyOneItem);

            item = mBottomToolbar.getMenu().findItem(R.id.menu_edit_delete_polygon);
            if (item != null)
                ControlHelper.setEnabled(item, onlyOneItem && isOuterRingSelected);

            item = mBottomToolbar.getMenu().findItem(R.id.menu_edit_delete_point);
            if (item != null) {
                boolean moreThanMin = true;
                int size = mSelectedItem.getSelectedRing() == null ? 0 : mSelectedItem.getSelectedRing().length;
                int minPoints = DrawItem.getMinPointCount(mLayer.getGeometryType()) * 2;

                switch (mLayer.getGeometryType()) {
                    case GeoConstants.GTMultiPoint:
                        moreThanMin = onlyOneItem;
                        break;
                    case GeoConstants.GTLineString:
                    case GeoConstants.GTMultiLineString:
                        moreThanMin = size > minPoints;
                        break;
                    case GeoConstants.GTPolygon:
                    case GeoConstants.GTMultiPolygon:
                        moreThanMin = size > minPoints;
                        break;
                }

                ControlHelper.setEnabled(item, moreThanMin);
            }

            item = mBottomToolbar.getMenu().findItem(R.id.menu_edit_by_walk);
            if (item != null)
                ControlHelper.setEnabled(item, !hasEdits);
        }
    }

    public int getMode() {
        return mMode;
    }

    public void setMode(int mode) {
        if (mode != MODE_NONE && mLayer == null)
            return;

        mMode = mode;
        switch (mMode) {
            case MODE_NONE:
                if (mLayer != null && mFeature != null)
                    mLayer.showFeature(mFeature.getId());
                clearAll();
                break;
            case MODE_HIGHLIGHT:
                if (mFeature != null)
                    mLayer.showFeature(mFeature.getId());
                break;
            case MODE_EDIT:
                if (mFeature == null)
                    break;

                mBottomToolbar.setTitle(null);
                mBottomToolbar.getMenu().clear();
                switch (mLayer.getGeometryType()) {
                    case GeoConstants.GTPoint:
                        mBottomToolbar.inflateMenu(R.menu.edit_point);
                        break;
                    case GeoConstants.GTMultiPoint:
                        mBottomToolbar.inflateMenu(R.menu.edit_multipoint);
                        break;
                    case GeoConstants.GTLineString:
                        mBottomToolbar.inflateMenu(R.menu.edit_line);
                        break;
                    case GeoConstants.GTMultiLineString:
                        mBottomToolbar.inflateMenu(R.menu.edit_multiline);
                        break;
                    case GeoConstants.GTPolygon:
                        mBottomToolbar.inflateMenu(R.menu.edit_polygon);
                        break;
                    case GeoConstants.GTMultiPolygon:
                        mBottomToolbar.inflateMenu(R.menu.edit_multipolygon);
                        break;
                    case GeoConstants.GTGeometryCollection:
                    default:
                        break;
                }

                hideNavigationButton();

                for (EditEventListener listener : mListeners)
                    listener.onStartEditSession();

                mLayer.hideFeature(mFeature.getId());
                break;
            case MODE_EDIT_BY_WALK:
                hideNavigationButton();

                for (EditEventListener listener : mListeners)
                    listener.onStartEditSession();

                mBottomToolbar.setTitle(R.string.title_edit_by_walk);
                mBottomToolbar.getMenu().clear();
                mBottomToolbar.inflateMenu(R.menu.edit_by_walk);
                mBottomToolbar.setOnMenuItemClickListener(
                        new BottomToolbar.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                if (menuItem.getItemId() == R.id.menu_settings) {
                                    IGISApplication app = (IGISApplication) ((Activity) mContext).getApplication();
                                    app.showSettings(SettingsConstantsUI.ACTION_PREFS_LOCATION);
                                }

                                return true;
                            }
                        }
                );

                startGeometryByWalk();
                break;
            case MODE_EDIT_BY_TOUCH:
                hideNavigationButton();
                mBottomToolbar.setTitle(R.string.title_edit_by_touch);
                mBottomToolbar.getMenu().clear();
                MenuItem apply = mBottomToolbar.getMenu().add(0, 0, 0, R.string.ok);
                apply.setIcon(R.drawable.ic_action_apply_dark);
                MenuItemCompat.setShowAsAction(apply, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
                mMapViewOverlays.setLockMap(true);
                break;
        }

        hideOverlayPoint();
        updateMap();
    }


    public void setOverlayPoint(MotionEvent event) {
        GeoPoint mapPoint = mMap.screenToMap(new GeoPoint(event.getX(), event.getY()));
        mapPoint.setCRS(GeoConstants.CRS_WEB_MERCATOR);
        mapPoint.project(GeoConstants.CRS_WGS84);
        mOverlayPoint.setCoordinates(mapPoint);
        mOverlayPoint.setVisible(true);
    }


    public void hideOverlayPoint() {
        mOverlayPoint.setVisible(false);
    }


    public void createPointFromOverlay() {
        clearDrawItems();

        float[] coordinates = new float[]{mOverlayPoint.getScreenX(), mOverlayPoint.getScreenY()};
        mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, coordinates);
        mDrawItems.add(mSelectedItem);

        update();
    }


    public boolean onOptionsItemSelected(int id) {
        if (mLayer == null || mSelectedItem == null)
            return false;

        boolean result = false;
        if (id == R.id.menu_edit_move_point_to_center) {
            result = moveSelectedPoint(mCanvasCenterX, mCanvasCenterY);
        } else if (id == R.id.menu_edit_move_point_to_current_location) {
            result = movePointToLocation();
        } else if (id == R.id.menu_edit_add_new_point) {
            result = addGeometryToMulti(GeoConstants.GTPoint);
        } else if (id == R.id.menu_edit_add_new_line) {
            result = addGeometryToMulti(GeoConstants.GTLineString);
        } else if (id == R.id.menu_edit_add_new_polygon) {
            result = addGeometryToMulti(GeoConstants.GTPolygon);
        } else if (id == R.id.menu_edit_add_new_inner_ring) {
            result = addInnerRing();
        } else if (id == R.id.menu_edit_delete_inner_ring) {
            result = deleteInnerRing();
        } else if (id == R.id.menu_edit_delete_line || id == R.id.menu_edit_delete_polygon) {
            result = deleteGeometry();
        } else if (id == R.id.menu_edit_delete_point) {
            result = deletePoint();
        } else if (id == R.id.menu_edit_by_walk) {
            result = true;
        } else if (id == R.id.menu_edit_by_touch) {
            result = true;
        }

        if (result)
            update();

        return result;
    }


    public void createNewGeometry() {
        clearDrawItems();

        float[] geoPoints = getNewGeometry(mLayer.getGeometryType(), mTolerancePX, mMap);
        mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, geoPoints);
        mDrawItems.add(mSelectedItem);

        update();
    }


    public static float[] getNewGeometry(int geometryType, float tolerance, MapDrawable map) {
        float[] geoPoints;
        float add = tolerance * 2;
        GeoPoint center = map.getFullScreenBounds().getCenter();

        switch (geometryType) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                geoPoints = new float[2];
                geoPoints[0] = (float) center.getX();
                geoPoints[1] = (float) center.getY();
                return geoPoints;
            case GeoConstants.GTLineString:
            case GeoConstants.GTMultiLineString:
                geoPoints = new float[4];
                geoPoints[0] = (float) center.getX() - add;
                geoPoints[1] = (float) center.getY() - add;
                geoPoints[2] = (float) center.getX() + add;
                geoPoints[3] = (float) center.getY() + add;
                return geoPoints;
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
                geoPoints = new float[6];
                geoPoints[0] = (float) center.getX() - add;
                geoPoints[1] = (float) center.getY() - add;
                geoPoints[2] = (float) center.getX() - add;
                geoPoints[3] = (float) center.getY() + add;
                geoPoints[4] = (float) center.getX() + add;
                geoPoints[5] = (float) center.getY() + add;
                return geoPoints;
            case GeoConstants.GTLinearRing:
                geoPoints = new float[6];
                geoPoints[0] = (float) center.getX() + add;
                geoPoints[1] = (float) center.getY() + add;
                geoPoints[2] = (float) center.getX() - add;
                geoPoints[3] = (float) center.getY() + add;
                geoPoints[4] = (float) center.getX() - add;
                geoPoints[5] = (float) center.getY() - add;
                return geoPoints;
            default:
                return null;
        }
    }


    protected boolean moveSelectedPoint(float x, float y) {
        mSelectedItem.setSelectedPointCoordinates(x, y);
        return true;
    }


    protected boolean movePointToLocation() {
        Activity parent = (Activity) mContext;
        GpsEventSource gpsEventSource = ((IGISApplication) parent.getApplication()).getGpsEventSource();
        Location location = gpsEventSource.getLastKnownLocation();

        if (null != location) {
            //change to screen coordinates
            GeoPoint pt = new GeoPoint(location.getLongitude(), location.getLatitude());
            pt.setCRS(GeoConstants.CRS_WGS84);
            pt.project(GeoConstants.CRS_WEB_MERCATOR);
            GeoPoint screenPt = mMap.mapToScreen(pt);
            return moveSelectedPoint((float) screenPt.getX(), (float) screenPt.getY());
        } else
            Toast.makeText(parent, R.string.error_no_location, Toast.LENGTH_SHORT).show();

        return false;
    }


    protected boolean addGeometryToMulti(int geometryType) {
        //insert geometry in appropriate position
        switch (geometryType) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTLineString:
            case GeoConstants.GTPolygon:
                float[] geoPoints = getNewGeometry(geometryType, mTolerancePX, mMap);
                mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, geoPoints);
                mDrawItems.add(mSelectedItem);
                break;
        }

        return true;
    }


    protected boolean addInnerRing() {
        mSelectedItem.addVertices(getNewGeometry(GeoConstants.GTLinearRing, mTolerancePX, mMap));
        mSelectedItem.setSelectedRing(mSelectedItem.getRingCount() - 1);
        mSelectedItem.setSelectedPoint(0);

        return true;
    }


    protected boolean deleteInnerRing() {
        if (mSelectedItem.getSelectedRingId() != 0) {
            mSelectedItem.deleteSelectedRing();
            return true;
        }

        return false;
    }


    protected boolean deleteGeometry() {
        mDrawItems.remove(mSelectedItem);
        selectLastItem();
        return true;
    }


    protected boolean deletePoint() {
        mSelectedItem.deleteSelectedPoint(mLayer);
        if (mSelectedItem.getRingCount() == 0) {
            mDrawItems.remove(mSelectedItem);
            selectLastItem();
        }

        return true;
    }


    protected void selectLastItem() {
        if (mDrawItems.size() > 0)
            mSelectedItem = mDrawItems.get(mDrawItems.size() - 1);
        else
            mSelectedItem = null;
    }


    public void newGeometryByWalk() {
        GeoGeometry geometry;
        switch (mLayer.getGeometryType()) {
            case GeoConstants.GTLineString:
                geometry = new GeoLineString();
                break;
            case GeoConstants.GTPolygon:
                geometry = new GeoPolygon();
                break;
            case GeoConstants.GTMultiLineString:
                GeoMultiLineString multiLine = new GeoMultiLineString();
                multiLine.add(new GeoLineString());
                geometry = multiLine;
                break;
            case GeoConstants.GTMultiPolygon:
                GeoMultiPolygon multiPolygon = new GeoMultiPolygon();
                multiPolygon.add(new GeoPolygon());
                geometry = multiPolygon;
                break;
            default:
                return;
        }

        mFeature = new Feature();
        mFeature.setGeometry(geometry);

        mDrawItems.clear();
        mSelectedItem = new DrawItem();
        mDrawItems.add(mSelectedItem);
    }


    protected void startGeometryByWalk() {
        // register broadcast events
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WalkEditService.WALKEDIT_CHANGE);
        mReceiver = new WalkEditReceiver();
        mContext.registerReceiver(mReceiver, intentFilter);

        if (WalkEditService.isServiceRunning(mContext))
            return;

        // start service if not started yet
        GeoGeometry geometry = mFeature.getGeometry();
        int selectedRing = mSelectedItem.getSelectedRingId();
        int selectedGeometry = mDrawItems.indexOf(mSelectedItem);

        switch (mLayer.getGeometryType()) {
            case GeoConstants.GTLineString:
                break;
            case GeoConstants.GTPolygon:
                GeoPolygon polygon = ((GeoPolygon) geometry);
                geometry = selectedRing == 0 ? polygon.getOuterRing() : polygon.getInnerRing(selectedRing - 1);
                break;
            case GeoConstants.GTMultiLineString:
                geometry = ((GeoMultiLineString) geometry).get(selectedGeometry);
                break;
            case GeoConstants.GTMultiPolygon:
                GeoPolygon selectedPolygon = ((GeoMultiPolygon) geometry).get(selectedGeometry);
                geometry = selectedRing == 0 ? selectedPolygon.getOuterRing() : selectedPolygon.getInnerRing(selectedRing - 1);
                break;
            default:
                return;
        }

        Intent trackerService = new Intent(mContext, WalkEditService.class);
        trackerService.setAction(WalkEditService.ACTION_START);
        trackerService.putExtra(ConstantsUI.KEY_LAYER_ID, mLayer.getId());
        trackerService.putExtra(ConstantsUI.KEY_GEOMETRY, geometry);
        trackerService.putExtra(ConstantsUI.TARGET_CLASS, mContext.getClass().getName());
        mContext.startService(trackerService);
    }


    public void stopGeometryByWalk() {
        // stop service
        Intent trackerService = new Intent(mContext, WalkEditService.class);
        trackerService.setAction(WalkEditService.ACTION_STOP);
        mContext.stopService(trackerService);

        // unregister events
        if(null != mReceiver) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }


    protected void fillGeometry() {
        GeoGeometry geometry;
        if (mLayer == null || mDrawItems.isEmpty() || mSelectedItem == null)
            return;

        switch (mLayer.getGeometryType()) {
            case GeoConstants.GTPoint:
                geometry = getBaseGeometry(mMap, GeoConstants.GTPoint, mSelectedItem);
                break;
            case GeoConstants.GTMultiPoint:
                geometry = new GeoMultiPoint();
                for (DrawItem drawItem : mDrawItems)
                    ((GeoMultiPoint) geometry).add(getBaseGeometry(mMap, GeoConstants.GTPoint, drawItem));
                break;
            case GeoConstants.GTLineString:
                geometry = getBaseGeometry(mMap, GeoConstants.GTLineString, mSelectedItem);
                break;
            case GeoConstants.GTMultiLineString:
                geometry = new GeoMultiLineString();
                for (DrawItem drawItem : mDrawItems)
                    ((GeoMultiLineString) geometry).add(getBaseGeometry(mMap, GeoConstants.GTLineString, drawItem));
                break;
            case GeoConstants.GTPolygon:
                geometry = getBaseGeometry(mMap, GeoConstants.GTPolygon, mSelectedItem);
                break;
            case GeoConstants.GTMultiPolygon:
                geometry = new GeoMultiPolygon();
                for (DrawItem drawItem : mDrawItems)
                    ((GeoMultiPolygon) geometry).add(getBaseGeometry(mMap, GeoConstants.GTPolygon, drawItem));
                break;
            default:
                geometry = null;
                break;
        }

        mFeature.setGeometry(geometry);
    }


    public static GeoGeometry getBaseGeometry(MapDrawable map, int geometryType, DrawItem drawItem) {
        GeoPoint[] geoPoints;
        GeoGeometry geometry;

        switch (geometryType) {
            case GeoConstants.GTPoint:
                geoPoints = map.screenToMap(drawItem.getRing(0));
                geometry = new GeoPoint(geoPoints[0].getX(), geoPoints[0].getY());
                break;
            case GeoConstants.GTLineString:
                geometry = new GeoLineString();
                geoPoints = map.screenToMap(drawItem.getRing(0));
                for (GeoPoint geoPoint : geoPoints)
                    ((GeoLineString) geometry).add(geoPoint);
                break;
            case GeoConstants.GTPolygon:
                geometry = new GeoPolygon();
                geoPoints = map.screenToMap(drawItem.getRing(0));
                for (GeoPoint geoPoint : geoPoints)
                    ((GeoPolygon) geometry).add(geoPoint);

                for (int i = 1; i < drawItem.getRingCount(); i++) {
                    geoPoints = map.screenToMap(drawItem.getRing(i));
                    GeoLinearRing ring = new GeoLinearRing();
                    ring.setCRS(GeoConstants.CRS_WEB_MERCATOR);
                    for (GeoPoint geoPoint : geoPoints)
                        ring.add(geoPoint);

                    ((GeoPolygon) geometry).addInnerRing(ring);
                }
                break;
            default:
                geometry = null;
                break;
        }

        if (geometry != null)
            geometry.setCRS(GeoConstants.CRS_WEB_MERCATOR);

        return geometry;
    }


    protected float[] mapToScreen(GeoPoint[] geoPoints) {
        return mMapViewOverlays.getMap().mapToScreen(geoPoints);
    }


    @Override
    public void draw(Canvas canvas, MapDrawable mapDrawable) {
        if (mOverlayPoint.isVisible())
            drawOverlayItem(canvas, mOverlayPoint);

        if (mMode == MODE_CHANGE || mFeature == null)
            return;

        fillDrawItems(mFeature.getGeometry());

        for (DrawItem drawItem : mDrawItems) {
            boolean isSelected = mSelectedItem == drawItem;
            drawItem(drawItem, canvas, isSelected);
        }

        drawCross(canvas);
    }


    @Override
    public void drawOnPanning(
            Canvas canvas,
            PointF currentMouseOffset) {
        if (mOverlayPoint.isVisible())
            drawOnPanning(canvas, currentMouseOffset, mOverlayPoint);

        List<DrawItem> drawItems = mDrawItems;
        for (DrawItem drawItem : drawItems) {
            boolean isSelected = mSelectedItem == drawItem;

            if (mMode != MODE_CHANGE && mMode != MODE_EDIT_BY_TOUCH) {
                drawItem = drawItem.pan(currentMouseOffset);

                if (isSelected) {
                    drawItem.setSelectedRing(mSelectedItem.getSelectedRingId());
                    drawItem.setSelectedPoint(mSelectedItem.getSelectedPointId());
                }
            }

            drawItem(drawItem, canvas, isSelected);
        }

        drawCross(canvas);
    }


    @Override
    public void drawOnZooming(
            Canvas canvas,
            PointF currentFocusLocation,
            float scale) {
        if (mOverlayPoint.isVisible())
            drawOnZooming(canvas, currentFocusLocation, scale, mOverlayPoint, false);

        List<DrawItem> drawItems = mDrawItems;
        for (DrawItem drawItem : drawItems) {
            boolean isSelected = mSelectedItem == drawItem;
            drawItem = drawItem.zoom(currentFocusLocation, scale);

            if (isSelected) {
                drawItem.setSelectedRing(mSelectedItem.getSelectedRingId());
                drawItem.setSelectedPoint(mSelectedItem.getSelectedPointId());
            }

            drawItem(drawItem, canvas, isSelected);
        }

        drawCross(canvas);
    }


    public void fillDrawItems(GeoGeometry geom) {
        int lastItemsCount = mDrawItems.size();
        int lastSelectedItemPosition = mDrawItems.indexOf(mSelectedItem);
        DrawItem lastSelectedItem = mSelectedItem;
        mDrawItems.clear();

        if (null == geom) {
            Log.w(Constants.TAG, "the geometry is null in fillDrawItems method");
            return;
        }

        GeoPoint[] geoPoints = new GeoPoint[1];
        switch (geom.getType()) {
            case GeoConstants.GTPoint:
                geoPoints[0] = (GeoPoint) geom;
                mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, mapToScreen(geoPoints));
                mDrawItems.add(mSelectedItem);
                break;
            case GeoConstants.GTMultiPoint:
                GeoMultiPoint geoMultiPoint = (GeoMultiPoint) geom;
                for (int i = 0; i < geoMultiPoint.size(); i++) {
                    geoPoints[0] = geoMultiPoint.get(i);
                    mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, mapToScreen(geoPoints));
                    mDrawItems.add(mSelectedItem);
                }
                break;
            case GeoConstants.GTLineString:
                fillDrawLine((GeoLineString) geom);
                break;
            case GeoConstants.GTMultiLineString:
                GeoMultiLineString multiLineString = (GeoMultiLineString) geom;
                for (int i = 0; i < multiLineString.size(); i++)
                    fillDrawLine(multiLineString.get(i));
                break;
            case GeoConstants.GTPolygon:
                fillDrawPolygon((GeoPolygon) geom);
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
        } else {
            mSelectedItem = mDrawItems.get(0);
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

        mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, points);
        mDrawItems.add(mSelectedItem);

        if (points.length < 2)
            return;

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


    protected void drawCross(Canvas canvas) {
        if (mMode != MODE_EDIT) {
            return;
        }
        mCanvasCenterX = canvas.getWidth() / 2;
        mCanvasCenterY = canvas.getHeight() / 2;

        canvas.drawLine(
                mCanvasCenterX - mTolerancePX, mCanvasCenterY, mCanvasCenterX + mTolerancePX,
                mCanvasCenterY, mPaint);
        canvas.drawLine(
                mCanvasCenterX, mCanvasCenterY - mTolerancePX, mCanvasCenterX,
                mCanvasCenterY + mTolerancePX, mPaint);
    }


    protected void drawItem(DrawItem drawItem, Canvas canvas, boolean isSelected) {
        isSelected = isSelected && mMode == MODE_EDIT;
        switch (mFeature.getGeometry().getType()) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                drawItem.drawPoints(canvas, isSelected);
                break;
            case GeoConstants.GTLineString:
            case GeoConstants.GTMultiLineString:
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
                boolean closed = mFeature.getGeometry().getType() == GeoConstants.GTPolygon || mFeature.getGeometry().getType() == GeoConstants.GTMultiPolygon;
                drawItem.drawLines(canvas, isSelected, mMode == MODE_EDIT || mMode == MODE_CHANGE, mMode == MODE_EDIT, closed);
                break;
            default:
                break;
        }
    }


    @Override
    public Bundle onSaveState() {
        Bundle bundle = super.onSaveState();
        bundle.putInt(BUNDLE_KEY_TYPE, mType);
        bundle.putInt(BUNDLE_KEY_MODE, mMode);
        bundle.putBoolean(BUNDLE_KEY_HAS_EDITS, mHasEdits);

        if (mOverlayPoint.isVisible())
            bundle.putSerializable(BUNDLE_KEY_OVERLAY_POINT, mOverlayPoint.getCoordinates(GeoConstants.CRS_WGS84));

        return bundle;
    }


    @Override
    public void onRestoreState(Bundle bundle) {
        if (null != bundle && mType == bundle.getInt(BUNDLE_KEY_TYPE, 0)) {
            mMode = bundle.getInt(BUNDLE_KEY_MODE);
            mHasEdits = bundle.getBoolean(BUNDLE_KEY_HAS_EDITS);

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


    @Override
    public void onLongPress(MotionEvent event) {
        //TODO: do we need some actions on long press on point or geometry?
    }


    /**
     * Select point in current geometry or new geometry from current layer
     *
     * @param event Motion event
     */
    @Override
    public void onSingleTapUp(MotionEvent event) {
    }


    public boolean selectGeometryInScreenCoordinates(float x, float y) {
        if (null == mLayer)
            return false;

        double dMinX = x - mTolerancePX;
        double dMaxX = x + mTolerancePX;
        double dMinY = y - mTolerancePX;
        double dMaxY = y + mTolerancePX;
        GeoEnvelope screenEnv = new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY);

        //1. search current geometry point
        if (null != mFeature && null != mFeature.getGeometry()) {
            for (DrawItem drawItem : mDrawItems) {
                if (drawItem.intersectsVertices(screenEnv)) {
                    mSelectedItem = drawItem;
                    setHasEdits(mHasEdits);
                    updateMap();
                    return false;
                }

                if (drawItem.intersectsEdges(screenEnv)) {
                    mSelectedItem = drawItem;
                    update();
                    return true;
                }
            }

            if (mHasEdits) // prevent select another geometry before saving current edited one. TODO toast?
                return false;
        }

        //2. select another geometry
        GeoEnvelope mapEnv = mMapViewOverlays.screenToMap(screenEnv);
        if (null == mapEnv)
            return false;

        List<Long> items = mLayer.query(mapEnv);
        if (items.isEmpty())
            return false;

        long previousFeatureId = Constants.NOT_FOUND;
        if (null != mFeature)
            previousFeatureId = mFeature.getId();

        for (int i = 0; i < items.size(); i++) {    // FIXME hack for bad RTree cache
            long featureId = items.get(i);
            GeoGeometry geometry = mLayer.getGeometryForId(featureId);
            if (geometry != null && previousFeatureId != featureId) {
                mFeature = new Feature(featureId, mLayer.getFields());
                mFeature.setGeometry(mLayer.getGeometryForId(featureId));
            }
        }

        if (mFeature == null || previousFeatureId == mFeature.getId())
            return false;

        if (mMode == MODE_HIGHLIGHT) {
            mMapViewOverlays.invalidate();
            return false;
        }

        // this part should execute only in edit mode
        if (previousFeatureId == Constants.NOT_FOUND)
            mLayer.hideFeature(mFeature.getId());
        else
            mLayer.swapFeaturesVisibility(previousFeatureId, mFeature.getId());

        return false;
    }


    @Override
    public void panStart(MotionEvent event) {
        if (mMode == MODE_EDIT) {
            if (null != mFeature && null != mFeature.getGeometry()) {
                //check if we are near selected point
                double dMinX = event.getX() - mTolerancePX * 2 - DrawItem.mAnchorTolerancePX;
                double dMaxX = event.getX() + mTolerancePX;
                double dMinY = event.getY() - mTolerancePX * 2 - DrawItem.mAnchorTolerancePX;
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
    public void panMoveTo(MotionEvent e) {
        if (mMode == MODE_CHANGE) {
            mSelectedItem.setSelectedPointCoordinates(
                    e.getX() + mTempPointOffset.x, e.getY() + mTempPointOffset.y);
        }

        if (mMode == MODE_EDIT_BY_TOUCH) {
            mSelectedItem.insertNewPoint(mSelectedItem.getSelectedPointId(), e.getX(), e.getY());
        }
    }


    @Override
    public void panStop() {
        if (mMode == MODE_CHANGE) {
            mMapViewOverlays.setLockMap(false);
            mMode = MODE_EDIT;

            update();
        }

        if (mMode == MODE_EDIT_BY_TOUCH)
            fillGeometry();
    }


    @Override
    public void onLayerAdded(int id) {

    }


    @Override
    public void onLayerDeleted(int id) { // TODO do we need this?
        //if delete edited layer cancel edit session
        if (null != mLayer && mLayer.getId() == id) {
            setHasEdits(false);
            setMode(MODE_NONE);
        }
    }


    @Override
    public void onLayerChanged(int id) {

    }


    @Override
    public void onExtentChanged(
            float zoom,
            GeoPoint center) {

    }


    @Override
    public void onLayersReordered() {

    }


    @Override
    public void onLayerDrawFinished(
            int id,
            float percent) {

    }


    @Override
    public void onLayerDrawStarted() {

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


    public class WalkEditReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            GeoGeometry geometry = (GeoGeometry) intent.getSerializableExtra(ConstantsUI.KEY_GEOMETRY);
            setGeometryFromWalkEdit(geometry);
            mMapViewOverlays.postInvalidate();
        }
    }

    public void setGeometryFromWalkEdit(GeoGeometry geometry) {
        int selectedGeometry = mDrawItems.indexOf(mSelectedItem);
        int selectedRing = mSelectedItem.getSelectedRingId();

        switch (mLayer.getGeometryType()) {
            case GeoConstants.GTLineString:
                mFeature.setGeometry(geometry);
                break;
            case GeoConstants.GTMultiLineString:
                GeoMultiLineString multiLine = (GeoMultiLineString) mFeature.getGeometry();
                multiLine.set(selectedGeometry, geometry);
                mFeature.setGeometry(multiLine);
                break;
            case GeoConstants.GTPolygon:
                GeoPolygon polygon = (GeoPolygon) mFeature.getGeometry();

                if (selectedRing == 0)
                    polygon.setOuterRing((GeoLinearRing) geometry);
                else
                    polygon.setInnerRing(selectedRing - 1, (GeoLinearRing) geometry);

                mFeature.setGeometry(polygon);
                break;
            case GeoConstants.GTMultiPolygon:
                GeoMultiPolygon multiPolygon = (GeoMultiPolygon) mFeature.getGeometry();
                GeoPolygon selectedPolygon = multiPolygon.get(selectedGeometry);
                selectedPolygon.setOuterRing((GeoLinearRing) geometry);

                if (selectedRing == 0)
                    selectedPolygon.setOuterRing((GeoLinearRing) geometry);
                else
                    selectedPolygon.setInnerRing(selectedRing - 1, (GeoLinearRing) geometry);

                multiPolygon.set(selectedGeometry, selectedPolygon);
                mFeature.setGeometry(multiPolygon);
                break;
        }
    }
}

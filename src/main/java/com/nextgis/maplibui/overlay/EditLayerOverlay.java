/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.cocosw.undobar.UndoBarController;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoLineString;
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
import com.nextgis.maplib.util.VectorCacheItem;
import com.nextgis.maplibui.BottomToolbar;
import com.nextgis.maplibui.MapViewOverlays;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.EditEventListener;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.util.ConstantsUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * The class for edit vector features
 */
public class EditLayerOverlay
        extends Overlay implements MapViewEventListener
{

    public final static int MODE_NONE      = 0;
    public final static int MODE_HIGHLIGHT = 1;
    public final static int MODE_EDIT      = 2;
    public final static int MODE_CHANGE    = 3;

    protected final static int VERTEX_RADIUS = 20;
    protected final static int EDGE_RADIUS   = 12;
    protected final static int LINE_WIDTH    = 4;

    public final static long DELAY = 750;

    protected VectorLayer     mLayer;
    protected VectorCacheItem mItem;
    protected int             mMode;
    protected Paint           mPaint;
    protected int             mFillColor;
    protected int             mOutlineColor;
    protected int             mSelectColor;
    protected Bitmap          mAnchor;
    protected float           mAnchorRectOffsetX, mAnchorRectOffsetY;
    protected float mAnchorCenterX, mAnchorCenterY;
    protected DrawItems mDrawItems;

    protected List<EditEventListener> mListeners;

    protected static final int mType = 3;

    protected static final String BUNDLE_KEY_MODE    = "mode";
    protected static final String BUNDLE_KEY_LAYER   = "layer";
    protected static final String BUNDLE_KEY_FEATURE = "feature";

    protected float mTolerancePX;
    protected float mAnchorTolerancePX;

    protected GeoGeometry   mOriginalGeometry; //For restore
    protected PointF        mTempPointOffset;
    protected boolean       mHasEdits;
    protected BottomToolbar mCurrentToolbar;


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

        mFillColor = mContext.getResources().getColor(R.color.accent);
        mOutlineColor = Color.BLACK;
        mSelectColor = Color.RED;

        mAnchor =
                BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_anchor);
        mAnchorRectOffsetX = -mAnchor.getWidth() * 0.1f;
        mAnchorRectOffsetY = -mAnchor.getHeight() * 0.1f;
        mAnchorCenterX = mAnchor.getWidth() * 0.75f;
        mAnchorCenterY = mAnchor.getHeight() * 0.75f;
        mAnchorTolerancePX = mAnchor.getScaledWidth(context.getResources().getDisplayMetrics());

        mDrawItems = new DrawItems();

        mListeners = new ArrayList<>();

        mMapViewOverlays.addListener(this);
        mHasEdits = false;
    }


    public void setMode(int mode)
    {
        mMode = mode;
        if (mMode == MODE_EDIT) {
            for (EditEventListener listener : mListeners) {
                listener.onStartEditSession();
            }
            mDrawItems.setSelectedPoint(0);
            mDrawItems.setSelectedRing(0);
            mMapViewOverlays.postInvalidate();
        } else if (mMode == MODE_NONE) {
            mLayer = null;
            mItem = null;
            mDrawItems.setSelectedPoint(Constants.NOT_FOUND);
            mDrawItems.setSelectedRing(Constants.NOT_FOUND);
            mMapViewOverlays.postInvalidate();
        } else if (mMode == MODE_HIGHLIGHT) {
            mMapViewOverlays.postInvalidate();
        }
    }


    public void setFeature(
            VectorLayer layer,
            VectorCacheItem item)
    {
        mLayer = layer;
        mItem = item;

        setMode(MODE_HIGHLIGHT);
    }


    @Override
    public void draw(
            Canvas canvas,
            MapDrawable mapDrawable)
    {
        if (null == mItem)
            return;
        GeoGeometry geom = mItem.getGeoGeometry();
        if (null == geom)
            return;

        fillDrawItems(geom, mapDrawable);

        switch (geom.getType()) {
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
    }


    protected void fillDrawItems(
            GeoGeometry geom,
            MapDrawable mapDrawable)
    {
        mDrawItems.clear();

        GeoPoint[] geoPoints = null;
        float[] points = null;
        GeoLineString lineString = null;
        switch (geom.getType()) {
            case GeoConstants.GTPoint:
                geoPoints = new GeoPoint[1];
                geoPoints[0] = (GeoPoint) geom;
                points = mapDrawable.mapToScreen(geoPoints);
                mDrawItems.addItems(0, points, DrawItems.TYPE_VERTEX);
                break;
            case GeoConstants.GTMultiPoint:
                GeoMultiPoint geoMultiPoint = (GeoMultiPoint) geom;
                geoPoints = new GeoPoint[geoMultiPoint.size()];
                for (int i = 0; i < geoMultiPoint.size(); i++) {
                    geoPoints[i] = geoMultiPoint.get(i);
                }
                points = mapDrawable.mapToScreen(geoPoints);
                mDrawItems.addItems(0, points, DrawItems.TYPE_VERTEX);
                break;
            case GeoConstants.GTLineString:
                /*TODO:
                lineString = (GeoLineString)geom;
                fillDrawLine(0, lineString, mapDrawable);
                */
                break;
            case GeoConstants.GTMultiLineString:
                /*TODO:
                GeoMultiLineString multiLineString = (GeoMultiLineString)geom;
                for(int i = 0; i < multiLineString.size(); i++){
                    fillDrawLine(i, multiLineString.get(i), mapDrawable);
                }
                 */
                break;
            case GeoConstants.GTPolygon:
                /*TODO:
                GeoPolygon polygon = (GeoPolygon)geom;
                fillDrawPolygon(polygon, mapDrawable);
                */
                break;
            case GeoConstants.GTMultiPolygon:
                /*TODO:
                GeoMultiPolygon multiPolygon = (GeoMultiPolygon)geom;
                for(int i = 0; i < multiPolygon.size(); i++){
                    fillDrawPolygon(multiPolygon.get(i), mapDrawable);
                }
                */
                break;
            case GeoConstants.GTGeometryCollection:
            default:
                break;
        }
    }

    protected void fillDrawPolygon(GeoPolygon polygon, MapDrawable mapDrawable){
        fillDrawLine(0, polygon.getOuterRing(), mapDrawable);
        for(int i = 0; i < polygon.getInnerRingCount(); i++){
            fillDrawLine(i + 1, polygon.getInnerRing(i), mapDrawable);
        }
    }

    protected void fillDrawLine(int ring, GeoLineString lineString, MapDrawable mapDrawable){
        GeoPoint[] geoPoints = lineString.getPoints().toArray(new GeoPoint[lineString.getPoints().size()]);
        float[] points = mapDrawable.mapToScreen(geoPoints);
        mDrawItems.addItems(ring, points, DrawItems.TYPE_VERTEX);
        float[] edgePoints = new float[points.length - 1];
        for(int i = 0; i < points.length - 2; i++){
            edgePoints[i] = (points[i] + points[i + 2]) * .5f;
        }
        mDrawItems.addItems(ring, edgePoints, DrawItems.TYPE_EDGE);
    }

    @Override
    public void drawOnPanning(
            Canvas canvas,
            PointF currentMouseOffset)
    {
        if(null == mItem)
            return;
        GeoGeometry geom = mItem.getGeoGeometry();
        if(null == geom)
            return;

        DrawItems drawItems = mDrawItems;
        if(mMode !=MODE_CHANGE) {
            drawItems = mDrawItems.pan(currentMouseOffset);
        }

        switch (geom.getType()) {
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
    }


    @Override
    public void drawOnZooming(
            Canvas canvas,
            PointF currentFocusLocation,
            float scale)
    {
        if(null == mItem)
            return;
        GeoGeometry geom = mItem.getGeoGeometry();
        if(null == geom)
            return;

        DrawItems drawItems = mDrawItems.zoom(currentFocusLocation, scale);

        switch (geom.getType()) {
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

    protected void setToolbarSaveState(boolean save){
        if(save){
            mCurrentToolbar.setNavigationIcon(R.drawable.ic_action_save);
            mCurrentToolbar.setNavigationContentDescription(R.string.save);
        }
        else{
            mCurrentToolbar.setNavigationIcon(R.drawable.ic_action_apply);
            mCurrentToolbar.setNavigationContentDescription(R.string.apply);
        }
    }

    public void setToolbar(BottomToolbar toolbar)
    {
        if(null == toolbar || null == mLayer)
            return;

        mCurrentToolbar = toolbar;
        toolbar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(mHasEdits){
                    saveEdits();
                }
                else {
                    for (EditEventListener listener : mListeners) {
                        listener.onFinishEditSession();
                    }

                    setMode(MODE_NONE);
                }
            }
        });

        setToolbarSaveState(false);
        switch (mLayer.getGeometryType()) {
            case GeoConstants.GTPoint:
                toolbar.inflateMenu(R.menu.edit_point);
                break;
            case GeoConstants.GTMultiPoint:
                toolbar.inflateMenu(R.menu.edit_multipoint);
                break;
            case GeoConstants.GTLineString:
                //TODO: toolbar.inflateMenu(R.menu.edit_line);
                break;
            case GeoConstants.GTMultiLineString:
                //toolbar.inflateMenu(R.menu.edit_multiline);
                break;
            case GeoConstants.GTPolygon:
                //TODO: toolbar.inflateMenu(R.menu.edit_polygon);
                break;
            case GeoConstants.GTMultiPolygon:
                //toolbar.inflateMenu(R.menu.edit_multipolygon);
                break;
            case GeoConstants.GTGeometryCollection:
            default:
                break;
        }

        toolbar.setOnMenuItemClickListener(new BottomToolbar.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
                if(null == mLayer)
                    return false;

                if(menuItem.getItemId() == R.id.menu_edit_attributes){
                    if(null == mItem || null == mItem.getGeoGeometry())
                        return false;
                    if(null != mLayer && mLayer instanceof  ILayerUI) {
                        ILayerUI vectorLayerUI = (ILayerUI) mLayer;
                        if (null != vectorLayerUI) {
                            vectorLayerUI.showEditForm(mContext, mItem.getId());
                        }
                    }
                }
                else if(menuItem.getItemId() == R.id.menu_edit_move_point_to_current_location){
                    if(null == mItem || null == mItem.getGeoGeometry())
                        return false;

                    Activity parent = (Activity) mContext;
                    GpsEventSource gpsEventSource = ((IGISApplication) parent.getApplication()).getGpsEventSource();
                    Location location = gpsEventSource.getLastKnownLocation();
                    if(null != location){
                        //change to screen coordinates
                        MapDrawable mapDrawable = mMapViewOverlays.getMap();
                        GeoPoint pt = new GeoPoint(location.getLongitude(), location.getLatitude());
                        pt.setCRS(GeoConstants.CRS_WGS84);
                        pt.project(GeoConstants.CRS_WEB_MERCATOR);
                        GeoPoint screenPt = mapDrawable.mapToScreen(pt);

                        mHasEdits = true;
                        setToolbarSaveState(true);
                        //store original geometry for cancel operation
                        if(null == mOriginalGeometry){
                            mOriginalGeometry = mItem.getGeoGeometry().copy();
                        }

                        mDrawItems.setSelectedPointValue((float)screenPt.getX(), (float)screenPt.getY());

                        mDrawItems.fillGeometry(0, mItem.getGeoGeometry(), mapDrawable);
                        updateMap();
                    }
                }
                else if(menuItem.getItemId() == R.id.menu_edit_add_new_multipoint){
                    mHasEdits = true;
                    setToolbarSaveState(true);

                    MapDrawable mapDrawable = mMapViewOverlays.getMap();
                    if(null == mapDrawable)
                        return false;

                    GeoPoint center = mapDrawable.getFullBounds().getCenter();

                    mItem = new VectorCacheItem(new GeoPoint(), Constants.NOT_FOUND);
                    mDrawItems.clear();
                    float[] geoPoints = new float[2];
                    geoPoints[0] = (float)center.getX();
                    geoPoints[1] = (float)center.getY();
                    mDrawItems.addItems(0, geoPoints, DrawItems.TYPE_VERTEX);
                    mDrawItems.setSelectedRing(0);
                    mDrawItems.setSelectedPoint(0);
                    //set new coordinates to GeoPoint from screen coordinates
                    mDrawItems.fillGeometry(0, mItem.getGeoGeometry(), mapDrawable);

                    updateMap();
                }
                else if(menuItem.getItemId() == R.id.menu_edit_add_new_point){
                    mHasEdits = true;
                    setToolbarSaveState(true);

                    MapDrawable mapDrawable = mMapViewOverlays.getMap();
                    if(null == mapDrawable)
                        return false;

                    GeoPoint center = mapDrawable.getFullBounds().getCenter();
                    if(mLayer.getGeometryType() == GeoConstants.GTPoint){
                        mItem = new VectorCacheItem(new GeoPoint(), Constants.NOT_FOUND);
                        mDrawItems.clear();
                        float[] geoPoints = new float[2];
                        geoPoints[0] = (float)center.getX();
                        geoPoints[1] = (float)center.getY();
                        mDrawItems.addItems(0, geoPoints, DrawItems.TYPE_VERTEX);
                        mDrawItems.setSelectedRing(0);
                        mDrawItems.setSelectedPoint(0);
                        //set new coordinates to GeoPoint from screen coordinates
                        mDrawItems.fillGeometry(0, mItem.getGeoGeometry(), mapDrawable);
                    }
                    else{
                        //TODO: insert point in appropriate position
                    }

                    updateMap();
                }
                else if(menuItem.getItemId() == R.id.menu_edit_delete_multipoint){
                    if(null == mItem || null == mItem.getGeoGeometry())
                        return false;

                    mHasEdits = true;
                    setToolbarSaveState(true);

                    mDrawItems.clear();
                    mItem.setGeoGeometry(null);

                    updateMap();
                }
                else if(menuItem.getItemId() == R.id.menu_edit_delete_point){
                    if(null == mItem || null == mItem.getGeoGeometry())
                        return false;

                    mHasEdits = true;
                    setToolbarSaveState(true);

                    //store original geometry for cancel operation
                    if(null == mOriginalGeometry){
                        mOriginalGeometry = mItem.getGeoGeometry().copy();
                    }

                    mDrawItems.deleteSelectedPoint();
                    GeoGeometry geom = mDrawItems.fillGeometry(0, mItem.getGeoGeometry(), mMapViewOverlays.getMap());
                    if(null == geom)
                        mItem.setGeoGeometry(geom);

                    updateMap();
                }
                return true;
            }
        });

    }

    protected void updateMap(){
        //do we need map refresh?
        mMapViewOverlays.onLayerChanged(mLayer.getId()); // redraw the map
        //or just overlay
        //mMapViewOverlays.postInvalidate();
    }

    protected void cancelEdits(){
        // restore
        setToolbarSaveState(false);
        mHasEdits = false;
        mMode = MODE_EDIT;
        mItem.setGeoGeometry(mOriginalGeometry); //restore original geometry
        mOriginalGeometry = null;
        mMapViewOverlays.onLayerChanged(mLayer.getId());
    }

    protected void saveEdits()
    {
        setToolbarSaveState(false);
        mHasEdits = false;
        mMode = MODE_EDIT;
        mOriginalGeometry = null;

        if(mItem.getGeoGeometry() == null && mItem.getId() != Constants.NOT_FOUND){
            mLayer.delete(VectorLayer.FIELD_ID + " = ?", new String[]{mItem.getId() + ""});
            return;
        }
        else if(mItem.getGeoGeometry() != null && mItem.getId() == Constants.NOT_FOUND){
            //create new
            ContentValues values = new ContentValues();
            try {
                values.put(VectorLayer.FIELD_GEOM, mItem.getGeoGeometry().toBlob());
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(mContext, mContext.getString(R.string.error_create_feature), Toast.LENGTH_SHORT).show();
                return;
            }
            long id = mLayer.insert(values);
            mItem = mLayer.getCacheItem(id);
        }
        //show attributes edit activity
        ILayerUI vectorLayerUI = (ILayerUI)mLayer;
        if(null != vectorLayerUI) {
            vectorLayerUI.showEditForm(mContext, mItem.getId());
        }
    }


    @Override
    public Bundle onSaveState()
    {
        Bundle bundle = super.onSaveState();
        bundle.putInt(BUNDLE_KEY_TYPE, mType);
        bundle.putInt(BUNDLE_KEY_MODE, mMode);
        if(null == mLayer){
            bundle.putInt(BUNDLE_KEY_LAYER, Constants.NOT_FOUND);
        }
        else {
            bundle.putInt(BUNDLE_KEY_LAYER, mLayer.getId());
        }

        if(null == mItem){
            bundle.putLong(BUNDLE_KEY_FEATURE, Constants.NOT_FOUND);
        }
        else{
            bundle.putLong(BUNDLE_KEY_FEATURE, mItem.getId());
        }
        return bundle;
    }


    @Override
    public void onRestoreState(Bundle bundle)
    {
        if(null != bundle){
            int type = bundle.getInt(BUNDLE_KEY_TYPE);
            if(mType == type) {
                mMode = bundle.getInt(BUNDLE_KEY_MODE);
                int layerId = bundle.getInt(BUNDLE_KEY_LAYER);
                ILayer layer = mMapViewOverlays.getLayerById(layerId);
                mLayer = null;
                if (null != layer && layer instanceof VectorLayer) {
                    mLayer = (VectorLayer)layer;
                    long featureId = bundle.getLong(BUNDLE_KEY_FEATURE);
                    for (VectorCacheItem item : mLayer.getVectorCache()) {
                        if (item.getId() == featureId) {
                            mItem = item;
                            break;
                        }
                    }
                }
                else {
                    mItem = null;
                }
            }
        }
        super.onRestoreState(bundle);
    }


    public void deleteItem() {
        if(null == mItem){
            Log.d(Constants.TAG, "delete null item");
            return;
        }
        final long itemId = mItem.getId();
        //mMapViewOverlays.setDelay(DELAY);

        if(null == mLayer){
            Log.d(Constants.TAG, "delete from null layer");
            return;
        }

        mLayer.deleteCacheItem(itemId);
        setFeature(mLayer, null);

        new UndoBarController.UndoBar((android.app.Activity) mContext)
                .message(mContext.getString(R.string.delete_done))
                .listener(new UndoBarController.AdvancedUndoListener()
        {
            @Override
            public void onHide(
                    @Nullable
                    Parcelable parcelable)
            {
                mMapViewOverlays.setSkipNextDraw(true);
                mLayer.delete(VectorLayer.FIELD_ID + " = ?", new String[]{itemId + ""});
                setFeature(null, null);
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
                //mMapViewOverlays.setDelay(DELAY);
                mLayer.restoreCacheItem();
                setFeature(null, null);
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
     * @param event Motion event
     */
    @Override
    public void onSingleTapUp(MotionEvent event)
    {
        if(null == mLayer)
            return;

        if(mHasEdits)
        {
            final float xCoord = event.getX();
            final float yCoord = event.getY();
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(mContext.getString(R.string.has_edits));
            builder.setCancelable(true);
            builder.setPositiveButton(mContext.getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        saveEdits();
                        dialog.cancel();
                    }
                });
            builder.setNegativeButton(mContext.getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        cancelEdits();
                        selectGeometryInScreenCoordinates(xCoord, yCoord);
                        dialog.cancel();
                    }
                });
            builder.create().show();
        }
        else {
            selectGeometryInScreenCoordinates(event.getX(), event.getY());
        }
    }

    protected void selectGeometryInScreenCoordinates(float x, float y){
        double dMinX = x - mTolerancePX;
        double dMaxX = x + mTolerancePX;
        double dMinY = y - mTolerancePX;
        double dMaxY = y + mTolerancePX;
        GeoEnvelope screenEnv = new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY);
        //1. search current geometry point
        if (mDrawItems.intersects(screenEnv)) {
            return;
        }

        //2 select another geometry
        GeoEnvelope mapEnv = mMapViewOverlays.screenToMap(screenEnv);
        if (null == mapEnv)
            return;
        List<VectorCacheItem> items = mLayer.query(mapEnv);
        if (items.isEmpty()) {
            return;
        }
        mItem = items.get(0);
        mDrawItems.setSelectedPoint(0);
        mDrawItems.setSelectedRing(0);

        mMapViewOverlays.postInvalidate();
    }


    @Override
    public void panStart(MotionEvent event)
    {
        if(mMode == MODE_EDIT) {

            //if pan current selected point

            double dMinX = event.getX() - mTolerancePX * 2 - mAnchorTolerancePX;
            double dMaxX = event.getX() + mTolerancePX;
            double dMinY = event.getY() - mTolerancePX * 2 - mAnchorTolerancePX;
            double dMaxY = event.getY() + mTolerancePX;
            GeoEnvelope screenEnv = new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY);
            //1. search current geometry point
            if(mDrawItems.intersects(screenEnv)) {
                PointF tempPoint = mDrawItems.getSelectedPoint();
                mTempPointOffset = new PointF(tempPoint.x - event.getX(), tempPoint.y - event.getY());
                if(null == mOriginalGeometry)
                    mOriginalGeometry = mItem.getGeoGeometry().copy();
                mMapViewOverlays.setLockMap(true);
                mMode = MODE_CHANGE;
            }
        }
    }


    @Override
    public void panMoveTo(MotionEvent e)
    {
        if(mMode == MODE_CHANGE) {
            mDrawItems.setSelectedPointValue(e.getX() + mTempPointOffset.x, e.getY() + mTempPointOffset.y);
        }
    }


    @Override
    public void panStop()
    {
        if(mMode == MODE_CHANGE) {
            mMapViewOverlays.setLockMap(false);
            mHasEdits = true;
            //change end edit session icon to save icon
            setToolbarSaveState(true);
            mMode = MODE_EDIT;
            mDrawItems.fillGeometry(0, mItem.getGeoGeometry(), mMapViewOverlays.getMap());

            mMapViewOverlays.onLayerChanged(mLayer.getId()); // redraw the map
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
        if(null != mLayer && mLayer.getId() == id){
            mHasEdits = false;
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


    protected class DrawItems{
        List<float[]> mDrawItemsVertex;
        List<float[]> mDrawItemsEdge;

        public static final int TYPE_VERTEX = 1;
        public static final int TYPE_EDGE   = 2;

        protected int mSelectedRing, mSelectedPoint;


        public DrawItems()
        {
            mDrawItemsVertex = new ArrayList<>();
            mDrawItemsEdge = new ArrayList<>();

            mSelectedRing = Constants.NOT_FOUND;
            mSelectedPoint = Constants.NOT_FOUND;
        }


        public void setSelectedRing(int selectedRing)
        {
            if(selectedRing >= mDrawItemsVertex.size())
                return;
            mSelectedRing = selectedRing;
        }


        public void setSelectedPoint(int selectedPoint)
        {
            mSelectedPoint = selectedPoint;
        }


        public void addItems(
                int ring,
                float[] points,
                int type)
        {
            if(type == TYPE_VERTEX)
                mDrawItemsVertex.add(ring, points);
            else if(type == TYPE_EDGE)
                mDrawItemsEdge.add(ring, points);
        }


        public void clear()
        {
            mDrawItemsVertex.clear();
            mDrawItemsEdge.clear();
        }

        public DrawItems zoom(PointF location, float scale){
            DrawItems drawItems = new DrawItems();
            drawItems.setSelectedRing(mSelectedRing);
            drawItems.setSelectedPoint(mSelectedPoint);

            int count = 0;
            for (float[] items : mDrawItemsVertex) {
                float[] newItems = new float[items.length];
                for(int i = 0; i < items.length - 1; i += 2){
                    newItems[i] = items[i] - (1 - scale) * (items[i] + location.x);
                    newItems[i + 1] = items[i + 1] - (1 - scale) * (items[i + 1] + location.y);
                }
                drawItems.addItems(count++, newItems, TYPE_VERTEX);
            }

            count = 0;
            for (float[] items : mDrawItemsEdge) {
                float[] newItems = new float[items.length];
                for(int i = 0; i < items.length - 1; i += 2){
                    newItems[i] = items[i] - (1 - scale) * (items[i] + location.x);
                    newItems[i + 1] = items[i + 1] - (1 - scale) * (items[i + 1] + location.y);
                }
                drawItems.addItems(count++, newItems, TYPE_EDGE);
            }

            return drawItems;
        }

        public DrawItems pan(PointF offset){
            DrawItems drawItems = new DrawItems();
            drawItems.setSelectedRing(mSelectedRing);
            drawItems.setSelectedPoint(mSelectedPoint);

            int count = 0;
            for (float[] items : mDrawItemsVertex) {
                float[] newItems = new float[items.length];
                for(int i = 0; i < items.length - 1; i += 2){
                    newItems[i] = items[i] - offset.x;
                    newItems[i + 1] = items[i + 1] - offset.y;
                }
                drawItems.addItems(count++, newItems, TYPE_VERTEX);
            }

            count = 0;
            for (float[] items : mDrawItemsEdge) {
                float[] newItems = new float[items.length];
                for(int i = 0; i < items.length - 1; i += 2){
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
            if(mSelectedPoint != Constants.NOT_FOUND) {

                float[] items = getSelectedRing();
                if(null != items) {
                    mPaint.setColor(mSelectColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS);

                    canvas.drawPoint(items[mSelectedPoint], items[mSelectedPoint + 1], mPaint);

                    float anchorX = items[mSelectedPoint] + mAnchorRectOffsetX;
                    float anchorY = items[mSelectedPoint + 1] + mAnchorRectOffsetY;
                    canvas.drawBitmap(mAnchor, anchorX, anchorY, null);
                }
            }
        }

        public void drawLines(Canvas canvas)
        {
            for (float[] items : mDrawItemsVertex) {

                mPaint.setColor(mFillColor);
                mPaint.setStrokeWidth(LINE_WIDTH);

                for(int i = 0; i < items.length - 3; i+= 2) {
                    canvas.drawLine(items[i], items[i + 1], items[i + 2], items[i + 3], mPaint);
                }

                if(mMode == MODE_EDIT) {
                    mPaint.setColor(mOutlineColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS + 2);
                    canvas.drawPoints(items, mPaint);

                    mPaint.setColor(mFillColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS);
                    canvas.drawPoints(items, mPaint);
                }
            }

            if(mMode == MODE_EDIT) {
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
            if(mSelectedPoint != Constants.NOT_FOUND) {
                float[] items = getSelectedRing();
                if(null != items && mSelectedPoint + 1 < items.length) {
                    mPaint.setColor(mSelectColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS);

                    canvas.drawPoint(items[mSelectedPoint], items[mSelectedPoint + 1], mPaint);
                }
            }
        }


        public boolean intersects(GeoEnvelope screenEnv)
        {
            int ring = 0;
            int point = 0;
            for (float[] items : mDrawItemsVertex) {
                for(int i = 0; i < items.length - 1; i += 2){
                    if(screenEnv.contains(new GeoPoint(items[i], items[i + 1]))){
                        mSelectedRing = ring;
                        mSelectedPoint = point;
                        return true;
                    }
                    point++;

                }
                ring++;
            }

            if(mMode == MODE_EDIT) {
                ring = 0;
                point = 0;
                for (float[] items : mDrawItemsEdge) {
                    for (int i = 0; i < items.length - 1; i += 2) {
                        if(screenEnv.contains(new GeoPoint(items[i], items[i + 1]))){
                            //mSelectedRing = ring;
                            //mSelectedPoint = point;
                            //TODO: store PointF and on pan this point add it to the vertex array
                            //and on pan stop add new edge points
                            return true;
                        }
                        point++;
                    }
                    ring++;
                }
            }
            return false;
        }


        public PointF getSelectedPoint()
        {
            float[] points = getSelectedRing();
            if(null == points || mSelectedPoint < 0 || points.length <= mSelectedPoint)
                return null;
            return new PointF(points[mSelectedPoint], points[mSelectedPoint + 1]);
        }

        protected int getMinPointCount(){
            switch (mLayer.getGeometryType()){
                case GeoConstants.GTPoint:
                case GeoConstants.GTMultiPoint:
                    return 2;
                case GeoConstants.GTLineString:
                case GeoConstants.GTMultiLineString:
                    return 4;
                case GeoConstants.GTPolygon:
                case GeoConstants.GTMultiPolygon:
                    return 6;
                default:
                    return 2;
            }
        }

        public void addNewPoint(float x, float y){
            float[] points = getSelectedRing();
            if(null == points)
                return;
            float[] newPoints = new float[points.length + 2];
            System.arraycopy(points, 0, newPoints, 0, points.length);
            newPoints[points.length] = x;
            newPoints[points.length + 1] = y;

            mDrawItemsVertex.set(mSelectedRing, newPoints);
        }

        protected float[] getSelectedRing(){
            if(mDrawItemsVertex.isEmpty() || mSelectedRing < 0 || mSelectedRing >= mDrawItemsVertex.size())
                return null;
            return mDrawItemsVertex.get(mSelectedRing);
        }

        public void deleteSelectedPoint(){
            float[] points = getSelectedRing();
            if(null == points || mSelectedPoint < 0)
                return;
            if(points.length <= getMinPointCount()){
                mDrawItemsVertex.remove(mSelectedRing);
                mSelectedRing--;
                mSelectedPoint = Constants.NOT_FOUND;
                return;
            }
            float[] newPoints = new float[points.length - 2];
            int counter = 0;
            for(int i = 0; i < points.length; i++){
                if(i == mSelectedPoint || i == mSelectedPoint + 1)
                    continue;
                newPoints[counter++] = points[i];
            }

            if(mSelectedPoint >= newPoints.length)
                mSelectedPoint = 0;

            mDrawItemsVertex.set(mSelectedRing, newPoints);
        }

        public void setSelectedPointValue(float x, float y){
            float[] points = getSelectedRing();
            if(null != points && mSelectedPoint > Constants.NOT_FOUND){
                points[mSelectedPoint] = x;
                points[mSelectedPoint + 1] = y;
            }
        }

        public GeoGeometry fillGeometry(int ring, GeoGeometry geometry, MapDrawable mapDrawable){
            if(null == geometry || null == mapDrawable || ring < 0 || ring >= mDrawItemsVertex.size() )
                return null;

            GeoPoint[] points;
            switch (geometry.getType()) {
                case GeoConstants.GTPoint:
                    if(mDrawItemsVertex.isEmpty())
                        return null;
                    points = mapDrawable.screenToMap(mDrawItemsVertex.get(ring));
                    GeoPoint point = (GeoPoint) geometry;
                    point.setCoordinates(points[0].getX(), points[0].getY());
                    break;
                case GeoConstants.GTMultiPoint:
                    if(mDrawItemsVertex.isEmpty())
                        return null;
                    points = mapDrawable.screenToMap(mDrawItemsVertex.get(ring));
                    GeoMultiPoint multiPoint = (GeoMultiPoint)geometry;
                    multiPoint.clear();
                    for (GeoPoint geoPoint : points) {
                        multiPoint.add(geoPoint);
                    }
                    break;
                case GeoConstants.GTLineString:
                    if(mDrawItemsVertex.isEmpty())
                        return null;
                    points = mapDrawable.screenToMap(mDrawItemsVertex.get(ring));
                    GeoLineString lineString = (GeoLineString)geometry;
                    lineString.clear();
                    for (GeoPoint geoPoint : points) {
                        if(null == geoPoint)
                            continue;
                        lineString.add(geoPoint);
                    }
                    break;
                case GeoConstants.GTMultiLineString:
                    GeoMultiLineString multiLineString = (GeoMultiLineString) geometry;

                    //  the geometry should be correspond the vertex list

                    for(int currentRing = 0; currentRing < multiLineString.size(); currentRing++){
                        fillGeometry(ring + currentRing, multiLineString.get(currentRing), mapDrawable);
                    }
                    break;
                case GeoConstants.GTPolygon:
                    if(mDrawItemsVertex.isEmpty())
                        return null;
                    GeoPolygon polygon = (GeoPolygon) geometry;
                    fillGeometry(ring, polygon.getOuterRing(), mapDrawable);

                    //  the geometry should be correspond the vertex list

                    for(int currentRing = 0; currentRing < polygon.getInnerRingCount(); currentRing++){
                        fillGeometry(ring + currentRing + 1, polygon.getInnerRing(currentRing), mapDrawable);
                    }
                    break;
                case GeoConstants.GTMultiPolygon:
                    GeoMultiPolygon multiPolygon = (GeoMultiPolygon)geometry;

                    //  the geometry should be correspond the vertex list

                    int currentRing = 0;
                    for(int i = 0; i < multiPolygon.size(); i++){
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
    }
}

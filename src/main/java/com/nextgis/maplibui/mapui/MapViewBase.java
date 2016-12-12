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

package com.nextgis.maplibui.mapui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplibui.api.MapViewEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.DEFAULT_MAX_ZOOM;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_KEEPSCREENON;


public class MapViewBase
        extends View
{

    protected MapDrawable                mMap;
    protected List<MapViewEventListener> mListeners;

    interface OnNeedRedraw {
        public void OnDirty();
    }

    public MapViewBase(
            Context context,
            MapDrawable map)
    {
        super(context);

        mMap = map;
        mListeners = new ArrayList<>();
    }


    public MapDrawable getMap()
    {
        return mMap;
    }


    @Override
    protected void onVisibilityChanged(
            @NonNull
            View changedView,
            int visibility)
    {
        super.onVisibilityChanged(changedView, visibility);

        if (View.GONE != visibility) {
            setKeepScreenOnByPref();
        }
    }


    @Override
    protected void onWindowVisibilityChanged(int visibility)
    {
        super.onWindowVisibilityChanged(visibility);

        if (View.GONE != visibility) {
            setKeepScreenOnByPref();
        }
    }


    @Override
    protected void onDetachedFromWindow()
    {
        if (null != mMap) {
            mMap.cancelDraw();
        }

        super.onDetachedFromWindow();
    }


    protected void setKeepScreenOnByPref()
    {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean keepScreen = sharedPreferences.getBoolean(KEY_PREF_KEEPSCREENON, false);
        setKeepScreenOn(keepScreen);
    }


    public void addListener(MapViewEventListener listener)
    {
        if (mMap != null) {
            mMap.addListener(listener);
        }

        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }


    public void removeListener(MapViewEventListener listener)
    {
        if (mMap != null) {
            mMap.removeListener(listener);
        }

        mListeners.remove(listener);
    }


    @Override
    protected void onDraw(Canvas canvas)
    {
        if (mMap != null) {
            mMap.draw(canvas, 0, 0, false);
        } else {
            super.onDraw(canvas);
        }
    }


    @Override
    protected void onSizeChanged(
            int w,
            int h,
            int oldw,
            int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mMap != null) {
            mMap.setViewSize(w, h);
        }
    }


    @Override
    protected void onLayout(
            boolean changed,
            int left,
            int top,
            int right,
            int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);
        if (mMap != null) {
            mMap.setViewSize(right - left, bottom - top);
        }
    }


    public boolean canZoomIn()
    {
        return mMap != null && mMap.getZoomLevel() < mMap.getMaxZoom();
    }


    public boolean canZoomOut()
    {
        return mMap != null && mMap.getZoomLevel() > mMap.getMinZoom();
    }


    public final float getZoomLevel()
    {
        if (mMap == null) {
            return 0;
        }
        return mMap.getZoomLevel();
    }


    public final float getMaxZoom()
    {
        if (mMap == null) {
            return DEFAULT_MAX_ZOOM;
        }
        return mMap.getMaxZoom();
    }


    public final float getMinZoom()
    {
        if (mMap == null) {
            return 0;
        }
        return mMap.getMinZoom();
    }


    public final GeoPoint getMapCenter()
    {
        if (mMap != null) {
            return mMap.getMapCenter();
        }
        return new GeoPoint();
    }


    public void setZoomAndCenter(
            float zoom,
            GeoPoint center)
    {
        if (mMap != null) {
            mMap.setZoomAndCenter(zoom, center);
        }
    }

    public void zoomToExtent(GeoEnvelope envelope){
        if (mMap != null) {
            mMap.zoomToExtent(envelope);
        }
    }


    public void zoomIn()
    {
        setZoomAndCenter((float) Math.ceil(getZoomLevel() + 0.5), getMapCenter());
    }


    public void zoomOut()
    {
        setZoomAndCenter((float) Math.floor(getZoomLevel() - 0.5), getMapCenter());
    }


    public void addRemoteLayer()
    {
        if (mMap != null) {
            mMap.getLayerFactory().createNewRemoteTMSLayer(getContext(), mMap);
        }
    }


    public void addNGWLayer()
    {
        if (mMap != null) {
            mMap.getLayerFactory().createNewNGWLayer(getContext(), mMap);
        }
    }


    public List<ILayer> getLayersByType(int types)
    {
        List<ILayer> ret = new ArrayList<>();

        if (mMap != null) {
            LayerGroup.getLayersByType(mMap, types, ret);
        }

        return ret;
    }


    public ILayer getLayerById(int id)
    {
        if (mMap != null) {
            return mMap.getLayerById(id);
        }
        return null;
    }


    public List<ILayer> getVectorLayersByType(int types)
    {
        List<ILayer> ret = new ArrayList<>();

        if (mMap != null) {
            LayerGroup.getVectorLayersByType(mMap, types, ret);
        }

        return ret;
    }


    public void addLocalTMSLayer(Uri uri)
    {
        if (mMap != null) {
            mMap.getLayerFactory().createNewLocalTMSLayer(getContext(), mMap, uri);
        }
    }


    public void addLocalVectorLayer(Uri uri)
    {
        if (mMap != null) {
            mMap.getLayerFactory().createNewVectorLayer(getContext(), mMap, uri);
        }
    }


    public void addLocalVectorLayerWithForm(Uri uri)
    {
        if (mMap != null) {
            mMap.getLayerFactory().createNewVectorLayerWithForm(getContext(), mMap, uri);
        }
    }


    public GeoEnvelope screenToMap(GeoEnvelope envelope)
    {
        if (mMap != null) {
            return mMap.screenToMap(envelope);
        }

        return null;
    }


}

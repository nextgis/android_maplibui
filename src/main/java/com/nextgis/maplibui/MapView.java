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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.MapEventListener;
import com.nextgis.maplibui.mapui.LayerFactoryUI;

import java.io.File;

import static com.nextgis.maplib.util.Constants.*;

public class MapView extends View {

    protected MapDrawable mMap;

    public MapView(Context context) {
        super(context);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        File defaultPath = context.getExternalFilesDir(PREFS_MAP);
        String mapPath = sharedPreferences.getString(KEY_PREF_MAP_PATH, defaultPath.getPath());
        String mapName = sharedPreferences.getString(KEY_PREF_MAP_NAME, "default.ngm");

        File mapFullPath = new File(mapPath, mapName);

        final Bitmap bkBitmap = BitmapFactory.decodeResource(context.getResources(), com.nextgis.maplibui.R.drawable.bk_tile);
        mMap = new MapDrawable(bkBitmap, mapFullPath, new LayerFactoryUI());

        if(sharedPreferences.getBoolean(KEY_PREF_KEEPSCREENON, false))
            setKeepScreenOn(true);
    }

    public void addListener(MapEventListener listener){
        if(mMap != null){
            mMap.addListener(listener);
        }
    }

    public void removeListener(MapEventListener listener){
        if(mMap != null){
            mMap.removeListener(listener);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mMap != null){
            canvas.drawBitmap(mMap.getMap(), 0, 0, null);
        }
        else{
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(mMap != null){
            mMap.setSize(w, h);
        }
    }

    /**
     * Standard error report function
     *
     * @param errMsg An error message
     */
    protected void reportError(String errMsg){
        Log.d(TAG, errMsg);
        Toast.makeText(getContext(), errMsg, Toast.LENGTH_SHORT).show();
    }

    public boolean canZoomIn(){
        return mMap != null && mMap.getZoomLevel() < mMap.getMaxZoomLevel();
    }

    public boolean canZoomOut(){
        return mMap != null && mMap.getZoomLevel() > mMap.getMinZoomLevel();
    }

    public final double getZoomLevel(){
        if(mMap == null)
            return 0;
        return mMap.getZoomLevel();
    }

    public final GeoPoint getMapCenter(){
        if(mMap != null)
            return mMap.getMapCenter();
        return new GeoPoint();
    }

    public void setZoomAndCenter(final double zoom, final GeoPoint center){
        if(mMap != null)
            mMap.setZoomAndCenter(zoom, center);
    }

    public void zoomIn() {
        setZoomAndCenter((float)Math.ceil(getZoomLevel() + 0.5), getMapCenter());
    }

    public void zoomOut() {
        setZoomAndCenter((float)Math.floor(getZoomLevel() - 0.5), getMapCenter());
    }

    public MapDrawable getMap() {
        return mMap;
    }
}

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui;

import android.content.Context;
import android.graphics.Canvas;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplibui.api.Overlay;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplibui.util.ConstantsUI.*;


public class MapViewOverlays
        extends MapView
{
    protected List<Overlay> mOverlays;


    public MapViewOverlays(
            Context context,
            MapDrawable map)
    {
        super(context, map);
        mOverlays = new ArrayList<>();
    }


    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (mMap != null) {
            switch (mDrawingState) {
                case DRAW_SATE_drawing:
                case DRAW_SATE_drawing_noclearbk:
                    for (Overlay overlay : mOverlays) {
                        overlay.draw(canvas, mMap);
                    }
                    break;
                case DRAW_SATE_panning:
                case DRAW_SATE_panning_fling:
                    for (Overlay overlay : mOverlays) {
                        overlay.drawOnPanning(canvas, mCurrentMouseOffset);
                    }
                    break;
                case DRAW_SATE_zooming:
                    for (Overlay overlay : mOverlays) {
                        overlay.drawOnZooming(canvas, mCurrentFocusLocation, (float) mScaleFactor);
                    }
                    break;
            }
        }
    }


    public MapDrawable getMap()
    {
        return mMap;
    }


    public void addOverlay(Overlay overlay)
    {
        mOverlays.add(overlay);
    }


    public void removeOverlay(Overlay overlay)
    {
        mOverlays.remove(overlay);
    }


    public List<Overlay> getOverlays()
    {
        return mOverlays;
    }
}

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

import android.graphics.Canvas;
import android.graphics.PointF;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplibui.api.Overlay;


/**
 * The class for edit vector features
 */
public class EditLayerOverlay
        extends Overlay
{

    @Override
    public void draw(
            Canvas canvas,
            MapDrawable mapDrawable)
    {

    }


    @Override
    public void drawOnPanning(
            Canvas canvas,
            PointF mCurrentMouseOffset)
    {

    }


    @Override
    public void drawOnZooming(
            Canvas canvas,
            PointF mCurrentFocusLocation,
            float scale)
    {

    }
}

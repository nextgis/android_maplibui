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

import android.content.Context;
import com.nextgis.maplib.map.MapDrawable;


/**
 * The class for edit vector features
 */
public class MapViewEditable extends MapViewOverlays
{
    protected static final int mToleranceDP = 20;
    protected final float mTolerancePX;


    public MapViewEditable(
            Context context,
            MapDrawable map)
    {
        super(context, map);
        mTolerancePX = getContext().getResources().getDisplayMetrics().density * mToleranceDP;
    }
}

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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.nextgis.maplib.map.MapDrawable;


public class MapView
        extends MapViewBase
        implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,  ScaleGestureDetector.OnScaleGestureListener
{

    public MapView(
            Context context,
            MapDrawable map)
    {
        super(context, map);
    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent)
    {
        return false;
    }


    @Override
    public boolean onDoubleTap(MotionEvent motionEvent)
    {
        return false;
    }


    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent)
    {
        return false;
    }


    @Override
    public boolean onDown(MotionEvent motionEvent)
    {
        return false;
    }


    @Override
    public void onShowPress(MotionEvent motionEvent)
    {

    }


    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent)
    {
        return false;
    }


    @Override
    public boolean onScroll(
            MotionEvent motionEvent,
            MotionEvent motionEvent2,
            float v,
            float v2)
    {
        return false;
    }


    @Override
    public void onLongPress(MotionEvent motionEvent)
    {

    }


    @Override
    public boolean onFling(
            MotionEvent motionEvent,
            MotionEvent motionEvent2,
            float v,
            float v2)
    {
        return false;
    }


    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector)
    {
        return false;
    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector)
    {
        return false;
    }


    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector)
    {

    }
}

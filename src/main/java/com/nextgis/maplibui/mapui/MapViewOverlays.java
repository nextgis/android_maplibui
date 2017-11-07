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
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplibui.api.Overlay;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplibui.util.ConstantsUI.*;


public class MapViewOverlays
        extends MapView
{
    protected List<Overlay> mOverlays;
    protected boolean       mLockMap;
    //protected boolean mSkipNextDraw;
    //protected long mDelay;


    public MapViewOverlays(
            Context context,
            MapDrawable map)
    {
        super(context, map);
        mOverlays = new ArrayList<>();
        mLockMap = false;
//        mSkipNextDraw = false;
//        mDelay = 0;
    }


    public boolean isLockMap()
    {
        return mLockMap;
    }


    public void setLockMap(boolean lockMap)
    {
        mLockMap = lockMap;
    }


    @Override
    protected void onDraw(Canvas canvas)
    {
        if (isLockMap()) {
            mMap.draw(canvas, 0, 0, false);
        } else {
            super.onDraw(canvas);
        }

        if (mMap != null) {
            switch (mDrawingState) {
                case DRAW_STATE_drawing:
                case DRAW_STATE_drawing_noclearbk:
                    for (Overlay overlay : mOverlays)
                        if (overlay.isVisible())
                            overlay.draw(canvas, mMap);
                    break;
                case DRAW_STATE_panning:
                case DRAW_STATE_panning_fling:
                    for (Overlay overlay : mOverlays)
                        if (overlay.isVisible())
                            overlay.drawOnPanning(canvas, mCurrentMouseOffset);
                    break;
                case DRAW_STATE_zooming:
                    for (Overlay overlay : mOverlays)
                        if (overlay.isVisible())
                            overlay.drawOnZooming(canvas, mCurrentFocusLocation, (float) mScaleFactor);
                    break;
            }
        }
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


    @Override
    protected Parcelable onSaveInstanceState()
    {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);

        for (Overlay overlay : mOverlays) {
            savedState.add(overlay.onSaveState());
        }

        return savedState;
    }


    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;

            int counter = 0;
            for (Overlay overlay : mOverlays) {
                overlay.onRestoreState(savedState.get(counter++));
            }

            super.onRestoreInstanceState(savedState.getSuperState());
        } else {
            super.onRestoreInstanceState(state);
        }
    }


    @Override
    public void setZoomAndCenter(
            float zoom,
            GeoPoint center)
    {
        if (isLockMap()) {
            Log.d(TAG, "setZoomAndCenter: isLockMap");
            mDrawingState = DRAW_STATE_drawing_noclearbk;
            return;
        }
        super.setZoomAndCenter(zoom, center);
    }


    public void buffer()
    {
        if (null != mMap) {
            mMap.buffer(0, 0, 1);
        }
    }


    public static class SavedState
            extends BaseSavedState
    {
        protected List<Bundle> mBundles;


        public SavedState(Parcelable parcel)
        {
            super(parcel);
            mBundles = new ArrayList<>();
        }


        private SavedState(Parcel in)
        {
            super(in);

            mBundles = new ArrayList<>();
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                Bundle bundle = in.readBundle();
                mBundles.add(bundle);
            }
        }


        public void add(Bundle bundle)
        {
            mBundles.add(bundle);
        }


        public Bundle get(int index)
        {
            return mBundles.get(index);
        }


        @Override
        public void writeToParcel(
                @NonNull
                Parcel out,
                int flags)
        {
            super.writeToParcel(out, flags);

            out.writeInt(mBundles.size());
            for (Bundle bundle : mBundles) {
                out.writeBundle(bundle);
            }
        }


        public static final Creator<SavedState> CREATOR = new Creator<SavedState>()
        {

            @Override
            public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in);
            }


            @Override
            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
    }
}

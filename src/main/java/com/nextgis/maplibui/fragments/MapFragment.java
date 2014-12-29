/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2012-2014 NextGIS
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
package com.nextgis.maplibui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.MapEventListener;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.mapui.MapView;


public class MapFragment extends Fragment implements MapEventListener, IFragmentBase {

    protected final static int mMargings = 10;

    protected MapView mMap;
    protected ImageView mivZoomIn;
    protected ImageView mivZoomOut;
    protected TextView mivZoomLevel;

    protected RelativeLayout mMapRelativeLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mapfragment, container, false);
        FrameLayout layout = (FrameLayout) view.findViewById(R.id.mapholder);

        //search relative view of map, if not found - add it
        if (mMap != null) {
            mMapRelativeLayout = (RelativeLayout) layout.findViewById(R.id.maprl);
            if (mMapRelativeLayout != null) {
                mMapRelativeLayout.addView(mMap, new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT));
                addMapButtons(mMapRelativeLayout);
            }
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        mMapRelativeLayout.removeView(mMap);
        super.onDestroyView();
    }

    protected void addMapButtons(RelativeLayout rl) {
        mivZoomIn = new ImageView(getActivity());
        mivZoomIn.setImageResource(R.drawable.ic_plus);
        //mivZoomIn.setId(R.drawable.ic_plus);

        mivZoomOut = new ImageView(getActivity());
        mivZoomOut.setImageResource(R.drawable.ic_minus);
        //mivZoomOut.setId(R.drawable.ic_minus);

        final ImageView ivMark = new ImageView(getActivity());
        ivMark.setImageResource(R.drawable.ic_mark);
        //ivMark.setId(R.drawable.ic_mark);

        //show zoom level between plus and minus
        mivZoomLevel = new TextView(getActivity());
        //ivZoomLevel.setAlpha(150);
        mivZoomLevel.setId(R.drawable.ic_zoomlevel);

        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (48 * scale + 0.5f);

        mivZoomLevel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        //ivZoomLevel.setTextAppearance(this, android.R.attr.textAppearanceLarge);

        mivZoomLevel.setWidth(pixels);
        mivZoomLevel.setHeight(pixels);
        mivZoomLevel.setTextColor(Color.DKGRAY);
        mivZoomLevel.setBackgroundColor(Color.argb(50, 128, 128, 128)); //Color.LTGRAY R.drawable.ic_zoomlevel);
        mivZoomLevel.setGravity(Gravity.CENTER);
        mivZoomLevel.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        mivZoomLevel.setText("" + (int) Math.floor(mMap.getZoomLevel()));

        mivZoomIn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mMap.zoomIn();
            }
        });

        mivZoomOut.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mMap.zoomOut();
            }
        });

        ivMark.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //TODO: onMark();
            }
        });

        final RelativeLayout.LayoutParams RightParams1 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RightParams1.setMargins(mMargings + 5, mMargings - 5, mMargings + 5, mMargings - 5);
        RightParams1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        RightParams1.addRule(RelativeLayout.CENTER_IN_PARENT);//ALIGN_PARENT_TOP
        rl.addView(mivZoomLevel, RightParams1);

        final RelativeLayout.LayoutParams RightParams4 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RightParams4.setMargins(mMargings + 5, mMargings - 5, mMargings + 5, mMargings - 5);
        RightParams4.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        RightParams4.addRule(RelativeLayout.ABOVE, R.drawable.ic_zoomlevel);//ALIGN_PARENT_TOP
        rl.addView(mivZoomIn, RightParams4);

        final RelativeLayout.LayoutParams RightParams3 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RightParams3.setMargins(mMargings + 5, mMargings - 5, mMargings + 5, mMargings - 5);
        RightParams3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        RightParams3.addRule(RelativeLayout.CENTER_IN_PARENT);//ALIGN_PARENT_TOP
        rl.addView(ivMark, RightParams3);

        final RelativeLayout.LayoutParams RightParams2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RightParams2.setMargins(mMargings + 5, mMargings - 5, mMargings + 5, mMargings - 5);
        RightParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        RightParams2.addRule(RelativeLayout.BELOW, R.drawable.ic_zoomlevel);//R.drawable.ic_plus);
        rl.addView(mivZoomOut, RightParams2);

        setZoomInEnabled(mMap.canZoomIn());
        setZoomOutEnabled(mMap.canZoomOut());
    }

    @Override
    public void onLayerAdded(Layer layer) {

    }

    @Override
    public void onLayerDeleted(int id) {

    }

    @Override
    public void onLayerChanged(Layer layer) {

    }

    @Override
    public void onExtentChanged(int zoom, GeoPoint center) {
        mivZoomLevel.setText("" + (int) Math.floor(mMap.getZoomLevel()));
        setZoomInEnabled(mMap.canZoomIn());
        setZoomOutEnabled(mMap.canZoomOut());
    }

    @Override
    public void onLayersReordered() {

    }

    protected void setZoomInEnabled(boolean bEnabled) {
        if (bEnabled) {
            mivZoomIn.getDrawable().setAlpha(255);
        } else {
            mivZoomIn.getDrawable().setAlpha(50);
        }
    }

    protected void setZoomOutEnabled(boolean bEnabled) {
        if (bEnabled) {
            mivZoomOut.getDrawable().setAlpha(255);
        } else {
            mivZoomOut.getDrawable().setAlpha(50);
        }
    }

    @Override
    public boolean onInit(String title, MapView map) {
        mMap = map;
        mMap.addListener(this);
        return true;
    }
}

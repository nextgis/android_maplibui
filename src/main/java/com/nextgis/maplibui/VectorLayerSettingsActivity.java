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

package com.nextgis.maplibui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.EditText;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;


/**
 * Vector layer settings activity. Include common settings (layer name) and renderer settings.
 */
public class VectorLayerSettingsActivity
        extends ActionBarActivity
{
    public final static String LAYER_ID_KEY = "layer_id";
    protected VectorLayer mVectorLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vectorlayer_settings);

        short layerId = Constants.NOT_FOUND;
        if(savedInstanceState != null){
            layerId = savedInstanceState.getShort(LAYER_ID_KEY);
        }
        else {
            layerId = getIntent().getShortExtra(LAYER_ID_KEY, layerId);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.getBackground().setAlpha(255);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        IGISApplication application = (IGISApplication) getApplication();

        MapBase map = application.getMap();
        if(null != map){
            ILayer layer = map.getLayerById(layerId);
            if(null != layer && layer.getType() == Constants.LAYERTYPE_LOCAL_VECTOR){
                mVectorLayer = (VectorLayer) layer;
            }
        }

        if(null != mVectorLayer){
            EditText editText = (EditText) findViewById(R.id.layer_name);
            editText.setText(mVectorLayer.getName());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

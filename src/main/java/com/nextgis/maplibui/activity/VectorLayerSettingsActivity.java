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

package com.nextgis.maplibui.activity;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IChooseColorResult;
import com.nextgis.maplibui.dialog.ChooseColorDialog;

import java.util.ArrayList;
import java.util.List;


/**
 * Vector layer settings activity. Include common settings (layer name) and renderer settings.
 */
public class VectorLayerSettingsActivity
        extends NGActivity
        implements IChooseColorResult
{
    public final static String LAYER_ID_KEY = "layer_id";
    protected VectorLayer                 mVectorLayer;
    protected List<Pair<Integer, String>> mColors;
    protected int                         mCurrentColor;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mColors = new ArrayList<>();
        mColors.add(new Pair<>(Color.RED, getString(R.string.red)));
        mColors.add(new Pair<>(Color.GREEN, getString(R.string.green)));
        mColors.add(new Pair<>(Color.BLUE, getString(R.string.blue)));
        mColors.add(new Pair<>(Color.MAGENTA, getString(R.string.magenta)));
        mColors.add(new Pair<>(Color.YELLOW, getString(R.string.yellow)));
        mColors.add(new Pair<>(Color.CYAN, getString(R.string.cyan)));

        setContentView(R.layout.activity_vectorlayer_settings);
        setToolbar(R.id.main_toolbar);

        short layerId = Constants.NOT_FOUND;
        if (savedInstanceState != null) {
            layerId = savedInstanceState.getShort(LAYER_ID_KEY);
        } else {
            layerId = getIntent().getShortExtra(LAYER_ID_KEY, layerId);
        }

        IGISApplication application = (IGISApplication) getApplication();

        MapBase map = application.getMap();
        if (null != map) {
            ILayer layer = map.getLayerById(layerId);
            if (null != layer && layer.getType() == Constants.LAYERTYPE_LOCAL_VECTOR) {
                mVectorLayer = (VectorLayer) layer;
            }
        }

        if (null != mVectorLayer) {
            EditText editText = (EditText) findViewById(R.id.layer_name);
            editText.setText(mVectorLayer.getName());

            LinearLayout color_row = (LinearLayout) findViewById(R.id.color_row);
            color_row.setOnClickListener(
                    new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            //show colors list
                            ChooseColorDialog newChooseColorDialog = new ChooseColorDialog();
                            newChooseColorDialog.setTitle(getString(R.string.select_color))
                                    .setColors(mColors)
                                    .show(
                                            VectorLayerSettingsActivity.this.getSupportFragmentManager(),
                                            "choose_color");
                        }
                    });

            // set color
            SimpleFeatureRenderer sfr = (SimpleFeatureRenderer) mVectorLayer.getRenderer();
            if (null != sfr) {
                Style style = sfr.getStyle();
                if (null != style) {
                    int color = style.getColor();
                    setColor(color);
                }
            }
        }
    }


    protected void setColor(int color)
    {
        // set color
        ImageView iv = (ImageView) findViewById(R.id.color_image);
        GradientDrawable sd = (GradientDrawable) iv.getDrawable();
        sd.setColor(color);
        iv.invalidate();

        // set color name
        TextView tv = (TextView) findViewById(R.id.color_name);
        tv.setText(getColorName(color));

        mCurrentColor = color;
    }

    protected String getColorName(int color)
    {
        for (Pair<Integer, String> colorEntry : mColors) {
            if (colorEntry.first == color) {
                return colorEntry.second;
            }
        }
        return "#" + Integer.toHexString(color & 0x00FFFFFF);
    }


    @Override
    public void onFinishChooseColorDialog(int color)
    {
        setColor(color);
    }


    protected void saveSettings()
    {
        if (null == mVectorLayer) {
            return;
        }
        EditText editText = (EditText) findViewById(R.id.layer_name);
        mVectorLayer.setName(editText.getEditableText().toString());
        // set color
        SimpleFeatureRenderer sfr = (SimpleFeatureRenderer) mVectorLayer.getRenderer();
        if (null != sfr) {
            Style style = sfr.getStyle();
            if (null != style) {
                style.setColor(mCurrentColor);
            }
        }

        mVectorLayer.save();
    }


    @Override
    protected void onPause()
    {
        super.onPause();
        saveSettings();
    }
}

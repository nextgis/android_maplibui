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

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.edmodo.rangebar.RangeBar;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IChooseColorResult;
import com.nextgis.maplibui.dialog.ChooseColorDialog;
import com.nextgis.maplibui.util.ConstantsUI;

import java.util.ArrayList;
import java.util.List;


/**
 * Vector layer settings activity. Include common settings (layer name) and renderer settings.
 */
public class VectorLayerSettingsActivity
        extends NGActivity
        implements IChooseColorResult
{
    protected VectorLayer                 mVectorLayer;
    protected List<Pair<Integer, String>> mColors;
    protected int                         mCurrentColor;
    protected BackgroundTask mBackgroundTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mBackgroundTask = null;

        mColors = new ArrayList<>();
        mColors.add(new Pair<>(Color.RED, getString(R.string.red)));
        mColors.add(new Pair<>(Color.GREEN, getString(R.string.green)));
        mColors.add(new Pair<>(Color.BLUE, getString(R.string.blue)));
        mColors.add(new Pair<>(Color.MAGENTA, getString(R.string.magenta)));
        mColors.add(new Pair<>(Color.YELLOW, getString(R.string.yellow)));
        mColors.add(new Pair<>(Color.CYAN, getString(R.string.cyan)));

        setContentView(R.layout.activity_vectorlayer_settings);
        setToolbar(R.id.main_toolbar);

        int layerId = Constants.NOT_FOUND;
        if (savedInstanceState != null) {
            layerId = savedInstanceState.getInt(ConstantsUI.KEY_LAYER_ID);
        } else {
            layerId = getIntent().getIntExtra(ConstantsUI.KEY_LAYER_ID, layerId);
        }

        IGISApplication application = (IGISApplication) getApplication();

        MapBase map = application.getMap();
        if (null != map) {
            ILayer layer = map.getLayerById(layerId);
            if (null != layer && (layer.getType() == Constants.LAYERTYPE_LOCAL_VECTOR ||
            layer.getType() == Constants.LAYERTYPE_NGW_VECTOR)) {
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
                            newChooseColorDialog.setColors(mColors)
                                    .setTitle(getString(R.string.select_color))
                                    .setTheme(getThemeId())
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

            //set range
            // Gets the RangeBar
            final RangeBar rangebar = (RangeBar) findViewById(R.id.rangebar);
            int nMinZoom = mVectorLayer.getMinZoom() < rangebar.getRightIndex() ? (int) mVectorLayer.getMinZoom() : rangebar.getRightIndex();
            int nMaxZoom = mVectorLayer.getMaxZoom() < rangebar.getRightIndex() ? (int) mVectorLayer.getMaxZoom() : rangebar.getRightIndex();
            rangebar.setThumbIndices(nMinZoom, nMaxZoom);
            // Gets the index value TextViews
            final TextView leftIndexValue = (TextView) findViewById(R.id.leftIndexValue);
            leftIndexValue.setText("min: " + nMinZoom);
            final TextView rightIndexValue = (TextView) findViewById(R.id.rightIndexValue);
            rightIndexValue.setText("max: " + nMaxZoom);

            // Sets the display values of the indices
            rangebar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
                @Override
                public void onIndexChangeListener(RangeBar rangeBar, int leftThumbIndex, int rightThumbIndex) {

                    leftIndexValue.setText("min: " + leftThumbIndex);
                    rightIndexValue.setText("max: " + rightThumbIndex);
                }
            });

            TextView featureCount = (TextView) findViewById(R.id.layer_feature_count);
            featureCount.setText(featureCount.getText() + ": " + mVectorLayer.getCount());

            TextView geomType = (TextView) findViewById(R.id.layer_geom_type);
            geomType.setText(geomType.getText() + ": " + getGeometryName(mVectorLayer.getGeometryType()));

            // rebuild cache
            final ProgressBar rebuildCacheProgress = (ProgressBar) findViewById(R.id.rebuildCacheProgressBar);
            final ImageButton buildCacheButton = (ImageButton) findViewById(R.id.buildCacheButton);
            buildCacheButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBackgroundTask = new BackgroundTask(VectorLayerSettingsActivity.this, mVectorLayer, rebuildCacheProgress);
                    mBackgroundTask.execute();
                }
            });
            final ImageButton cancelBuildCacheButton = (ImageButton) findViewById(R.id.cancelBuildCahceButton);
            cancelBuildCacheButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mBackgroundTask)
                        mBackgroundTask.setCancel(true);
                }
            });

        }
    }

    private String getGeometryName(int geometryType) {
        switch (geometryType){
            case GeoConstants.GTPoint:
                return getString(R.string.point);
            case GeoConstants.GTMultiPoint:
                return getString(R.string.multi_point);
            case GeoConstants.GTLineString:
                return getString(R.string.linestring);
            case GeoConstants.GTMultiLineString:
                return getString(R.string.multi_linestring);
            case GeoConstants.GTPolygon:
                return getString(R.string.polygon);
            case GeoConstants.GTMultiPolygon:
                return getString(R.string.multi_polygon);
            default:
                return getString(R.string.n_a);

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

        final RangeBar rangebar = (RangeBar) findViewById(R.id.rangebar);
        mVectorLayer.setMinZoom(rangebar.getLeftIndex());
        mVectorLayer.setMaxZoom(rangebar.getRightIndex());

        mVectorLayer.save();
    }


    @Override
    protected void onPause()
    {
        super.onPause();
        saveSettings();
    }

    protected static class BackgroundTask
            extends AsyncTask<Void, Void, Void> implements IProgressor
    {
        protected ProgressBar mProgress;
        protected Activity mActivity;
        protected VectorLayer mVectorLayer;
        protected boolean mCancel;

        public BackgroundTask(Activity activity, VectorLayer layer, ProgressBar progressBar)
        {
            mProgress = progressBar;
            mActivity = activity;
            mVectorLayer = layer;
        }


        @Override
        protected Void doInBackground(Void... voids)
        {
            mVectorLayer.rebuildCache(this);
            return null;
        }


        @Override
        protected void onPreExecute()
        {
            //not good solution but rare used so let it be
            lockScreenOrientation();
        }


        @Override
        protected void onPostExecute(Void aVoid)
        {
            mProgress.setProgress(0);
            unlockScreenOrientation();
        }


        protected void lockScreenOrientation()
        {
            int currentOrientation = mActivity.getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }


        protected void unlockScreenOrientation()
        {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        @Override
        public void setMax(int maxValue) {
            mProgress.setMax(maxValue);
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        public void setCancel(boolean cancel) {
            mCancel = cancel;
        }

        @Override
        public void setValue(int value) {
            mProgress.setProgress(value);
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {
            mProgress.setIndeterminate(indeterminate);
        }

        @Override
        public void setMessage(String message) {
        }
    }
}

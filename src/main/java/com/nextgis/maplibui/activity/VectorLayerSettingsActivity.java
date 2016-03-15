/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.edmodo.rangebar.RangeBar;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LayerUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IChooseColorResult;
import com.nextgis.maplibui.dialog.ChooseColorDialog;
import com.nextgis.maplibui.service.RebuildCacheService;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Vector layer settings activity. Include common settings (layer name) and renderer settings.
 */
public class VectorLayerSettingsActivity
        extends NGActivity
        implements IChooseColorResult, View.OnClickListener {
    protected VectorLayer                 mVectorLayer;
    protected List<Pair<Integer, String>> mColors;
    protected int                         mCurrentColor;
    protected BroadcastReceiver           mRebuildCacheReceiver;
    protected CharSequence[]              mFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            TextView form = (TextView) findViewById(R.id.layer_custom_form);
            File formPath = new File(mVectorLayer.getPath(), ConstantsUI.FILE_FORM);
            form.setText(formPath.exists() ? R.string.layer_has_form : R.string.layer_has_no_form);

            TextView path = (TextView) findViewById(R.id.layer_local_lath);
            path.setText(String.format(getString(R.string.layer_local_path), mVectorLayer.getPath()));

            Button fields = (Button) findViewById(R.id.layer_fields);
            fields.setOnClickListener(this);
            fillFields();

            if (mVectorLayer instanceof NGWVectorLayer) {
                TextView remote = (TextView) findViewById(R.id.layer_remote_path);
                remote.setText(String.format(getString(R.string.layer_remote_path), ((NGWVectorLayer) mVectorLayer).getRemoteUrl()));
                remote.setVisibility(View.VISIBLE);
            }

            EditText editText = (EditText) findViewById(R.id.layer_name);
            editText.setText(mVectorLayer.getName());

            LinearLayout color_row = (LinearLayout) findViewById(R.id.color_row);
            color_row.setOnClickListener(this);

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
            leftIndexValue.setText(String.format(getString(R.string.min), nMinZoom));
            final TextView rightIndexValue = (TextView) findViewById(R.id.rightIndexValue);
            rightIndexValue.setText(String.format(getString(R.string.max), nMaxZoom));

            // Sets the display values of the indices
            rangebar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
                @Override
                public void onIndexChangeListener(RangeBar rangeBar, int leftThumbIndex, int rightThumbIndex) {
                    leftIndexValue.setText(String.format(getString(R.string.min), leftThumbIndex));
                    rightIndexValue.setText(String.format(getString(R.string.max), rightThumbIndex));
                }
            });

            Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
            setTitle(String.format(getString(R.string.layer_geom_type), getGeometryName(mVectorLayer.getGeometryType())));
            toolbar.setSubtitle(String.format(getString(R.string.feature_count), mVectorLayer.getCount()));

            final ProgressBar rebuildCacheProgress = (ProgressBar) findViewById(R.id.rebuildCacheProgressBar);
            final ImageButton buildCacheButton = (ImageButton) findViewById(R.id.buildCacheButton);
            buildCacheButton.setOnClickListener(this);
            final ImageButton cancelBuildCacheButton = (ImageButton) findViewById(R.id.cancelBuildCahceButton);
            cancelBuildCacheButton.setOnClickListener(this);

            mRebuildCacheReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    rebuildCacheProgress.setMax(intent.getIntExtra(RebuildCacheService.KEY_MAX, 0));
                    rebuildCacheProgress.setProgress(intent.getIntExtra(RebuildCacheService.KEY_PROGRESS, 0));
                }
            };
        }
    }

    private void fillFields() {
        int fieldsCount = mVectorLayer.getFields().size();
        mFields = new CharSequence[fieldsCount];
        String labelField = mVectorLayer.getPreferences().getString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, Constants.FIELD_ID);

        for (int i = 0; i < fieldsCount; i++) {
            Field field = mVectorLayer.getFields().get(i);
            String fieldInfo = field.getName() + " - " + LayerUtil.typeToString(this, field.getType());
            if (field.getName().equals(labelField))
                fieldInfo += getString(R.string.label_field);

            mFields[i] = fieldInfo;
        }
    }

    @Override
    public void onClick(View v) {
        // rebuild cache
        final Intent intent = new Intent(this, RebuildCacheService.class);
        intent.putExtra(ConstantsUI.KEY_LAYER_ID, mVectorLayer.getId());

        int i = v.getId();
        if (i == R.id.color_row) {//show colors list
            ChooseColorDialog newChooseColorDialog = new ChooseColorDialog();
            newChooseColorDialog.setColors(mColors)
                    .setTitle(getString(R.string.select_color))
                    .setTheme(getThemeId())
                    .show(VectorLayerSettingsActivity.this.getSupportFragmentManager(), "choose_color");
        } else if (i == R.id.layer_fields) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(R.string.fields)
                    .setPositiveButton(android.R.string.ok, null)
                    .setItems(mFields, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String fieldName = mFields[which].toString().split(" - ")[0];
                            mVectorLayer.getPreferences().edit().putString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, fieldName).commit();
                            fillFields();
                        }
                    });
            dialog.show().setCanceledOnTouchOutside(false);
            Toast.makeText(this, R.string.label_field_toast, Toast.LENGTH_SHORT).show();
        } else if (i == R.id.buildCacheButton) {
            intent.setAction(RebuildCacheService.ACTION_ADD_TASK);
            startService(intent);
        } else if (i == R.id.cancelBuildCahceButton) {
            intent.setAction(RebuildCacheService.ACTION_STOP);
            startService(intent);
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

    protected void setColor(int color) {
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

    protected String getColorName(int color) {
        for (Pair<Integer, String> colorEntry : mColors) {
            if (colorEntry.first == color) {
                return colorEntry.second;
            }
        }
        return "#" + Integer.toHexString(color & 0x00FFFFFF);
    }

    @Override
    public void onFinishChooseColorDialog(int color) {
        setColor(color);
    }

    protected void saveSettings() {
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
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(RebuildCacheService.ACTION_UPDATE);
        registerReceiver(mRebuildCacheReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mRebuildCacheReceiver);
        saveSettings();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ConstantsUI.KEY_LAYER_ID, mVectorLayer.getId());
    }

}

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
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.SimpleLineStyle;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.maplib.display.SimplePolygonStyle;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LayerUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.fragment.LayerGeneralSettingsFragment;
import com.nextgis.maplibui.service.RebuildCacheService;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;


/**
 * Vector layer settings activity. Include common settings (layer name) and renderer settings.
 */
public class VectorLayerSettingsActivity
        extends LayerSettingsActivity
        implements View.OnClickListener {
    protected static VectorLayer mVectorLayer;
    protected static int mFillColor, mStrokeColor;
    protected static Style mStyle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mLayer == null)
            return;

        if (mLayer.getType() == Constants.LAYERTYPE_LOCAL_VECTOR || mLayer.getType() == Constants.LAYERTYPE_NGW_VECTOR) {
            mVectorLayer = (VectorLayer) mLayer;
            mLayerMinZoom = mVectorLayer.getMinZoom();
            mLayerMaxZoom = mVectorLayer.getMaxZoom();

            Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
            setTitle(String.format(getString(R.string.layer_geom_type), getGeometryName(mVectorLayer.getGeometryType())));
            toolbar.setSubtitle(String.format(getString(R.string.feature_count), mVectorLayer.getCount()));

            // set color
            SimpleFeatureRenderer sfr = (SimpleFeatureRenderer) mVectorLayer.getRenderer();
            if (null != sfr)
                mStyle = sfr.getStyle();
        }
    }

    @Override
    protected void addFragments() {
        mAdapter.addFragment(new StyleFragment(), R.string.style);
        mAdapter.addFragment(new FieldsFragment(), R.string.fields);
        mAdapter.addFragment(new LayerGeneralSettingsFragment(), R.string.general);
        mAdapter.addFragment(new CacheFragment(), R.string.cache);
    }

    @Override
    public void onClick(View v) {
        // rebuild cache
        final Intent intent = new Intent(this, RebuildCacheService.class);
        intent.putExtra(ConstantsUI.KEY_LAYER_ID, mVectorLayer.getId());

        int i = v.getId();
        if (i == R.id.color_fill) {//show colors dialog
            AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, mFillColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    mFillColor = color;
                    StyleFragment.setFillColor(color);
                    mStyle.setColor(color);
                }

                @Override
                public void onCancel(AmbilWarnaDialog dialog) {

                }
            });

            dialog.show();
        } else if (i == R.id.color_stroke) {//show colors dialog
            AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, mStrokeColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    mStrokeColor = color;
                    StyleFragment.setStrokeColor(color);

                    if (mStyle instanceof SimpleMarkerStyle)
                        ((SimpleMarkerStyle) mStyle).setOutlineColor(color);
                    else if (mStyle instanceof SimpleLineStyle)
                        ((SimpleLineStyle) mStyle).setOutColor(color);
                }

                @Override
                public void onCancel(AmbilWarnaDialog dialog) {

                }
            });

            dialog.show();
        } else if (i == R.id.buildCacheButton) {
            intent.setAction(RebuildCacheService.ACTION_ADD_TASK);
            startService(intent);
        } else if (i == R.id.cancelBuildCacheButton) {
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


    @Override
    protected void saveSettings() {
        if (null == mVectorLayer)
            return;

        mVectorLayer.setName(mLayerName);

        mVectorLayer.setMinZoom(mLayerMinZoom);
        mVectorLayer.setMaxZoom(mLayerMaxZoom);

        mVectorLayer.save();
    }

    public static class StyleFragment extends Fragment {
        protected static ImageView mColorFillImage, mColorStrokeImage;
        protected static TextView mColorFillName, mColorStrokeName;

        public StyleFragment() {

        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (getView() != null)
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ScrollView v = (ScrollView) inflater.inflate(R.layout.fragment_vector_layer_style, container, false);

            mFillColor = mStyle.getColor();
            if (mStyle instanceof SimpleMarkerStyle) {
                v.addView(inflater.inflate(R.layout.style_marker, container, false));
                inflateMarker(v);
            } else if (mStyle instanceof SimpleLineStyle) {
                v.addView(inflater.inflate(R.layout.style_line, container, false));
                inflateLine(v);
            } else if (mStyle instanceof SimplePolygonStyle) {
                v.addView(inflater.inflate(R.layout.style_polygon, container, false));
                inflatePolygon(v);
            }

            return v;
        }

        private void inflateMarker(View v) {
            Spinner type = (Spinner) v.findViewById(R.id.type);
            type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    ((SimpleMarkerStyle) mStyle).setType(position + 1);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            type.setSelection(((SimpleMarkerStyle) mStyle).getType() - 1);

            float size = ((SimpleMarkerStyle) mStyle).getSize();
            EditText sizeText = (EditText) v.findViewById(R.id.size);
            sizeText.setText(String.format(Locale.getDefault(), "%.0f", size));
            sizeText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        ((SimpleMarkerStyle) mStyle).setSize(Float.parseFloat(s.toString()));
                    } catch (Exception ignored) { }
                }
            });

            mStrokeColor = ((SimpleMarkerStyle) mStyle).getOutlineColor();
            mColorFillName = (TextView) v.findViewById(R.id.color_fill_name);
            mColorFillImage = (ImageView) v.findViewById(R.id.color_fill_ring);
            mColorStrokeName = (TextView) v.findViewById(R.id.color_stroke_name);
            mColorStrokeImage = (ImageView) v.findViewById(R.id.color_stroke_ring);

            LinearLayout color_fill = (LinearLayout) v.findViewById(R.id.color_fill);
            LinearLayout color_stroke = (LinearLayout) v.findViewById(R.id.color_stroke);
            color_fill.setOnClickListener((View.OnClickListener) getActivity());
            color_stroke.setOnClickListener((View.OnClickListener) getActivity());
            setFillColor(mFillColor);
            setStrokeColor(mStrokeColor);

            float width = ((SimpleMarkerStyle) mStyle).getWidth();
            EditText widthText = (EditText) v.findViewById(R.id.width);
            widthText.setText(String.format(Locale.getDefault(), "%.0f", width));
            widthText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        ((SimpleMarkerStyle) mStyle).setWidth(Float.parseFloat(s.toString()));
                    } catch (Exception ignored) { }
                }
            });
        }

        private void inflateLine(View v) {
            mStrokeColor = ((SimpleLineStyle) mStyle).getOutColor();

            mColorFillName = (TextView) v.findViewById(R.id.color_fill_name);
            mColorFillImage = (ImageView) v.findViewById(R.id.color_fill_ring);
            mColorStrokeName = (TextView) v.findViewById(R.id.color_stroke_name);
            mColorStrokeImage = (ImageView) v.findViewById(R.id.color_stroke_ring);

            LinearLayout color_fill = (LinearLayout) v.findViewById(R.id.color_fill);
            LinearLayout color_stroke = (LinearLayout) v.findViewById(R.id.color_stroke);
            color_fill.setOnClickListener((View.OnClickListener) getActivity());
            color_stroke.setOnClickListener((View.OnClickListener) getActivity());
            setFillColor(mFillColor);
            setStrokeColor(mStrokeColor);

            float width = ((SimpleLineStyle) mStyle).getWidth();
            EditText widthText = (EditText) v.findViewById(R.id.width);
            widthText.setText(String.format(Locale.getDefault(), "%.0f", width));
            widthText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        ((SimpleLineStyle) mStyle).setWidth(Float.parseFloat(s.toString()));
                    } catch (Exception ignored) { }
                }
            });

            Spinner type = (Spinner) v.findViewById(R.id.type);
            type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    ((SimpleLineStyle) mStyle).setType(position + 1);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            type.setSelection(((SimpleLineStyle) mStyle).getType() - 1);
        }

        private void inflatePolygon(View v) {
            float width = ((SimplePolygonStyle) mStyle).getWidth();
            boolean fill = ((SimplePolygonStyle) mStyle).isFill();

            mColorFillName = (TextView) v.findViewById(R.id.color_fill_name);
            mColorFillImage = (ImageView) v.findViewById(R.id.color_fill_ring);

            CheckBox fillCheck = (CheckBox) v.findViewById(R.id.fill);
            fillCheck.setChecked(fill);
            fillCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ((SimplePolygonStyle) mStyle).setFill(isChecked);
                }
            });

            EditText widthText = (EditText) v.findViewById(R.id.width);
            widthText.setText(String.format(Locale.getDefault(), "%.0f", width));
            widthText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        ((SimplePolygonStyle) mStyle).setWidth(Float.parseFloat(s.toString()));
                    } catch (Exception ignored) { }
                }
            });

            LinearLayout color_fill = (LinearLayout) v.findViewById(R.id.color_fill);
            color_fill.setOnClickListener((View.OnClickListener) getActivity());
            setFillColor(mFillColor);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        protected static void setFillColor(int color) {
            setColor(mColorFillImage, mColorFillName, color);
        }

        protected static void setStrokeColor(int color) {
            setColor(mColorStrokeImage, mColorStrokeName, color);
        }

        private static void setColor(ImageView image, TextView text, int color) {
            // set color
            GradientDrawable sd = (GradientDrawable) image.getDrawable();
            sd.setColor(color);
            image.invalidate();

            // set color name
            text.setText(getColorName(color));
        }

        protected static String getColorName(int color) {
            return String.format("#%06X", (0xFFFFFF & color));
        }
    }

    public static class FieldsFragment extends Fragment {
        protected List<String> mFields;
        protected int mDefault = -1;

        public FieldsFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_vector_layer_fields, container, false);
            ListView fields = (ListView) v.findViewById(R.id.listView);
            fillFields();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_single_choice, mFields);
            fields.setAdapter(adapter);
            if (mDefault >= 0)
                fields.setItemChecked(mDefault, true);

            fields.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String fieldName = mFields.get(position).split(" - ")[0];
                    mVectorLayer.getPreferences().edit().putString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, fieldName).commit();
                }
            });
            return v;
        }

        private void fillFields() {
            mFields = new ArrayList<>();
            int fieldsCount = mVectorLayer.getFields().size();
            String labelField = mVectorLayer.getPreferences().getString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, Constants.FIELD_ID);

            for (int i = 0; i < fieldsCount; i++) {
                Field field = mVectorLayer.getFields().get(i);
                String fieldInfo = field.getName() + " - " + LayerUtil.typeToString(getContext(), field.getType());
                if (field.getName().equals(labelField))
                    mDefault = i;

                mFields.add(fieldInfo);
            }
        }
    }

    public static class CacheFragment extends Fragment {
        protected BroadcastReceiver mRebuildCacheReceiver;

        public CacheFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_vector_layer_cache, container, false);

            final ProgressBar rebuildCacheProgress = (ProgressBar) v.findViewById(R.id.rebuildCacheProgressBar);
            final ImageButton buildCacheButton = (ImageButton) v.findViewById(R.id.buildCacheButton);
            buildCacheButton.setOnClickListener((View.OnClickListener) getActivity());
            final ImageButton cancelBuildCacheButton = (ImageButton) v.findViewById(R.id.cancelBuildCacheButton);
            cancelBuildCacheButton.setOnClickListener((View.OnClickListener) getActivity());

            mRebuildCacheReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    rebuildCacheProgress.setMax(intent.getIntExtra(RebuildCacheService.KEY_MAX, 0));
                    rebuildCacheProgress.setProgress(intent.getIntExtra(RebuildCacheService.KEY_PROGRESS, 0));
                }
            };

            return v;
        }

        @Override
        public void onResume() {
            super.onResume();
            IntentFilter intentFilter = new IntentFilter(RebuildCacheService.ACTION_UPDATE);
            getActivity().registerReceiver(mRebuildCacheReceiver, intentFilter);
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivity().unregisterReceiver(mRebuildCacheReceiver);
        }

    }
}

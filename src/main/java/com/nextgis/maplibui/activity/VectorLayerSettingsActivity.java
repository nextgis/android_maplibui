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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.edmodo.rangebar.RangeBar;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.Style;
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
        extends LayerSettingsActivity
        implements IChooseColorResult, View.OnClickListener {
    protected static VectorLayer mVectorLayer;
    protected static int mCurrentColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mLayer == null)
            return;

        if (mLayer.getType() == Constants.LAYERTYPE_LOCAL_VECTOR || mLayer.getType() == Constants.LAYERTYPE_NGW_VECTOR) {
            mVectorLayer = (VectorLayer) mLayer;

            Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
            setTitle(String.format(getString(R.string.layer_geom_type), getGeometryName(mVectorLayer.getGeometryType())));
            toolbar.setSubtitle(String.format(getString(R.string.feature_count), mVectorLayer.getCount()));

            // set color
            SimpleFeatureRenderer sfr = (SimpleFeatureRenderer) mVectorLayer.getRenderer();
            if (null != sfr) {
                Style style = sfr.getStyle();
                if (null != style)
                    mCurrentColor = style.getColor();
            }

            mLayerName = mLayer.getName();
            mLayerMinZoom = mVectorLayer.getMinZoom();
            mLayerMaxZoom = mVectorLayer.getMaxZoom();
        }
    }

    @Override
    protected void addFragments() {
        mAdapter.addFragment(new StyleFragment(), R.string.style);
        mAdapter.addFragment(new FieldsFragment(), R.string.fields);
        mAdapter.addFragment(new VectorGeneralFragment(), R.string.general);
        mAdapter.addFragment(new CacheFragment(), R.string.cache);
    }

    @Override
    public void onClick(View v) {
        // rebuild cache
        final Intent intent = new Intent(this, RebuildCacheService.class);
        intent.putExtra(ConstantsUI.KEY_LAYER_ID, mVectorLayer.getId());

        int i = v.getId();
        if (i == R.id.color_row) {//show colors list
            ChooseColorDialog newChooseColorDialog = new ChooseColorDialog();
            newChooseColorDialog.setColors(StyleFragment.getColors())
                    .setTitle(getString(R.string.select_color))
                    .setTheme(getThemeId())
                    .show(VectorLayerSettingsActivity.this.getSupportFragmentManager(), "choose_color");
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
    public void onFinishChooseColorDialog(int color) {
        StyleFragment.setColor(color);
    }

    @Override
    protected void saveSettings() {
        if (null == mVectorLayer)
            return;

        mVectorLayer.setName(mLayerName);

        // set color
        SimpleFeatureRenderer sfr = (SimpleFeatureRenderer) mVectorLayer.getRenderer();
        if (null != sfr) {
            Style style = sfr.getStyle();
            if (null != style)
                style.setColor(mCurrentColor);
        }

        mVectorLayer.setMinZoom(mLayerMinZoom);
        mVectorLayer.setMaxZoom(mLayerMaxZoom);

        mVectorLayer.save();
    }

    public static class StyleFragment extends Fragment {
        protected static List<Pair<Integer, String>> mColors;
        protected static ImageView mColorImage;
        protected static TextView mColorName;

        public StyleFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_vector_layer_style, container, false);

            mColors = new ArrayList<>();
            mColors.add(new Pair<>(Color.RED, getString(R.string.red)));
            mColors.add(new Pair<>(Color.GREEN, getString(R.string.green)));
            mColors.add(new Pair<>(Color.BLUE, getString(R.string.blue)));
            mColors.add(new Pair<>(Color.MAGENTA, getString(R.string.magenta)));
            mColors.add(new Pair<>(Color.YELLOW, getString(R.string.yellow)));
            mColors.add(new Pair<>(Color.CYAN, getString(R.string.cyan)));

            mColorName = (TextView) v.findViewById(R.id.color_name);
            mColorImage = (ImageView) v.findViewById(R.id.color_image);

            LinearLayout color_row = (LinearLayout) v.findViewById(R.id.color_row);
            color_row.setOnClickListener((View.OnClickListener) getActivity());
            setColor(mCurrentColor);

            return v;
        }

        public static List<Pair<Integer,String>> getColors() {
            return mColors;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        protected static void setColor(int color) {
            // set color
            GradientDrawable sd = (GradientDrawable) mColorImage.getDrawable();
            sd.setColor(color);
            mColorImage.invalidate();

            // set color name
            mColorName.setText(getColorName(color));

            mCurrentColor = color;
        }

        protected static String getColorName(int color) {
            for (Pair<Integer, String> colorEntry : mColors) {
                if (colorEntry.first == color) {
                    return colorEntry.second;
                }
            }
            return "#" + Integer.toHexString(color & 0x00FFFFFF);
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

    public static class VectorGeneralFragment extends Fragment {
        protected static EditText mEditText;
        protected static RangeBar mRangeBar;

        public VectorGeneralFragment() {

        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            mLayerName = mEditText.getEditableText().toString();
            mLayerMinZoom = mRangeBar.getLeftIndex();
            mLayerMaxZoom = mRangeBar.getRightIndex();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_layer_general, container, false);
            TextView form = (TextView) v.findViewById(R.id.layer_custom_form);
            File formPath = new File(mVectorLayer.getPath(), ConstantsUI.FILE_FORM);
            form.setText(formPath.exists() ? R.string.layer_has_form : R.string.layer_has_no_form);

            TextView path = (TextView) v.findViewById(R.id.layer_local_lath);
            path.setText(String.format(getString(R.string.layer_local_path), mVectorLayer.getPath()));

            if (mVectorLayer instanceof NGWVectorLayer) {
                TextView remote = (TextView) v.findViewById(R.id.layer_remote_path);
                remote.setText(String.format(getString(R.string.layer_remote_path), ((NGWVectorLayer) mVectorLayer).getRemoteUrl()));
                remote.setVisibility(View.VISIBLE);
            }

            mEditText = (EditText) v.findViewById(R.id.layer_name);
            mEditText.setText(mVectorLayer.getName());
            mEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    mLayerName = s.toString();
                }
            });

            //set range
            // Gets the RangeBar
            mRangeBar = (RangeBar) v.findViewById(R.id.rangebar);
            int nMinZoom = mLayerMinZoom < mRangeBar.getRightIndex() ? (int) mLayerMinZoom : mRangeBar.getRightIndex();
            int nMaxZoom = mLayerMaxZoom < mRangeBar.getRightIndex() ? (int) mLayerMaxZoom : mRangeBar.getRightIndex();
            mRangeBar.setThumbIndices(nMinZoom, nMaxZoom);
            // Gets the index value TextViews
            final TextView leftIndexValue = (TextView) v.findViewById(R.id.leftIndexValue);
            leftIndexValue.setText(String.format(getString(R.string.min), nMinZoom));
            final TextView rightIndexValue = (TextView) v.findViewById(R.id.rightIndexValue);
            rightIndexValue.setText(String.format(getString(R.string.max), nMaxZoom));

            // Sets the display values of the indices
            mRangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
                @Override
                public void onIndexChangeListener(RangeBar rangeBar, int leftThumbIndex, int rightThumbIndex) {
                    mLayerMinZoom = leftThumbIndex;
                    mLayerMaxZoom = rightThumbIndex;
                    leftIndexValue.setText(String.format(getString(R.string.min), leftThumbIndex));
                    rightIndexValue.setText(String.format(getString(R.string.max), rightThumbIndex));
                }
            });

            return v;
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

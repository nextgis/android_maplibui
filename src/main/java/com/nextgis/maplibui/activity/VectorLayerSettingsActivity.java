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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.nextgis.maplib.api.IRenderer;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.display.FieldStyleRule;
import com.nextgis.maplib.display.RuleFeatureRenderer;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LayerUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.display.RendererUI;
import com.nextgis.maplibui.display.RuleFeatureRendererUI;
import com.nextgis.maplibui.display.SimpleFeatureRendererUI;
import com.nextgis.maplibui.fragment.LayerGeneralSettingsFragment;
import com.nextgis.maplibui.service.RebuildCacheService;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.util.ArrayList;
import java.util.List;


/**
 * Vector layer settings activity. Include common settings (layer name) and renderer settings.
 */
public class VectorLayerSettingsActivity
        extends LayerSettingsActivity
        implements View.OnClickListener {
    protected static VectorLayer mVectorLayer;
    protected static IRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mLayer == null)
            return;

        if (mLayer.getType() == Constants.LAYERTYPE_LOCAL_VECTOR
                || mLayer.getType() == Constants.LAYERTYPE_NGW_VECTOR
                || mLayer.getType() == Constants.LAYERTYPE_NGW_WEBMAP) {
            mVectorLayer = (VectorLayer) mLayer;
            mLayerMinZoom = mVectorLayer.getMinZoom();
            mLayerMaxZoom = mVectorLayer.getMaxZoom();
            mRenderer = mVectorLayer.getRenderer();

            Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
            setTitle(String.format(getString(R.string.layer_geom_type), getGeometryName(mVectorLayer.getGeometryType())));
            toolbar.setSubtitle(String.format(getString(R.string.feature_count), mVectorLayer.getCount()));
        }
    }

    @Override
    protected void addFragments() {
        mAdapter.addFragment(new StyleFragment(), R.string.style);
        mAdapter.addFragment(new FieldsFragment(), R.string.fields);
        LayerGeneralSettingsFragment generalSettingsFragment = new LayerGeneralSettingsFragment();
        generalSettingsFragment.setRoot(mLayer, this);
        mAdapter.addFragment(generalSettingsFragment, R.string.general);
        mAdapter.addFragment(new CacheFragment(), R.string.cache);
    }

    @Override
    public void onClick(View v) {
        // rebuild cache
        final Intent intent = new Intent(this, RebuildCacheService.class);
        intent.putExtra(ConstantsUI.KEY_LAYER_ID, mVectorLayer.getId());

        int i = v.getId();
        if (i == R.id.buildCacheButton) {
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
        List<RendererUI> mRenderers;

        public StyleFragment() {
            mRenderers = new ArrayList<>();
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
            Spinner spinner = (Spinner) v.findViewById(R.id.renderer);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mVectorLayer.setRenderer(mRenderers.get(position).getRenderer());
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    Fragment settings = mRenderers.get(position).getSettingsScreen(mVectorLayer);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.replace(R.id.settings, settings);
                    ft.commit();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            Style style = null;
            try {
                style = mVectorLayer.getDefaultStyle();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mRenderer instanceof RuleFeatureRenderer) {
                RuleFeatureRendererUI rfr = new RuleFeatureRendererUI((RuleFeatureRenderer) mRenderer, mVectorLayer);
                mRenderers.add(new SimpleFeatureRendererUI(new SimpleFeatureRenderer(mVectorLayer, style)));
                mRenderers.add(rfr);
                spinner.setSelection(1);
            } else if (mRenderer instanceof SimpleFeatureRenderer) {
                SimpleFeatureRendererUI sfr = new SimpleFeatureRendererUI((SimpleFeatureRenderer) mRenderer);
                mRenderers.add(sfr);
                mRenderers.add(new RuleFeatureRendererUI(new RuleFeatureRenderer(mVectorLayer, new FieldStyleRule(mVectorLayer), style), mVectorLayer));
                spinner.setSelection(0);
            }

            return v;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            mRenderers.clear();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
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
                    Toast.makeText(getContext(), String.format(getString(R.string.label_field_toast), fieldName), Toast.LENGTH_SHORT).show();
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

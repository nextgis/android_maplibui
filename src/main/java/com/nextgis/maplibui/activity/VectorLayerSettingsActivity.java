/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.IRenderer;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.display.FieldStyleRule;
import com.nextgis.maplib.display.RuleFeatureRenderer;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LayerUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.display.RendererUI;
import com.nextgis.maplibui.display.RuleFeatureRendererUI;
import com.nextgis.maplibui.display.SimpleFeatureRendererUI;
import com.nextgis.maplibui.fragment.LayerGeneralSettingsFragment;
import com.nextgis.maplibui.fragment.NGWSettingsFragment;
import com.nextgis.maplibui.service.RebuildCacheService;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_SYNC_PERIOD;

/**
 * Vector layer settings activity. Include common settings (layer name) and renderer settings.
 */
public class VectorLayerSettingsActivity
        extends LayerSettingsActivity
        implements View.OnClickListener {
    protected static VectorLayer mVectorLayer;
    protected static IRenderer mRenderer;
    private Toolbar mToolbar;

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
            mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
            setSubtitle();
        }
    }

    private void setSubtitle() {
        String subtitle = getGeometryName(mVectorLayer.getGeometryType()).toLowerCase();
        File formPath = new File(mLayer.getPath(), ConstantsUI.FILE_FORM);
        if (formPath.exists())
            subtitle += " " + getString(R.string.layer_has_form);

        subtitle = String.format(getString(R.string.feature_count), mVectorLayer.getCount(), subtitle);
        mToolbar.setSubtitle(subtitle);
    }

    @Override
    protected void addFragments() {
        mAdapter.addFragment(new StyleFragment(), R.string.style);
        mAdapter.addFragment(new FieldsFragment(), R.string.fields);
        if (mLayer instanceof NGWVectorLayer)
            mAdapter.addFragment(new SyncFragment(), R.string.sync);
        LayerGeneralSettingsFragment generalSettingsFragment = new LayerGeneralSettingsFragment();
        generalSettingsFragment.setRoot(mLayer, this);
        mAdapter.addFragment(generalSettingsFragment, R.string.general);
        CacheFragment cacheFragment = new CacheFragment();
        cacheFragment.setActivity(this);
        mAdapter.addFragment(cacheFragment, R.string.cache);
    }

    @Override
    public void onClick(View v) {
        // rebuild cache
        final Intent intent = new Intent(this, RebuildCacheService.class);
        intent.putExtra(ConstantsUI.KEY_LAYER_ID, mVectorLayer.getId());

        int i = v.getId();
        if (i == R.id.rebuild_cache) {
            intent.setAction(RebuildCacheService.ACTION_ADD_TASK);
            startService(intent);
            v.setEnabled(false);
            v.getRootView().findViewById(R.id.rebuild_progress).setVisibility(View.VISIBLE);
        } else if (i == R.id.cancelBuildCacheButton) {
            intent.setAction(RebuildCacheService.ACTION_STOP);
            startService(intent);
            v.getRootView().findViewById(R.id.rebuild_cache).setEnabled(true);
            v.getRootView().findViewById(R.id.rebuild_progress).setVisibility(View.GONE);
        }
    }

    private String getGeometryName(int geometryType) {
        switch (geometryType) {
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

    public void onFeaturesCountChanged() {
        setSubtitle();
    }

    @Override
    protected void saveSettings() {
        if (null == mVectorLayer)
            return;

        mVectorLayer.setName(mLayerName);
        mVectorLayer.setMinZoom(mLayerMinZoom);
        mVectorLayer.setMaxZoom(mLayerMaxZoom);
        boolean changes = mRenderer != mVectorLayer.getRenderer() || mLayerMaxZoom != mVectorLayer.getMaxZoom() || mLayerMinZoom != mVectorLayer.getMinZoom();
        mVectorLayer.save();
        if (changes)
            mMap.setDirty(true);
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
            if (mVectorLayer == null)
                return v;

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
        protected List<String> mFieldAliases;
        protected List<String> mFieldNames;
        protected int mDefault = 0;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_vector_layer_fields, container, false);
            if (mVectorLayer == null)
                return v;

            ListView fields = (ListView) v.findViewById(R.id.listView);
            fillFields();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_single_choice, mFieldAliases);

            fields.setAdapter(adapter);
            fields.setItemChecked(mDefault, true);
            fields.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String fieldName = mFieldNames.get(position);
                    mVectorLayer.getPreferences().edit().putString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, fieldName).apply();
                    Toast.makeText(getContext(), String.format(getString(R.string.label_field_toast), fieldName), Toast.LENGTH_SHORT).show();
                }
            });

            return v;
        }

        private void fillFields() {
            mFieldNames = new ArrayList<>();
            mFieldAliases = new ArrayList<>();
            mFieldNames.add(FIELD_ID);
            mFieldAliases.add(FIELD_ID + " - " + LayerUtil.typeToString(getContext(), GeoConstants.FTInteger));

            int fieldsCount = mVectorLayer.getFields().size();
            String labelField = mVectorLayer.getPreferences().getString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, Constants.FIELD_ID);

            for (int i = 0; i < fieldsCount; i++) {
                Field field = mVectorLayer.getFields().get(i);
                String fieldInfo = field.getAlias() + " - " + LayerUtil.typeToString(getContext(), field.getType());
                if (field.getName().equals(labelField))
                    mDefault = i + 1;

                mFieldNames.add(field.getName());
                mFieldAliases.add(fieldInfo);
            }
        }
    }

    public static class CacheFragment extends Fragment {
        private BroadcastReceiver mRebuildCacheReceiver;
        private VectorLayerSettingsActivity mActivity;

        public CacheFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_vector_layer_cache, container, false);
            if (mVectorLayer == null)
                return v;

            final ProgressBar rebuildCacheProgress = (ProgressBar) v.findViewById(R.id.rebuildCacheProgressBar);
            final Button buildCacheButton = (Button) v.findViewById(R.id.rebuild_cache);
            buildCacheButton.setOnClickListener((View.OnClickListener) getActivity());
            final ImageButton cancelBuildCacheButton = (ImageButton) v.findViewById(R.id.cancelBuildCacheButton);
            cancelBuildCacheButton.setOnClickListener((View.OnClickListener) getActivity());
            final View progressView = v.findViewById(R.id.rebuild_progress);

            mRebuildCacheReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    int max = intent.getIntExtra(RebuildCacheService.KEY_MAX, 0);
                    int progress = intent.getIntExtra(RebuildCacheService.KEY_PROGRESS, 0);
                    int layer = intent.getIntExtra(ConstantsUI.KEY_LAYER_ID, NOT_FOUND);

                    if (layer == mVectorLayer.getId()) {
                        rebuildCacheProgress.setMax(max);
                        rebuildCacheProgress.setProgress(progress);
                    }

                    if (progress == 0) {
                        buildCacheButton.setEnabled(true);
                        progressView.setVisibility(View.GONE);
                    } else {
                        buildCacheButton.setEnabled(false);
                        progressView.setVisibility(View.VISIBLE);
                    }

                    mActivity.onFeaturesCountChanged();
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

        public void setActivity(VectorLayerSettingsActivity activity) {
            mActivity = activity;
        }
    }

    public static class SyncFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_ngw_vector_layer_sync, container, false);
            if (mVectorLayer == null)
                return v;

            final IGISApplication app = (IGISApplication) getActivity().getApplication();
            final NGWVectorLayer ngwLayer = ((NGWVectorLayer) mVectorLayer);
            final Account account = app.getAccount(ngwLayer.getAccountName());

            if (account == null)
                return null;

            TextView accountName = (TextView) v.findViewById(R.id.account_name);
            accountName.setText(String.format(getString(R.string.ngw_account), account.name));

            final Spinner direction = (Spinner) v.findViewById(R.id.sync_direction);
            CheckBox enabled = (CheckBox) v.findViewById(R.id.sync_enabled);
            enabled.setChecked(0 == (ngwLayer.getSyncType() & Constants.SYNC_NONE));
            enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (checked)
                        ngwLayer.setSyncType(Constants.SYNC_ALL);
                    else
                        ngwLayer.setSyncType(Constants.SYNC_NONE);

                    ngwLayer.save();
                    direction.setEnabled(checked);
                }
            });

            direction.setSelection(ngwLayer.getSyncDirection() - 1);
            direction.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    ngwLayer.setSyncDirection(i + 1);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            final Spinner period = (Spinner) v.findViewById(R.id.sync_interval);
            CheckBox auto = (CheckBox) v.findViewById(R.id.sync_auto);
            boolean isAccountSyncEnabled = NGWSettingsFragment.isAccountSyncEnabled(account, app.getAuthority());
            auto.setChecked(isAccountSyncEnabled);
            auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    NGWSettingsFragment.setAccountSyncEnabled(account, app.getAuthority(), checked);
                    period.setEnabled(checked);
                }
            });

            period.setEnabled(auto.isChecked());
            String prefValue = "" + Constants.DEFAULT_SYNC_PERIOD;
            List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, app.getAuthority());
            if (null != syncs && !syncs.isEmpty()) {
                for (PeriodicSync sync : syncs) {
                    Bundle bundle = sync.extras;
                    String savedPeriod = bundle.getString(KEY_PREF_SYNC_PERIOD);
                    if (savedPeriod != null) {
                        prefValue = savedPeriod;
                        break;
                    }
                }
            }

            final CharSequence[] keys = NGWSettingsFragment.getPeriodTitles(getActivity());
            final CharSequence[] values = NGWSettingsFragment.getPeriodValues();

            SpinnerAdapter adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, keys);
            period.setAdapter(adapter);
            period.setSelection(4);

            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(prefValue)) {
                    period.setSelection(i);
                    break;
                }
            }

            period.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    String value = values[i].toString();
                    long interval = Long.parseLong(value);
                    Bundle bundle = new Bundle();
                    bundle.putString(KEY_PREF_SYNC_PERIOD, value);

                    if (interval == NOT_FOUND) {
                        ContentResolver.removePeriodicSync(account, app.getAuthority(), bundle);
                    } else {
                        ContentResolver.addPeriodicSync(account, app.getAuthority(), bundle, interval);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            return v;
        }
    }
}

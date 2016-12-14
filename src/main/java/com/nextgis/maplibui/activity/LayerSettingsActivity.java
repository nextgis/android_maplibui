/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.ConstantsUI;

import java.util.ArrayList;
import java.util.List;

public abstract class LayerSettingsActivity extends NGActivity {
    protected ILayer mLayer;
    protected TabLayout mTabLayout;
    protected ViewPager mViewPager;
    protected LayerTabsAdapter mAdapter;
    protected MapBase mMap;

    public String mLayerName;
    public float mLayerMinZoom;
    public float mLayerMaxZoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layer_settings);
        setToolbar(R.id.main_toolbar);

        int layerId = Constants.NOT_FOUND;
        if (savedInstanceState != null) {
            layerId = savedInstanceState.getInt(ConstantsUI.KEY_LAYER_ID);
        } else {
            layerId = getIntent().getIntExtra(ConstantsUI.KEY_LAYER_ID, layerId);
        }

        IGISApplication application = (IGISApplication) getApplication();
        mMap = application.getMap();

        if (null != mMap) {
            ILayer layer = mMap.getLayerById(layerId);
            if (null != layer) {
                mLayer = layer;
                mLayerName = mLayer.getName();
                setTitle(mLayerName);
            }
        }

        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mAdapter = new LayerTabsAdapter(getSupportFragmentManager());
        addFragments();
        mViewPager.setAdapter(mAdapter);

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mLayer != null)
            outState.putInt(ConstantsUI.KEY_LAYER_ID, mLayer.getId());
    }

    public void onFeaturesCountChanged() {

    }

    abstract void addFragments();
    abstract void saveSettings();

    class LayerTabsAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        LayerTabsAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, int title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(getString(title));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}


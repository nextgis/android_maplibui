/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2018-2019 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.formcontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IControl;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.control.GreyLine;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.JSON_TYPE_KEY;
import static com.nextgis.maplibui.activity.FormBuilderModifyAttributesActivity.appendData;
import static com.nextgis.maplibui.activity.FormBuilderModifyAttributesActivity.getControl;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_CAPTION_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_COORDINATES_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DEFAULT_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ELEMENTS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_PAGES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TABS_KEY;

public class Tabs extends LinearLayout implements IFormControl
{
    protected int mDefaultTab = 0;
    protected Map<String, IControl> mFields;
    protected List<Fragment> mTabs;

    protected VectorLayer mLayer;
    protected long mFeatureId;
    protected boolean mIsViewOnly;
    protected GeoGeometry mGeometry;
    private Map<String, List<String>> mTable;
    private int mRow = -1;
    protected SharedPreferences mSharedPreferences;
    protected SharedPreferences mPreferences;
    protected FragmentManager mFragmentManager;

    public Tabs(Context context) {
        super(context);
    }

    public Tabs(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(VectorLayer layer, long featureId, GeoGeometry geometry, Map<String, List<String>> table, int row,
                     SharedPreferences sharedPreferences, SharedPreferences preferences,
                     FragmentManager supportFragmentManager, boolean isViewOnly) {
        mLayer = layer;
        mFeatureId = featureId;
        mIsViewOnly = isViewOnly;
        mGeometry = geometry;
        mTable = table;
        mRow = row;
        mSharedPreferences = sharedPreferences;
        mPreferences = preferences;
        mFragmentManager = supportFragmentManager;
    }

    @Override
    public void init(JSONObject tabs,
                     List<Field> fields,
                     Bundle savedState,
                     Cursor featureCursor,
                     SharedPreferences preferences) throws JSONException{

        mFields = new HashMap<>();
        mTabs = new ArrayList<>();
        TabLayout tabLayout = (TabLayout) getChildAt(0);
        JSONArray pages = tabs.getJSONArray(JSON_PAGES_KEY);
        for (int i = 0; i < pages.length(); i++) {
            JSONObject item = pages.getJSONObject(i);
            mDefaultTab = item.optBoolean(JSON_DEFAULT_KEY, false) ? i : mDefaultTab;
            TabLayout.Tab tab = tabLayout.newTab();
            String title = item.optString(JSON_CAPTION_KEY);
            tab.setText(title);

            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(VERTICAL);
            JSONArray elements = item.getJSONArray(JSON_ELEMENTS_KEY);
            for (int j = 0; j < elements.length(); j++) {
                JSONObject element = elements.getJSONObject(j);
                String type = element.optString(JSON_TYPE_KEY);
                if (type.equals(JSON_COORDINATES_VALUE)) {
                    JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    String fieldY = attributes.optString(JSON_FIELD_NAME_KEY + "_lat");
                    attributes.put(JSON_FIELD_NAME_KEY, fieldY);
                    element.put(JSON_TYPE_KEY, type + "_lat");
                    IFormControl control = getControl(getContext(), element, mLayer, mFeatureId, mGeometry, mIsViewOnly);
                    addToLayout(control, element, fields, savedState, featureCursor, layout);

                    attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    String fieldX = attributes.optString(JSON_FIELD_NAME_KEY + "_long");
                    attributes.put(JSON_FIELD_NAME_KEY, fieldX);
                    element.put(JSON_TYPE_KEY, type + "_lon");
                }
                IFormControl control = getControl(getContext(), element, mLayer, mFeatureId, mGeometry, mIsViewOnly);
                addToLayout(control, element, fields, savedState, featureCursor, layout);
            }
            TabFragment fragment = new TabFragment();
            fragment.setLayout(layout);
            mTabs.add(fragment);
            tabLayout.addTab(tab);
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                replaceFragment(mTabs.get(tab.getPosition()));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        FrameLayout container = new FrameLayout(getContext());
        container.setId(R.id.root_view);
        addView(container);

        if (savedState != null)
            mDefaultTab = savedState.getInt(ControlHelper.getSavedStateKey(JSON_TABS_KEY), mDefaultTab);

        if (mDefaultTab > 0)
            //noinspection ConstantConditions
            tabLayout.getTabAt(mDefaultTab).select();
        else
            replaceFragment(mTabs.get(0));
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = mFragmentManager;
        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.root_view, fragment);
        transaction.commit();
    }

    protected void addToLayout(IFormControl control, JSONObject element, List<Field> fields, Bundle savedState, Cursor featureCursor, LinearLayout layout)
            throws JSONException {
        if (null != control) {
            appendData(mLayer, mPreferences, mTable, mRow, control, element);

            control.init(element, fields, savedState, featureCursor, mSharedPreferences);
            control.addToLayout(layout);
            if (mIsViewOnly)
                control.setEnabled(false);

            String fieldName = control.getFieldName();
            if (null != fieldName)
                mFields.put(fieldName, control);
            if (control instanceof Tabs) {
                Tabs tabs = (Tabs) control;
                mFields.putAll(tabs.getFields());
            }
        }
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {

    }

    @Override
    public boolean isShowLast() {
        return false;
    }


    public String getFieldName()
    {
        return null;
    }

    public Map<String, IControl> getFields()
    {
        return mFields;
    }


    @Override
    public void addToLayout(ViewGroup layout)
    {
        layout.addView(this);
        GreyLine.addToLayout(layout);
    }


    @Override
    public Object getValue()
    {
        return ((TabLayout) getChildAt(0)).getSelectedTabPosition();
    }

    @Override
    public void saveState(Bundle outState) {
        for (Map.Entry<String, IControl> control : mFields.entrySet()) {
            control.getValue().saveState(outState);
        }
        int value = (Integer) getValue();
        outState.putInt(ControlHelper.getSavedStateKey(JSON_TABS_KEY), value);
    }

}

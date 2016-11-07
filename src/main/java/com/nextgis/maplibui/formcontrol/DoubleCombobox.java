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

package com.nextgis.maplibui.formcontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.control.AliasList;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DEFAULT_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_LEVEL1_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_LEVEL2_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_ALIAS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_NAME_KEY;


public class DoubleCombobox extends AppCompatSpinner implements IFormControl
{
    protected Spinner mSubCombobox;

    protected String mFieldName;
    protected String mSubFieldName;

    protected boolean mIsShowLast;

    protected Map<String, String>              mAliasValueMap;
    protected Map<String, Map<String, String>> mSubAliasValueMaps;
    protected Map<String, AliasList>           mAliasSubListMap;

    protected boolean mFirstShow = true;

    public DoubleCombobox(Context context) {
        super(context);
    }

    public DoubleCombobox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DoubleCombobox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    //TODO: add mode_dialog if attribute asDialog == true, Spinner.MODE_DIALOG API Level 11+

    @Override
    public void init(JSONObject element, List<Field> fields, Bundle savedState, Cursor featureCursor, SharedPreferences preferences) throws JSONException {
        mSubCombobox = new Spinner(getContext());
        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = attributes.getString(JSON_FIELD_LEVEL1_KEY);
        mSubFieldName = attributes.getString(JSON_FIELD_LEVEL2_KEY);
        mIsShowLast = ControlHelper.isSaveLastValue(attributes);
        setEnabled(ControlHelper.isEnabled(fields, mFieldName));

        String lastValue = null;
        String subLastValue = null;
        if (ControlHelper.hasKey(savedState, mFieldName) && ControlHelper.hasKey(savedState, mSubFieldName)) {
            lastValue = savedState.getString(ControlHelper.getSavedStateKey(mFieldName));
            subLastValue = savedState.getString(ControlHelper.getSavedStateKey(mSubFieldName));
        } else if (null != featureCursor) {
            int column = featureCursor.getColumnIndex(mFieldName);
            int subColumn = featureCursor.getColumnIndex(mSubFieldName);
            if (column >= 0)
                lastValue = featureCursor.getString(column);
            if (subColumn >= 0)
                subLastValue = featureCursor.getString(subColumn);
        } else if (mIsShowLast) {
            lastValue = preferences.getString(mFieldName, null);
            subLastValue = preferences.getString(mSubFieldName, null);
        }

        JSONArray values = attributes.optJSONArray(JSON_VALUES_KEY);
        int defaultPosition = 0;
        int lastValuePosition = -1;
        int subLastValuePosition = -1;
        mAliasValueMap = new HashMap<>();
        mSubAliasValueMaps = new HashMap<>();
        mAliasSubListMap = new HashMap<>();

        final ArrayAdapter<String> comboboxAdapter = new ArrayAdapter<>(getContext(), R.layout.formtemplate_double_spinner);
        setAdapter(comboboxAdapter);

        if (values != null) {
            for (int j = 0; j < values.length(); j++) {
                JSONObject keyValue = values.getJSONObject(j);
                String value = keyValue.getString(JSON_VALUE_NAME_KEY);
                String valueAlias = keyValue.getString(JSON_VALUE_ALIAS_KEY);

                Map<String, String> subAliasValueMap = new HashMap<>();
                AliasList subAliasList = new AliasList();

                mAliasValueMap.put(valueAlias, value);
                mSubAliasValueMaps.put(valueAlias, subAliasValueMap);
                mAliasSubListMap.put(valueAlias, subAliasList);
                comboboxAdapter.add(valueAlias);

                if (keyValue.has(JSON_DEFAULT_KEY) && keyValue.getBoolean(JSON_DEFAULT_KEY))
                    defaultPosition = j;

                if (null != lastValue && lastValue.equals(value)) // if modify data
                    lastValuePosition = j;

                JSONArray subValues = keyValue.getJSONArray(JSON_VALUES_KEY);
                for (int k = 0; k < subValues.length(); k++) {
                    JSONObject subKeyValue = subValues.getJSONObject(k);
                    String subValue = subKeyValue.getString(JSON_VALUE_NAME_KEY);
                    String subValueAlias = subKeyValue.getString(JSON_VALUE_ALIAS_KEY);

                    subAliasValueMap.put(subValueAlias, subValue);
                    subAliasList.aliasList.add(subValueAlias);

                    if (subKeyValue.has(JSON_DEFAULT_KEY) && subKeyValue.getBoolean(JSON_DEFAULT_KEY))
                        subAliasList.defaultPosition = k;

                    if (null != subLastValue && subLastValue.equals(subValue)) { // if modify data
                        lastValuePosition = j;
                        subLastValuePosition = k;
                    }
                }
            }
        }

        setSelection(lastValuePosition >= 0 ? lastValuePosition : defaultPosition);
        final int subLastValuePositionFinal = subLastValuePosition;

        // The drop down view
        comboboxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        float minHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
        setPadding(0, (int) minHeight, 0, (int) minHeight);
        mSubCombobox.setPadding(0, (int) minHeight, 0, (int) minHeight);

        setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener()
                {
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id)
                    {
                        String selectedValueAlias = comboboxAdapter.getItem(position);
                        AliasList subAliasList = mAliasSubListMap.get(selectedValueAlias);

                        ArrayAdapter<String> subComboboxAdapter = new ArrayAdapter<>(
                                view.getContext(), R.layout.formtemplate_double_spinner,
                                subAliasList.aliasList);
                        subComboboxAdapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);

                        mSubCombobox.setAdapter(subComboboxAdapter);
                        mSubCombobox.setSelection(
                                mFirstShow && subLastValuePositionFinal >= 0
                                ? subLastValuePositionFinal
                                : subAliasList.defaultPosition);

                        if (mFirstShow) {
                            mFirstShow = false;
                        }
                    }


                    public void onNothingSelected(AdapterView<?> arg0)
                    {
                    }
                });
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {
        DoubleComboboxValue result = (DoubleComboboxValue) getValue();
        preferences.edit().putString(result.mFieldName, result.mValue).commit();
        preferences.edit().putString(result.mSubFieldName, result.mSubValue).commit();
    }

    @Override
    public boolean isShowLast() {
        return mIsShowLast;
    }


    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        mSubCombobox.setEnabled(enabled);
    }


    public String getFieldName()
    {
        return mFieldName;
    }


    @Override
    public void addToLayout(ViewGroup layout)
    {
        layout.addView(this);
        layout.addView(mSubCombobox);
    }


    @Override
    public Object getValue()
    {
        String valueAlias = (String) getSelectedItem();
        String subValueAlias = (String) mSubCombobox.getSelectedItem();

        String value = mAliasValueMap.get(valueAlias);
        String subValue = mSubAliasValueMaps.get(valueAlias).get(subValueAlias);

        DoubleComboboxValue retValue = new DoubleComboboxValue();

        retValue.mFieldName = mFieldName;
        retValue.mValue = value;

        retValue.mSubFieldName = mSubFieldName;
        retValue.mSubValue = subValue;

        return retValue;
    }

    @Override
    public void saveState(Bundle outState) {
        DoubleComboboxValue result = (DoubleComboboxValue) getValue();
        outState.putString(ControlHelper.getSavedStateKey(mFieldName), result.mValue);
        outState.putString(ControlHelper.getSavedStateKey(mSubFieldName), result.mSubValue);
    }
}

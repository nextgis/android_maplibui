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
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.controlui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.nextgis.maplib.datasource.Field;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplibui.util.ConstantsUI.*;


@SuppressLint("ViewConstructor")
public class DoubleComboboxJsonControl
        extends Spinner
        implements IControl
{
    protected Spinner mSubCombobox;

    protected String mFieldName;
    protected String mSubFieldName;

    protected boolean mIsShowLast;

    protected Map<String, String>              mAliasValueMap;
    protected Map<String, Map<String, String>> mSubAliasValueMaps;
    protected Map<String, ArrayList<String>>   mSubComboboxValueListMap;


    public DoubleComboboxJsonControl(
            Context context,
            JSONObject element,
            List<Field> fields,
            Cursor featureCursor)
            throws JSONException
    {
        super(context);

        //TODO: add mode_dialog if attribute asDialog == true, Spinner.MODE_DIALOG API Level 11+

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

        mFieldName = attributes.getString(JSON_FIELD_LEVEL1_KEY);
        mSubFieldName = attributes.getString(JSON_FIELD_LEVEL2_KEY);

        boolean isEnabled = false;
        for (Field field : fields) {
            String fieldName = field.getName();
            if (fieldName.equals(mFieldName)) {
                isEnabled = true;
                break;
            }
        }
        setEnabled(isEnabled);

        mIsShowLast = false;
        if (attributes.has(JSON_SHOW_LAST_KEY) && !attributes.isNull(
                JSON_SHOW_LAST_KEY)) {
            mIsShowLast = attributes.getBoolean(JSON_SHOW_LAST_KEY);
        }

        String lastValue = null;
        String subLastValue = null;
        if (mIsShowLast) {
            if (null != featureCursor) {
                int column = featureCursor.getColumnIndex(mFieldName);
                int subColumn = featureCursor.getColumnIndex(mSubFieldName);
                if (column >= 0) {
                    lastValue = featureCursor.getString(column);
                }
                if (subColumn >= 0) {
                    subLastValue = featureCursor.getString(subColumn);
                }
            }
        }


        JSONArray values = attributes.getJSONArray(JSON_VALUES_KEY);
        int defaultPosition = 0;
        int subDefaultPosition = 0;
        int lastValuePosition = -1;
        int subLastValuePosition = -1;
        mSubAliasValueMaps = new HashMap<>();

        final ArrayAdapter<String> comboboxAdapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
        setAdapter(comboboxAdapter);

        for (int j = 0; j < values.length(); j++) {
            JSONObject keyValue = values.getJSONObject(j);
            String value = keyValue.getString(JSON_VALUE_NAME_KEY);
            String valueAlias = keyValue.getString(JSON_VALUE_ALIAS_KEY);

            if (keyValue.has(JSON_DEFAULT_KEY) && keyValue.getBoolean(
                    JSON_DEFAULT_KEY)) {
                defaultPosition = j;
            }

            Map<String, String> subAliasValueMap = new HashMap<>();
            mAliasValueMap.put(valueAlias, value);
            mSubAliasValueMaps.put(valueAlias, subAliasValueMap);
            comboboxAdapter.add(valueAlias);

            JSONArray subValues = keyValue.getJSONArray(JSON_VALUES_KEY);

            if (mIsShowLast && null != lastValue && lastValue.equals(value)) { // if modify data
                lastValuePosition = j;
            }


            for (int k = 0; k < subValues.length(); k++) {
                JSONObject subKeyValue = subValues.getJSONObject(k);
                String subValue = subKeyValue.getString(JSON_VALUE_NAME_KEY);
                String subAliasValue = subKeyValue.getString(JSON_VALUE_ALIAS_KEY);

                if (subKeyValue.has(JSON_DEFAULT_KEY) && subKeyValue.getBoolean(
                        JSON_DEFAULT_KEY)) {
                    subDefaultPosition = j;
                }

                if (mIsShowLast && null != subLastValue &&
                    subLastValue.equals(value)) { // if modify data

                    lastValuePosition = j;
                    subLastValuePosition = k;
                }

                subAliasValueMap.put(subAliasValue, subValue);
            }
        }

        setSelection(lastValuePosition >= 0 ? lastValuePosition : defaultPosition);
        final int subLastValuePositionFinal = subLastValuePosition;
        final int subDefaultPositionFinal = subDefaultPosition;

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
                        List<String> subComboboxValueList =
                                mSubComboboxValueListMap.get(selectedValueAlias);

                        ArrayAdapter<String> subComboboxAdapter = new ArrayAdapter<>(
                                view.getContext(), android.R.layout.simple_spinner_item,
                                subComboboxValueList);
                        subComboboxAdapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);
                        mSubCombobox.setAdapter(subComboboxAdapter);

                        mSubCombobox.setSelection(
                                subLastValuePositionFinal >= 0
                                ? subLastValuePositionFinal
                                : subDefaultPositionFinal);
                    }


                    public void onNothingSelected(AdapterView<?> arg0)
                    {
                    }
                });
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
        GreyLine.addToLayout(layout);
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
}

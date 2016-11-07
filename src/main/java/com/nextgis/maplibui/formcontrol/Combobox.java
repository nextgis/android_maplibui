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
import android.provider.SyncStateContract;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.NGWLookupTable;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DEFAULT_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_NGW_ID_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_ALIAS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_NAME_KEY;

public class Combobox extends AppCompatSpinner implements IFormControl
{
    protected String              mFieldName;
    protected boolean             mIsShowLast;
    protected Map<String, String> mAliasValueMap;

    public Combobox(Context context) {
        super(context);
    }

    public Combobox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Combobox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void init(JSONObject element,
                     List<Field> fields,
                     Bundle savedState,
                     Cursor featureCursor,
                     SharedPreferences preferences) throws JSONException{

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = attributes.getString(JSON_FIELD_NAME_KEY);
        mIsShowLast = ControlHelper.isSaveLastValue(attributes);
        setEnabled(ControlHelper.isEnabled(fields, mFieldName));

        String lastValue = null;
        if (ControlHelper.hasKey(savedState, mFieldName))
            lastValue = savedState.getString(ControlHelper.getSavedStateKey(mFieldName));
        else if (null != featureCursor) {
                int column = featureCursor.getColumnIndex(mFieldName);
                if (column >= 0)
                    lastValue = featureCursor.getString(column);
        } else if (mIsShowLast)
            lastValue = preferences.getString(mFieldName, null);

        int defaultPosition = 0;
        int lastValuePosition = -1;
        mAliasValueMap = new HashMap<>();

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getContext(), R.layout.formtemplate_spinner);
        setAdapter(spinnerArrayAdapter);

        if (attributes.has(ConstantsUI.JSON_NGW_ID_KEY) && attributes.getLong(ConstantsUI.JSON_NGW_ID_KEY) != -1) {
            MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
            if (null == map)
                throw new IllegalArgumentException("The map should extends MapContentProviderHelper or inherited");

            String account = element.optString(SyncStateContract.Columns.ACCOUNT_NAME);
            long id = attributes.getLong(JSON_NGW_ID_KEY);
            for (int i = 0; i < map.getLayerCount(); i++) {
                if (map.getLayer(i) instanceof NGWLookupTable) {
                    NGWLookupTable table = (NGWLookupTable) map.getLayer(i);
                    if (table.getRemoteId() != id || !table.getAccountName().equals(account))
                        continue;

                    int j = 0;
                    for (Map.Entry<String, String> entry : table.getData().entrySet()) {
                        mAliasValueMap.put(entry.getValue(), entry.getKey());

                        if (null != lastValue && lastValue.equals(entry.getKey()))
                            lastValuePosition = j;

                        spinnerArrayAdapter.add(entry.getValue());
                        j++;
                    }

                    break;
                }
            }
        } else {
            JSONArray values = attributes.optJSONArray(JSON_VALUES_KEY);
            if (values != null) {
                for (int j = 0; j < values.length(); j++) {
                    JSONObject keyValue = values.getJSONObject(j);
                    String value = keyValue.getString(JSON_VALUE_NAME_KEY);
                    String value_alias = keyValue.getString(JSON_VALUE_ALIAS_KEY);

                    if (keyValue.has(JSON_DEFAULT_KEY) && keyValue.getBoolean(JSON_DEFAULT_KEY))
                        defaultPosition = j;

                    if (null != lastValue && lastValue.equals(value))
                        lastValuePosition = j;

                    mAliasValueMap.put(value_alias, value);
                    spinnerArrayAdapter.add(value_alias);
                }
            }
        }

        setSelection(lastValuePosition >= 0 ? lastValuePosition : defaultPosition);

        // The drop down view
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        float minHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
        setPadding(0, (int) minHeight, 0, (int) minHeight);
    }


    @Override
    public void saveLastValue(SharedPreferences preferences) {
        preferences.edit().putString(mFieldName, (String) getValue()).commit();
    }


    @Override
    public boolean isShowLast() {
        return mIsShowLast;
    }


    public String getFieldName()
    {
        return mFieldName;
    }


    @Override
    public void addToLayout(ViewGroup layout)
    {
        layout.addView(this);
    }


    @Override
    public Object getValue()
    {
        String valueAlias = (String) getSelectedItem();
        return mAliasValueMap.get(valueAlias);
    }


    @Override
    public void saveState(Bundle outState) {
        outState.putString(ControlHelper.getSavedStateKey(mFieldName), (String) getValue());
    }
}

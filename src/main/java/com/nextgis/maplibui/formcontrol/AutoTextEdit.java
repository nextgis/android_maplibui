/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.formcontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_ALLOW_NEW_VALUES;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_NGW_ID_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_ALIAS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_NAME_KEY;

public class AutoTextEdit extends AppCompatAutoCompleteTextView implements IFormControl
{
    protected String            mFieldName;
    protected boolean           mIsShowLast;
    protected boolean           mAllowSaveNewValue;
    protected Map<String, String>   mAliasValueMap;
    protected ArrayAdapter<String>  mAdapter;

    public AutoTextEdit(Context context) {
        super(context);
    }

    public AutoTextEdit(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoTextEdit(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void init(JSONObject element,
                     List<Field> fields,
                     Bundle savedState,
                     Cursor featureCursor,
                     SharedPreferences preferences) throws JSONException{
        ControlHelper.setClearAction(this);

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = attributes.getString(JSON_FIELD_NAME_KEY);
        mIsShowLast = ControlHelper.isSaveLastValue(attributes);
        mAllowSaveNewValue = attributes.optBoolean(JSON_ALLOW_NEW_VALUES);

        if (!ControlHelper.isEnabled(fields, mFieldName)) {
            setEnabled(false);
            setTextColor(Color.GRAY);
        }

        String lastValue = null;
        if (ControlHelper.hasKey(savedState, mFieldName))
            lastValue = savedState.getString(ControlHelper.getSavedStateKey(mFieldName));
        else if (null != featureCursor) {
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0)
                lastValue = featureCursor.getString(column);
        } else if (mIsShowLast)
            lastValue = preferences.getString(mFieldName, null);

        mAliasValueMap = new LinkedHashMap<>();
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

                    for (Map.Entry<String, String> entry : table.getData().entrySet()) {
                        mAliasValueMap.put(entry.getValue(), entry.getKey());
                        if (entry.getKey().equals(lastValue))
                            lastValue = entry.getValue();
                    }

                    break;
                }
            }
        } else {
            JSONArray values = attributes.getJSONArray(JSON_VALUES_KEY);
            for (int j = 0; j < values.length(); j++) {
                JSONObject keyValue = values.getJSONObject(j);
                String value = keyValue.getString(JSON_VALUE_NAME_KEY);
                String alias = keyValue.getString(JSON_VALUE_ALIAS_KEY);
                mAliasValueMap.put(alias, value);
                if (value.equals(lastValue))
                    lastValue = alias;
            }
        }

        setText(lastValue);
        mAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(mAliasValueMap.keySet()));
        setAdapter(mAdapter);

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus && getText().length() == 0) {
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showDropDown();
                        }
                    }, 100);
                }
            }
        });

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() < 1)
                    getOnFocusChangeListener().onFocusChange(AutoTextEdit.this, true);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
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
        if (!mAllowSaveNewValue && !mAliasValueMap.containsKey(getText().toString())) {
            Toast.makeText(getContext(), R.string.value_not_from_list, Toast.LENGTH_SHORT).show();
            return null;
        }

        return mAliasValueMap.get(getText().toString());
    }


    @Override
    public void saveState(Bundle outState) {
        outState.putString(ControlHelper.getSavedStateKey(mFieldName), (String) getValue());
    }
}

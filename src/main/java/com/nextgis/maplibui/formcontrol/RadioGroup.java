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
import android.database.Cursor;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.control.GreyLine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DEFAULT_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_SHOW_LAST_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_ALIAS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_NAME_KEY;

public class RadioGroup extends android.widget.RadioGroup implements IFormControl
{
    protected String              mFieldName;
    protected boolean             mIsShowLast;
    protected Map<String, String> mAliasValueMap;

    public RadioGroup(Context context) {
        super(context);
    }

    public RadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(JSONObject element,
                     List<Field> fields,
                     Cursor featureCursor) throws JSONException{

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

        mFieldName = attributes.getString(JSON_FIELD_NAME_KEY);

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
        if (mIsShowLast) {
            if (null != featureCursor) {
                int column = featureCursor.getColumnIndex(mFieldName);
                if (column >= 0) {
                    lastValue = featureCursor.getString(column);
                }
            }
        }


        JSONArray values = attributes.getJSONArray(JSON_VALUES_KEY);
        int defaultPosition = 0;
        int lastValuePosition = -1;
        mAliasValueMap = new HashMap<>();

        for (int j = 0; j < values.length(); j++) {
            JSONObject keyValue = values.getJSONObject(j);
            String value = keyValue.getString(JSON_VALUE_NAME_KEY);
            String value_alias = keyValue.getString(JSON_VALUE_ALIAS_KEY);

            if (keyValue.has(JSON_DEFAULT_KEY) && keyValue.getBoolean(
                    JSON_DEFAULT_KEY)) {
                defaultPosition = j;
            }

            if (mIsShowLast && null != lastValue && lastValue.equals(value)) { // if modify data
                lastValuePosition = j;
            }

            mAliasValueMap.put(value_alias, value);
            AppCompatRadioButton radioButton = new AppCompatRadioButton(getContext());
            radioButton.setText(value_alias);
            addView(radioButton);
        }

        check(getChildAt(lastValuePosition >= 0 ? lastValuePosition : defaultPosition).getId());
        setOrientation(RadioGroup.VERTICAL);
    }


    public String getFieldName()
    {
        return mFieldName;
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
        RadioButton radioButton = (RadioButton) findViewById(getCheckedRadioButtonId());
        String value_alias = (String) radioButton.getText();
        return mAliasValueMap.get(value_alias);
    }

}

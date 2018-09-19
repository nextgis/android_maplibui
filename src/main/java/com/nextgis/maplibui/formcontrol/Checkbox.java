/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016, 2018 NextGIS, info@nextgis.com
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
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_INIT_VALUE_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TEXT_KEY;

public class Checkbox extends AppCompatCheckBox implements IFormControl {
    protected String mFieldName;
    protected boolean mIsShowLast;

    public Checkbox(Context context) {
        super(context);
    }

    public Checkbox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(JSONObject element,
                     List<Field> fields,
                     Bundle savedState,
                     Cursor featureCursor,
                     SharedPreferences preferences) throws JSONException {

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = attributes.getString(JSON_FIELD_NAME_KEY);
        mIsShowLast = ControlHelper.isSaveLastValue(attributes);

        Boolean value = null;
        if (ControlHelper.hasKey(savedState, mFieldName))
            value = savedState.getBoolean(ControlHelper.getSavedStateKey(mFieldName));
        else if (null != featureCursor) {
            int column = featureCursor.getColumnIndex(mFieldName);

            if (column >= 0)
                value = featureCursor.getInt(column) != 0;
        } else {
            value = attributes.getBoolean(JSON_INIT_VALUE_KEY);

            if (mIsShowLast)
                value = preferences.getBoolean(mFieldName, value);
        }

        if (value == null)
            value = false;

        setChecked(value);
        setText(attributes.getString(JSON_TEXT_KEY));
        setEnabled(ControlHelper.isEnabled(fields, mFieldName));
    }

    public String getFieldName() {
        return mFieldName;
    }

    @Override
    public void addToLayout(ViewGroup layout) {
        layout.addView(this);
    }

    @Override
    public Object getValue() {
        return isChecked() ? 1 : 0;
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putBoolean(ControlHelper.getSavedStateKey(mFieldName), isChecked());
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {
        preferences.edit().putBoolean(mFieldName, isChecked()).apply();
    }

    @Override
    public boolean isShowLast() {
        return mIsShowLast;
    }

}

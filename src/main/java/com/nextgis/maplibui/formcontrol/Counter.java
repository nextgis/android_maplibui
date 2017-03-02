/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016-2017 NextGIS, info@nextgis.com
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.AttributeSet;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_INIT_VALUE_KEY;


@SuppressLint("ViewConstructor")
public class Counter extends TextEdit
        implements IFormControl
{
    private static final String INCREMENT = "increment";
    public static final String PREFIX = "prefix";
    public static final String SUFFIX = "suffix";
    public static final String PREFIX_LIST = "prefix_from_list";
    public static final String SUFFIX_LIST = "suffix_from_list";

    protected long mIncremented = -1;

    public Counter(Context context) {
        super(context);
    }

    public Counter(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Counter(Context context, AttributeSet attrs, int defStyleAttr) {
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
        mIsShowLast = true;

        String value = null;
        if (null != featureCursor) { // feature exists
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0)
                value = featureCursor.getString(column);
        } else if (ControlHelper.hasKey(savedState, mFieldName)) {
            mIncremented = savedState.getLong(ControlHelper.getSavedStateKey(mFieldName));
            value = getNewValue(attributes);
        } else {    // new feature
            String last = preferences.getString(mFieldName, null);
            if (last == null)
                last = attributes.getInt(JSON_INIT_VALUE_KEY) - 1 + "";

            int inc = attributes.getInt(INCREMENT);
            mIncremented = Long.valueOf(last) + inc;
            value = getNewValue(attributes);
        }

        setEnabled(false);
        setText(value);
        setSingleLine(true);
    }

    private String getNewValue(JSONObject attributes) {
        String prefix = attributes.optString(PREFIX);
        String suffix = attributes.optString(SUFFIX);
        return prefix + mIncremented + suffix;
    }

    @Override
    public void saveState(Bundle outState) {
        super.saveState(outState);
        outState.putLong(ControlHelper.getSavedStateKey(mFieldName), mIncremented);
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {
        if (mIncremented != -1)
            preferences.edit().putString(mFieldName, Long.toString(mIncremented)).apply();
    }

}

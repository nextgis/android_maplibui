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

package com.nextgis.maplibui.formcontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.ViewGroup;

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
    private static final String PREFIX = "prefix";
    private static final String SUFFIX = "suffix";

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
        if (ControlHelper.hasKey(savedState, mFieldName))
            value = savedState.getString(ControlHelper.getSavedStateKey(mFieldName));
        else if (null != featureCursor) { // feature exists
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0)
                value = featureCursor.getString(column);
        } else {    // new feature
            String last = preferences.getString(mFieldName, null);
            if (last == null)
                last = attributes.getInt(JSON_INIT_VALUE_KEY) - 1 + "";

            String prefix = attributes.optString(PREFIX);
            String suffix = attributes.optString(SUFFIX);
            int inc = attributes.getInt(INCREMENT);
            mIncremented = Long.valueOf(last) + inc;
            value = prefix + mIncremented + suffix;
        }

        setEnabled(false);
        setText(value);
        setSingleLine(true);
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {
        if (mIncremented != -1)
            preferences.edit().putString(mFieldName, Long.toString(mIncremented)).commit();
    }

}

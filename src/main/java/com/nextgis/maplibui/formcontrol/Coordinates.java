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
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_HIDDEN_KEY;


@SuppressLint("ViewConstructor")
public class Coordinates extends TextEdit
        implements IFormControl
{
    protected boolean mHidden;
    protected double mValue;
    protected boolean mIsLat = false;

    public Coordinates(Context context) {
        super(context);
    }

    public Coordinates(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Coordinates(Context context, AttributeSet attrs, int defStyleAttr) {
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
        mHidden = attributes.optBoolean(JSON_HIDDEN_KEY);

        // TODO crs, format
        String value;
        if (ControlHelper.hasKey(savedState, mFieldName))
            value = savedState.getString(ControlHelper.getSavedStateKey(mFieldName));
        else
            value = getFormattedValue();

        setText(value);
        setSingleLine(true);
        setEnabled(false);
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {

    }

    @Override
    public boolean isShowLast() {
        return mIsShowLast;
    }

    @Override
    public String getFieldName() {
        return mFieldName;
    }

    @Override
    public void addToLayout(ViewGroup layout) {
        if (!mHidden)
            layout.addView(this);
    }

    @Override
    public Object getValue() {
        return getText().toString();
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putString(ControlHelper.getSavedStateKey(mFieldName), getText().toString());
    }

    public void setValue(double value) {
        mValue = value;
    }

    public String getFormattedValue() {
        DecimalFormat format = new DecimalFormat("#.######", new DecimalFormatSymbols(Locale.US));
        return format.format(mValue);
    }

    public void setIsLat() {
        mIsLat = true;
    }

    public boolean isLat() {
        return mIsLat;
    }
}

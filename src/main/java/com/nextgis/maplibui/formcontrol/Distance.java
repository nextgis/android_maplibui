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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_HIDDEN_KEY;

@SuppressLint("ViewConstructor")
public class Distance extends LinearLayout implements IFormControl {
    protected boolean mHidden;
    protected double mValue;
    protected String mFieldName;
    protected Location mLocation;

    public Distance(Context context) {
        super(context);
    }

    public Distance(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Distance(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void init(JSONObject element, List<Field> fields, Bundle savedState, Cursor featureCursor, SharedPreferences preferences) throws JSONException {
        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = attributes.getString(JSON_FIELD_NAME_KEY);
        mHidden = attributes.optBoolean(JSON_HIDDEN_KEY);

        String value;
        if (null != featureCursor) { // feature exists
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0)
                mValue = featureCursor.getDouble(column);
            value = getFormattedValue();
        } else if (ControlHelper.hasKey(savedState, mFieldName))
            value = savedState.getString(ControlHelper.getSavedStateKey(mFieldName));
        else {
            calculate(false);
            value = getFormattedValue();
        }

        ((AppCompatEditText) findViewById(R.id.distance)).setText(value);
        findViewById(R.id.refresh).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate(true);
                ((AppCompatEditText) findViewById(R.id.distance)).setText(getFormattedValue());
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void calculate(boolean toast) {
        if (getContext() != null) {
            IGISApplication app = (IGISApplication) getContext().getApplicationContext();
            GpsEventSource gpsEventSource = app.getGpsEventSource();
            Location current = gpsEventSource.getLastKnownLocation();
            if (current != null && mLocation != null)
                mValue = current.distanceTo(mLocation);
            else if (toast)
                Toast.makeText(getContext(), R.string.error_no_location, Toast.LENGTH_SHORT).show();
        } else if (toast)
            Toast.makeText(getContext(), R.string.error_no_location, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {

    }

    @Override
    public boolean isShowLast() {
        return false;
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
        return mValue;
    }

    @Override
    public void saveState(Bundle outState) {
        String text = ((AppCompatEditText) findViewById(R.id.distance)).getText().toString();
        outState.putString(ControlHelper.getSavedStateKey(mFieldName), text);
    }

    public void setValue(double value) {
        mValue = value;
    }

    public String getFormattedValue() {
        return LocationUtil.formatLength(getContext(), mValue, 2);
    }

    public void setLocation(Location location) {
        mLocation = location;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        findViewById(R.id.refresh).setEnabled(enabled);
    }
}

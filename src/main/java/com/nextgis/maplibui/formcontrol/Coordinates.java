/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016, 2020-2021 NextGIS, info@nextgis.com
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

import static android.widget.Toast.LENGTH_LONG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.ModifyAttributesActivity;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_HIDDEN_KEY;

import androidx.appcompat.app.AlertDialog;


@SuppressLint("ViewConstructor")
public class Coordinates extends TextEdit
        implements IFormControl
{
    protected boolean mHidden;
    protected double mValue;
    protected boolean mIsLat = false;
    boolean useDisabledClick = false;

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
                     SharedPreferences preferences,
                     Map<String, Map<String, String>> translations,
                     final ModifyAttributesActivity modifyAttributesActivity) throws JSONException{
        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = ControlHelper.getFieldName(attributes.getString(JSON_FIELD_NAME_KEY));
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

        if (!ControlHelper.isEnabled(fields, mFieldName)) {
            useDisabledClick = true;
            setEnabled(false);
            setTextColor(Color.GRAY);
            setBackgroundColor(Color.LTGRAY);
            setOnClickListener( view -> {
                AlertDialog dialog = new AlertDialog.Builder(getContext())
                        .setMessage(R.string.form_trouble)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            });
        }

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
    private OnClickListener mCustomClickListener;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (  useDisabledClick &&   event.getAction() == MotionEvent.ACTION_UP) {
            // Вызываем клик при отжатии пальца
            if (mCustomClickListener != null) {
                mCustomClickListener.onClick(this);
            }
            return true; // Говорим, что обработали
        }
        return super.onTouchEvent(event);
    }

    // Переопределяем setOnClickListener, чтобы сохранить наш слушатель
    @Override
    public void setOnClickListener(OnClickListener l) {
        mCustomClickListener = l;
    }

}

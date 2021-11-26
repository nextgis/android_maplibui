/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2020-2021 NextGIS, info@nextgis.com
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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;

@SuppressLint("ViewConstructor")
public class Averaging extends LinearLayout implements IFormControl, View.OnClickListener {
    private static final String MEASUREMENT_COUNT = "num_values";

    protected String mFieldName;
    protected long mMeasures = 1;
    protected long mDoneMeasures = 0;
    protected Double mValue = 0.0;

    public Averaging(Context context) {
        super(context);
    }

    public Averaging(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Averaging(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void init(JSONObject element, List<Field> fields, Bundle savedState,
                     Cursor featureCursor, SharedPreferences preferences,
                     Map<String, Map<String, String>> translations) throws JSONException {

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = ControlHelper.getFieldName(attributes.getString(JSON_FIELD_NAME_KEY));
        mMeasures = attributes.getLong(MEASUREMENT_COUNT);

        if (null != featureCursor) { // feature exists
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0)
                mValue = featureCursor.getDouble(column);
        } else if (ControlHelper.hasKey(savedState, mFieldName)) {
            mValue = savedState.getDouble(ControlHelper.getSavedStateKey(mFieldName));
        } else {    // new feature
            mValue = 0.0;
        }

        setEnabled(false);
        setValue();
        Button average = findViewById(R.id.average);
        average.setOnClickListener(this);
    }

    private void setValue() {
        EditText strValue = findViewById(R.id.value);
        strValue.setText(String.format(Locale.getDefault(), "%f", mValue));
    }

    @Override
    public String getFieldName() {
        return mFieldName;
    }

    @Override
    public void addToLayout(ViewGroup layout) {
        layout.addView(this);
    }

    @Override
    public Object getValue() {
        return mValue;
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putDouble(ControlHelper.getSavedStateKey(mFieldName), mValue);
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {

    }

    @Override
    public boolean isShowLast() {
        return false;
    }

    @Override
    public void onClick(View view) {
        mDoneMeasures = 0;
        createDialog();
    }

    private void createDialog() {
        final EditText editText = new EditText(getContext());
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        String title = getContext().getString(R.string.averaging_enter_value, mDoneMeasures + 1, mMeasures);
        builder.setTitle(title).setView(editText).setPositiveButton(R.string.average_next, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mDoneMeasures++;
                double value = 0;
                try {
                    value = Double.parseDouble(editText.getText().toString());
                } catch (Exception ignored) {}
                mValue += value;
                if (mDoneMeasures < mMeasures) {
                    createDialog();
                } else {
                    mValue /= mMeasures;
                    setValue();
                }
            }
        });
        builder.create().show();
    }
}

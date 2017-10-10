/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2017 NextGIS, info@nextgis.com
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
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.AppCompatTextView;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.widget.LinearLayout.HORIZONTAL;
import static android.widget.LinearLayout.VERTICAL;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DEFAULT_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_LABEL2_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_LABEL_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_ALIAS2_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_ALIAS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_VALUE_NAME_KEY;

public class SplitCombobox extends FrameLayout implements IFormControl
{
    protected String              mFieldName;
    protected boolean             mIsShowLast;
    protected Map<String, String> mAliasValueMap;
    protected Map<String, String> mAlias2ValueMap;
    protected AppCompatSpinner    mSpinner;
    protected AppCompatSpinner    mSpinner2;
    protected AppCompatTextView   mTitle;
    protected AppCompatTextView   mTitle2;
    protected LinearLayout        mTitles;
    protected LinearLayout        mSpinners;

    public SplitCombobox(Context context) {
        super(context);
    }

    public SplitCombobox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(JSONObject element,
                     List<Field> fields,
                     Bundle savedState,
                     Cursor featureCursor,
                     SharedPreferences preferences) throws JSONException{
        mTitles = new LinearLayout(getContext());
        mTitles.setOrientation(HORIZONTAL);
        mSpinners = new LinearLayout(getContext());
        mSpinners.setOrientation(HORIZONTAL);
        mTitle = new AppCompatTextView(getContext());
        mTitle2 = new AppCompatTextView(getContext());
        mSpinner = new AppCompatSpinner(getContext());
        mSpinner2 = new AppCompatSpinner(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
        mSpinner.setLayoutParams(lp);
        mSpinner2.setLayoutParams(lp);
        mTitle.setLayoutParams(lp);
        mTitle2.setLayoutParams(lp);
        mTitles.addView(mTitle);
        mTitles.addView(mTitle2);
        mSpinners.addView(mSpinner);
        mSpinners.addView(mSpinner2);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSpinner2.setSelection(i, true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        mSpinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSpinner.setSelection(i, true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = attributes.getString(JSON_FIELD_NAME_KEY);
        mIsShowLast = ControlHelper.isSaveLastValue(attributes);
        mSpinner.setEnabled(ControlHelper.isEnabled(fields, mFieldName));
        mSpinner2.setEnabled(ControlHelper.isEnabled(fields, mFieldName));

        mTitle.setText(attributes.getString(JSON_LABEL_KEY));
        mTitle2.setText(attributes.getString(JSON_LABEL2_KEY));

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
        mAlias2ValueMap = new HashMap<>();

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getContext(), R.layout.formtemplate_spinner);
        mSpinner.setAdapter(spinnerArrayAdapter);

        ArrayAdapter<String> spinnerArrayAdapter2 = new ArrayAdapter<>(getContext(), R.layout.formtemplate_spinner);
        mSpinner2.setAdapter(spinnerArrayAdapter2);

//        if (attributes.has(ConstantsUI.JSON_NGW_ID_KEY) && attributes.getLong(ConstantsUI.JSON_NGW_ID_KEY) != -1) {
//            MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
//            if (null == map)
//                throw new IllegalArgumentException("The map should extends MapContentProviderHelper or inherited");
//
//            String account = element.optString(SyncStateContract.Columns.ACCOUNT_NAME);
//            long id = attributes.getLong(JSON_NGW_ID_KEY);
//            for (int i = 0; i < map.getLayerCount(); i++) {
//                if (map.getLayer(i) instanceof NGWLookupTable) {
//                    NGWLookupTable table = (NGWLookupTable) map.getLayer(i);
//                    if (table.getRemoteId() != id || !table.getAccountName().equals(account))
//                        continue;
//
//                    int j = 0;
//                    for (Map.Entry<String, String> entry : table.getData().entrySet()) {
//                        mAliasValueMap.put(entry.getValue(), entry.getKey());
//
//                        if (null != lastValue && lastValue.equals(entry.getKey()))
//                            lastValuePosition = j;
//
//                        spinnerArrayAdapter.add(entry.getValue());
//                        j++;
//                    }
//
//                    break;
//                }
//            }
//        } else {
            JSONArray values = attributes.optJSONArray(JSON_VALUES_KEY);
            if (values != null) {
                for (int j = 0; j < values.length(); j++) {
                    JSONObject keyValue = values.getJSONObject(j);
                    String value = keyValue.getString(JSON_VALUE_NAME_KEY);
                    String value_alias = keyValue.getString(JSON_VALUE_ALIAS_KEY);
                    String value_alias2 = keyValue.getString(JSON_VALUE_ALIAS2_KEY);

                    if (keyValue.has(JSON_DEFAULT_KEY) && keyValue.getBoolean(JSON_DEFAULT_KEY))
                        defaultPosition = j;

                    if (null != lastValue && lastValue.equals(value))
                        lastValuePosition = j;

                    mAliasValueMap.put(value_alias, value);
                    mAlias2ValueMap.put(value_alias2, value);
                    spinnerArrayAdapter.add(value_alias);
                    spinnerArrayAdapter2.add(value_alias2);
                }
            }
//        }

        mSpinner.setSelection(lastValuePosition >= 0 ? lastValuePosition : defaultPosition);
        mSpinner2.setSelection(lastValuePosition >= 0 ? lastValuePosition : defaultPosition);

        // The drop down view
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerArrayAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        float minHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
        mSpinner.setPadding(0, (int) minHeight, 0, (int) minHeight);
        mSpinner2.setPadding(0, (int) minHeight, 0, (int) minHeight);
    }


    @Override
    public void saveLastValue(SharedPreferences preferences) {
        preferences.edit().putString(mFieldName, (String) getValue()).apply();
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
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(VERTICAL);
        container.addView(mTitles);
        container.addView(mSpinners);
        addView(container);

        if (!AccountUtil.isProUser(getContext())) {
            FrameLayout splash = new FrameLayout(getContext());
            splash.setClickable(true);
            splash.setBackgroundColor(Color.argb(128, 128, 128, 128));
            ImageView sign = new ImageView(getContext());
            sign.setImageResource(R.drawable.ic_action_warning_dark);
            sign.setScaleType(ImageView.ScaleType.FIT_CENTER);
            sign.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    ControlHelper.showProDialog(getContext());
                }
            });

            splash.addView(sign);
            addView(splash);
        }

        layout.addView(this);
    }


    @Override
    public Object getValue()
    {
        String valueAlias = (String) mSpinner.getSelectedItem();
        return mAliasValueMap.get(valueAlias);
    }


    @Override
    public void saveState(Bundle outState) {
        outState.putString(ControlHelper.getSavedStateKey(mFieldName), (String) getValue());
    }
}

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
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.activity;

import android.content.ContentValues;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplibui.util.ConstantsUI.*;


/**
 * Activity to add or modify vector layer attributes
 */
public class CustomModifyAttributesActivity
        extends ModifyAttributesActivity
{
    public static final String JSON_ATTRIBUTES_KEY        = "attributes";
    public static final String JSON_TYPE_KEY              = "type";
    public static final String JSON_TABS_KEY              = "tabs";
    public static final String JSON_ALBUM_ELEMENTS_KEY    = "album_elements";
    public static final String JSON_PORTRAIT_ELEMENTS_KEY = "portrait_elements";
    public static final String JSON_TEXT_KEY              = "text";
    public static final String JSON_FIELD_NAME_KEY        = "field";
    public static final String JSON_SHOW_LAST_KEY         = "last";
    public static final String JSON_MAX_STRING_COUNT_KEY  = "max_string_count";
    public static final String JSON_ONLY_FIGURES_KEY      = "only_figures";
    public static final String JSON_VALUES_KEY            = "values";
    public static final String JSON_VALUE_ALIAS_KEY       = "alias";
    public static final String JSON_DATE_TYPE_KEY         = "date_type";
    public static final String JSON_DEFAULT_KEY           = "default";
    public static final String JSON_FIELD_LEVEL1_KEY      = "field_level1";
    public static final String JSON_FIELD_LEVEL2_KEY      = "field_level2";

    public static final String JSON_TEXT_LABEL_VALUE      = "text_label";
    public static final String JSON_TEXT_EDIT_VALUE       = "text_edit";
    public static final String JSON_DATE_TIME_VALUE       = "date_time";
    public static final String JSON_RADIO_GROUP_VALUE     = "radio_group";
    public static final String JSON_COMBOBOX_VALUE        = "combobox";
    public static final String JSON_DOUBLE_COMBOBOX_VALUE = "double_combobox";

    protected Map<String, String>              mLastValues;
    protected Map<String, Map<String, String>> mKeyValuesForField;
    protected Map<View, String>                mDoubleComboFirstKeys;
    protected Map<View, Integer>               mDateTimePickerType;

    protected static final String FILE_LAST_VALUES = "last_values.json";


    protected void createAndFillControls(final IGISApplication app)
    {
        //TODO: add location control via fragment only defined by user space
        //create and fill controls
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            short layerId = extras.getShort(KEY_LAYER_ID);
            mFeatureId = extras.getLong(KEY_FEATURE_ID);
            mGeometry = (GeoGeometry) extras.getSerializable(KEY_GEOMETRY);
            File form = (File) extras.getSerializable(KEY_FORM_PATH);

            MapBase map = app.getMap();
            mLayer = (VectorLayer) map.getLayerById(layerId);
            if (null != mLayer) {
                readLastValues();
                int orientation = getResources().getConfiguration().orientation;
                boolean isLand = orientation == Configuration.ORIENTATION_LANDSCAPE;
                LinearLayout layout = (LinearLayout) findViewById(R.id.controls_list);
                createAndFillControls(layout, form, isLand);
            }
        }

        mKeyValuesForField = new HashMap<>();
        mDoubleComboFirstKeys = new HashMap<>();
        mDateTimePickerType = new HashMap<>();
    }


    protected void readLastValues()
    {
        mLastValues = new HashMap<>();

        try {
            JSONArray lastJsonArray = new JSONArray(
                    FileUtil.readFromFile(new File(mLayer.getPath(), FILE_LAST_VALUES)));

            for (int i = 0; i < lastJsonArray.length(); i++) {
                JSONObject lastValue = lastJsonArray.getJSONObject(i);
                String fieldName = lastValue.getString(JSON_NAME_KEY);
                String fieldValue = lastValue.getString(JSON_VALUE_KEY);
                mLastValues.put(fieldName, fieldValue);
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    protected void writeLastValues()
    {
        JSONArray lastJsonArray = new JSONArray();

        for (Map.Entry<String, String> entry : mLastValues.entrySet()) {
            String fieldName = entry.getKey();
            View fieldView = mFields.get(fieldName);
            String fieldValue;

            if (fieldView instanceof Spinner) {
                Spinner sp = (Spinner) fieldView;
                fieldValue = (String) sp.getSelectedItem();

            } else if (fieldView instanceof RadioGroup) {
                RadioGroup rg = (RadioGroup) fieldView;
                RadioButton checkedRB = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
                fieldValue = (String) checkedRB.getText();

            } else {
                TextView tv = (TextView) fieldView;

                //String value = entry.getValue();
                fieldValue = tv.getText().toString();
            }

            JSONObject lastValue = new JSONObject();

            try {
                lastValue.put(JSON_NAME_KEY, fieldName);
                lastValue.put(JSON_VALUE_KEY, fieldValue);
                lastJsonArray.put(lastValue);
            } catch (JSONException e) {
                //skip broken field/value
                Log.d(TAG, e.getLocalizedMessage());
            }
        }

        try {
            FileUtil.writeToFile(
                    new File(mLayer.getPath(), FILE_LAST_VALUES), lastJsonArray.toString());

        } catch (IOException e) {
            //skip save if something wrong
            e.printStackTrace();
        }
    }


    protected void createAndFillControls(
            LinearLayout layout,
            File form,
            boolean isLand)
    {
        try {
            JSONObject jsonFormContents = new JSONObject(FileUtil.readFromFile(form));
            JSONArray tabs = jsonFormContents.getJSONArray(JSON_TABS_KEY);
            //TODO: add support more than one tab
            JSONObject tab0 = tabs.getJSONObject(0);
            if (isLand && !tab0.isNull(JSON_ALBUM_ELEMENTS_KEY)) {
                JSONArray elements = tab0.getJSONArray(JSON_ALBUM_ELEMENTS_KEY);
                if (null != elements && elements.length() > 0) {
                    createAndFillControls(layout, elements);
                }
            } else {
                JSONArray elements = tab0.getJSONArray(JSON_PORTRAIT_ELEMENTS_KEY);
                createAndFillControls(layout, elements);
            }

        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_form_create), Toast.LENGTH_SHORT).show();
        }
    }


    protected void createAndFillControls(
            LinearLayout layout,
            JSONArray elements)
            throws JSONException
    {
        Cursor cursor = null;

        if (mFeatureId != NOT_FOUND) {
            cursor = mLayer.query(null, FIELD_ID + " = " + mFeatureId, null, null);
            if (!cursor.moveToFirst()) {
                cursor = null;
            }
        }

        List<Field> fields = mLayer.getFields();


        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            String type = element.getString(JSON_TYPE_KEY);
            switch (type) {
                case JSON_TEXT_LABEL_VALUE:
                    addTextLabel(layout, element);
                    break;
                case JSON_TEXT_EDIT_VALUE:
                    addTextEdit(layout, element, cursor);
                    break;
                case JSON_DATE_TIME_VALUE:
                    addDateTime(layout, element, cursor);
                    break;
                case JSON_RADIO_GROUP_VALUE:
                    addRadioGroup(layout, element, cursor);
                    break;
                case JSON_COMBOBOX_VALUE:
                    addCombobox(layout, element, cursor, fields);
                    break;
                case JSON_DOUBLE_COMBOBOX_VALUE:
                    addDoubleCombobox(layout, element, cursor);
                    break;
                //TODO: add controls
                //checkbox
                //button
                //group
                //space
                //orientation
                //tabs
                //compass
                default:
                    break;
            }
        }

        if (null != cursor) {
            cursor.close();
        }
    }


    protected void addTextLabel(
            LinearLayout layout,
            JSONObject element)
            throws JSONException
    {
        //create static text with alias
        TextView textLabel = new TextView(this);

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        textLabel.setText(attributes.getString(JSON_TEXT_KEY));

        textLabel.setEllipsize(TextUtils.TruncateAt.END);
        textLabel.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        textLabel.setTextColor(getResources().getColor(android.R.color.darker_gray));

        layout.addView(textLabel);
    }


    protected void addTextEdit(
            LinearLayout layout,
            JSONObject element,
            Cursor cursor)
            throws JSONException
    {
        EditText stringEdit = new EditText(this);

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

        String fieldName = attributes.getString(JSON_FIELD_NAME_KEY);

        boolean last = false;
        if (attributes.has(JSON_SHOW_LAST_KEY) && !attributes.isNull(JSON_SHOW_LAST_KEY)) {
            last = attributes.getBoolean(JSON_SHOW_LAST_KEY);
        }

        String lastVal = mLastValues.get(fieldName);
        if (TextUtils.isEmpty(lastVal) && attributes.has(JSON_TEXT_KEY)) {
            lastVal = attributes.getString(JSON_TEXT_KEY);
        }

        if (last) {
            mLastValues.put(fieldName, lastVal);
        }

        //let's create control
        int maxLines = attributes.getInt(JSON_MAX_STRING_COUNT_KEY);
        if (maxLines < 2) {
            stringEdit.setSingleLine(true);
        } else {
            stringEdit.setMaxLines(maxLines);
        }

        boolean onlyFigures = attributes.getBoolean(JSON_ONLY_FIGURES_KEY);
        if (onlyFigures) {
            List<Field> layerFields = mLayer.getFields();

            int fieldType = NOT_FOUND;
            for (Field layerField : layerFields) {
                if (layerField.getName().equals(fieldName)) {
                    fieldType = layerField.getType();
                    break;
                }
            }

            //check field type
            switch (fieldType) {

                case GeoConstants.FTInteger:
                    stringEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
                    break;

                case GeoConstants.FTReal:
                    stringEdit.setInputType(
                            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    break;
            }
        }

        if (null == cursor) {
            stringEdit.setText(lastVal);

        } else {
            int column = cursor.getColumnIndex(fieldName);

            if (column >= 0) {
                String stringVal = cursor.getString(column);
                stringEdit.setText(stringVal);
            }
        }

        layout.addView(stringEdit);
        mFields.put(fieldName, stringEdit);
    }


    protected void addDateTime(
            LinearLayout layout,
            JSONObject element,
            Cursor cursor)
            throws JSONException
    {
        TextView dateEdit = null;

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

        int picker_type = DATETIME;
        if (attributes.has("date_type")) {
            picker_type = attributes.getInt("date_type");
        }

        SimpleDateFormat sdf = null;

        switch (picker_type) {

            case DATE:
            case TIME:
            case DATETIME:
                sdf = (SimpleDateFormat) DateFormat.getDateInstance();
                String pattern = sdf.toLocalizedPattern();

                dateEdit =
                        (TextView) getLayoutInflater().inflate(R.layout.spinner_datepicker, null);
                dateEdit.setSingleLine(true);
                dateEdit.setFocusable(false);
                dateEdit.setOnClickListener(getDateUpdateWatcher(dateEdit, picker_type));
                dateEdit.setHint(pattern);
                break;

            default:
                return;
        }

        mDateTimePickerType.put(dateEdit, picker_type);


        String fieldName = attributes.getString(JSON_FIELD_NAME_KEY);

        boolean last = false;
        if (attributes.has(JSON_SHOW_LAST_KEY) && !attributes.isNull(JSON_SHOW_LAST_KEY)) {
            last = attributes.getBoolean(JSON_SHOW_LAST_KEY);
        }

        String lastVal = mLastValues.get(fieldName);
        if (TextUtils.isEmpty(lastVal) && attributes.has(JSON_TEXT_KEY)) {
            lastVal = attributes.getString(JSON_TEXT_KEY);
        }

        if (last) {
            mLastValues.put(fieldName, lastVal);
        }


        if (null == cursor) {
            dateEdit.setText(lastVal);

        } else {
            int column = cursor.getColumnIndex(fieldName);

            if (column >= 0) {
                long millis = cursor.getLong(column);

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(millis);
                dateEdit.setText(sdf.format(calendar.getTime()));
            }
        }

        layout.addView(dateEdit);
        mFields.put(fieldName, dateEdit);


        // add grey line view here
        float height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        View greyLine = new View(this);
        layout.addView(greyLine);
        ViewGroup.LayoutParams params = greyLine.getLayoutParams();
        params.height = (int) height;
        greyLine.setLayoutParams(params);
        greyLine.setBackgroundResource(android.R.color.darker_gray);
    }


    protected void addRadioGroup(
            LinearLayout layout,
            JSONObject element,
            Cursor cursor)
            throws JSONException
    {
        RadioGroup radioGroup = new RadioGroup(this);

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

        String fieldName = attributes.getString(JSON_FIELD_NAME_KEY);

        boolean last = false;
        if (attributes.has(JSON_SHOW_LAST_KEY) && !attributes.isNull(JSON_SHOW_LAST_KEY)) {
            last = attributes.getBoolean(JSON_SHOW_LAST_KEY);
        }

        String lastVal = mLastValues.get(fieldName);
        if (TextUtils.isEmpty(lastVal)) {
            last = false;
        }

        String cursorVal = null;
        if (null != cursor) {
            int column = cursor.getColumnIndex(fieldName);
            if (column >= 0) {
                cursorVal = cursor.getString(column);
            }
        }

        JSONArray values = attributes.getJSONArray(JSON_VALUES_KEY);
        Map<String, String> keyValueMap = new HashMap<>();
        int index = 0;

        for (int j = 0; j < values.length(); j++) {
            JSONObject keyValue = values.getJSONObject(j);
            String key = keyValue.getString(JSON_VALUE_ALIAS_KEY);
            String value = keyValue.getString(JSON_NAME_KEY);

            if (j == 0 || null != cursorVal && cursorVal.equals(value) /*if modify data*/ ||
                !last && keyValue.has(JSON_DEFAULT_KEY) && keyValue.getBoolean(JSON_DEFAULT_KEY)) {

                lastVal = key;
                index = j;
            }

            keyValueMap.put(key, value);

            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(key);
            radioGroup.addView(radioButton);
        }

        mKeyValuesForField.put(fieldName, keyValueMap);

        if (last) {
            mLastValues.put(fieldName, lastVal);
        }

        radioGroup.check(radioGroup.getChildAt(index).getId());
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        layout.addView(radioGroup);
        mFields.put(fieldName, radioGroup);


        // add grey line view here
        float height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        View greyLine = new View(this);
        layout.addView(greyLine);
        ViewGroup.LayoutParams params = greyLine.getLayoutParams();
        params.height = (int) height;
        greyLine.setLayoutParams(params);
        greyLine.setBackgroundResource(android.R.color.darker_gray);
    }


    protected void addCombobox(
            LinearLayout layout,
            JSONObject element,
            Cursor cursor,
            List<Field> fields)
            throws JSONException
    {
        //TODO: add mode_dialog if attribute asDialog == true, Spinner.MODE_DIALOG API Level 11+
        Spinner spinner = new Spinner(this);

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

        String jsonFieldName = attributes.getString(JSON_FIELD_NAME_KEY);

        boolean isEnabled = false;

        for (Field field : fields) {
            String fieldName = field.getName();
            if (fieldName.equals(jsonFieldName)) {
                isEnabled = true;
                break;
            }
        }

        spinner.setEnabled(isEnabled);

        boolean last = false;
        if (attributes.has(JSON_SHOW_LAST_KEY) && !attributes.isNull(JSON_SHOW_LAST_KEY)) {
            last = attributes.getBoolean(JSON_SHOW_LAST_KEY);
        }

        String lastVal = mLastValues.get(jsonFieldName);
        if (TextUtils.isEmpty(lastVal)) {
            last = false;
        }

        String cursorVal = null;
        if (null != cursor) {
            int column = cursor.getColumnIndex(jsonFieldName);
            if (column >= 0) {
                cursorVal = cursor.getString(column);
            }
        }

        JSONArray values = attributes.getJSONArray(JSON_VALUES_KEY);

        Map<String, String> keyValueMap = new HashMap<>();

        ArrayAdapter<String> spinnerArrayAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);

        for (int j = 0; j < values.length(); j++) {
            JSONObject keyValue = values.getJSONObject(j);
            String key = keyValue.getString(JSON_VALUE_ALIAS_KEY);
            String value = keyValue.getString(JSON_NAME_KEY);

            if (j == 0 || null != cursorVal && cursorVal.equals(value) /*if modify data*/ ||
                !last && keyValue.has(JSON_DEFAULT_KEY) && keyValue.getBoolean(JSON_DEFAULT_KEY)) {

                lastVal = key;
            }

            keyValueMap.put(key, value);
            spinnerArrayAdapter.add(key);
        }

        mKeyValuesForField.put(jsonFieldName, keyValueMap);

        if (last) {
            mLastValues.put(jsonFieldName, lastVal);
        }

        // The drop down view
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        int spinnerPosition = spinnerArrayAdapter.getPosition(lastVal);
        float minHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());

        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setSelection(spinnerPosition);
        spinner.setPadding(0, (int) minHeight, 0, (int) minHeight);

        layout.addView(spinner);
        mFields.put(jsonFieldName, spinner);


        // add grey line view here
        float height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        View greyLine = new View(this);
        layout.addView(greyLine);
        ViewGroup.LayoutParams params = greyLine.getLayoutParams();
        params.height = (int) height;
        greyLine.setLayoutParams(params);
        greyLine.setBackgroundResource(R.color.hint_foreground_material_light);
    }


    protected void addDoubleCombobox(
            LinearLayout layout,
            JSONObject element,
            Cursor cursor)
            throws JSONException
    {
        //TODO: add mode_dialog if attribute asDialog == true, Spinner.MODE_DIALOG API Level 11+
        final Spinner spinner = new Spinner(this);
        final Spinner subSpinner = new Spinner(this);

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

        String fieldName = attributes.getString(JSON_FIELD_LEVEL1_KEY);
        String subFieldName = attributes.getString(JSON_FIELD_LEVEL2_KEY);

        boolean last = false;
        if (attributes.has(JSON_SHOW_LAST_KEY) && !attributes.isNull(JSON_SHOW_LAST_KEY)) {
            last = attributes.getBoolean(JSON_SHOW_LAST_KEY);
        }

        String lastVal = mLastValues.get(fieldName);
        String lastSubVal = mLastValues.get(subFieldName);
        if (TextUtils.isEmpty(lastVal) || TextUtils.isEmpty(lastSubVal)) {
            last = false;
        }

        String cursorVal = null;
        String cursorSubVal = null;
        if (null != cursor) {
            int column = cursor.getColumnIndex(fieldName);
            int subColumn = cursor.getColumnIndex(subFieldName);
            if (column >= 0 && subColumn >= 0) {
                cursorVal = cursor.getString(column);
                cursorSubVal = cursor.getString(subColumn);
            }
        }


        JSONArray values = attributes.getJSONArray(JSON_VALUES_KEY);

        Map<String, String> keyValueMap = new HashMap<>();
        Map<String, String> subKeyValueMap = new HashMap<>();
        final Map<String, ArrayList<String>> categoriesMap = new HashMap<>();

        final ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);

        for (int j = 0; j < values.length(); j++) {
            JSONObject keyValue = values.getJSONObject(j);
            String valueAlias = keyValue.getString(JSON_VALUE_ALIAS_KEY);
            String value = keyValue.getString(JSON_NAME_KEY);

            boolean isDefault = false;

            if (j == 0 || null != cursorVal && cursorVal.equals(value) /*if modify data*/ ||
                !last && keyValue.has(JSON_DEFAULT_KEY) && keyValue.getBoolean(JSON_DEFAULT_KEY)) {

                lastVal = valueAlias;
                isDefault = true;
            }

            adapter.add(valueAlias);
            keyValueMap.put(valueAlias, value);

            categoriesMap.put(valueAlias, new ArrayList<String>());
            JSONArray subValues = keyValue.getJSONArray(JSON_VALUES_KEY);

            for (int k = 0; k < subValues.length(); k++) {
                JSONObject subKeyValue = subValues.getJSONObject(k);
                String subValueAlias = subKeyValue.getString(JSON_VALUE_ALIAS_KEY);
                String subValue = subKeyValue.getString(JSON_NAME_KEY);

                if (isDefault) {
                    if (k == 0 || null != cursorSubVal && cursorSubVal.equals(subValue) /*if modify data*/ ||
                        !last && subKeyValue.has(JSON_DEFAULT_KEY) && subKeyValue.getBoolean(JSON_DEFAULT_KEY)) {

                        lastSubVal = subValueAlias;
                    }
                }

                subKeyValueMap.put(valueAlias + "->" + subValueAlias, subValue);
                categoriesMap.get(valueAlias).add(subValueAlias);
            }
        }

        mKeyValuesForField.put(fieldName, keyValueMap);
        mKeyValuesForField.put(subFieldName, subKeyValueMap);

        if (last) {
            mLastValues.put(fieldName, lastVal);
            mLastValues.put(subFieldName, lastSubVal);
        }


        // The drop down view
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        int spinnerPosition = adapter.getPosition(lastVal);
        float minHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());

        spinner.setAdapter(adapter);
        spinner.setSelection(spinnerPosition);
        spinner.setPadding(0, (int) minHeight, 0, (int) minHeight);

        final String lastSubValFinal = lastSubVal;

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener()
                {
                    public void onItemSelected(
                            AdapterView<?> arg0,
                            View arg1,
                            int arg2,
                            long arg3)
                    {
                        String sCat = adapter.getItem(arg2).toString();
                        List<String> subCatList = categoriesMap.get(sCat);

                        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(
                                CustomModifyAttributesActivity.this,
                                android.R.layout.simple_spinner_item, subCatList);

                        subAdapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);

                        subSpinner.setAdapter(subAdapter);
                        int subSpinnerPosition = subAdapter.getPosition(lastSubValFinal);

                        if (subSpinnerPosition < 0) {
                            subSpinner.setSelection(0);
                        } else {
                            subSpinner.setSelection(subSpinnerPosition);
                        }

                        String firstKey = (String) spinner.getSelectedItem();
                        mDoubleComboFirstKeys.put(subSpinner, firstKey);
                    }


                    public void onNothingSelected(AdapterView<?> arg0)
                    {
                    }
                });

        subSpinner.setPadding(0, (int) minHeight, 0, (int) minHeight);


        layout.addView(spinner);
        mFields.put(fieldName, spinner);
        layout.addView(subSpinner);
        mFields.put(subFieldName, subSpinner);


        // add grey line view here
        float height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        View greyLine = new View(this);
        layout.addView(greyLine);
        ViewGroup.LayoutParams params = greyLine.getLayoutParams();
        params.height = (int) height;
        greyLine.setLayoutParams(params);
        greyLine.setBackgroundResource(android.R.color.darker_gray);
    }


    protected void onMenuApplyClicked()
    {
        writeLastValues();
        super.onMenuApplyClicked();
    }


    protected void putFieldValue(
            ContentValues values,
            Field field)
    {
        View fieldView = mFields.get(field.getName());

        if (null == fieldView) {
            return;
        }

        String stringVal = null;

        if (fieldView instanceof Spinner) {
            Spinner sp = (Spinner) fieldView;
            String key = (String) sp.getSelectedItem();
            String firstKey = mDoubleComboFirstKeys.get(sp);

            if (!TextUtils.isEmpty(firstKey)) {
                key = firstKey + "->" + key;
            }

            Map<String, String> keyValue = mKeyValuesForField.get(field.getName());

            if (null != keyValue) {
                stringVal = keyValue.get(key);
            }

        } else if (fieldView instanceof RadioGroup) {
            RadioGroup rg = (RadioGroup) fieldView;
            RadioButton radioButton = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
            String key = (String) radioButton.getText();
            Map<String, String> keyValue = mKeyValuesForField.get(field.getName());

            if (null != keyValue) {
                stringVal = keyValue.get(key);
            }

        } else {
            TextView textView = (TextView) fieldView;
            stringVal = textView.getText().toString();
        }

        Log.d(TAG, "field: " + field.getName() + " value: " + stringVal);

        if (!TextUtils.isEmpty(stringVal)) {

            if (mDateTimePickerType.containsKey(fieldView)) {
                //if (field.getType() == GeoConstants.FTDateTime) {
                SimpleDateFormat sdf;
                int pickerType = mDateTimePickerType.get(fieldView);

                switch (pickerType) {
                    case DATE:
                        sdf = (SimpleDateFormat) DateFormat.getDateInstance();
                        break;
                    case TIME:
                        sdf = (SimpleDateFormat) DateFormat.getTimeInstance();
                        break;
                    case DATETIME:
                    default:
                        sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
                        break;
                }

                try {
                    Date date = sdf.parse(stringVal);
                    values.put(field.getName(), date.getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            } else {
                values.put(field.getName(), stringVal);
            }
        }
    }
}

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

package com.nextgis.maplibui;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplibui.util.Constants.*;
import static com.nextgis.maplib.util.Constants.*;

/**
 * Activity to add or modify vector layer attributes
 */
public class CustomModifyAttributesActivity
        extends ModifyAttributesActivity
{

    protected static final String JSON_ATTRIBUTES_KEY = "attributes";
    protected static final String JSON_TYPE_KEY = "type";
    protected static final String JSON_TABS_KEY = "tabs";
    protected static final String JSON_ALBUM_KEY = "album_elements";
    protected static final String JSON_PORTRAIT_KEY = "portrait_elements";
    protected static final String JSON_TEXT_KEY = "text";
    protected static final String JSON_FIELD_KEY = "field";
    protected static final String JSON_FILLLAST_KEY = "last";
    protected static final String JSON_MAXSTRINGCOUNT_KEY = "max_string_count";
    protected static final String JSON_ONLYFIGURES_KEY = "only_figures";
    protected static final String JSON_VALUES_KEY = "values";
    protected static final String JSON_ALIAS_KEY = "alias";

    protected Map<String, String> mLastValues;
    protected Map<String, Map<String, String>> mKeyValuesForField;

    protected static final String FILE_LAST = "last.json";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mKeyValuesForField = new HashMap<>();

        //TODO: add location control via fragment only defined by user space
        setContentView(R.layout.activity_standard_attributes);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.getBackground().setAlpha(255);
        setSupportActionBar(toolbar);


        LinearLayout layout = (LinearLayout)findViewById(R.id.controls_list);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final IGISApplication app = (IGISApplication) getApplication();
        //create and fill controls
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            short layerId = extras.getShort(KEY_LAYER_ID);
            mFeatureId = extras.getLong(KEY_FEATURE_ID);
            mGeometry = (GeoGeometry) extras.getSerializable(KEY_GEOMETRY);
            File form = (File) extras.getSerializable(KEY_FORM_PATH);

            MapBase map = app.getMap();
            mLayer = (VectorLayer) map.getLayerById(layerId);
            if(null != mLayer){
                serializeLast(false);
                int orientation = getResources().getConfiguration().orientation;
                boolean isLand = orientation == Configuration.ORIENTATION_LANDSCAPE;
                createAndFillControls(layout, form, isLand);
            }
        }

        if(null == mGeometry && mFeatureId == NOT_FOUND) {
            mLatView = (TextView) findViewById(R.id.latitude_view);
            mLongView = (TextView) findViewById(R.id.longitude_view);
            mAltView = (TextView) findViewById(R.id.altitude_view);
            mAccView = (TextView) findViewById(R.id.accuracy_view);

            ImageButton refreshLocation = (ImageButton) findViewById(R.id.refresh);
            refreshLocation.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (null != app) {
                        GpsEventSource gpsEventSource = app.getGpsEventSource();
                        setLocationText(gpsEventSource.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                    }
                }
            });
        }
        else{
            //hide location panel
            ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
            rootView.removeView(findViewById(R.id.location_panel));
        }
    }


    protected void serializeLast(boolean store)
    {
        if(store){
            JSONArray lastJsonArray = new JSONArray();
            for (Map.Entry<String, String> entry : mLastValues.entrySet()) {
                String field = entry.getKey();

                View v = mFields.get(field);
                String key;
                if(v instanceof Spinner){
                    Spinner sp = (Spinner)v;
                    key = (String) sp.getSelectedItem();
                } else if(v instanceof RadioGroup) {
                    RadioGroup rg = (RadioGroup)v;
                    RadioButton radioButton =
                            (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
                    key = (String) radioButton.getText();
                }
                else {
                    TextView tv = (TextView) v;
                    Editable editText = tv.getEditableText();
                    if (null == editText)
                        continue;

                    //String value = entry.getValue();
                    key = editText.toString();
                }

                JSONObject lastValue = new JSONObject();
                try {
                    lastValue.put(JSON_NAME_KEY, field);
                    lastValue.put(JSON_VALUE_KEY, key);
                    lastJsonArray.put(lastValue);
                } catch (JSONException e) {
                    //skip broken field/value
                    e.printStackTrace();
                }
            }
            try {
                FileUtil.writeToFile(new File(mLayer.getPath(), FILE_LAST), lastJsonArray.toString());
            } catch (IOException e) {
                //skip save if something wrong
                e.printStackTrace();
            }
        }
        else{
            mLastValues = new HashMap<>();
            try {
                JSONArray lastJsonArray = new JSONArray(FileUtil.readFromFile(new File(mLayer.getPath(), FILE_LAST)));
                for(int i = 0; i < lastJsonArray.length(); i++){
                    JSONObject lastValue = lastJsonArray.getJSONObject(i);
                    String fieldName = lastValue.getString(JSON_NAME_KEY);
                    String fieldValue = lastValue.getString(JSON_VALUE_KEY);
                    mLastValues.put(fieldName, fieldValue);
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }


    protected void createAndFillControls(
            LinearLayout layout,
            JSONArray elements)
            throws JSONException
    {
        Cursor cursor = null;
        if(mFeatureId != NOT_FOUND)
            cursor = mLayer.query(null, VectorLayer.FIELD_ID + " = " + mFeatureId, null, null);

        float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                                                 getResources().getDisplayMetrics());

        for(int i = 0; i < elements.length(); i++){
            JSONObject element = elements.getJSONObject(i);
            String type = element.getString(JSON_TYPE_KEY);
            Log.d(TAG, "form type: " + type);
            JSONObject attributes;
            JSONArray values;
            String field;
            boolean last;
            String lastVal;
            Map<String, String> keyValueMap;
            String cursorVal = null;
            switch(type){
                case "text_edit":
                    attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    field = attributes.getString(JSON_FIELD_KEY);
                    last = false;
                    if(attributes.has(JSON_FILLLAST_KEY))
                        last = attributes.getBoolean(JSON_FILLLAST_KEY);

                    boolean onlyFigures = attributes.getBoolean(JSON_ONLYFIGURES_KEY);
                    int maxLines = attributes.getInt(JSON_MAXSTRINGCOUNT_KEY);
                    lastVal = mLastValues.get(field);
                    if(TextUtils.isEmpty(lastVal))
                        lastVal = attributes.getString(JSON_TEXT_KEY);

                    if(last){
                        mLastValues.put(field, lastVal);
                    }

                    //let's create control
                    EditText stringEdit = new EditText(this);
                    if(maxLines < 2)
                        stringEdit.setSingleLine(true);
                    else
                        stringEdit.setMaxLines(maxLines);
                    if(onlyFigures)
                        stringEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
                    layout.addView(stringEdit);
                    if(null == cursor) {
                        stringEdit.setText(lastVal);
                    }
                    else{
                        String stringVal = cursor.getString(cursor.getColumnIndex(field));
                        stringEdit.setText(stringVal);
                    }
                    mFields.put(field, stringEdit);
                    break;
                case "text_label":
                    attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    //create static text with alias
                    TextView aliasText = new TextView(this);
                    aliasText.setText(attributes.getString(JSON_TEXT_KEY));
                    aliasText.setEllipsize(TextUtils.TruncateAt.END);
                    aliasText.setTextAppearance(this, android.R.style.TextAppearance_Medium);
                    aliasText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    layout.addView(aliasText);
                    break;
                case "date_time":
                    attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    field = attributes.getString(JSON_FIELD_KEY);
                    last = false;
                    if(attributes.has(JSON_FILLLAST_KEY))
                        last = attributes.getBoolean(JSON_FILLLAST_KEY);

                    TextView dateEdit = (TextView)getLayoutInflater().inflate(R.layout.spinner_datepicker, null);
                    //TextView dateEdit = new TextView(this);
                    dateEdit.setSingleLine(true);
                    dateEdit.setOnClickListener(getDateUpdateWatcher(dateEdit));
                    dateEdit.setFocusable(false);
                    SimpleDateFormat dateText = new SimpleDateFormat();
                    String pattern = dateText.toLocalizedPattern();
                    dateEdit.setHint(pattern);

                    lastVal = mLastValues.get(field);
                    if(TextUtils.isEmpty(lastVal) && attributes.has(JSON_TEXT_KEY))
                        lastVal = attributes.getString(JSON_TEXT_KEY);

                    if(last){
                        mLastValues.put(field, lastVal);
                    }

                    layout.addView(dateEdit);
                    if(null == cursor) {
                        dateEdit.setText(lastVal);
                    }
                    else{
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(field)));
                        dateEdit.setText(dateText.format(calendar.getTime()));
                    }
                    mFields.put(field, dateEdit);

                    //add grey view here
                    View greyLine = new View(this);
                    greyLine.setBackgroundResource(android.R.color.darker_gray);
                    layout.addView(greyLine);
                    ViewGroup.LayoutParams params = greyLine.getLayoutParams();
                    params.height = (int) height;
                    greyLine.setLayoutParams(params);
                    break;
                case "radio_group":
                    attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    field = attributes.getString(JSON_FIELD_KEY);
                    last = false;
                    if(attributes.has(JSON_FILLLAST_KEY))
                        last = attributes.getBoolean(JSON_FILLLAST_KEY);
                    values = attributes.getJSONArray(JSON_VALUES_KEY);

                    lastVal = mLastValues.get(field);

                    RadioGroup rg = new RadioGroup(this);
                    rg.setOrientation(RadioGroup.VERTICAL);

                    keyValueMap = new HashMap<>();
                    if(null != cursor) {
                        cursorVal = cursor.getString(cursor.getColumnIndex(field));
                    }

                    int index = 0;
                    for(int j = 0; j < values.length(); j++){
                        JSONObject keyValue = values.getJSONObject(j);
                        String key = keyValue.getString(JSON_NAME_KEY);
                        String value = keyValue.getString(JSON_ALIAS_KEY);

                        RadioButton rb = new RadioButton(this);
                        rb.setText(value);

                        if(j == 0) {
                            lastVal = key;
                            index = j;
                        }

                        if(null != cursorVal && cursorVal.equals(value)){ //if modify data
                            lastVal = key;
                            index = j;
                        }
                        else if(!last && keyValue.has("default") && keyValue.getBoolean("default")){
                            lastVal = key;
                            index = j;
                        }

                        rg.addView(rb);
                        keyValueMap.put(value, key);
                    }

                    rg.check(rg.getChildAt(index).getId());

                    if(last){
                        mLastValues.put(field, lastVal);
                    }

                    layout.addView(rg);

                    mFields.put(field, rg);

                    break;
                case "double_combobox":
                    break;
                case "combobox":
                    attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    field = attributes.getString(JSON_FIELD_KEY);
                    last = false;
                    if(attributes.has(JSON_FILLLAST_KEY))
                        last = attributes.getBoolean(JSON_FILLLAST_KEY);
                    values = attributes.getJSONArray(JSON_VALUES_KEY);

                    keyValueMap = new HashMap<>();
                    List<String> valuesArray = new ArrayList<>();

                    lastVal = mLastValues.get(field);

                    if(null != cursor) {
                        cursorVal = cursor.getString(cursor.getColumnIndex(field));
                    }

                    for(int j = 0; j < values.length(); j++){
                        JSONObject keyValue = values.getJSONObject(j);
                        String key = keyValue.getString(JSON_NAME_KEY);
                        String value = keyValue.getString(JSON_ALIAS_KEY);

                        if(j == 0)
                            lastVal = key;

                        if(null != cursorVal && cursorVal.equals(value)){ //if modify data
                            lastVal = key;
                        }
                        else if(!last && keyValue.has("default") && keyValue.getBoolean("default")){
                            lastVal = key;
                        }

                        valuesArray.add(value);
                        keyValueMap.put(value, key);
                    }

                    if(last){
                        mLastValues.put(field, lastVal);
                    }

                    mKeyValuesForField.put(field, keyValueMap);
                    Spinner spinner = new Spinner(this); //TODO: add mode_dialog if attribute asDialog == true, Spinner.MODE_DIALOG API Level 11+
                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item, valuesArray);
                    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
                    spinner.setAdapter(spinnerArrayAdapter);

                    int spinnerPostion = spinnerArrayAdapter.getPosition(lastVal);
                    spinner.setSelection(spinnerPostion);

                    layout.addView(spinner);

                    mFields.put(field, spinner);

                    break;
                default:
                    break;
            }
        }
    }

    protected void createAndFillControls(LinearLayout layout, File form, boolean isLand){
        try {
            JSONObject jsonFormContents = new JSONObject(FileUtil.readFromFile(form));
            JSONArray tabs = jsonFormContents.getJSONArray(JSON_TABS_KEY);
            //TODO: add support more than one tab
            JSONObject tab0 = tabs.getJSONObject(0);
            if(isLand && !tab0.isNull(JSON_ALBUM_KEY)) {
                JSONArray elements = tab0.getJSONArray(JSON_ALBUM_KEY);
                if (null != elements && elements.length() > 0) {
                    createAndFillControls(layout, elements);
                }
            }
            else{
                JSONArray elements = tab0.getJSONArray(JSON_PORTRAIT_KEY);
                createAndFillControls(layout, elements);
            }

        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_form_create), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_cancel || id == android.R.id.home) {
            finish();
            return true;
        }
        else if(id == R.id.menu_settings) {
            final IGISApplication app = (IGISApplication) getApplication();
            app.showSettings();
            return true;
        }
        else if(id == R.id.menu_apply) {
            serializeLast(true);

            //create new row or modify existing
            List<Field> fields = mLayer.getFields();
            ContentValues values = new ContentValues();
            for(Field field : fields){
                View v = mFields.get(field.getName());
                String stringVal = null;
                if(v instanceof Spinner){
                    Spinner sp = (Spinner)v;
                    String key = (String) sp.getSelectedItem();
                    Map<String, String> keyValue = mKeyValuesForField.get(field.getName());
                    if(null != keyValue){
                        stringVal = keyValue.get(key);
                    }
                }
                else if(v instanceof RadioGroup) {
                    RadioGroup rg = (RadioGroup)v;
                    RadioButton radioButton =
                            (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
                    String key = (String) radioButton.getText();
                    Map<String, String> keyValue = mKeyValuesForField.get(field.getName());
                    if(null != keyValue){
                        stringVal = keyValue.get(key);
                    }
                }
                else {
                    TextView textView = (TextView) v;
                    Editable editText = textView.getEditableText();
                    if (null == editText)
                        continue;
                    stringVal = editText.toString();
                }

                if(!TextUtils.isEmpty(stringVal)) {
                    if (field.getType() == GeoConstants.FTDateTime) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat();
                        try {
                            Date date = dateFormat.parse(stringVal);
                            values.put(field.getName(), date.getTime());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        values.put(field.getName(), stringVal);
                    }
                }
            }

            if(mFeatureId == Constants.NOT_FOUND){
                if(mGeometry == null) {
                    //create point geometry
                    GeoGeometry geometry = null;
                    GeoPoint pt;
                    switch (mLayer.getGeometryType()) {
                        case GeoConstants.GTPoint:
                            pt = new GeoPoint(mLocation.getLongitude(), mLocation.getLatitude());
                            pt.setCRS(GeoConstants.CRS_WGS84);
                            pt.project(GeoConstants.CRS_WEB_MERCATOR);
                            geometry = pt;
                            break;
                        case GeoConstants.GTMultiPoint:
                            GeoMultiPoint mpt = new GeoMultiPoint();
                            pt = new GeoPoint(mLocation.getLongitude(), mLocation.getLatitude());
                            pt.setCRS(GeoConstants.CRS_WGS84);
                            pt.project(GeoConstants.CRS_WEB_MERCATOR);
                            mpt.add(pt);
                            geometry = mpt;
                            break;
                    }
                    if(null != geometry) {
                        try {
                            values.put(VectorLayer.FIELD_GEOM, geometry.toBlob());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else{
                    try {
                        values.put(VectorLayer.FIELD_GEOM, mGeometry.toBlob());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                IGISApplication app = (IGISApplication) getApplication();
                if(null == app) {
                    throw new IllegalArgumentException("Not a IGISApplication");
                }
                Uri uri = Uri.parse("content://" + app.getAuthority() + "/" + mLayer.getPath().getName());

                if(getContentResolver().insert(uri, values) == null){
                    Toast.makeText(this, getText(R.string.error_db_insert), Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            else{
                IGISApplication app = (IGISApplication) getApplication();
                if(null == app) {
                    throw new IllegalArgumentException("Not a IGISApplication");
                }
                Uri uri = Uri.parse("content://" + app.getAuthority() + "/" + mLayer.getPath().getName());
                Uri updateUri = ContentUris.withAppendedId(uri, mFeatureId);

                if(getContentResolver().update(updateUri, values, null, null) == 0){
                    Toast.makeText(this, getText(R.string.error_db_update), Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

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

import android.app.DatePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.api.GpsEventListener;
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
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.util.SettingsConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

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
                createAndFillControls(layout, form);
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

    protected void createAndFillControls(JSONArray elements)
            throws JSONException
    {
        for(int i = 0; i < elements.length(); i++){
            JSONObject element = elements.getJSONObject(i);
            String type = element.getString("type");
            switch(type){
                case "text_edit":
                    break;
                case "text_label":
                    break;
                case "date_time":
                    break;
                case "radio_group":
                    break;
                case "combobox":
                    break;
                default:
                    break;
            }
        }
    }

    protected void createAndFillControls(LinearLayout layout, File form){

        Cursor cursor = null;
        if(mFeatureId != NOT_FOUND)
            cursor = mLayer.query(null, VectorLayer.FIELD_ID + " = " + mFeatureId, null, null);

        float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                                  getResources().getDisplayMetrics());

        try {
            JSONObject jsonFormContents = new JSONObject(FileUtil.readFromFile(form));
            JSONArray tabs = jsonFormContents.getJSONArray("tabs");
            //TODO: add support more than one tab
            JSONObject tab0 = tabs.getJSONObject(0);
            //get layout - portrait/landscape
            if(landscape && tab0.has("album_elements") && tab0.getJSONArray("album_elements")){
                createAndFillControls(tab0.getJSONArray("album_elements"));
            }
            else{
                createAndFillControls(tab0.getJSONArray("portrait_elements"));
            }

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
//
        /*
        mFields = new HashMap<>();
        List<Field> fields = mLayer.getFields();
        for(Field field : fields){
            //create static text with alias
            TextView aliasText = new TextView(this);
            aliasText.setText(field.getAlias());
            aliasText.setEllipsize(TextUtils.TruncateAt.END);
            aliasText.setTextAppearance(this, android.R.style.TextAppearance_Medium);
            aliasText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            layout.addView(aliasText);

            //create control
            switch (field.getType()){
                case GeoConstants.FTString:
                    EditText stringEdit = new EditText(this);
                    stringEdit.setSingleLine(true);
                    layout.addView(stringEdit);
                    if(null != cursor){
                        String stringVal = cursor.getString(cursor.getColumnIndex(field.getName()));
                        stringEdit.setText(stringVal);
                    }
                    mFields.put(field.getName(), stringEdit);
                    break;
                case GeoConstants.FTInteger:
                    EditText intEdit = new EditText(this);
                    intEdit.setSingleLine(true);
                    intEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
                    layout.addView(intEdit);
                    if(null != cursor){
                        int intVal = cursor.getInt(cursor.getColumnIndex(field.getName()));
                        intEdit.setText(""+intVal);
                    }
                    mFields.put(field.getName(), intEdit);
                    break;
                case GeoConstants.FTReal:
                    EditText realEdit = new EditText(this);
                    realEdit.setSingleLine(true);
                    realEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    layout.addView(realEdit);
                    if(null != cursor){
                        float realVal = cursor.getInt(cursor.getColumnIndex(field.getName()));
                        realEdit.setText(""+realVal);
                    }
                    mFields.put(field.getName(), realEdit);
                    break;
                case GeoConstants.FTDateTime:
                    TextView dateEdit = (TextView)getLayoutInflater().inflate(R.layout.spinner_datepicker, null);
                    //TextView dateEdit = new TextView(this);
                    dateEdit.setSingleLine(true);
                    dateEdit.setOnClickListener(getDateUpdateWatcher(dateEdit));
                    dateEdit.setFocusable(false);
                    SimpleDateFormat dateText = new SimpleDateFormat();
                    String pattern = dateText.toLocalizedPattern();
                    dateEdit.setHint(pattern);
                    layout.addView(dateEdit);
                    if(null != cursor){
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(field.getName())));
                        dateEdit.setText(dateText.format(calendar.getTime()));
                    }
                    mFields.put(field.getName(), dateEdit);

                    //add grey view here
                    View greyLine = new View(this);
                    greyLine.setBackgroundResource(android.R.color.darker_gray);
                    layout.addView(greyLine);
                    ViewGroup.LayoutParams params = greyLine.getLayoutParams();
                    params.height = (int) height;
                    greyLine.setLayoutParams(params);
                    break;
                case GeoConstants.FTIntegerList:
                case GeoConstants.FTBinary:
                case GeoConstants.FTStringList:
                case GeoConstants.FTRealList:
                default:
                    //TODO:
                    break;
            }
        }
        */
    }
}

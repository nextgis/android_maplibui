/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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

package com.nextgis.maplibui;

import android.app.DatePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
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
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplibui.util.ConstantsUI.*;
import static com.nextgis.maplib.util.Constants.*;

/**
 * Activity to add or modify vector layer attributes
 */
public class ModifyAttributesActivity
        extends ActionBarActivity implements GpsEventListener
{
    protected TextView mLatView;
    protected TextView mLongView;
    protected TextView mAltView;
    protected TextView mAccView;
    protected Location mLocation;
    protected VectorLayer mLayer;
    protected long mFeatureId;
    protected Map<String, View> mFields;
    protected GeoGeometry mGeometry;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

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

            MapBase map = app.getMap();
            mLayer = (VectorLayer) map.getLayerById(layerId);
            if(null != mLayer){
                createAndFillControls(layout);
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
                        Location location = gpsEventSource.getLastKnownLocation();
                        setLocationText(location);
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

    protected void createAndFillControls(LinearLayout layout){

        Cursor cursor = null;
        if(mFeatureId != Constants.NOT_FOUND) {
            cursor = mLayer.query(null, VectorLayer.FIELD_ID + " = " + mFeatureId, null, null);
            cursor.moveToFirst();
        }
        float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                                  getResources().getDisplayMetrics());


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
                    //stringEdit.setSingleLine(true);
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
    }


    @Override
    protected void onPause()
    {
        if(null != findViewById(R.id.location_panel)) {
            IGISApplication app = (IGISApplication) getApplication();
            if (null != app) {
                GpsEventSource gpsEventSource = app.getGpsEventSource();
                gpsEventSource.removeListener(this);
            }
        }
        super.onPause();
    }


    @Override
    protected void onResume()
    {
        if(null != findViewById(R.id.location_panel)) {
            IGISApplication app = (IGISApplication) getApplication();
            if (null != app) {
                GpsEventSource gpsEventSource = app.getGpsEventSource();
                gpsEventSource.addListener(this);
                setLocationText(gpsEventSource.getLastKnownLocation());
            }
        }
        super.onResume();
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
            //create new row or modify existing
            List<Field> fields = mLayer.getFields();
            ContentValues values = new ContentValues();
            for(Field field : fields){
                TextView textView = (TextView) mFields.get(field.getName());
                Editable editText = textView.getEditableText();
                if(null == editText)
                    continue;
                String stringVal = editText.toString();
                if(field.getType() == GeoConstants.FTDateTime){
                    SimpleDateFormat dateFormat = new SimpleDateFormat();
                    try {
                        Date date = dateFormat.parse(stringVal);
                        values.put(field.getName(), date.getTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    values.put(field.getName(), stringVal);
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

    protected void setLocationText(Location location)
    {
        if(null == location || null == mLatView || null == mLongView || null == mAccView || null == mAltView) {
            mLatView.setText(getString(R.string.latitude_caption_short) + ": " + getString(R.string.n_a));
            mLongView.setText(getString(R.string.longitude_caption_short) + ": " + getString(R.string.n_a));
            mAltView.setText(getString(R.string.altitude_caption_short) + ": " + getString(R.string.n_a));
            mAccView.setText(getString(R.string.accuracy_caption_short) + ": " + getString(R.string.n_a));
            return;
        }

        mLocation = location;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int nFormat = prefs.getInt(SettingsConstantsUI.KEY_PREF_COORD_FORMAT + "_int",
                                   Location.FORMAT_SECONDS);
        DecimalFormat df = new DecimalFormat("0.0");

        mLatView.setText(getString(R.string.latitude_caption_short) + ": " +
                         LocationUtil.formatLatitude(location.getLatitude(), nFormat,
                                                     getResources()));
        mLongView.setText(getString(R.string.longitude_caption_short) + ": " +
                          LocationUtil.formatLongitude(location.getLongitude(), nFormat, getResources()));

        double altitude = location.getAltitude();
        mAltView.setText(getString(R.string.altitude_caption_short) + ": " + df.format(altitude) + " " +
                   getString(R.string.unit_meter));

        float accuracy = location.getAccuracy();
        mAccView.setText(getString(R.string.accuracy_caption_short) + ": " + df.format(accuracy) + " " +
                   getString(R.string.unit_meter));

    }


    @Override
    public void onLocationChanged(Location location)
    {

    }


    @Override
    public void onGpsStatusChanged(int event)
    {

    }

    protected View.OnClickListener getDateUpdateWatcher(final TextView dateEdit){
        View.OnClickListener out = new View.OnClickListener() {
            protected Calendar calendar = Calendar.getInstance();

            DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener()
            {

                @Override
                public void onDateSet(
                        DatePicker view,
                        int year,
                        int monthOfYear,
                        int dayOfMonth)
                {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, monthOfYear);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    updateLabel();
                }

            };

            protected void updateLabel() {
                SimpleDateFormat sdf = new SimpleDateFormat();
                dateEdit.setText(sdf.format(calendar.getTime()));
            }


            @Override
            public void onClick(View view)
            {
                new DatePickerDialog(ModifyAttributesActivity.this, date, calendar.get(Calendar.YEAR),
                                     calendar.get(Calendar.MONTH),
                                     calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        };

        return out;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.standard_attributes, menu);

        return true;
    }
}

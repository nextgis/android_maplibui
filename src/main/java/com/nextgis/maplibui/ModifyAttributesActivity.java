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

import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.util.SettingsConstants;

import java.text.DecimalFormat;
import java.util.List;


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

    public final static String KEY_LAYER_ID = "layer_id";
    public final static String KEY_FEATURE_ID = "feature_id";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_standard_attributes);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.getBackground().setAlpha(255);
        setSupportActionBar(toolbar);

        mLatView = (TextView) findViewById(R.id.latitude_view);
        mLongView = (TextView) findViewById(R.id.longitude_view);
        mAltView = (TextView) findViewById(R.id.altitude_view);
        mAccView = (TextView) findViewById(R.id.accuracy_view);
        LinearLayout layout = (LinearLayout)findViewById(R.id.controls_list);

        final IGISApplication app = (IGISApplication) getApplication();
        ImageButton refreshLocation = (ImageButton)findViewById(R.id.refresh);
        refreshLocation.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(null != app){
                    GpsEventSource gpsEventSource = app.getGpsEventSource();
                    setLocationText(gpsEventSource.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                }
            }
        });

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //create and fill controls
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            short layer_id = extras.getShort(KEY_LAYER_ID);
            long feature_id = extras.getLong(KEY_FEATURE_ID);

            MapBase map = app.getMap();
            VectorLayer layer = (VectorLayer) map.getLayerById(layer_id);
            if(null != layer){
                createAndFillControls(layer, feature_id);
            }
        }
    }

    protected void createAndFillControls(VectorLayer layer, long feature_id){
        List<Field> fields = layer.getFields();
        for(Field field : fields){
            //create static text with alias

            //create control
        }
    }


    @Override
    protected void onPause()
    {
        IGISApplication app = (IGISApplication) getApplication();
        if(null != app){
            GpsEventSource gpsEventSource = app.getGpsEventSource();
            gpsEventSource.removeListener(this);
        }
        super.onPause();
    }


    @Override
    protected void onResume()
    {
        IGISApplication app = (IGISApplication) getApplication();
        if(null != app){
            GpsEventSource gpsEventSource = app.getGpsEventSource();
            gpsEventSource.addListener(this);
            setLocationText(gpsEventSource.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        }
        super.onResume();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void setLocationText(Location location)
    {
        if(null == location || null == mLatView || null == mLongView || null == mAccView || null == mAltView)
            return;

        mLocation = location;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int nFormat = prefs.getInt(SettingsConstants.KEY_PREF_COORD_FORMAT + "_int",
                                   Location.FORMAT_SECONDS);
        DecimalFormat df = new DecimalFormat("0.0");

        mLatView.setText(getString(R.string.latitude_caption) + ": " +
                         LocationUtil.formatLatitude(location.getLatitude(), nFormat,
                                                     getResources()));
        mLongView.setText(getString(R.string.longitude_caption) + ": " +
                          LocationUtil.formatLongitude(location.getLongitude(), nFormat, getResources()));

        double altitude = location.getAltitude();
        mAltView.setText(getString(R.string.altitude_caption) + ": " + df.format(altitude) + " " +
                   getString(R.string.unit_meter));

        float accuracy = location.getAccuracy();
        mAccView.setText(getString(R.string.accuracy_caption) + ": " + df.format(accuracy) + " " +
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
}

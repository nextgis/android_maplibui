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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.controlui.DateTimeControl;
import com.nextgis.maplibui.controlui.IControl;
import com.nextgis.maplibui.controlui.TextEditControl;
import com.nextgis.maplibui.controlui.TextLabelControl;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplibui.util.ConstantsUI.*;


/**
 * Activity to add or modify vector layer attributes
 */
public class ModifyAttributesActivity
        extends NGActivity
        implements GpsEventListener
{
    protected Map<String, IControl> mFields;

    protected VectorLayer           mLayer;
    protected long                  mFeatureId;
    protected GeoGeometry           mGeometry;

    protected TextView              mLatView;
    protected TextView              mLongView;
    protected TextView              mAltView;
    protected TextView              mAccView;
    protected Location              mLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_standard_attributes);
        setToolbar(R.id.main_toolbar);

        final IGISApplication app = (IGISApplication) getApplication();
        createView(app);
        createLocationPanelView(app);
    }

    protected void createLocationPanelView(final IGISApplication app)
    {
        if (null == mGeometry && mFeatureId == NOT_FOUND) {
            mLatView = (TextView) findViewById(R.id.latitude_view);
            mLongView = (TextView) findViewById(R.id.longitude_view);
            mAltView = (TextView) findViewById(R.id.altitude_view);
            mAccView = (TextView) findViewById(R.id.accuracy_view);

            ImageButton refreshLocation = (ImageButton) findViewById(R.id.refresh);
            refreshLocation.setOnClickListener(
                    new View.OnClickListener()
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
        } else {
            //hide location panel
            ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
            rootView.removeView(findViewById(R.id.location_panel));
        }
    }


    protected void createView(final IGISApplication app)
    {
        //create and fill controls
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            short layerId = extras.getShort(KEY_LAYER_ID);
            MapBase map = app.getMap();
            mLayer = (VectorLayer) map.getLayerById(layerId);

            if (null != mLayer) {
                mFields = new HashMap<>();
                mFeatureId = extras.getLong(KEY_FEATURE_ID);
                mGeometry = (GeoGeometry) extras.getSerializable(KEY_GEOMETRY);
                LinearLayout layout = (LinearLayout) findViewById(R.id.controls_list);
                fillControls(layout);
            }
        }
    }


    protected void fillControls(LinearLayout layout)
    {
        Cursor featureCursor = null;

        if (mFeatureId != NOT_FOUND) {
            featureCursor = mLayer.query(null, FIELD_ID + " = " + mFeatureId, null, null);
            if (!featureCursor.moveToFirst()) {
                featureCursor = null;
            }
        }

        List<Field> fields = mLayer.getFields();

        for (Field field : fields) {
            //create static text with alias
            TextLabelControl textLabel = new TextLabelControl(this, field.getAlias());
            textLabel.addToLayout(layout);

            IControl control = null;

            //create control
            switch (field.getType()) {

                case GeoConstants.FTString:
                case GeoConstants.FTInteger:
                case GeoConstants.FTReal:
                    control = new TextEditControl(this, field, featureCursor);
                    break;

                case GeoConstants.FTDateTime:
                    control = new DateTimeControl(this, field, featureCursor);
                    break;

                case GeoConstants.FTBinary:
                case GeoConstants.FTStringList:
                case GeoConstants.FTIntegerList:
                case GeoConstants.FTRealList:
                    //TODO: add support for this types
                    break;

                default:
                    break;
            }

            if (null != control) {
                control.addToLayout(layout);
                String fieldName = control.getFieldName();

                if (null != fieldName) {
                    mFields.put(fieldName, control);
                }
            }
        }

        if (null != featureCursor) {
            featureCursor.close();
        }
    }


    @Override
    protected void onPause()
    {
        if (null != findViewById(R.id.location_panel)) {
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
        if (null != findViewById(R.id.location_panel)) {
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.edit_attributes, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.menu_cancel || id == android.R.id.home) {
            finish();
            return true;

        } else if (id == R.id.menu_settings) {
            final IGISApplication app = (IGISApplication) getApplication();
            app.showSettings();
            return true;

        } else if (id == R.id.menu_apply) {
            saveFeature();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void saveFeature()
    {
        //create new row or modify existing
        List<Field> fields = mLayer.getFields();
        ContentValues values = new ContentValues();

        for (Field field : fields) {
            putFieldValue(values, field);
        }

        putGeometry(values);


        IGISApplication app = (IGISApplication) getApplication();

        if (null == app) {
            throw new IllegalArgumentException("Not a IGISApplication");
        }

        Uri uri = Uri.parse(
                "content://" + app.getAuthority() + "/" + mLayer.getPath().getName());

        if (mFeatureId == NOT_FOUND) {

            int nUniqId = mLayer.getUniqId();
            if (nUniqId < 1000) {
                nUniqId = 1000;// + mLayer.getCount();
            }

            values.put(FIELD_ID, nUniqId);

            if (getContentResolver().insert(uri, values) == null) {
                Toast.makeText(this, getText(R.string.error_db_insert), Toast.LENGTH_SHORT).show();
            }

        } else {
            Uri updateUri = ContentUris.withAppendedId(uri, mFeatureId);

            if (getContentResolver().update(updateUri, values, null, null) == 0) {
                Toast.makeText(this, getText(R.string.error_db_update), Toast.LENGTH_SHORT).show();
            }
        }
    }


    protected Object putFieldValue(
            ContentValues values,
            Field field)
    {
        IControl control = mFields.get(field.getName());

        if (null == control) {
            return null;
        }

        Object value = control.getValue();

        if (null != value) {
            Log.d(TAG, "field: " + field.getName() + " value: " + value.toString());

            if (value instanceof Long) {
                values.put(field.getName(), (Long) value);

            } else if (value instanceof String) {
                values.put(field.getName(), (String) value);
            }
        }

        return value;
    }


    protected boolean putGeometry(ContentValues values)
    {
        GeoGeometry geometry = null;

        if (null != mGeometry) {
            geometry = mGeometry;

        } else if (NOT_FOUND == mFeatureId) {

            if (null == mLocation) {
                Toast.makeText(
                        this, getText(R.string.error_no_location), Toast.LENGTH_SHORT).show();
                return false;
            }

            //create point geometry
            GeoPoint pt;

            switch (mLayer.getGeometryType()) {

                case GeoConstants.GTPoint:
                    pt = new GeoPoint(mLocation.getLongitude(), mLocation.getLatitude());
                    pt.setCRS(GeoConstants.CRS_WGS84);
                    pt.project(GeoConstants.CRS_WEB_MERCATOR);

                    geometry = pt;
                    break;

                case GeoConstants.GTMultiPoint:
                    pt = new GeoPoint(mLocation.getLongitude(), mLocation.getLatitude());
                    pt.setCRS(GeoConstants.CRS_WGS84);
                    pt.project(GeoConstants.CRS_WEB_MERCATOR);

                    GeoMultiPoint mpt = new GeoMultiPoint();
                    mpt.add(pt);
                    geometry = mpt;
                    break;
            }
        }

        if (null != geometry) {
            try {
                values.put(FIELD_GEOM, geometry.toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }


    protected void setLocationText(Location location)
    {
        if (null == location || null == mLatView || null == mLongView || null == mAccView ||
            null == mAltView) {

            mLatView.setText(
                    getString(R.string.latitude_caption_short) + ": " + getString(R.string.n_a));
            mLongView.setText(
                    getString(R.string.longitude_caption_short) + ": " + getString(R.string.n_a));
            mAltView.setText(
                    getString(R.string.altitude_caption_short) + ": " + getString(R.string.n_a));
            mAccView.setText(
                    getString(R.string.accuracy_caption_short) + ": " + getString(R.string.n_a));

            return;
        }

        mLocation = location;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int nFormat = prefs.getInt(
                SettingsConstantsUI.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);
        DecimalFormat df = new DecimalFormat("0.0");

        mLatView.setText(
                getString(R.string.latitude_caption_short) + ": " +
                LocationUtil.formatLatitude(location.getLatitude(), nFormat, getResources()));

        mLongView.setText(
                getString(R.string.longitude_caption_short) + ": " +
                LocationUtil.formatLongitude(location.getLongitude(), nFormat, getResources()));

        double altitude = location.getAltitude();
        mAltView.setText(
                getString(R.string.altitude_caption_short) + ": " + df.format(altitude) + " " +
                getString(R.string.unit_meter));

        float accuracy = location.getAccuracy();
        mAccView.setText(
                getString(R.string.accuracy_caption_short) + ": " + df.format(accuracy) + " " +
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

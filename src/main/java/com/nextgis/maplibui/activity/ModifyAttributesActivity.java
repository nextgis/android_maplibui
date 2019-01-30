/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016, 2018-2019 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.activity;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.AccurateLocationTaker;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IControl;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.api.ISimpleControl;
import com.nextgis.maplibui.control.DateTime;
import com.nextgis.maplibui.control.PhotoGallery;
import com.nextgis.maplibui.control.TextEdit;
import com.nextgis.maplibui.control.TextLabel;
import com.nextgis.maplibui.formcontrol.Sign;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.NotificationHelper;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_FEATURE_ID;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_GEOMETRY;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_GEOMETRY_CHANGED;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_LAYER_ID;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_VIEW_ONLY;


/**
 * Activity to add or modify vector layer attributes
 */
public class ModifyAttributesActivity
        extends NGActivity
        implements GpsEventListener
{
    protected long PROGRESS_DELAY = 1000L;
    protected long MAX_TAKE_TIME  = Integer.MAX_VALUE;

    protected Map<String, IControl> mFields;
    protected VectorLayer           mLayer;
    protected long                  mFeatureId;

    protected GeoGeometry           mGeometry;
    protected TextView              mLatView;
    protected TextView              mLongView;
    protected TextView              mAltView;
    protected TextView              mAccView;
    protected SwitchCompat          mAccurateLocation;
    protected AppCompatSpinner      mAccuracyCE;
    protected AlertDialog           mGPSDialog;

    protected Location              mLocation;
    protected SharedPreferences mSharedPreferences;

    protected int mMaxTakeCount;
    protected boolean mIsGeometryChanged;
    protected boolean mIsViewOnly;
    protected SoundPool mSoundPool;
    private int mBeepId;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_standard_attributes);
        setToolbar(R.id.main_toolbar);

        final IGISApplication app = (IGISApplication) getApplication();
        createView(app, savedInstanceState);
        createLocationPanelView(app);
        createSoundPool();
    }

    protected void createLocationPanelView(final IGISApplication app)
    {
        if (null == mGeometry && mFeatureId == NOT_FOUND) {
            mLatView = findViewById(R.id.latitude_view);
            mLongView = findViewById(R.id.longitude_view);
            mAltView = findViewById(R.id.altitude_view);
            mAccView = findViewById(R.id.accuracy_view);
            final ImageButton refreshLocation = findViewById(R.id.refresh);
            mAccurateLocation = findViewById(R.id.accurate_location);
            mAccurateLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (mAccurateLocation.getTag() == null) {
                        refreshLocation.performClick();
                        mAccurateLocation.setTag(new Object());
                    }
                }
            });

            mAccuracyCE = findViewById(R.id.accurate_ce);
            SpinnerAdapter adapter = mAccuracyCE.getAdapter();
            String def = adapter.getItem(0).toString();
            def = mSharedPreferences.getString(SettingsConstants.KEY_PREF_LOCATION_ACCURATE_CE, def);
            int id = 0;
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equals(def)) {
                    id = i;
                    break;
                }
            }
            mAccuracyCE.setSelection(id);

            refreshLocation.setOnClickListener(
                    new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            RotateAnimation rotateAnimation = new RotateAnimation(
                                    0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                                    Animation.RELATIVE_TO_SELF, 0.5f);
                            rotateAnimation.setDuration(500);
                            rotateAnimation.setRepeatCount(1);
                            refreshLocation.startAnimation(rotateAnimation);

                            if (mAccurateLocation.isChecked()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ModifyAttributesActivity.this);
                                View layout = View.inflate(ModifyAttributesActivity.this, R.layout.dialog_progress_accurate_location, null);
                                TextView message = layout.findViewById(R.id.message);
                                final ProgressBar progress = layout.findViewById(R.id.progress);
                                final TextView progressPercent = layout.findViewById(R.id.progress_percent);
                                final TextView progressNumber = layout.findViewById(R.id.progress_number);
                                final CheckBox finishBeep = layout.findViewById(R.id.finish_beep);
                                builder.setView(layout);
                                builder.setTitle(R.string.accurate_location);

                                String selected = (String) mAccuracyCE.getSelectedItem();
                                mSharedPreferences.edit().putString(SettingsConstants.KEY_PREF_LOCATION_ACCURATE_CE, selected).apply();
                                final AccurateLocationTaker accurateLocation =
                                        new AccurateLocationTaker(view.getContext(), 100f,
                                                mMaxTakeCount, MAX_TAKE_TIME, PROGRESS_DELAY, selected);

                                progress.setIndeterminate(true);
                                message.setText(R.string.accurate_taking);
                                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        accurateLocation.cancelTaking();
                                    }
                                });
                                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        accurateLocation.cancelTaking();
                                    }
                                });
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        ControlHelper.unlockScreenOrientation(ModifyAttributesActivity.this);
                                    }
                                });

                                final AlertDialog dialog = builder.create();
                                accurateLocation.setOnProgressUpdateListener(new AccurateLocationTaker.OnProgressUpdateListener() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void onProgressUpdate(Long... values) {
                                        int value = values[0].intValue();
                                        if (value == 1) {
                                            progress.setIndeterminate(false);
                                            progress.setMax(mMaxTakeCount);
                                        }

                                        if (value > 0)
                                            progress.setProgress(value);

                                        progressPercent.setText(value * 100 / mMaxTakeCount + " %");
                                        progressNumber.setText(value + " / " + mMaxTakeCount);
                                    }
                                });

                                accurateLocation.setOnGetAccurateLocationListener(new AccurateLocationTaker.OnGetAccurateLocationListener() {
                                    @Override
                                    public void onGetAccurateLocation(Location accurateLocation, Long... values) {
                                        dialog.dismiss();
                                        if (finishBeep.isChecked())
                                            playBeep();

                                        setLocationText(accurateLocation);
                                    }
                                });

                                ControlHelper.lockScreenOrientation(ModifyAttributesActivity.this);
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.show();
                                accurateLocation.startTaking();
                            } else if (null != app) {
                                GpsEventSource gpsEventSource = app.getGpsEventSource();
                                Location location = gpsEventSource.getLastKnownLocation();
                                setLocationText(location);
                            }
                        }
                    });
        } else {
            //hide location panel
            ViewGroup rootView = findViewById(R.id.controls_list);
            rootView.removeView(findViewById(R.id.location_panel));
        }
    }

    @SuppressWarnings("deprecation")
    protected void createSoundPool() {
        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
        mBeepId = mSoundPool.load(this, R.raw.beep, 1);
    }

    protected void playBeep() {
        mSoundPool.play(mBeepId, 1, 1, 10, 0, 1);
    }

    protected void createView(final IGISApplication app, Bundle savedState)
    {
        //create and fill controls
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            int layerId = extras.getInt(KEY_LAYER_ID);
            MapBase map = app.getMap();
            mLayer = (VectorLayer) map.getLayerById(layerId);

            if (null != mLayer) {
                mSharedPreferences = mLayer.getPreferences();

                mFields = new HashMap<>();
                mFeatureId = extras.getLong(KEY_FEATURE_ID);
                mIsViewOnly = extras.getBoolean(KEY_VIEW_ONLY, false);
                mIsGeometryChanged = extras.getBoolean(KEY_GEOMETRY_CHANGED, true);
                mGeometry = (GeoGeometry) extras.getSerializable(KEY_GEOMETRY);
                LinearLayout layout = findViewById(R.id.controls_list);
                fillControls(layout, savedState);
            } else {
                Toast.makeText(this, R.string.error_layer_not_inited, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    protected Cursor getFeatureCursor() {
        Cursor featureCursor = null;
        if (mFeatureId != NOT_FOUND) {
            String selection = FIELD_ID + " = " + mFeatureId;
            featureCursor = mLayer.query(null, selection, null, null, null);
            if (!featureCursor.moveToFirst())
                featureCursor = null;
        }
        return featureCursor;
    }

    protected void fillControls(LinearLayout layout, Bundle savedState)
    {
        Cursor featureCursor = getFeatureCursor();
        List<Field> fields = mLayer.getFields();
        Collections.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field lhs, Field rhs) {
                return lhs.getAlias().compareToIgnoreCase(rhs.getAlias());
            }
        });

        for (Field field : fields) {
            //create static text with alias
            TextLabel textLabel = (TextLabel)getLayoutInflater().inflate(R.layout.template_textlabel, layout, false);
            textLabel.setText(field.getAlias());
            textLabel.addToLayout(layout);

            ISimpleControl control = null;

            //create control
            switch (field.getType()) {

                case GeoConstants.FTString:
                case GeoConstants.FTInteger:
                case GeoConstants.FTReal:
                    TextEdit textEdit = (TextEdit) getLayoutInflater().inflate(R.layout.template_textedit, layout, false);
                    if (mIsViewOnly) {
                        textEdit.setEnabled(false);
                    }
                    control = textEdit;
                    break;
                case GeoConstants.FTDate:
                case GeoConstants.FTTime:
                case GeoConstants.FTDateTime:
                    DateTime dateTime = (DateTime) getLayoutInflater().inflate(R.layout.template_datetime, layout, false);
                    dateTime.setPickerType(mLayer.getFieldByName(field.getName()).getType());
                    if (mIsViewOnly) {
                        dateTime.setEnabled(false);
                    }
                    control = dateTime;
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
                control.init(field, savedState, featureCursor);
                control.addToLayout(layout);
                String fieldName = control.getFieldName();

                if (null != fieldName) {
                    mFields.put(fieldName, control);
                }
            }
        }

        try {
            IFormControl control = (PhotoGallery) getLayoutInflater().inflate(R.layout.formtemplate_photo, layout, false);
            if (mIsViewOnly)
                control = (PhotoGallery) getLayoutInflater().inflate(R.layout.formtemplate_photo_disabled, layout, false);
            ((PhotoGallery) control).init(mLayer, mFeatureId);
            control.init(null, null, null, null, null);
            control.addToLayout(layout);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (null != featureCursor) {
            featureCursor.close();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LinearLayout controlLayout = findViewById(R.id.controls_list);
        for (int i = 0; i < controlLayout.getChildCount(); i++)
            if (controlLayout.getChildAt(i) instanceof IControl)
                ((IControl) controlLayout.getChildAt(i)).saveState(outState);

        super.onSaveInstanceState(outState);
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
                if (mGPSDialog == null || !mGPSDialog.isShowing())
                    mGPSDialog = NotificationHelper.showLocationInfo(this);
                setLocationText(gpsEventSource.getLastKnownLocation());
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            mMaxTakeCount = Integer.parseInt(prefs.getString(SettingsConstants.KEY_PREF_LOCATION_ACCURATE_COUNT, "20"));
        }
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.edit_attributes, menu);
        if (mIsViewOnly) {
            MenuItem item = menu.findItem(R.id.menu_apply);
            if (item != null)
                item.setVisible(false);
        }
        return true;
    }


    private boolean checkEdits() {
        if (hasEdits() && !mIsViewOnly) {
            AlertDialog builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.save)
                    .setMessage(R.string.has_edits)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveFeature();
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).create();
            builder.show();
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            if (!checkEdits())
                finish();
            return true;
        } else if (id == R.id.menu_settings) {
            final IGISApplication app = (IGISApplication) getApplication();
            app.showSettings(SettingsConstantsUI.ACTION_PREFS_GENERAL);
            return true;
        } else if (id == R.id.menu_apply) {
            saveFeature();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!checkEdits())
            super.onBackPressed();
    }

    private boolean hasEdits() {
        boolean result = mFeatureId == NOT_FOUND;

        if (mLayer == null) {
            Toast.makeText(this, R.string.error_layer_not_inited, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!result) {
            Cursor featureCursor = mLayer.query(null, FIELD_ID + " = " + mFeatureId, null, null, null);
            if (featureCursor == null || !featureCursor.moveToFirst())
                return false;

            for (Map.Entry<String, IControl> field : mFields.entrySet()) {
                int column = featureCursor.getColumnIndex(field.getKey());
                if (column >= 0) {
                    IControl control = field.getValue();
                    String saved = featureCursor.getString(column);
                    Object modified = control.getValue();
                    if (modified != null)
                        result = !modified.equals(saved);
                    else result = saved != null;
                }

                if (result)
                    break;
            }

            featureCursor.close();
        }

        return result;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        PhotoGallery gallery = findViewById(R.id.pg_photos);
        if (gallery != null)
            gallery.onActivityResult(requestCode, resultCode, data);
    }


    protected boolean saveFeature()
    {
        if (mIsViewOnly) {
            return false;
        }

        if (mLayer == null) {
            Toast.makeText(this, R.string.error_layer_not_inited, Toast.LENGTH_SHORT).show();
            return false;
        }

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

        boolean error;
        if (mFeatureId == NOT_FOUND) {
            // we need to get proper mFeatureId for new features first
            Uri result = getContentResolver().insert(uri, values);
            if (error = result == null)
                Toast.makeText(this, getText(R.string.error_db_insert), Toast.LENGTH_SHORT).show();
            else
                mFeatureId = Long.parseLong(result.getLastPathSegment());
        } else {
            Uri updateUri = ContentUris.withAppendedId(uri, mFeatureId);
            boolean valuesUpdated = getContentResolver().update(updateUri, values, null, null) == 1;
            if (error = !valuesUpdated)
                Toast.makeText(this, getText(R.string.error_db_update), Toast.LENGTH_SHORT).show();
        }

        putAttaches();
        putSign();
        Intent data = new Intent();
        data.putExtra(ConstantsUI.KEY_FEATURE_ID, mFeatureId);
        setResult(RESULT_OK, data);
        return !error;
    }


    protected void putSign() {
        LinearLayout layout = findViewById(R.id.controls_list);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof Sign) {
                IGISApplication application = (IGISApplication) getApplication();
                Uri uri = Uri.parse("content://" + application.getAuthority() + "/" +
                        mLayer.getPath().getName() + "/" + mFeatureId + "/" + Constants.URI_ATTACH);

                ContentValues values = new ContentValues();
                values.put(VectorLayer.ATTACH_DISPLAY_NAME, "_signature");
                values.put(VectorLayer.ATTACH_DESCRIPTION, "_signature");
                values.put(VectorLayer.ATTACH_MIME_TYPE, "image/jpeg");

                String selection = VectorLayer.ATTACH_ID + " =  ?";
                String[] args = new String[]{Sign.SIGN_FILE};
                Cursor saved = getContentResolver().query(uri, null, selection, args, null);
                boolean hasSign = false;
                if (saved != null) {
                    hasSign = saved.moveToFirst();
                    saved.close();
                }

                if (!hasSign) {
                    Uri result = getContentResolver().insert(uri, values);
                    if (result != null) {
                        long id = Long.parseLong(result.getLastPathSegment());
                        values.clear();
                        values.put(VectorLayer.ATTACH_ID, Integer.MAX_VALUE);
                        uri = Uri.withAppendedPath(uri, id + "");
                        getContentResolver().update(uri, values, null, null);
                    }
                }

                File png = new File(mLayer.getPath(), mFeatureId + "");
                Sign sign = (Sign) child;
                try {
                    if (!png.isDirectory())
                        FileUtil.createDir(png);

                    png = new File(png, Sign.SIGN_FILE);
                    sign.save(sign.getWidth(), sign.getHeight(), true, png);
                } catch (IOException | RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    protected Object putFieldValue(
            ContentValues values,
            Field field)
    {
        String fieldName = field.getName();
        IControl control = mFields.get(fieldName);

        if (null == control) {
            return null;
        }

        Object value = control.getValue();
        fieldName = "'" + fieldName + "'";

        if (null != value) {
            Log.d(TAG, "field: " + field.getName() + " value: " + value.toString());

            if (value instanceof Long) {
                values.put(fieldName, (Long) value);
            } else if (value instanceof Integer) {
                values.put(fieldName, (Integer) value);
            } else if (value instanceof String) {
                values.put(fieldName, (String) value);
            } else if (value instanceof Double) {
                values.put(fieldName, (Double) value);
            } else if (value instanceof Float) {
                values.put(fieldName, (Float) value);
            }
        }

        return value;
    }


    protected boolean putGeometry(ContentValues values)
    {
        GeoGeometry geometry = null;

        if (null != mGeometry && mIsGeometryChanged) {
            geometry = mGeometry;
        } else if (NOT_FOUND == mFeatureId) {
            if (null == mLocation) {
                Toast.makeText(this, getText(R.string.error_no_location), Toast.LENGTH_SHORT).show();
                return false;
            }

            GeoPoint pt = new GeoPoint(mLocation.getLongitude(), mLocation.getLatitude());
            pt.setCRS(GeoConstants.CRS_WGS84);
            pt.project(GeoConstants.CRS_WEB_MERCATOR);

            switch (mLayer.getGeometryType()) {
                case GeoConstants.GTPoint:
                    geometry = pt;
                    break;
                case GeoConstants.GTMultiPoint:
                    geometry = new GeoMultiPoint();
                    ((GeoMultiPoint) geometry).add(pt);
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

    protected int putAttaches() {
        int total = 0;
        PhotoGallery gallery = findViewById(R.id.pg_photos);

        if (gallery != null && mFeatureId != NOT_FOUND) {
            List<Integer> deletedAttaches = gallery.getDeletedAttaches();
            IGISApplication application = (IGISApplication) getApplication();
            Uri uri = Uri.parse("content://" + application.getAuthority() + "/" +
                    mLayer.getPath().getName() + "/" + mFeatureId + "/" + Constants.URI_ATTACH);

            int size = deletedAttaches.size();
            String[] args = new String[size];
            for (int i = 0; i < size; i++)
                args[i] = deletedAttaches.get(i).toString();

            if (size > 0)
                total += getContentResolver().delete(uri, MapUtil.makePlaceholders(size), args);

            if (total == 0 && size > 0) {
                Toast.makeText(this, getText(R.string.photo_fail_attach), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "attach delete failed");
            } else {
                Log.d(TAG, "attach delete success: " + total);
            }

            List<String> imagesPath =  gallery.getNewAttaches();
            for (String path : imagesPath) {
                String[] segments = path.split("/");
                String name = segments.length > 0 ? segments[segments.length - 1] : "image.jpg";
                ContentValues values = new ContentValues();
                values.put(VectorLayer.ATTACH_DISPLAY_NAME, name);
                values.put(VectorLayer.ATTACH_MIME_TYPE, "image/jpeg");

                Uri result = getContentResolver().insert(uri, values);
                if (result == null) {
                    Toast.makeText(this, getText(R.string.photo_fail_attach), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "attach insert failed");
                } else {
                    if (copyToStream(result, path))
                        total++;

                    Log.d(TAG, "attach insert success: " + result.toString());
                }
            }
        }

        return total;
    }

    protected boolean copyToStream(Uri uri, String path) {
        try {
            OutputStream outStream = getContentResolver().openOutputStream(uri);

            if (outStream != null) {
                InputStream inStream = new FileInputStream(path);
                byte[] buffer = new byte[8192];
                int counter;

                while ((counter = inStream.read(buffer, 0, buffer.length)) > 0) {
                    outStream.write(buffer, 0, counter);
                    outStream.flush();
                }

                outStream.close();
                inStream.close();

                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }


    protected void setLocationText(Location location)
    {
        if (null == mLatView || null == mLongView || null == mAccView || null == mAltView)
            return;

        if (null == location) {
            mLatView.setText(formatCoordinates(Double.NaN, R.string.latitude_caption_short));
            mLongView.setText(formatCoordinates(Double.NaN, R.string.longitude_caption_short));
            mAltView.setText(formatMeters(Double.NaN, R.string.altitude_caption_short));
            mAccView.setText(formatMeters(Double.NaN, R.string.accuracy_caption_short));
            return;
        }

        mLocation = location;
        mLatView.setText(formatCoordinates(location.getLatitude(), R.string.latitude_caption_short));
        mLongView.setText(formatCoordinates(location.getLongitude(), R.string.longitude_caption_short));

        mAltView.setText(formatMeters(location.getAltitude(), R.string.altitude_caption_short));
        mAccView.setText(formatMeters(location.getAccuracy(), R.string.accuracy_caption_short));
    }

    private String formatCoordinates(double value, int caption) {
        String appendix;
        if (value != Double.NaN) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            int nFormat = Integer.parseInt(prefs.getString(SettingsConstantsUI.KEY_PREF_COORD_FORMAT, Location.FORMAT_DEGREES + ""));
            int nFraction = prefs.getInt(SettingsConstantsUI.KEY_PREF_COORD_FRACTION, 6);
            appendix = LocationUtil.formatLatitude(value, nFormat, nFraction, getResources());
        } else
            appendix = getString(R.string.n_a);

        return getString(caption) + ": " + appendix;
    }


    private String formatMeters(double value, int caption) {
        String appendix;
        if (value != Double.NaN) {
            DecimalFormat df = new DecimalFormat("0.0");
            appendix = df.format(value) + " " + getString(R.string.unit_meter);
        } else
            appendix = getString(R.string.n_a);

        return getString(caption) + ": " + appendix;
    }


    @Override
    public void onLocationChanged(Location location)
    {

    }


    @Override
    public void onBestLocationChanged(Location location)
    {

    }


    @Override
    public void onGpsStatusChanged(int event)
    {

    }
}

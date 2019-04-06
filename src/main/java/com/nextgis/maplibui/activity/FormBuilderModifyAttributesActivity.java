/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2019 NextGIS, info@nextgis.com
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

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IControl;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.control.PhotoGallery;
import com.nextgis.maplibui.formcontrol.AutoTextEdit;
import com.nextgis.maplibui.formcontrol.Checkbox;
import com.nextgis.maplibui.formcontrol.Combobox;
import com.nextgis.maplibui.formcontrol.Coordinates;
import com.nextgis.maplibui.formcontrol.Counter;
import com.nextgis.maplibui.formcontrol.DateTime;
import com.nextgis.maplibui.formcontrol.Distance;
import com.nextgis.maplibui.formcontrol.DoubleCombobox;
import com.nextgis.maplibui.formcontrol.DoubleComboboxValue;
import com.nextgis.maplibui.formcontrol.RadioGroup;
import com.nextgis.maplibui.formcontrol.Sign;
import com.nextgis.maplibui.formcontrol.Space;
import com.nextgis.maplibui.formcontrol.SplitCombobox;
import com.nextgis.maplibui.formcontrol.Tabs;
import com.nextgis.maplibui.formcontrol.TextEdit;
import com.nextgis.maplibui.formcontrol.TextLabel;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.JSON_TYPE_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ALBUM_ELEMENTS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_CHECKBOX_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_COMBOBOX_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_COORDINATES_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_COUNTER_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DATE_TIME_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DISTANCE_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DOUBLE_COMBOBOX_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_KEY_LIST_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_KEY_LIST_SAVED_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_LISTS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_PHOTO_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_PORTRAIT_ELEMENTS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_RADIO_GROUP_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_SIGN_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_SPACE_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_SPLIT_COMBOBOX_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TABS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TEXT_EDIT_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TEXT_LABEL_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_FORM_PATH;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_META_PATH;
import static com.nextgis.maplibui.util.NGIDUtils.PREF_FIRST_NAME;
import static com.nextgis.maplibui.util.NGIDUtils.PREF_LAST_NAME;
import static com.nextgis.maplibui.util.NGIDUtils.PREF_USERNAME;

/**
 * Activity to add or modify vector layer attributes
 */
public class FormBuilderModifyAttributesActivity extends ModifyAttributesActivity {
    private static final int RESELECT_ROW = 999;

    private Map<String, List<String>> mTable;
    private int mRow = -1;
    private File mMeta;
    private String mColumn;

    interface OnAskRowListener {
        void onRowChosen();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (mColumn != null) {
            MenuItem apply = menu.add(0, RESELECT_ROW, 50, R.string.select_row);
            apply.setIcon(R.drawable.ic_altitude);
            MenuItemCompat.setShowAsAction(apply, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        }

        if (mIsViewOnly) {
            MenuItem item = menu.findItem(R.id.menu_apply);
            if (item != null)
                item.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case RESELECT_ROW:
                if (mMeta != null && mMeta.exists()) {
                    try {
                        String metaString = FileUtil.readFromFile(mMeta);
                        JSONObject metaJson = new JSONObject(metaString);
                        metaJson.remove(JSON_KEY_LIST_SAVED_KEY);
                        FileUtil.writeToFile(mMeta, metaJson.toString());
                        refreshActivityView();
                    } catch (JSONException | IOException ignored) {}
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void fillControls(LinearLayout layout, Bundle savedState) {
        //TODO: add location control via fragment only defined by user space
        Bundle extras = getIntent().getExtras();

        if (mTable == null) {
            fillTable(layout, savedState, extras);
            return;
        }

        try {
            File form = (File) extras.getSerializable(KEY_FORM_PATH);

            int orientation = getResources().getConfiguration().orientation;
            boolean isLand = orientation == Configuration.ORIENTATION_LANDSCAPE;

            String formString = FileUtil.readFromFile(form);
            Object json = new JSONTokener(formString).nextValue();
            if (json instanceof JSONArray) {
                JSONArray elements = new JSONArray(formString);
                if (elements.length() > 0)
                    fillTabControls(layout, savedState, elements);
            } else {
                JSONObject jsonFormContents = new JSONObject(formString);
                JSONArray tabs = jsonFormContents.getJSONArray(JSON_TABS_KEY);

                for (int i = 0; i < tabs.length(); i++) {
                    JSONObject tab = tabs.getJSONObject(i);
                    JSONArray elements = null;

                    if (isLand && !tab.isNull(JSON_ALBUM_ELEMENTS_KEY)) {
                        elements = tab.getJSONArray(JSON_ALBUM_ELEMENTS_KEY);
                    }

                    if (null == elements) {
                        if (!isLand && !tab.isNull(JSON_PORTRAIT_ELEMENTS_KEY)) {
                            elements = tab.getJSONArray(JSON_PORTRAIT_ELEMENTS_KEY);
                        }
                    }

                    if (null == elements) {
                        if (!tab.isNull(JSON_ALBUM_ELEMENTS_KEY)) {
                            elements = tab.getJSONArray(JSON_ALBUM_ELEMENTS_KEY);
                        }

                        if (!tab.isNull(JSON_PORTRAIT_ELEMENTS_KEY)) {
                            elements = tab.getJSONArray(JSON_ALBUM_ELEMENTS_KEY);
                        }
                    }

                    if (null != elements && elements.length() > 0) {
                        fillTabControls(layout, savedState, elements);
                    }
                }
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_form_create), Toast.LENGTH_SHORT).show();
        }
    }

    private void fillTable(final LinearLayout layout, final Bundle savedState, Bundle extras) {
        mTable = new HashMap<>();
        mMeta = (File) extras.getSerializable(KEY_META_PATH);
        if (mMeta != null && mMeta.exists()) {
            try {
                String metaString = FileUtil.readFromFile(mMeta);
                final JSONObject metaJson = new JSONObject(metaString);
                if (metaJson.has(JSON_LISTS_KEY)) {
                    JSONObject lists = metaJson.getJSONObject(JSON_LISTS_KEY);
                    Iterator<String> i = lists.keys();
                    while (i.hasNext()) {
                        String key = i.next();
                        JSONArray list = lists.getJSONArray(key);
                        List<String> value = new ArrayList<>();
                        for (int j = 0; j < list.length(); j++)
                            value.add(list.getString(j));

                        mTable.put(key, value);
                    }
                }

                if (metaJson.has(JSON_KEY_LIST_KEY)) {
                    mColumn = metaJson.getString(JSON_KEY_LIST_KEY);

                    if (metaJson.has(JSON_KEY_LIST_SAVED_KEY))
                        mRow = metaJson.getInt(JSON_KEY_LIST_SAVED_KEY);
                    else {
                        askForRow(new OnAskRowListener() {
                            @Override
                            public void onRowChosen() {
                                if (mRow == -1)
                                    finish();
                                else {
                                    try {
                                        metaJson.put(JSON_KEY_LIST_SAVED_KEY, mRow);
                                        FileUtil.writeToFile(mMeta, metaJson.toString());
                                    } catch (JSONException | IOException ignored) {}
                                    fillControls(layout, savedState);
                                }
                            }
                        });
                        return;
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }

        fillControls(layout, savedState);
    }

    private void askForRow(final OnAskRowListener listener) {
        ArrayAdapter<String> spinnerMenu = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mTable.get(mColumn));
        final Spinner spinner = (Spinner) View.inflate(this, R.layout.table_select_row, null);
        spinner.setAdapter(spinnerMenu);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_row).setView(spinner)
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       mRow = spinner.getSelectedItemPosition();
                   }
               })
               .setOnDismissListener(new DialogInterface.OnDismissListener() {
                   @Override
                   public void onDismiss(DialogInterface dialogInterface) {
                       listener.onRowChosen();
                   }
               });
        AlertDialog dialog = builder.show();
        dialog.setCanceledOnTouchOutside(false);
    }

    @Override
    protected Cursor getFeatureCursor() {
        Cursor featureCursor = super.getFeatureCursor();
        if (featureCursor != null) {
            int id = featureCursor.getColumnIndex("author");
            if (id >= 0) {
                try {
                    String author = featureCursor.getString(id);
                    String fName = mPreferences.getString(PREF_FIRST_NAME, "");
                    String lName = mPreferences.getString(PREF_LAST_NAME, "");
                    String uName = mPreferences.getString(PREF_USERNAME, "");
                    String name = TextEdit.formUserName(fName, lName, uName);
                    mIsViewOnly = mIsViewOnly || !author.equals(name);
                } catch (Exception ignored) {}
            }
        }
        return featureCursor;
    }

    protected void fillTabControls(
            LinearLayout layout,
            Bundle savedState,
            JSONArray elements)
            throws JSONException {

        Cursor featureCursor = getFeatureCursor();
        List<Field> fields = mLayer.getFields();
        for (int i = 0; i < elements.length(); i++) {
            IFormControl control;
            JSONObject element = elements.getJSONObject(i);
            String type = element.optString(JSON_TYPE_KEY);
            if (type.equals(JSON_COORDINATES_VALUE)) {
                JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                String fieldY = attributes.optString(JSON_FIELD_NAME_KEY + "_lat");
                attributes.put(JSON_FIELD_NAME_KEY, fieldY);
                element.put(JSON_TYPE_KEY, type + "_lat");
                control = getControl(this, element, mLayer, mFeatureId, mGeometry, mIsViewOnly);
                addToLayout(control, element, fields, savedState, featureCursor, layout);

                attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                String fieldX = attributes.optString(JSON_FIELD_NAME_KEY + "_long");
                attributes.put(JSON_FIELD_NAME_KEY, fieldX);
                element.put(JSON_TYPE_KEY, type + "_lon");
            }

            control = getControl(this, element, mLayer, mFeatureId, mGeometry, mIsViewOnly);
            if (type.equals(JSON_TABS_KEY))
                ((Tabs) control).init(mLayer, mFeatureId, mGeometry, mTable, mRow, mSharedPreferences,
                                      mPreferences, getSupportFragmentManager(), mIsViewOnly);

            addToLayout(control, element, fields, savedState, featureCursor, layout);
        }

        if (null != featureCursor) {
            featureCursor.close();
        }

        layout.requestLayout();
    }

    public static IFormControl getControl(Context context, JSONObject element, VectorLayer layer, long feature,
                                          GeoGeometry geometry, boolean isViewOnly) throws JSONException {
        String type = element.getString(JSON_TYPE_KEY);
        IFormControl control = null;

        switch (type) {
            case JSON_TEXT_LABEL_VALUE:
                control = (TextLabel) View.inflate(context, R.layout.formtemplate_textlabel, null);
                break;

            case JSON_TEXT_EDIT_VALUE:
                control = (TextEdit) View.inflate(context, R.layout.formtemplate_edittext, null);
                break;

            case JSON_DATE_TIME_VALUE:
                control = (DateTime) View.inflate(context, R.layout.formtemplate_datetime, null);
                break;

            case JSON_RADIO_GROUP_VALUE:
                control = (RadioGroup) View.inflate(context, R.layout.formtemplate_radiogroup, null);
                break;

            case JSON_COMBOBOX_VALUE:
                if (ControlHelper.isAutoComplete(element.getJSONObject(JSON_ATTRIBUTES_KEY)))
                    control = (AutoTextEdit) View.inflate(context, R.layout.formtemplate_autoedittext, null);
                else
                    control = (Combobox) View.inflate(context, R.layout.formtemplate_combobox, null);
                break;

            case JSON_SPLIT_COMBOBOX_VALUE:
                control = new SplitCombobox(context);
                break;

            case JSON_DOUBLE_COMBOBOX_VALUE:
                control = (DoubleCombobox) View.inflate(context, R.layout.formtemplate_doublecombobox, null);
                break;

            case JSON_SPACE_VALUE:
                control = (Space) View.inflate(context, R.layout.formtemplate_space, null);
                break;

            case JSON_CHECKBOX_VALUE:
                control = (Checkbox) View.inflate(context, R.layout.formtemplate_checkbox, null);
                break;

            case JSON_PHOTO_VALUE:
                if (isViewOnly)
                    control = (PhotoGallery) View.inflate(context, R.layout.formtemplate_photo_disabled, null);
                else
                    control = (PhotoGallery) View.inflate(context, R.layout.formtemplate_photo, null);
                ((PhotoGallery) control).init(layer, feature);
                break;

            case JSON_SIGN_VALUE:
                control = (Sign) View.inflate(context, R.layout.formtemplate_sign, null);
                ((Sign) control).setPath(layer.getPath().getPath() + File.separator + feature);
                break;

            case JSON_COUNTER_VALUE:
                control = (Counter) View.inflate(context, R.layout.formtemplate_counter, null);
                break;

            case JSON_DISTANCE_VALUE:
                control = (Distance) View.inflate(context, R.layout.formtemplate_distance, null);
                if (geometry instanceof GeoPoint || geometry instanceof GeoMultiPoint) {
                    GeoPoint point;
                    if (geometry instanceof GeoMultiPoint)
                        point = (GeoPoint) ((GeoMultiPoint) geometry).get(0).copy();
                    else
                        point = (GeoPoint) geometry.copy();
                    point.setCRS(GeoConstants.CRS_WEB_MERCATOR);
                    point.project(GeoConstants.CRS_WGS84);
                    Location location = new Location(LocationManager.GPS_PROVIDER);
                    location.setLatitude(point.getY());
                    location.setLongitude(point.getX());
                    ((Distance) control).setLocation(location);
                }
                break;

            case JSON_COORDINATES_VALUE + "_lon":
                Double x = getCoordinate(geometry, false);
                control = (Coordinates) View.inflate(context, R.layout.formtemplate_coordinates, null);
                if (control != null && x != null)
                    ((Coordinates) control).setValue(x);
                break;

            case JSON_COORDINATES_VALUE + "_lat":
                Double y = getCoordinate(geometry, true);
                control = (Coordinates) View.inflate(context, R.layout.formtemplate_coordinates, null);
                if (control != null) {
                    ((Coordinates) control).setIsLat();
                    if (y != null)
                        ((Coordinates) control).setValue(y);
                }
                break;

            case JSON_TABS_KEY:
                control = (Tabs) View.inflate(context, R.layout.formtemplate_tabs, null);
                break;

            //TODO: add controls
            //button
            //group
            //orientation
            //compass

            default:
                break;
        }

        return control;
    }

    protected static Double getCoordinate(GeoGeometry geometry, boolean latitude) {
        Double x, y;
        x = y = null;
        if (geometry instanceof GeoPoint) {
            GeoPoint point = (GeoPoint) geometry.copy();
            point.setCRS(GeoConstants.CRS_WEB_MERCATOR);
            point.project(GeoConstants.CRS_WGS84);
            y = point.getY();
            x = point.getX();
        }
        return latitude ? y : x;
    }

    @Override
    protected void setLocationText(Location location) {
        super.setLocationText(location);
        for (Map.Entry<String, IControl> control : mFields.entrySet()) {
            IControl current = control.getValue();
            if (current instanceof Coordinates) {
                double lat = location == null ? 0 : location.getLatitude();
                double lon = location == null ? 0 : location.getLongitude();
                ((Coordinates) current).setValue(((Coordinates) current).isLat() ? lat : lon);
                ((Coordinates) current).setText(((Coordinates) current).getFormattedValue());
            }
        }
    }

    protected void addToLayout(IFormControl control, JSONObject element, List<Field> fields, Bundle savedState,
                               Cursor featureCursor, LinearLayout layout) throws JSONException {
        if (null != control) {
            appendData(mLayer, mPreferences, mTable, mRow, control, element);

            control.init(element, fields, savedState, featureCursor, mSharedPreferences);
            control.addToLayout(layout);
            if (mIsViewOnly)
                control.setEnabled(false);

            String fieldName = control.getFieldName();
            if (null != fieldName)
                mFields.put(fieldName, control);
            if (control instanceof Tabs) {
                Tabs tabs = (Tabs) control;
                mFields.putAll(tabs.getFields());
            }
        }
    }

    public static void appendData(VectorLayer layer, SharedPreferences preferences, Map<String, List<String>> table, int row,
                                  IFormControl control, JSONObject element) throws JSONException {
        if (layer instanceof NGWVectorLayer)
            element.put(SyncStateContract.Columns.ACCOUNT_NAME, ((NGWVectorLayer) layer).getAccountName());

        element.put(PREF_FIRST_NAME, preferences.getString(PREF_FIRST_NAME, ""));
        element.put(PREF_LAST_NAME, preferences.getString(PREF_LAST_NAME, ""));
        element.put(PREF_USERNAME, preferences.getString(PREF_USERNAME, ""));

        if (control instanceof Counter && table != null && row != -1) {
            JSONObject attrs = element.getJSONObject(JSON_ATTRIBUTES_KEY);
            if (!attrs.isNull(Counter.PREFIX_LIST)) {
                String prefix = attrs.getString(Counter.PREFIX_LIST);
                prefix = table.get(prefix).get(row);
                attrs.put(Counter.PREFIX, prefix);
            }

            if (!attrs.isNull(Counter.SUFFIX_LIST)) {
                String suffix = attrs.getString(Counter.SUFFIX_LIST);
                suffix = table.get(suffix).get(row);
                attrs.put(Counter.SUFFIX, suffix);
            }
        }
    }

    @Override
    protected boolean saveFeature() {
        boolean success = super.saveFeature();
        if (success)
            for (Field field : mLayer.getFields())
                saveLastValue(field);

        return success;
    }

    protected Object putFieldValue(ContentValues values, Field field) {
        Object value = super.putFieldValue(values, field);
        IFormControl control = (IFormControl) mFields.get(field.getName());
        if (null == control)
            return null;

        if (null != value) {
            if (value instanceof DoubleComboboxValue) {
                DoubleComboboxValue dcValue = (DoubleComboboxValue) value;
                values.put(dcValue.mFieldName, dcValue.mValue);
                values.put(dcValue.mSubFieldName, dcValue.mSubValue);
            }
        }

        return value;
    }

    protected void saveLastValue(Field field) {
        IFormControl control = (IFormControl) mFields.get(field.getName());
        if (null != control && control.isShowLast())
            control.saveLastValue(mSharedPreferences);
    }
}

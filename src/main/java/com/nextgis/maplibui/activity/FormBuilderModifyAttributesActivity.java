/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.NGWVectorLayer;
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
import com.nextgis.maplibui.formcontrol.DoubleCombobox;
import com.nextgis.maplibui.formcontrol.DoubleComboboxValue;
import com.nextgis.maplibui.formcontrol.RadioGroup;
import com.nextgis.maplibui.formcontrol.Sign;
import com.nextgis.maplibui.formcontrol.Space;
import com.nextgis.maplibui.formcontrol.SplitCombobox;
import com.nextgis.maplibui.formcontrol.TextEdit;
import com.nextgis.maplibui.formcontrol.TextLabel;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.JSON_TYPE_KEY;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ALBUM_ELEMENTS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_CHECKBOX_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_COMBOBOX_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_COORDINATES_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_COUNTER_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DATE_TIME_VALUE;
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
            if (TextUtils.indexOf(formString, "tabs") == -1) {
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

    protected void fillTabControls(
            LinearLayout layout,
            Bundle savedState,
            JSONArray elements)
            throws JSONException {
        Cursor featureCursor = null;

        if (mFeatureId != NOT_FOUND) {
            featureCursor = mLayer.query(null, FIELD_ID + " = " + mFeatureId, null, null, null);
            if (!featureCursor.moveToFirst()) {
                featureCursor = null;
            }
        }

        List<Field> fields = mLayer.getFields();

        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            String type = element.getString(JSON_TYPE_KEY);
            IFormControl control = null;

            switch (type) {
                case JSON_TEXT_LABEL_VALUE:
                    control = (TextLabel) getLayoutInflater().inflate(R.layout.formtemplate_textlabel, layout, false);
                    break;

                case JSON_TEXT_EDIT_VALUE:
                    control = (TextEdit) getLayoutInflater().inflate(R.layout.formtemplate_edittext, layout, false);
                    break;

                case JSON_DATE_TIME_VALUE:
                    control = (DateTime) getLayoutInflater().inflate(R.layout.formtemplate_datetime, layout, false);
                    break;

                case JSON_RADIO_GROUP_VALUE:
                    control = (RadioGroup) getLayoutInflater().inflate(R.layout.formtemplate_radiogroup, layout, false);
                    break;

                case JSON_COMBOBOX_VALUE:
                    if (ControlHelper.isAutoComplete(element.getJSONObject(JSON_ATTRIBUTES_KEY)))
                        control = (AutoTextEdit) getLayoutInflater().inflate(R.layout.formtemplate_autoedittext, layout, false);
                    else
                        control = (Combobox) getLayoutInflater().inflate(R.layout.formtemplate_combobox, layout, false);
                    break;

                case JSON_SPLIT_COMBOBOX_VALUE:
                    control = new SplitCombobox(this);
                    break;

                case JSON_DOUBLE_COMBOBOX_VALUE:
                    control = (DoubleCombobox) getLayoutInflater().inflate(R.layout.formtemplate_doublecombobox, layout, false);
                    break;

                case JSON_SPACE_VALUE:
                    control = (Space) getLayoutInflater().inflate(R.layout.formtemplate_space, layout, false);
                    break;

                case JSON_CHECKBOX_VALUE:
                    control = (Checkbox) getLayoutInflater().inflate(R.layout.formtemplate_checkbox, layout, false);
                    break;

                case JSON_PHOTO_VALUE:
                    control = (PhotoGallery) getLayoutInflater().inflate(R.layout.formtemplate_photo, layout, false);
                    ((PhotoGallery) control).init(mLayer, mFeatureId);
                    break;

                case JSON_SIGN_VALUE:
                    control = (Sign) getLayoutInflater().inflate(R.layout.formtemplate_sign, layout, false);
                    ((Sign) control).setPath(mLayer.getPath().getPath() + File.separator + mFeatureId);
                    break;

                case JSON_COUNTER_VALUE:
                    control = (Counter) getLayoutInflater().inflate(R.layout.formtemplate_counter, layout, false);
                    break;

                case JSON_COORDINATES_VALUE:
                    Double x, y;
                    x = y = null;
                    if (mGeometry != null && mGeometry instanceof GeoPoint) {
                        GeoPoint point = (GeoPoint) mGeometry.copy();
                        point.setCRS(GeoConstants.CRS_WEB_MERCATOR);
                        point.project(GeoConstants.CRS_WGS84);
                        y = point.getY();
                        x = point.getX();
                    }

                    JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    String field = attributes.optString(JSON_FIELD_NAME_KEY + "_lat");
                    attributes.put(JSON_FIELD_NAME_KEY, field);
                    control = (Coordinates) getLayoutInflater().inflate(R.layout.formtemplate_coordinates, layout, false);
                    if (control != null) {
                        ((Coordinates) control).setIsLat();
                        if (y != null)
                            ((Coordinates) control).setValue(y);
                    }

                    addToLayout(control, element, fields, savedState, featureCursor, layout);

                    field = attributes.optString(JSON_FIELD_NAME_KEY + "_long");
                    attributes.put(JSON_FIELD_NAME_KEY, field);
                    control = (Coordinates) getLayoutInflater().inflate(R.layout.formtemplate_coordinates, layout, false);
                    if (control != null && x != null)
                        ((Coordinates) control).setValue(x);
                    break;

                //TODO: add controls
                //button
                //group
                //orientation
                //tabs
                //compass

                default:
                    break;
            }

            addToLayout(control, element, fields, savedState, featureCursor, layout);
        }

        if (null != featureCursor) {
            featureCursor.close();
        }

        layout.requestLayout();
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

    protected void addToLayout(IFormControl control, JSONObject element, List<Field> fields, Bundle savedState, Cursor featureCursor, LinearLayout layout)
            throws JSONException {
        if (null != control) {
            appendData(control, element);

            control.init(element, fields, savedState, featureCursor, mSharedPreferences);
            control.addToLayout(layout);

            String fieldName = control.getFieldName();
            if (null != fieldName)
                mFields.put(fieldName, control);
        }
    }

    private void appendData(IFormControl control, JSONObject element) throws JSONException {
        if (mLayer instanceof NGWVectorLayer)
            element.put(SyncStateContract.Columns.ACCOUNT_NAME, ((NGWVectorLayer) mLayer).getAccountName());

        if (control instanceof Counter && mTable != null && mRow != -1) {
            JSONObject attrs = element.getJSONObject(JSON_ATTRIBUTES_KEY);
            if (!attrs.isNull(Counter.PREFIX_LIST)) {
                String prefix = attrs.getString(Counter.PREFIX_LIST);
                prefix = mTable.get(prefix).get(mRow);
                attrs.put(Counter.PREFIX, prefix);
            }

            if (!attrs.isNull(Counter.SUFFIX_LIST)) {
                String suffix = attrs.getString(Counter.SUFFIX_LIST);
                suffix = mTable.get(suffix).get(mRow);
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

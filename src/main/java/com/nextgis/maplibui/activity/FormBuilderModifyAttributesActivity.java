/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2020 NextGIS, info@nextgis.com
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

import android.accounts.AccountManager;
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
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.keenfin.easypicker.PhotoPicker;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoMultiPolygon;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Connections;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IControl;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.control.PhotoGallery;
import com.nextgis.maplibui.dialog.SelectNGWResourceDialog;
import com.nextgis.maplibui.formcontrol.AutoTextEdit;
import com.nextgis.maplibui.formcontrol.Averaging;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.JSON_TYPE_KEY;
import static com.nextgis.maplib.util.NetworkUtil.getUserAgent;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ALBUM_ELEMENTS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_AVERAGING_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_CAPTION_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_CHECKBOX_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_COMBOBOX_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_COORDINATES_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_COUNTER_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DATE_TIME_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DEFAULT_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DISTANCE_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DOUBLE_COMBOBOX_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ELEMENTS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_KEY_LIST_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_KEY_LIST_SAVED_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_LISTS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_PAGES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_PHOTO_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_PORTRAIT_ELEMENTS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_RADIO_GROUP_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_SIGN_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_SPACE_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_SPLIT_COMBOBOX_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TABS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TEXT_EDIT_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TEXT_LABEL_VALUE;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TRANSLATIONS_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_FORM_PATH;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_IS_FORM_JSONTYPE;
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
    private Map<String, Map<String, String>> mTranslations;
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

                        Log.d("FormBuilder", "metastring: " + metaString);
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
        boolean isJsonTypeForm = extras.getBoolean(KEY_IS_FORM_JSONTYPE, true);
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

            if (isJsonTypeForm) {
                JSONObject jsonObject = new JSONObject(formString);
                JSONObject forbuilder = jsonObject.optJSONObject("formbuilder_form");
                JSONObject value = forbuilder.optJSONObject("value");
                JSONArray tabs = value.getJSONArray("items");

                if (tabs.length() > 0)
                    fillTabControls(layout, savedState, tabs, isJsonTypeForm);
            }
            else {
                if (json instanceof JSONArray) {
                    JSONArray elements = new JSONArray(formString);
                    if (elements.length() > 0)
                        fillTabControls(layout, savedState, elements, isJsonTypeForm);
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
                            fillTabControls(layout, savedState, elements, isJsonTypeForm);
                        }
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
        mTranslations = new HashMap<>();
        boolean isJsonTypeForm = extras.getBoolean(KEY_IS_FORM_JSONTYPE, true);
        if (isJsonTypeForm) {
            mColumn = null; //reset mColumn,if key_list abpsent
            fillControls(layout, savedState);
        } else {
            mMeta = (File) extras.getSerializable(KEY_META_PATH);
            if (mMeta != null && mMeta.exists()) {
                try {
                    String metaString = FileUtil.readFromFile(mMeta);
                    final JSONObject metaJson = new JSONObject(metaString);
                    if (metaJson.has(JSON_LISTS_KEY) && !metaJson.isNull(JSON_LISTS_KEY)) {
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

                    if (metaJson.has(JSON_KEY_LIST_KEY) && !metaJson.isNull(JSON_KEY_LIST_KEY)) {
                        mColumn = metaJson.getString(JSON_KEY_LIST_KEY);
                        List<String> columnList = mTable.get(mColumn);
                        if (mColumn == null || columnList == null || columnList.isEmpty()) {
                            Toast.makeText(this, "R.string.error_invalid_column", Toast.LENGTH_SHORT).show();
                            mColumn = null; // Сбрасываем mColumn, чтобы избежать ошибок
                            fillControls(layout, savedState);
                            return;
                        }

                        if (metaJson.has(JSON_KEY_LIST_SAVED_KEY)) {
                            mRow = metaJson.getInt(JSON_KEY_LIST_SAVED_KEY);
                            if (mRow >= columnList.size() || mRow < 0) {
                                Toast.makeText(this, "R.string.error_invalid_row", Toast.LENGTH_SHORT).show();
                                mRow = -1;
                            }
                        } else {
                            askForRow(new OnAskRowListener() {
                                @Override
                                public void onRowChosen() {
                                    if (mRow == -1)
                                        finish();
                                    else {
                                        try {
                                            metaJson.put(JSON_KEY_LIST_SAVED_KEY, mRow);
                                            FileUtil.writeToFile(mMeta, metaJson.toString());
                                        } catch (JSONException | IOException ignored) {
                                        }
                                        fillControls(layout, savedState);
                                    }
                                }
                            });
                            return;
                        }
                    } else {
                        mColumn = null; //reset mColumn,if key_list abpsent
                    }

                    if (metaJson.has(JSON_TRANSLATIONS_KEY) && !metaJson.isNull(JSON_TRANSLATIONS_KEY)) {
                        JSONArray translations = metaJson.getJSONArray(JSON_TRANSLATIONS_KEY);
                        if (translations != null) {
                            for (int i = 0; i < translations.length(); i++) {
                                JSONObject translation = translations.getJSONObject(i);
                                String translationKey = null;
                                Map<String, String> mappings = new HashMap<>();
                                for (Iterator<String> iter = translation.keys(); iter.hasNext(); ) {
                                    String key = iter.next();
                                    String value = translation.getString(key);
                                    if (key.equals("key")) {
                                        translationKey = value;
                                    } else {
                                        mappings.put(key, value);
                                    }
                                }
                                if (translationKey != null)
                                    mTranslations.put(translationKey, mappings);
                            }
                        }
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.error_form_create, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            fillControls(layout, savedState);
        }
    }

    private void askForRow(final OnAskRowListener listener) {

        Log.d("FormBuilder", "mColumn: " + mColumn + ", mTable: " + mTable.toString());

        List<String> items = mTable.get(mColumn);
        if (items == null || items.isEmpty()) {
            Toast.makeText(this, "R.string.error_empty_list", Toast.LENGTH_SHORT).show();
            mRow = -1;
            listener.onRowChosen();
            return;
        }

        Log.d("FormBuilder", "Items for spinner: " + items.toString());
        ArrayAdapter<String> spinnerMenu = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        final Spinner spinner = (Spinner) View.inflate(this, R.layout.table_select_row, null);
        spinner.setAdapter(spinnerMenu);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_row).setView(spinner)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mRow = spinner.getSelectedItemPosition();
                        Log.d("FormBuilder", "Selected row: " + mRow);
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

    private void setViewOnly(String author) {
        String fName = mPreferences.getString(PREF_FIRST_NAME, "");
        String lName = mPreferences.getString(PREF_LAST_NAME, "");
        String uName = mPreferences.getString(PREF_USERNAME, "");
        String name = TextEdit.formUserName(fName, lName, uName);
        mIsViewOnly = mIsViewOnly || !author.equals(name);
    }

    @Override
    protected Cursor getFeatureCursor() {
        Cursor featureCursor = super.getFeatureCursor();
        if (featureCursor != null) {
            Feature feature = mLayer.getFeature(mFeatureId);
            List<Field> fields = feature.getFields();
//            for (Field field : fields) { comment - looks like it old code
////                if (field.getAlias().toLowerCase().equals("author")) {
////                    String author = feature.getFieldValueAsString(field.getName());
////                    setViewOnly(author);
////                    return featureCursor;
////                }
//            }
//            int id = featureCursor.getColumnIndex("author");
//            if (id >= 0) {
//                try {
//                    String author = featureCursor.getString(id);
//                    setViewOnly(author);
//                } catch (Exception ignored) {}
//            }
        }
        return featureCursor;
    }

    protected void fillTabControls(
            LinearLayout layout,
            Bundle savedState,
            JSONArray elements,
            boolean isJsonTypeForm)
            throws JSONException {
        Cursor featureCursor = getFeatureCursor();
        List<Field> fields = mLayer.getFields();
        if (isJsonTypeForm){
            for (int i = 0; i < elements.length(); i++) {
                IFormControl control;
                JSONObject element = convertToLegacyElement(elements.getJSONObject(i)); //convertToLegacyElement - convert to old format - change to new after some time
                String type = element.optString(JSON_TYPE_KEY);

                if (type.equals(JSON_COORDINATES_VALUE)) {
                    JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    String fieldY = attributes.optString(JSON_FIELD_NAME_KEY + "_lat");
                    attributes.put(JSON_FIELD_NAME_KEY, fieldY);
                    element.put(JSON_TYPE_KEY, type + "_lat");
                    control = getControl(this, element, mLayer, mFeatureId, mGeometry, mIsViewOnly, this);
                    addToLayout(control, element, fields, savedState, featureCursor, layout, this);

                    attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    String fieldX = attributes.optString(JSON_FIELD_NAME_KEY + "_long");
                    attributes.put(JSON_FIELD_NAME_KEY, fieldX);
                    element.put(JSON_TYPE_KEY, type + "_lon");
                }

                control = getControl(this, element, mLayer, mFeatureId, mGeometry, mIsViewOnly, this);
                if (type.equals(JSON_TABS_KEY))
                    ((Tabs) control).init(mLayer, mFeatureId, mGeometry, mTable, mRow, mSharedPreferences,
                            mPreferences, getSupportFragmentManager(), mIsViewOnly);

                addToLayout(control, element, fields, savedState, featureCursor, layout, this);
            }
        }

            // old parse

//            for (int i = 0; i < elements.length(); i++) {
//                IFormControl control;
//                JSONObject element = elements.getJSONObject(i);
//                String type = element.optString(JSON_TYPE_KEY);
//                if (type.equals(JSON_COORDINATES_VALUE)) {
//                    JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
//                    String fieldY = attributes.optString(JSON_FIELD_NAME_KEY + "_lat");
//                    attributes.put(JSON_FIELD_NAME_KEY, fieldY);
//                    element.put(JSON_TYPE_KEY, type + "_lat");
//                    control = getControl(this, element, mLayer, mFeatureId, mGeometry, mIsViewOnly, this);
//                    addToLayout(control, element, fields, savedState, featureCursor, layout, this);
//
//                    attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
//                    String fieldX = attributes.optString(JSON_FIELD_NAME_KEY + "_long");
//                    attributes.put(JSON_FIELD_NAME_KEY, fieldX);
//                    element.put(JSON_TYPE_KEY, type + "_lon");
//                }
//
//                control = getControl(this, element, mLayer, mFeatureId, mGeometry, mIsViewOnly, this);
//                if (type.equals(JSON_TABS_KEY))
//                    ((Tabs) control).init(mLayer, mFeatureId, mGeometry, mTable, mRow, mSharedPreferences,
//                            mPreferences, getSupportFragmentManager(), mIsViewOnly);
//
//                addToLayout(control, element, fields, savedState, featureCursor, layout, this);
//            }
        //}
        else {
            for (int i = 0; i < elements.length(); i++) {
                IFormControl control;
                JSONObject element = elements.getJSONObject(i);
                String type = element.optString(JSON_TYPE_KEY);
                if (type.equals(JSON_COORDINATES_VALUE)) {
                    JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    String fieldY = attributes.optString(JSON_FIELD_NAME_KEY + "_lat");
                    attributes.put(JSON_FIELD_NAME_KEY, fieldY);
                    element.put(JSON_TYPE_KEY, type + "_lat");
                    control = getControl(this, element, mLayer, mFeatureId, mGeometry, mIsViewOnly, this);
                    addToLayout(control, element, fields, savedState, featureCursor, layout, this);

                    attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
                    String fieldX = attributes.optString(JSON_FIELD_NAME_KEY + "_long");
                    attributes.put(JSON_FIELD_NAME_KEY, fieldX);
                    element.put(JSON_TYPE_KEY, type + "_lon");
                }

                control = getControl(this, element, mLayer, mFeatureId, mGeometry, mIsViewOnly, this);
                if (type.equals(JSON_TABS_KEY))
                    ((Tabs) control).init(mLayer, mFeatureId, mGeometry, mTable, mRow, mSharedPreferences,
                            mPreferences, getSupportFragmentManager(), mIsViewOnly);
                addToLayout(control, element, fields, savedState, featureCursor, layout, this);
            }
        }
        if (null != featureCursor) {
            featureCursor.close();
        }
        layout.requestLayout();
    }


    private static final Map<String, String> NEW_TO_LEGACY_TYPE = new HashMap<>();
    static {
        // map  new - old form elements naming
        NEW_TO_LEGACY_TYPE.put("label",         "text_label");
        NEW_TO_LEGACY_TYPE.put("tabs",          "tabs");
        NEW_TO_LEGACY_TYPE.put("spacer",        "space");
        NEW_TO_LEGACY_TYPE.put("textbox",       "text_edit");
        NEW_TO_LEGACY_TYPE.put("checkbox",      "checkbox");
        NEW_TO_LEGACY_TYPE.put("system",        "text_edit");
        NEW_TO_LEGACY_TYPE.put("datetime",      "date_time");
        NEW_TO_LEGACY_TYPE.put("radio",         "radio_group");
        NEW_TO_LEGACY_TYPE.put("dropdown",      "combobox");
        NEW_TO_LEGACY_TYPE.put("dropdown_dual", "split_combobox");
        NEW_TO_LEGACY_TYPE.put("cascade",       "double_combobox");
        NEW_TO_LEGACY_TYPE.put("coordinates",   "coordinates");
        NEW_TO_LEGACY_TYPE.put("distance",      "distance");
        NEW_TO_LEGACY_TYPE.put("average",       "average_counter");
        NEW_TO_LEGACY_TYPE.put("photo",         "photo");
    }

    /** convert new format to ngfp like . */
    private static JSONObject convertToLegacyElement(JSONObject src) throws JSONException {

//        Log.e("JSSON", src.toString());
        if (src.has(JSON_ATTRIBUTES_KEY)) {
//            Log.e("JSSON", "ALready old");
            return src;
        }

        String newType = src.optString(JSON_TYPE_KEY);
        String legacyType = NEW_TO_LEGACY_TYPE.get(newType);
        if (legacyType == null) {
            Log.e("JSSON", "Unknown type !!!!!!");
            return src; // неизвестный тип — не трогаем
        }

        JSONObject a = new JSONObject();
        if ("tabs".equals(newType)) {
            JSONArray pages = new JSONArray();
            JSONArray tabs = src.optJSONArray("tabs");

            for (int i = 0; tabs != null && i < tabs.length(); i++) {
                JSONObject tab = tabs.getJSONObject(i);

                JSONObject page = new JSONObject();
                page.put(JSON_CAPTION_KEY, tab.optString("title"));
                if (tab.optBoolean("active", false)) {
                    page.put(JSON_DEFAULT_KEY, true);
                }

                JSONArray elements = new JSONArray();
                JSONArray items = tab.optJSONArray("items");
                for (int j = 0; items != null && j < items.length(); j++) {
                    elements.put(convertToLegacyElement(items.getJSONObject(j))); // рекурсия
                }
                page.put(JSON_ELEMENTS_KEY, elements);
                pages.put(page);
            }

            JSONObject result = new JSONObject();
            result.put(JSON_TYPE_KEY, legacyType);                 // "tabs"
            result.put(JSON_ATTRIBUTES_KEY, new JSONObject());     // "attributes": {}
            result.put(JSON_PAGES_KEY, pages);
            return result;
        }

        switch (newType) {
            case "label":
                a.put("text", src.optString("label"));
                break;

            case "spacer":
                break;

//            case "tabs": tabs in root - process completely

            case "textbox":
                a.put("field", src.optString("field"));
                a.put("last", src.optBoolean("remember", false));
                a.put("text", src.optString("initial", ""));
                a.put("max_string_count", src.optInt("max_lines", 1));
                a.put("ngid_login", false);
                a.put("ngw_login", false);
                a.put("only_figures", src.optBoolean("only_figures", false));
                break;

            case "system": {
                String system = src.optString("system");
                a.put("field", src.optString("field"));
                a.put("last", false);
                a.put("text", "");
                a.put("max_string_count", 1);
                a.put("ngid_login", "ngid_username".equals(system));
                a.put("ngw_login", "ngw_username".equals(system));
                a.put("only_figures", false);
                break;
            }

            case "checkbox":
                a.put("field", src.optString("field"));
                a.put("last", src.optBoolean("remember", false));
                a.put("init_value", src.optBoolean("initial", false));
                a.put("text", src.optString("label"));
                break;

            case "datetime": {
                a.put("field", src.optString("field"));
                a.put("last", src.optBoolean("remember", false));
                int dateType;
                switch (src.optString("datetime", "date")) {
                    case "time":     dateType = 1; break;
                    case "datetime": dateType = 2; break;
                    default:         dateType = 0; // "date"
                }
                a.put("date_type", dateType);
                // old "datetime":
                break;
            }

            case "radio":
                a.put("field", src.optString("field"));
                a.put("last", src.optBoolean("remember", false));
                a.put("values", convertOptions(src.optJSONArray("options"), false, null));
                break;

            case "dropdown":
                a.put("field", src.optString("field"));
                a.put("last", src.optBoolean("remember", false));
                a.put("input_search", src.optBoolean("search", false));
                a.put("allow_adding_values", src.optBoolean("free_input", false));
                a.put("values", convertOptions(src.optJSONArray("options"), false, null));
                break;

            case "dropdown_dual": {
                a.put("field", src.optString("field"));
                a.put("last", src.optBoolean("remember", false));
                a.put("label1", src.optString("label_first"));
                a.put("label2", src.optString("label_second"));
                JSONArray values = new JSONArray();
                JSONArray options = src.optJSONArray("options");
                for (int i = 0; options != null && i < options.length(); i++) {
                    JSONObject o = options.getJSONObject(i);
                    JSONObject v = new JSONObject();
                    v.put("name", o.optString("value"));
                    v.put("alias", o.optString("first"));
                    v.put("alias2", o.optString("second"));
                    v.put("default", o.optBoolean("initial", false));
                    values.put(v);
                }
                a.put("values", values);
                break;
            }

            case "cascade":
                a.put("field_level1", src.optString("field_primary"));
                a.put("field_level2", src.optString("field_secondary"));
                a.put("last", src.optBoolean("remember", false));
                a.put("values", convertOptions(src.optJSONArray("options"), true, "items"));
                break;

            case "coordinates":
                a.put("field_lat", src.optString("field_lat"));
                a.put("field_long", src.optString("field_lon"));
                a.put("hidden", src.optBoolean("hidden", false));
                a.put("crs", 0);
                a.put("format", 0);
                break;

            case "distance":
                a.put("field", src.optString("field"));
                break;

            case "average":
                a.put("field", src.optString("field"));
                a.put("num_values", src.optInt("samples", 2));
                break;

            case "photo":
                a.put("gallery_size", src.optInt("max_count", 1));
                a.put("comment", src.optString("comment", ""));
                break;
        }

        JSONObject result = new JSONObject();
        result.put(JSON_TYPE_KEY, legacyType);
        result.put(JSON_ATTRIBUTES_KEY, a);

        Log.e("JSSON", "result = " + result.toString());
        return result;
    }

    private static JSONArray convertOptions(JSONArray options, boolean nested, String childKey)
            throws JSONException {
        JSONArray values = new JSONArray();
        for (int i = 0; options != null && i < options.length(); i++) {
            JSONObject o = options.getJSONObject(i);
            JSONObject v = new JSONObject();
            v.put("name", o.optString("value"));
            v.put("alias", o.optString("label"));
            v.put("default", o.optBoolean("initial", false));
            if (nested) {
                v.put("values", convertOptions(o.optJSONArray(childKey), false, null));
            }
            values.put(v);
        }
        return values;
    }


    public static IFormControl getControl(Context context, JSONObject element, VectorLayer layer, long feature,
                                          GeoGeometry geometry, boolean isViewOnly, final ModifyAttributesActivity modifyAttributesActivity) throws JSONException {

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

            case JSON_AVERAGING_VALUE:
                control = (Averaging) View.inflate(context, R.layout.formtemplate_averaging, null);
                break;

            case JSON_PHOTO_VALUE:
                if (isViewOnly)
                    control = (PhotoGallery) View.inflate(context, R.layout.formtemplate_photo_disabled, null);
                else
                    control = (PhotoGallery) View.inflate(context, R.layout.formtemplate_photo, null);

                AccountManager accountManager = AccountManager.get(context);
                Connections connections = SelectNGWResourceDialog.fillConnections(context, accountManager);
                Connection found = null;
                if (layer instanceof NGWVectorLayer) {
                    for (int i = 0; i < connections.getChildrenCount(); i++) {
                        if (connections.getChild(i).getName().equals((((NGWVectorLayer) layer).getAccountName()))) {
                            found = (Connection) connections.getChild(i);
                        }
                    }
                }

                ((PhotoGallery) control).init(layer, feature);

                if (found != null)
                    ((PhotoPicker) control).setLoginPass(found.getLogin(), found.getPassword());

                modifyAttributesActivity.photoPickerWeakReference = new WeakReference<>((PhotoPicker) control);

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
                if (control != null) {
                    if (x != null)
                        ((Coordinates) control).setValue(x);
                    if (!(geometry instanceof GeoPoint) && !(geometry instanceof GeoMultiPoint))
                        ((Coordinates) control).setVisibility(View.GONE);
                }
                break;

            case JSON_COORDINATES_VALUE + "_lat":
                Double y = getCoordinate(geometry, true);
                control = (Coordinates) View.inflate(context, R.layout.formtemplate_coordinates, null);
                if (control != null) {
                    ((Coordinates) control).setIsLat();
                    if (y != null)
                        ((Coordinates) control).setValue(y);
                    if (!(geometry instanceof GeoPoint) && !(geometry instanceof GeoMultiPoint)
                        &&!(geometry instanceof GeoPolygon)
                            &&!(geometry instanceof GeoMultiPolygon))
                        ((Coordinates) control).setVisibility(View.GONE);
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
        double x, y;
        x = y = Double.NaN;
        GeoPoint point = null;
        if (geometry instanceof GeoPoint) {
            point = (GeoPoint) geometry.copy();
        }
        if (geometry instanceof GeoMultiPoint) {
            point = ((GeoMultiPoint) geometry.copy()).get(0);
        }

        if (geometry instanceof GeoPolygon) {
            GeoEnvelope envelope = geometry.getEnvelope();
            point = (GeoPoint) (envelope.getCenter().copy());
        }

        if (geometry instanceof GeoMultiPolygon) {
            GeoPolygon polygon = ((GeoMultiPolygon)geometry).get(0);
            GeoEnvelope envelope = polygon.getEnvelope();
            point = (GeoPoint) (envelope.getCenter().copy());
        }

        if (point != null) {
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
                               Cursor featureCursor, LinearLayout layout, final ModifyAttributesActivity modifyAttributesActivity) throws JSONException {
        if (null != control) {
            appendData(mLayer, mPreferences, mTable, mRow, control, element);

            control.init(element, fields, savedState, featureCursor, mSharedPreferences, mTranslations, modifyAttributesActivity);
            if (control instanceof PhotoPicker)
                ((PhotoPicker)control).setUserAgent( getUserAgent(Constants.MAPLIB_USER_AGENT_PART));
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

        if (preferences != null) {
            element.put(PREF_FIRST_NAME, preferences.getString(PREF_FIRST_NAME, ""));
            element.put(PREF_LAST_NAME, preferences.getString(PREF_LAST_NAME, ""));
            element.put(PREF_USERNAME, preferences.getString(PREF_USERNAME, ""));
        }

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

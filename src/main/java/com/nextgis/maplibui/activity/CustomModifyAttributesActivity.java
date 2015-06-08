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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.controlui.ComboboxJsonControl;
import com.nextgis.maplibui.controlui.DateTimeJsonControl;
import com.nextgis.maplibui.controlui.DoubleComboboxJsonControl;
import com.nextgis.maplibui.controlui.DoubleComboboxValue;
import com.nextgis.maplibui.controlui.IControl;
import com.nextgis.maplibui.controlui.RadioGroupJsonControl;
import com.nextgis.maplibui.controlui.TextEditJsonControl;
import com.nextgis.maplibui.controlui.TextLabelJsonControl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
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
    protected Map<String, Map<String, String>> mKeyValuesForField;
    protected Map<View, String>                mDoubleComboFirstKeys;
    protected Map<View, Integer>               mDateTimePickerType;


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
        Cursor featureCursor = null;

        if (mFeatureId != NOT_FOUND) {
            featureCursor = mLayer.query(null, FIELD_ID + " = " + mFeatureId, null, null);
            if (!featureCursor.moveToFirst()) {
                featureCursor = null;
            }
        }

        List<Field> fields = mLayer.getFields();

        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            String type = element.getString(JSON_TYPE_KEY);

            IControl control = null;

            switch (type) {

                case JSON_TEXT_LABEL_VALUE:
                    control = new TextLabelJsonControl(this, element);
                    break;

                case JSON_TEXT_EDIT_VALUE:
                    control = new TextEditJsonControl(this, element, fields, featureCursor);
                    break;

                case JSON_DATE_TIME_VALUE:
                    control = new DateTimeJsonControl(this, element, fields, featureCursor);
                    break;

                case JSON_RADIO_GROUP_VALUE:
                    control = new RadioGroupJsonControl(this, element, fields, featureCursor);
                    break;

                case JSON_COMBOBOX_VALUE:
                    control = new ComboboxJsonControl(this, element, fields, featureCursor);
                    break;

                case JSON_DOUBLE_COMBOBOX_VALUE:
                    control = new DoubleComboboxJsonControl(this, element, fields, featureCursor);
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

            if (null != control) {
                control.addToLayout(layout);
                String fieldName = control.getFieldName();

                if (null != fieldName) {
                    mFields.put(control.getFieldName(), control);
                }
            }
        }

        if (null != featureCursor) {
            featureCursor.close();
        }
    }


    protected Object putFieldValue(
            ContentValues values,
            Field field)
    {
        Object value = super.putFieldValue(values, field);

        if (null != value) {

            if (value instanceof DoubleComboboxValue) {
                DoubleComboboxValue dcValue = (DoubleComboboxValue) value;
                values.put(dcValue.mFieldName, dcValue.mValue);
                values.put(dcValue.mSubFieldName, dcValue.mSubValue);
            }
        }

        return value;
    }
}

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

package com.nextgis.maplibui.controlui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.text.InputType;
import android.util.AttributeSet;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.util.GeoConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplibui.util.ConstantsUI.*;


@SuppressLint("ViewConstructor")
public class TextEditJsonControl
        extends TextEditControl
{
    protected boolean mIsShowLast;

    public TextEditJsonControl(Context context, AttributeSet attrs, int defStyleAttr, JSONObject element,
                               List<Field> fields,
                               Cursor featureCursor) throws JSONException {
        super(context, attrs, defStyleAttr);

        init(element, fields, featureCursor);
    }

    public TextEditJsonControl(
            Context context,
            JSONObject element,
            List<Field> fields,
            Cursor featureCursor) throws JSONException

    {
        super(context);

        init(element, fields, featureCursor);
    }

    protected void init(JSONObject element,
                        List<Field> fields,
                        Cursor featureCursor) throws JSONException{
        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

        mFieldName = attributes.getString(JSON_FIELD_NAME_KEY);

        boolean isEnabled = false;
        int fieldType = NOT_FOUND;
        for (Field field : fields) {
            if (field.getName().equals(mFieldName)) {
                isEnabled = true;
                fieldType = field.getType();
                break;
            }
        }
        setEnabled(isEnabled);

        mIsShowLast = false;
        if (attributes.has(JSON_SHOW_LAST_KEY) && !attributes.isNull(
                JSON_SHOW_LAST_KEY)) {
            mIsShowLast = attributes.getBoolean(JSON_SHOW_LAST_KEY);
        }

        String lastValue = null;
        if (mIsShowLast) {
            if (null != featureCursor) {
                int column = featureCursor.getColumnIndex(mFieldName);
                if (column >= 0) {
                    lastValue = featureCursor.getString(column);
                }
            }
        }

        if (mIsShowLast && null != lastValue) {
            setText(lastValue);
        } else {
            if (attributes.has(JSON_TEXT_KEY) && !attributes.isNull(
                    JSON_TEXT_KEY)) {
                String defaultValue = attributes.getString(JSON_TEXT_KEY);
                setText(defaultValue);
            }
        }


        //let's create control
        int maxLines = attributes.getInt(JSON_MAX_STRING_COUNT_KEY);
        if (maxLines < 2) {
            setSingleLine(true);
        } else {
            setMaxLines(maxLines);
        }


        boolean onlyFigures = attributes.getBoolean(JSON_ONLY_FIGURES_KEY);
        if (onlyFigures) {

            //check field type
            switch (fieldType) {

                case GeoConstants.FTInteger:
                    setInputType(InputType.TYPE_CLASS_NUMBER);
                    break;

                case GeoConstants.FTReal:
                    setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    break;
            }
        }
    }

}

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

package com.nextgis.maplibui.controlui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static com.nextgis.maplibui.util.ConstantsUI.*;


@SuppressLint("ViewConstructor")
public class DateTimeJsonControl
        extends DateTimeControl
{
    protected boolean mIsShowLast;


    public DateTimeJsonControl(
            Context context,
            JSONObject element,
            List<Field> fields,
            Cursor featureCursor)
            throws JSONException
    {
        super(context, null, R.attr.DatePickSpinnerStyle);

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

        mFieldName = attributes.getString(JSON_FIELD_NAME_KEY);

        boolean isEnabled = false;
        for (Field field : fields) {
            if (field.getName().equals(mFieldName)) {
                isEnabled = true;
                break;
            }
        }
        setEnabled(isEnabled);

        mIsShowLast = false;
        if (attributes.has(JSON_SHOW_LAST_KEY) && !attributes.isNull(
                JSON_SHOW_LAST_KEY)) {
            mIsShowLast = attributes.getBoolean(JSON_SHOW_LAST_KEY);
        }

        int picker_type = DATETIME;
        if (attributes.has(JSON_DATE_TYPE_KEY)) {
            picker_type = attributes.getInt(JSON_DATE_TYPE_KEY);
        }

        switch (picker_type) {
            case DATE:
                mDateFormat = (SimpleDateFormat) DateFormat.getDateInstance();
                break;
            case TIME:
                mDateFormat = (SimpleDateFormat) DateFormat.getTimeInstance();
                break;
            case DATETIME:
                mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
                break;
            default:
                picker_type = DATETIME;
                mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        }


        String lastValue = null;
        if (mIsShowLast) {
            if (null != featureCursor) {
                int column = featureCursor.getColumnIndex(mFieldName);
                if (column >= 0) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(featureCursor.getLong(column));
                    lastValue = mDateFormat.format(calendar.getTime());
                }
            }
        }

        if (mIsShowLast && null != lastValue) {
            setText(lastValue);
        } else {
            if (attributes.has(JSON_TEXT_KEY) && !attributes.isNull(
                    JSON_TEXT_KEY)) {
                String defaultValue = attributes.getString(JSON_TEXT_KEY);

                // TODO: check format of defaultValue

                setText(defaultValue);
            }
        }

        setSingleLine(true);
        setFocusable(false);
        setOnClickListener(getDateUpdateWatcher(picker_type));

        String pattern = mDateFormat.toLocalizedPattern();
        setHint(pattern);
    }
}

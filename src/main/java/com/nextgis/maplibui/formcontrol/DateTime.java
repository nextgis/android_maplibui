/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016, 2020-2021 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.formcontrol;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.nextgis.maplib.util.GeoConstants.FTDate;
import static com.nextgis.maplib.util.GeoConstants.FTDateTime;
import static com.nextgis.maplib.util.GeoConstants.FTTime;
import static com.nextgis.maplibui.control.DateTime.TYPE_DATE;
import static com.nextgis.maplibui.control.DateTime.TYPE_DATE_TIME;
import static com.nextgis.maplibui.control.DateTime.TYPE_TIME;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DATE_TYPE_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TEXT_KEY;


public class DateTime
        extends AppCompatTextView
        implements IFormControl
{
    protected int mPickerType = FTDateTime;

    int dateSelectorType= TYPE_DATE_TIME;
    int myDay, myMonth, myYear, myHour, myMinute;

    protected boolean          mIsShowLast;
    protected String           mFieldName;
    protected SimpleDateFormat mDateFormat;

    protected Calendar mCalendar = Calendar.getInstance();
    protected Long mValue = Calendar.getInstance().getTimeInMillis();

    android.app.TimePickerDialog.OnTimeSetListener onTimeSetListener = new android.app.TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Calendar newCalendar = Calendar.getInstance();
            newCalendar.set(Calendar.YEAR, myYear);
            newCalendar.set(Calendar.MONTH, myMonth);
            newCalendar.set(Calendar.DAY_OF_MONTH, myDay);
            newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            newCalendar.set(Calendar.MINUTE, minute);
            newCalendar.set(Calendar.SECOND, 0);

            mValue = newCalendar.getTimeInMillis();
            mCalendar.setTimeInMillis(newCalendar.getTimeInMillis());
            DateTime.this.setText(mDateFormat.format(mCalendar.getTime()));

        }
    } ;

    DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

            int hour = -1, minute = 0;

            myYear = year;
            myDay = dayOfMonth;
            myMonth = month;
            if (dateSelectorType == TYPE_DATE_TIME) {
                if (myHour != -1 ){
                    hour = myHour;
                    minute=myMinute;
                } else {
                    Calendar c = Calendar.getInstance();
                    hour = c.get(Calendar.HOUR_OF_DAY);
                    minute = c.get(Calendar.MINUTE);
                }
                android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(getContext(),
                        onTimeSetListener, hour, minute,
                        true);
                timePickerDialog.show();
            } else { /// только дата -
                Calendar newCalendar = Calendar.getInstance();
                newCalendar.set(Calendar.YEAR, myYear);
                newCalendar.set(Calendar.MONTH, myMonth);
                newCalendar.set(Calendar.DAY_OF_MONTH, myDay);

                mValue = newCalendar.getTimeInMillis();
                mCalendar.setTimeInMillis(newCalendar.getTimeInMillis());

                DateTime.this.setText(mDateFormat.format(mCalendar.getTime()));

            }
        }
    };

    Calendar calendar = Calendar.getInstance();
    DatePickerDialog datePickerDialog = new DatePickerDialog(
            getContext(), onDateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH));

    public DateTime(Context context)
    {
        super(context);
    }


    public DateTime(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
    }

    public DateTime(
            Context context,
            AttributeSet attrs,
            int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public void init(
            JSONObject element,
            List<Field> fields,
            Bundle savedState,
            Cursor featureCursor,
            SharedPreferences preferences,
            Map<String, Map<String, String>> translations)
            throws JSONException
    {
        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = ControlHelper.getFieldName(attributes.getString(JSON_FIELD_NAME_KEY));
        mIsShowLast = ControlHelper.isSaveLastValue(attributes);

        if (!ControlHelper.isEnabled(fields, mFieldName)) {
            setEnabled(false);
            getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
        }

        if (attributes.has(JSON_DATE_TYPE_KEY)) {
            mPickerType = attributes.getInt(JSON_DATE_TYPE_KEY);
        }

        switch (mPickerType) {

            case 0:
                mDateFormat = (SimpleDateFormat) DateFormat.getDateInstance();
                mPickerType = GeoConstants.FTDate;
                break;

            case 1:
                mDateFormat = (SimpleDateFormat) DateFormat.getTimeInstance();
                mPickerType = GeoConstants.FTTime;
                break;

            default:
                mPickerType = FTDateTime;
            case 2:
                mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
                mPickerType = GeoConstants.FTDateTime;
                break;
        }

        long timestamp = System.currentTimeMillis();
        if (ControlHelper.hasKey(savedState, mFieldName)) {
            timestamp = savedState.getLong(ControlHelper.getSavedStateKey(mFieldName));
            mValue = timestamp;
        } else if (null != featureCursor) { // feature exists
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0) {
                timestamp = featureCursor.getLong(column);
                mValue = timestamp;
            }
        } else {    // new feature
            if (attributes.has(JSON_TEXT_KEY) && !TextUtils.isEmpty(
                    attributes.getString(JSON_TEXT_KEY).trim())) {
                String defaultValue = attributes.getString(JSON_TEXT_KEY);
                timestamp = parseDateTime(defaultValue, mPickerType);
            }

            if (mIsShowLast) { timestamp = preferences.getLong(mFieldName, timestamp); }
        }

        mCalendar.setTimeInMillis(timestamp);
        setText(mDateFormat.format(mCalendar.getTime()));
        setSingleLine(true);
        setFocusable(false);
        setOnClickListener(getDateUpdateWatcher(mPickerType));

        String pattern = mDateFormat.toLocalizedPattern();
        setHint(pattern);
    }


    protected long parseDateTime(
            String value,
            int type)
    {
        long result = 0;
        SimpleDateFormat sdf = null;

        switch (type) {
            case FTDate:
                sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                break;
            case FTTime:
                sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                break;
            case FTDateTime:
                sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                break;
        }

        if (sdf != null) {
            try {
                result = sdf.parse(value).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return result;
    }


    @Override
    public void saveLastValue(SharedPreferences preferences)
    {
        preferences.edit().putLong(mFieldName, (Long) getValue()).commit();
    }

    @Override
    public boolean isShowLast()
    {
        return mIsShowLast;
    }

    protected View.OnClickListener getDateUpdateWatcher(final int pickerType)
    {
        return new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (null != mValue) {
                    mCalendar.setTimeInMillis(mValue);
                } else {
                    mCalendar.setTime(new Date());
                }

                switch (pickerType) {
                    case GeoConstants.FTDate:
                        dateSelectorType = TYPE_DATE;
                        break;
                    case GeoConstants.FTTime:
                        dateSelectorType = TYPE_TIME;
                        break;
                    case GeoConstants.FTDateTime:
                        dateSelectorType = TYPE_DATE_TIME;
                        break;
                }

                if (dateSelectorType == TYPE_DATE_TIME ||
                        dateSelectorType == TYPE_DATE) {
                    int day = 0, month = 0, year = 0;
                    year = mCalendar.get(Calendar.YEAR);
                    month = mCalendar.get(Calendar.MONTH);
                    day = mCalendar.get(Calendar.DAY_OF_MONTH);
                    myHour = mCalendar.get(Calendar.HOUR_OF_DAY);
                    myMinute = mCalendar.get(Calendar.MINUTE);
                    datePickerDialog.getDatePicker().init(year, month, day, null);
                    datePickerDialog.show();
                } else {
                    int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
                    int minute = mCalendar.get(Calendar.MINUTE);
                    android.app.TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                            onTimeSetListener, hour, minute,
                            true);
                    timePickerDialog.show();
                }

            }
        };
    }


    @Override
    public String getFieldName()
    {
        return mFieldName;
    }


    @Override
    public void addToLayout(ViewGroup layout)
    {
        layout.addView(this);
    }


    @Override
    public Object getValue()
    {
        return mCalendar.getTimeInMillis();
    }


    @Override
    public void saveState(Bundle outState) {
        outState.putLong(ControlHelper.getSavedStateKey(mFieldName), mCalendar.getTimeInMillis());
        if (datePickerDialog != null)
            datePickerDialog.dismiss();
    }
}

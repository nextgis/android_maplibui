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

package com.nextgis.maplibui.formcontrol;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.nextgis.maplib.util.GeoConstants.FTDate;
import static com.nextgis.maplib.util.GeoConstants.FTDateTime;
import static com.nextgis.maplib.util.GeoConstants.FTTime;
import static com.nextgis.maplibui.util.ConstantsUI.DATE;
import static com.nextgis.maplibui.util.ConstantsUI.DATETIME;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DATE_TYPE_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TEXT_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.TIME;


public class DateTime extends AppCompatTextView implements IFormControl
{
    protected boolean mIsShowLast;
    protected String mFieldName;
    SimpleDateFormat mDateFormat;
    protected Calendar mCalendar = Calendar.getInstance();

    public DateTime(Context context) {
        super(context);
    }

    public DateTime(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DateTime(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void init(JSONObject element, List<Field> fields, Bundle savedState, Cursor featureCursor, SharedPreferences preferences) throws JSONException {
        mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = attributes.getString(JSON_FIELD_NAME_KEY);
        mIsShowLast = ControlHelper.isSaveLastValue(attributes);
        setEnabled(ControlHelper.isEnabled(fields, mFieldName));

        int picker_type = DATETIME, dataType;
        if (attributes.has(JSON_DATE_TYPE_KEY)) {
            picker_type = attributes.getInt(JSON_DATE_TYPE_KEY);
        }

        switch (picker_type) {
            case DATE:
                mDateFormat = (SimpleDateFormat) DateFormat.getDateInstance();
                dataType = GeoConstants.FTDate;
                break;
            case TIME:
                mDateFormat = (SimpleDateFormat) DateFormat.getTimeInstance();
                dataType = GeoConstants.FTTime;
                break;
            case DATETIME:
                mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
                dataType = GeoConstants.FTDateTime;
                break;
            default:
                picker_type = DATETIME;
                mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
                dataType = GeoConstants.FTDateTime;
        }

        long timestamp = System.currentTimeMillis();
        if (ControlHelper.hasKey(savedState, mFieldName))
            timestamp = savedState.getLong(ControlHelper.getSavedStateKey(mFieldName));
        else if (null != featureCursor) { // feature exists
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0) {
                timestamp = featureCursor.getLong(column);
            }
        } else {    // new feature
            if (attributes.has(JSON_TEXT_KEY) && !TextUtils.isEmpty(attributes.getString(JSON_TEXT_KEY).trim())) {
                String defaultValue = attributes.getString(JSON_TEXT_KEY);
                timestamp = parseDateTime(defaultValue, dataType);
            }

            if (mIsShowLast)
                timestamp = preferences.getLong(mFieldName, timestamp);
        }

        mCalendar.setTimeInMillis(timestamp);
        setText(mDateFormat.format(mCalendar.getTime()));
        setSingleLine(true);
        setFocusable(false);
        setOnClickListener(getDateUpdateWatcher(picker_type));

        String pattern = mDateFormat.toLocalizedPattern();
        setHint(pattern);
    }


    protected long parseDateTime(String value, int type) {
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

        if (sdf != null)
            try {
                result = sdf.parse(value).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

        return result;
    }


    @Override
    public void saveLastValue(SharedPreferences preferences) {
        preferences.edit().putLong(mFieldName, (Long) getValue()).commit();
    }

    @Override
    public boolean isShowLast() {
        return mIsShowLast;
    }

    public void setCurrentDate(){
        setText(mDateFormat.format(Calendar.getInstance().getTime()));
    }

    protected View.OnClickListener getDateUpdateWatcher(final int pickerType)
    {
        return new View.OnClickListener()
        {
            protected void setValue()
            {
                DateTime.this.setText(mDateFormat.format(mCalendar.getTime()));
            }


            @Override
            public void onClick(View view)
            {
                Context context = DateTime.this.getContext();

                switch (pickerType) {

                    case DATE:
                        DatePickerDialog.OnDateSetListener onDateSetListener =
                                new DatePickerDialog.OnDateSetListener()
                                {
                                    @Override
                                    public void onDateSet(
                                            DatePicker view,
                                            int year,
                                            int monthOfYear,
                                            int dayOfMonth)
                                    {
                                        mCalendar.set(Calendar.YEAR, year);
                                        mCalendar.set(Calendar.MONTH, monthOfYear);
                                        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                        setValue();
                                    }

                                };

                        DatePickerDialog datePickerDialog = new DatePickerDialog(
                                context, onDateSetListener, mCalendar.get(Calendar.YEAR),
                                mCalendar.get(Calendar.MONTH),
                                mCalendar.get(Calendar.DAY_OF_MONTH));
                        datePickerDialog.show();
                        break;

                    case TIME:
                        TimePickerDialog.OnTimeSetListener onTimeSetListener =
                                new TimePickerDialog.OnTimeSetListener()
                                {
                                    @Override
                                    public void onTimeSet(
                                            TimePicker view,
                                            int hourOfDay,
                                            int minute)
                                    {
                                        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                        mCalendar.set(Calendar.MINUTE, minute);

                                        setValue();
                                    }
                                };

                        TimePickerDialog timePickerDialog = new TimePickerDialog(
                                context, onTimeSetListener, mCalendar.get(Calendar.HOUR_OF_DAY),
                                mCalendar.get(Calendar.MINUTE),
                                android.text.format.DateFormat.is24HourFormat(context));
                        timePickerDialog.show();
                        break;

                    case DATETIME:
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);

                        builder.setTitle(mDateFormat.format(mCalendar.getTime()));
                        builder.setPositiveButton(
                                R.string.ok, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which)
                                    {
                                        setValue();
                                        dialog.dismiss();
                                    }
                                });

                        final AlertDialog alert = builder.create();

                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE);
                        View datetimePickerLayout =
                                inflater.inflate(R.layout.dialog_datetimepicker, null);
                        alert.setView(datetimePickerLayout);

                        DatePicker dt =
                                (DatePicker) datetimePickerLayout.findViewById(R.id.datePicker);

                        dt.init(
                                mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                                mCalendar.get(Calendar.DAY_OF_MONTH),
                                new DatePicker.OnDateChangedListener()
                                {

                                    @Override
                                    public void onDateChanged(
                                            DatePicker view,
                                            int year,
                                            int monthOfYear,
                                            int dayOfMonth)
                                    {
                                        mCalendar.set(Calendar.YEAR, year);
                                        mCalendar.set(Calendar.MONTH, monthOfYear);
                                        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                        alert.setTitle(mDateFormat.format(mCalendar.getTime()));
                                    }
                                });

                        TimePicker tp =
                                (TimePicker) datetimePickerLayout.findViewById(R.id.timePicker);
                        tp.setIs24HourView(
                                android.text.format.DateFormat.is24HourFormat(context));
                        tp.setOnTimeChangedListener(
                                new TimePicker.OnTimeChangedListener()
                                {
                                    @Override
                                    public void onTimeChanged(
                                            TimePicker view,
                                            int hourOfDay,
                                            int minute)
                                    {
                                        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                        mCalendar.set(Calendar.MINUTE, minute);

                                        alert.setTitle(mDateFormat.format(mCalendar.getTime()));
                                    }
                                });

                        alert.show();
                        break;
                }
            }
        };
    }


    @Override
    public String getFieldName() {
        return mFieldName;
    }

    @Override
    public void addToLayout(ViewGroup layout) {
        layout.addView(this);
    }

    @Override
    public Object getValue() {
        return mCalendar.getTimeInMillis();
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putLong(ControlHelper.getSavedStateKey(mFieldName), mCalendar.getTimeInMillis());
    }
}

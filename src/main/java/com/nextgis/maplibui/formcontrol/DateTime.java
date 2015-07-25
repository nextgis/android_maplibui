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
import android.database.Cursor;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IFormControl;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.nextgis.maplibui.util.ConstantsUI.DATE;
import static com.nextgis.maplibui.util.ConstantsUI.DATETIME;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DATE_TYPE_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_SHOW_LAST_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TEXT_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.TIME;


public class DateTime extends AppCompatTextView implements IFormControl
{
    protected boolean mIsShowLast;
    protected String mFieldName;
    SimpleDateFormat mDateFormat;

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
    public void init(JSONObject element, List<Field> fields, Cursor featureCursor) throws JSONException {
        mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();


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

    public void setCurrentDate(){
        setText(mDateFormat.format(Calendar.getInstance().getTime()));
    }

    protected View.OnClickListener getDateUpdateWatcher(final int pickerType)
    {
        return new View.OnClickListener()
        {
            protected Calendar mCalendar = Calendar.getInstance();


            protected void setValue()
            {
                DateTime.this.setText(mDateFormat.format(mCalendar.getTime()));
            }


            @Override
            public void onClick(View view)
            {
                Context context = DateTime.this.getContext();
                mCalendar.setTime(new Date());

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
        return getText().toString();
    }
}

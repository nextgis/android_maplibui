/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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
package com.nextgis.maplibui.control;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ISimpleControl;
import com.nextgis.maplibui.util.ControlHelper;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.nextgis.maplib.util.Constants.TAG;

public class DateTime
        extends AppCompatTextView
        implements ISimpleControl {

    public static int TYPE_DATE_TIME = 0;
    public static int TYPE_DATE = 1;
    public static int TYPE_TIME = 2;

    int dateSelectorType= TYPE_DATE_TIME;
    int myDay, myMonth, myYear, myHour, myMinute;

    TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
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
            DateTime.this.setText(getText());
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

                Calendar c = Calendar.getInstance();
                if (myHour != -1 ){
                    hour = myHour;
                    minute=myMinute;
                } else {
                    hour = c.get(Calendar.HOUR_OF_DAY);
                    minute = c.get(Calendar.MINUTE);
                }
                TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                        onTimeSetListener, hour, minute,
                        true);
                timePickerDialog.show();
            } else { /// только дата -
                Calendar newCalendar = Calendar.getInstance();
                newCalendar.set(Calendar.YEAR, myYear);
                newCalendar.set(Calendar.MONTH, myMonth);
                newCalendar.set(Calendar.DAY_OF_MONTH, myDay);

                mValue = newCalendar.getTimeInMillis();
                DateTime.this.setText(getText());

            }
        }
    };

    Calendar calendar = Calendar.getInstance();
    DatePickerDialog datePickerDialog = new DatePickerDialog(
            getContext(), onDateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH));

    protected int mPickerType = GeoConstants.FTDateTime;

    protected String           mFieldName;
    protected SimpleDateFormat mDateFormat;

    protected Long mValue = Calendar.getInstance().getTimeInMillis();


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


    public void setPickerType(int pickerType)
    {
        mPickerType = pickerType;
    }


    protected View.OnClickListener getDateUpdateWatcher(final int pickerType)
    {
        return new View.OnClickListener()
        {
            protected Calendar mCalendar = Calendar.getInstance();


            protected void setValue()
            {
                mValue = mCalendar.getTimeInMillis();
                DateTime.this.setText(getText());
            }


            @Override
            public void onClick(View view)
            {
                Context context = DateTime.this.getContext();

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
                    TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                            onTimeSetListener, hour, minute,
                            true);
                    timePickerDialog.show();
                }
            }
        };
    }


    public String getFieldName()
    {
        return mFieldName;
    }


    @Override
    public void addToLayout(ViewGroup layout)
    {
        layout.addView(this);
        GreyLine.addToLayout(layout);
    }


    @Override
    public Object getValue()
    {
        return mValue;
    }


    @Override
    public void init(
            Field field,
            Bundle savedState,
            Cursor featureCursor)
    {
        if (null != field) {
            mFieldName = field.getName();
        }

        switch (mPickerType) {

            case GeoConstants.FTDate:
                mDateFormat = (SimpleDateFormat) DateFormat.getDateInstance();
                break;

            case GeoConstants.FTTime:
                mDateFormat = (SimpleDateFormat) DateFormat.getTimeInstance();
                break;

            default:
                mPickerType = GeoConstants.FTDateTime;
            case GeoConstants.FTDateTime:
                mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
                break;
        }

        String text = "";

        if (ControlHelper.hasKey(savedState, mFieldName)) {
            mValue = savedState.getLong(ControlHelper.getSavedStateKey(mFieldName));
        } else if (null != featureCursor) {
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0) {
                mValue = featureCursor.getLong(column);
            }
        }

        if (null != mValue) {
            text = getText();
        }

        setText(text);
        setSingleLine(true);
        setFocusable(false);
        setOnClickListener(getDateUpdateWatcher(mPickerType));

        String pattern = mDateFormat.toLocalizedPattern();
        setHint(pattern);
    }


    @Override
    public void saveState(Bundle outState) {
        outState.putLong(ControlHelper.getSavedStateKey(mFieldName), mValue);
        if (datePickerDialog != null)
            datePickerDialog.dismiss();
    }


    @Override
    public String getText()
    {
        if (null == mValue) {
            return "";
        }

        return mDateFormat.format(new Date(mValue));
    }


    public void setValue(Object val)
    {
        if (val instanceof Long) {
            mValue = (long) val;
        } else if (val instanceof Date) {
            mValue = ((Date) val).getTime();
        } else if (val instanceof Calendar) {
            mValue = ((Calendar) val).getTimeInMillis();
        } else if (val instanceof String) {
            try {
                String stringVal = (String) val;
                Date date = mDateFormat.parse(stringVal);
                mValue = date.getTime();
            } catch (ParseException e) {
                Log.d(TAG, "Date parse error, " + e.getLocalizedMessage());
                mValue = 0L;
            }
        } else {
            return;
        }

        setText(getText());

        setSingleLine(true);
        setFocusable(false);
        setOnClickListener(getDateUpdateWatcher(mPickerType));

        String pattern = mDateFormat.toLocalizedPattern();
        setHint(pattern);
    }
}

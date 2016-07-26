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

import com.jzxiang.pickerview.TimePickerDialog;
import com.jzxiang.pickerview.data.Type;
import com.jzxiang.pickerview.listener.OnDateSetListener;
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
        implements ISimpleControl
{
    protected int mPickerType = GeoConstants.FTDateTime;

    protected String           mFieldName;
    protected SimpleDateFormat mDateFormat;

    protected Long mValue = Calendar.getInstance().getTimeInMillis();
    protected TimePickerDialog mTimeDialog;


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


    public void setCurrentDate()
    {
        mValue = Calendar.getInstance().getTimeInMillis();
        setText(getText());
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

                String title = null;
                Type type = Type.ALL;
                switch (pickerType) {
                    case GeoConstants.FTDate:
                        title = context.getString(R.string.field_type_date);
                        type = Type.YEAR_MONTH_DAY;
                        break;
                    case GeoConstants.FTTime:
                        title = context.getString(R.string.field_type_time);
                        type = Type.HOURS_MINS;
                        break;
                    case GeoConstants.FTDateTime:
                        title = context.getString(R.string.field_type_datetime);
                        type = Type.ALL;
                        break;
                }

                OnDateSetListener onDateSetListener = new OnDateSetListener() {
                    @Override
                    public void onDateSet(TimePickerDialog timePickerView, long milliseconds) {
                        mCalendar.setTimeInMillis(milliseconds);
                        setValue();
                    }
                };

                mTimeDialog = new TimePickerDialog.Builder()
                        .setCallBack(onDateSetListener)
                        .setTitleStringId(title)
                        .setSureStringId(context.getString(android.R.string.ok))
                        .setCancelStringId(context.getString(android.R.string.cancel))
                        .setYearText(" " + context.getString(R.string.unit_year))
                        .setMonthText(" " + context.getString(R.string.unit_month))
                        .setDayText(" " + context.getString(R.string.unit_day))
                        .setHourText(" " + context.getString(R.string.unit_hour))
                        .setMinuteText(" " + context.getString(R.string.unit_min))
                        .setType(type)
                        .setCyclic(false)
                        .setMinMillseconds(1)
                        .setMaxMillseconds(Long.MAX_VALUE)
                        .setCurrentMillseconds(mCalendar.getTimeInMillis())
                        .setWheelItemTextSize(12)
                        .setWheelItemTextNormalColor(ContextCompat.getColor(getContext(), R.color.timetimepicker_default_text_color))
                        .setWheelItemTextSelectorColor(ContextCompat.getColor(getContext(), R.color.accent))
                        .setThemeColor(ContextCompat.getColor(getContext(), R.color.primary_dark))
                        .build();

                AppCompatActivity activity = ControlHelper.getActivity(getContext());
                if (activity != null)
                    mTimeDialog.show(activity.getSupportFragmentManager(), "TimePickerDialog");
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
        if (mTimeDialog != null)
            mTimeDialog.dismiss();
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

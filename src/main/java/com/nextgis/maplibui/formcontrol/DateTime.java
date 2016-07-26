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

package com.nextgis.maplibui.formcontrol;

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

import com.jzxiang.pickerview.TimePickerDialog;
import com.jzxiang.pickerview.data.Type;
import com.jzxiang.pickerview.listener.OnDateSetListener;
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
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_DATE_TYPE_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TEXT_KEY;


public class DateTime
        extends AppCompatTextView
        implements IFormControl
{
    protected int mPickerType = FTDateTime;

    protected boolean          mIsShowLast;
    protected String           mFieldName;
    protected SimpleDateFormat mDateFormat;

    protected Calendar mCalendar = Calendar.getInstance();
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


    @Override
    public void init(
            JSONObject element,
            List<Field> fields,
            Bundle savedState,
            Cursor featureCursor,
            SharedPreferences preferences)
            throws JSONException
    {
        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = attributes.getString(JSON_FIELD_NAME_KEY);
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
        } else if (null != featureCursor) { // feature exists
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0) {
                timestamp = featureCursor.getLong(column);
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


    public void setCurrentDate()
    {
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
        if (mTimeDialog != null)
            mTimeDialog.dismiss();
    }
}

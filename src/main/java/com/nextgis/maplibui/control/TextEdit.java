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

package com.nextgis.maplibui.control;

import static com.nextgis.maplib.util.LayerUtil.getColumnIndexSafely;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.widget.AppCompatEditText;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ISimpleControl;
import com.nextgis.maplibui.util.ControlHelper;


public class TextEdit
        extends AppCompatEditText
        implements ISimpleControl
{
    String mFieldName;

    public boolean showNullHint = false; // for Text value - show null when value was deleted (by clear cross) and now value is null
    // if value clead by delete key - it empty value - "" - not null


    public TextEdit(Context context) {
        super(context);
    }

    public TextEdit(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextEdit(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void init(final Field field,
                     final Bundle savedState,
                     final Cursor featureCursor){
        ControlHelper.setClearAction(this, field.getType() == GeoConstants.FTString);
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}

            @Override
            public void onTextChanged(CharSequence s,int start,int before,int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && field.getType() == GeoConstants.FTString)
                    setClearedToEmpty();
            }
        });

        mFieldName = field.getName();
        String text = null;

        //boolean isNull = false;
        if (ControlHelper.hasKey(savedState, mFieldName))
            text = savedState.getString(ControlHelper.getSavedStateKey(mFieldName));
        else if (null != featureCursor) {
            int column = getColumnIndexSafely(featureCursor, mFieldName); //featureCursor.getColumnIndex(mFieldName);
            if (column >= 0) {
                text = featureCursor.getString(column);
//                if (featureCursor.isNull(column))
//                    isNull = true;
            }
        }
//        else { // no key no cursor = check for types for null
//            if ( (field.getType() == GeoConstants.FTLong ||
//                    field.getType() == GeoConstants.FTReal ||
//                    field.getType() == GeoConstants.FTInteger))
//                isNull = true;
//        }

        // null hint for numbers
        if ( (field.getType() == GeoConstants.FTLong ||
                field.getType() == GeoConstants.FTReal ||
                field.getType() == GeoConstants.FTInteger)) {
            setHint("NULL");
        }

        if (text == null && field.getType() == GeoConstants.FTString) {
            setHint("NULL");
        }

        setHintTextColor(getContext().getResources().getColor(R.color.color_grey_500));

        if (text == null)
            showNullHint = true;

        setText(text);
//        if (isNull) {
//            setHint("NULL");
//            setHintTextColor(getContext().getResources().getColor(R.color.color_grey_500));
//        }

        switch (field.getType()) {

            case GeoConstants.FTString:
                break;

            case GeoConstants.FTInteger:
                setSingleLine(true);
                setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED );
                break;

            case GeoConstants.FTLong:
                setSingleLine(true);
                setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED );
                break;

            case GeoConstants.FTReal:
                setSingleLine(true);
                setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED );
                break;
        }
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putString(ControlHelper.getSavedStateKey(mFieldName), getText().toString());
    }

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
        return getText().toString();
    }

    public void setClearedToNull(boolean isTextValue){
        if (isTextValue){
            setHint("NULL");
            showNullHint = true;
        }
    }

    public void setClearedToEmpty(){
        setHint("");
        showNullHint = false;
    }
}

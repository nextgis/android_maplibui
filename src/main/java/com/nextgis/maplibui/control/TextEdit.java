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

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.api.ISimpleControl;
import com.nextgis.maplibui.util.ControlHelper;


public class TextEdit
        extends AppCompatEditText
        implements ISimpleControl
{
    String mFieldName;

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
    public void init(Field field,
                     Bundle savedState,
                     Cursor featureCursor){
        ControlHelper.setClearAction(this);

        mFieldName = field.getName();
        String text = "";

        if (ControlHelper.hasKey(savedState, mFieldName))
            text = savedState.getString(ControlHelper.getSavedStateKey(mFieldName));
        else if (null != featureCursor) {
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0)
                text = featureCursor.getString(column);
        }

        setText(text);

        switch (field.getType()) {

            case GeoConstants.FTString:
                break;

            case GeoConstants.FTInteger:
                setSingleLine(true);
                setInputType(InputType.TYPE_CLASS_NUMBER);
                break;

            case GeoConstants.FTReal:
                setSingleLine(true);
                setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
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
}

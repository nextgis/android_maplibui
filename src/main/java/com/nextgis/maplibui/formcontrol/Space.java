/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.formcontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.api.IFormControl;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Space extends FrameLayout implements IFormControl {
    public Space(Context context) {
        super(context);
    }

    public Space(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Space(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public void init(JSONObject element, List<Field> fields, Bundle savedState, Cursor featureCursor, SharedPreferences preferences) throws JSONException {
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {

    }

    @Override
    public boolean isShowLast() {
        return false;
    }

    @Override
    public String getFieldName() {
        return null;
    }

    @Override
    public void addToLayout(ViewGroup layout) {
        layout.addView(this);
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void saveState(Bundle outState) {

    }
}

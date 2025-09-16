/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015, 2020 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.api;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.activity.ModifyAttributesActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Interface for formbuilder controls
 */
public interface IFormControl extends IControl {
    void init(JSONObject element, List<Field> fields, Bundle savedState, Cursor featureCursor,
              SharedPreferences lastValue, Map<String,
                      Map<String, String>> translations, final ModifyAttributesActivity modifyAttributesActivity) throws JSONException;

    void saveLastValue(SharedPreferences preferences);

    boolean isShowLast();
}

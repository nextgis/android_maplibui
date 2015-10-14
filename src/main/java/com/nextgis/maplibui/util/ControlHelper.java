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

package com.nextgis.maplibui.util;

import android.content.Context;
import android.os.Bundle;

import com.nextgis.maplib.datasource.Field;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplibui.util.ConstantsUI.JSON_SHOW_LAST_KEY;

public final class ControlHelper {
    private final static String BUNDLE_SAVED_STATE = "nextgis_control_";

    public static String getPercentValue(Context context, int stringLabel, float value) {
        return context.getString(stringLabel) + ": " + ((int) value * 100 / 255) + "%";
    }

    public static boolean isEnabled(List<Field> fields, String fieldName) {
        for (Field field : fields)
            if (field.getName().equals(fieldName))
                return true;

        return false;
    }

    public static boolean isSaveLastValue(JSONObject attributes) throws JSONException {
        return attributes.has(JSON_SHOW_LAST_KEY) && !attributes.isNull(JSON_SHOW_LAST_KEY)
                && attributes.getBoolean(JSON_SHOW_LAST_KEY);
    }

    public static boolean hasKey(Bundle savedState, String fieldName) {
        return savedState != null && savedState.containsKey(getSavedStateKey(fieldName));
    }

    public static String getSavedStateKey(String fieldName) {
        return BUNDLE_SAVED_STATE + fieldName;
    }
}

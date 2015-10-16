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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.GTLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPoint;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPolygon;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;
import static com.nextgis.maplib.util.GeoConstants.GTPolygon;
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

    public static Drawable getIconByVectorType(Context context, int geometryType, int color, int defaultIcon, boolean syncable) {
        int drawableId;

        switch (geometryType) {
            case GTPoint:
                drawableId = R.drawable.ic_type_point;
                break;
            case GTMultiPoint:
                drawableId = R.drawable.ic_type_multipoint;
                break;
            case GTLineString:
                drawableId = R.drawable.ic_type_line;
                break;
            case GTMultiLineString:
                drawableId = R.drawable.ic_type_multiline;
                break;
            case GTPolygon:
                drawableId = R.drawable.ic_type_polygon;
                break;
            case GTMultiPolygon:
                drawableId = R.drawable.ic_type_multipolygon;
                break;
            default:
                return context.getResources().getDrawable(R.drawable.ic_local_vector);
        }

        BitmapDrawable icon = (BitmapDrawable) context.getResources().getDrawable(drawableId);
        if (icon != null) {
            Bitmap src = icon.getBitmap();
            Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
            Canvas canvas = new Canvas(bitmap);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            paint.setColorFilter(filter);
            canvas.drawBitmap(src, 0, 0, paint);

            if (syncable) {
                // TODO
                // context.getTheme() == Dark ? R.drawable.ic_action_refresh_dark : R.drawable.ic_action_refresh_light;
                int syncIconId = R.drawable.ic_action_refresh_light;
                src = BitmapFactory.decodeResource(context.getResources(), syncIconId);
                src = Bitmap.createScaledBitmap(src, bitmap.getWidth() / 2, bitmap.getWidth() / 2, true);
                canvas.drawBitmap(src, bitmap.getWidth() - bitmap.getWidth() / 2, bitmap.getWidth() - bitmap.getWidth() / 2, new Paint());
            }

            icon = new BitmapDrawable(context.getResources(), bitmap);
        }

        return icon;
    }
}

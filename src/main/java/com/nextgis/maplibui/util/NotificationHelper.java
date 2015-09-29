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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.DisplayMetrics;

import com.nextgis.maplibui.R;

public final class NotificationHelper {
    public static int dpToPx(int dp, Resources resources) {
        DisplayMetrics dm = resources.getDisplayMetrics();
        return Math.round(dp* (dm.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static Bitmap getLargeIcon(int iconResourceId, Resources resources) {
        Bitmap icon = BitmapFactory.decodeResource(resources, iconResourceId);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return icon;

        int iconSize = dpToPx(40, resources);
        int innerIconSize = dpToPx(24, resources);
        icon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, false);
        Bitmap largeIcon = icon.copy(Bitmap.Config.ARGB_8888, true);
        icon = Bitmap.createScaledBitmap(icon, innerIconSize, innerIconSize, false);

        Canvas canvas = new Canvas(largeIcon);
        int center = canvas.getHeight() / 2;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(resources.getColor(R.color.accent));
        canvas.drawCircle(center, center, center, paint);
        paint.setColor(Color.WHITE);
        canvas.drawBitmap(icon, center - icon.getWidth() / 2, center - icon.getWidth() / 2, paint);

        return largeIcon;
    }
}

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * Based on https://github.com/nextgis/nextgismobile/blob/master/src/com/nextgis/mobile/forms/CompassImage.java
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
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CompassImage extends ImageView {
    protected float mAngle = 0;

    public CompassImage(Context context) {
        super(context);
    }

    public CompassImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.rotate(mAngle, this.getMeasuredWidth()/2, this.getMeasuredHeight()/2);
        super.onDraw(canvas);
    }

    public void setAngle(float a) {
        mAngle = a;
    }

    public float getAngle() {
        return mAngle;
    }

    @Override
    public boolean isInEditMode () {
        return false;
    }

}
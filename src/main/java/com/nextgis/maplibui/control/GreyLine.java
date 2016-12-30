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

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import com.nextgis.maplibui.R;


public class GreyLine
{
    public static void addToLayout(ViewGroup layout)
    {
        // add grey line view here
        float lineHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, layout.getContext().getResources().getDisplayMetrics());
        View greyLine = new View(layout.getContext());
        layout.addView(greyLine);
        ViewGroup.LayoutParams params = greyLine.getLayoutParams();
        params.height = (int) lineHeight;
        greyLine.setLayoutParams(params);
        greyLine.setBackgroundResource(R.color.color_grey_600);
    }

}

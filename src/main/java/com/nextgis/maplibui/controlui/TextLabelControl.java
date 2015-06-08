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
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.controlui;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.TextView;
import com.nextgis.maplibui.R;


public class TextLabelControl
        extends TextView
        implements IControl
{
    public TextLabelControl(Context context)
    {
        super(context);
        init(context);
    }

    public TextLabelControl(Context context, String text)
    {
        super(context);
        init(context);
        setText(text);
    }


    protected void init(Context context)
    {
        setEllipsize(TextUtils.TruncateAt.END);
        setTextAppearance(context, R.style.Base_TextAppearance_AppCompat_Medium);
        setTextColor(getResources().getColor(R.color.hint_foreground_material_light));
    }


    @Override
    public void addToLayout(ViewGroup layout)
    {
        layout.addView(this);
    }


    @Override
    public Object getValue()
    {
        // do nothing
        return null;
    }
}

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

package com.nextgis.maplibui.mapui;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.nextgis.maplib.map.LayerFactory;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ILayerUI;

import java.io.File;


/**
 * UI for Layer group
 */
public class LayerGroupUI
        extends LayerGroup
        implements ILayerUI
{
    public LayerGroupUI(
            Context context,
            File path,
            LayerFactory layerFactory)
    {
        super(context, path, layerFactory);
    }


    @Override
    public Drawable getIcon(Context context)
    {
        return mContext.getResources().getDrawable(R.drawable.ic_ngw_folder);
    }


    @Override
    public void changeProperties(Context context)
    {

    }
}

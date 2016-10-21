/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.map.NGWRasterLayer;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.TMSLayerSettingsActivity;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.dialog.SelectZoomLevelsDialog;
import com.nextgis.maplibui.util.ConstantsUI;

import java.io.File;


public class NGWRasterLayerUI
        extends NGWRasterLayer
        implements ILayerUI
{
    public NGWRasterLayerUI(
            Context context,
            File path)
    {
        super(context, path);
    }


    @Override
    public Drawable getIcon(Context context)
    {
        return mContext.getResources().getDrawable(R.drawable.ic_raster);
    }


    @Override
    public void changeProperties(Context context)
    {
        Intent settings = new Intent(context, TMSLayerSettingsActivity.class);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settings.putExtra(ConstantsUI.KEY_LAYER_ID, getId());
        context.startActivity(settings);
    }

    public void downloadTiles(Context context, GeoEnvelope env) {
        FragmentActivity fragmentActivity = (FragmentActivity) context;
        SelectZoomLevelsDialog newFragment = new SelectZoomLevelsDialog();
        newFragment.setEnvelope(env).setLayerId(getId()).
                show(fragmentActivity.getSupportFragmentManager(), "select_zoom_levels");
    }
}

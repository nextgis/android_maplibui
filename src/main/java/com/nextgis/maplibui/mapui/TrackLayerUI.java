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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.nextgis.maplib.display.TrackRenderer;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.TracksActivity;
import com.nextgis.maplibui.api.ILayerUI;

import java.io.File;


public class TrackLayerUI
        extends TrackLayer
        implements ILayerUI
{
    public TrackLayerUI(
            Context context,
            File path)
    {
        super(context, path);
        mColor = ContextCompat.getColor(mContext, R.color.accent);
        ((TrackRenderer) mRenderer).setEndingMarker(R.drawable.ic_track_flag);
    }


    @Override
    public Drawable getIcon(Context context)
    {
        int[] attrs = new int[] { R.attr.ic_track };
        TypedArray ta = context.obtainStyledAttributes(attrs);
        Drawable track = ta.getDrawable(0);
        ta.recycle();
        return track;
    }


    @Override
    public void changeProperties(Context context)
    {
        Intent tracksSettings = new Intent(context, TracksActivity.class);
        tracksSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(tracksSettings);
    }


    @Override
    public boolean delete()
    {
        Toast.makeText(mContext, R.string.layer_permanent, Toast.LENGTH_SHORT).show();
        return false;
    }
}

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

package com.nextgis.maplibui.mapui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.widget.Toast;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.CustomModifyAttributesActivity;
import com.nextgis.maplibui.ModifyAttributesActivity;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ILayerUI;

import java.io.File;

import static com.nextgis.maplibui.util.Constants.KEY_FEATURE_ID;
import static com.nextgis.maplibui.util.Constants.KEY_FORM_PATH;
import static com.nextgis.maplibui.util.Constants.KEY_LAYER_ID;


/**
 * A UI for vector layer
 */
public class VectorLayerUI extends VectorLayer implements ILayerUI
{
    public VectorLayerUI(
            Context context,
            File path)
    {
        super(context, path);
    }


    @Override
    public Drawable getIcon()
    {
        return mContext.getResources().getDrawable(R.drawable.ic_local_vector);
    }


    @Override
    public void changeProperties()
    {

    }


    @Override
    public void showEditForm(Context context)
    {
        if(!mIsInitialized)
        {
            Toast.makeText(context, context.getString(R.string.error_layer_not_inited),
                           Toast.LENGTH_SHORT).show();
            return;
        }
        //check custom form
        File form = new File(mPath, com.nextgis.maplibui.util.Constants.FILE_FORM);
        if(form.exists()){
            //show custom form
            Intent intent = new Intent(context, CustomModifyAttributesActivity.class);
            intent.putExtra(KEY_LAYER_ID, getId());
            intent.putExtra(KEY_FEATURE_ID, (long) Constants.NOT_FOUND);
            intent.putExtra(KEY_FORM_PATH, form);
            context.startActivity(intent);
        }
        else {
            //if not exist show standard form
            Intent intent = new Intent(context, ModifyAttributesActivity.class);
            intent.putExtra(KEY_LAYER_ID, getId());
            intent.putExtra(KEY_FEATURE_ID, (long) Constants.NOT_FOUND);
            context.startActivity(intent);
        }
    }
}

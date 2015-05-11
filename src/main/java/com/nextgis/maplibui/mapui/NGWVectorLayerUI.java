/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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

package com.nextgis.maplibui.mapui;


import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.widget.Toast;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplibui.CustomModifyAttributesActivity;
import com.nextgis.maplibui.ModifyAttributesActivity;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.VectorLayerSettingsActivity;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.util.ConstantsUI;

import java.io.File;


import static com.nextgis.maplibui.util.ConstantsUI.*;


public class NGWVectorLayerUI extends NGWVectorLayer implements ILayerUI, IVectorLayerUI
{
    public NGWVectorLayerUI(
            Context context,
            File path)
    {
        super(context, path);
    }


    @Override
    public Drawable getIcon()
    {
        return mContext.getResources().getDrawable(R.drawable.ic_ngw_vector);
    }


    @Override
    public void changeProperties(Context context)
    {
        Intent settings = new Intent(context, VectorLayerSettingsActivity.class);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settings.putExtra(VectorLayerSettingsActivity.LAYER_ID_KEY, getId());
        context.startActivity(settings);
    }


    @Override
    public void showEditForm( Context context, long featureId, GeoGeometry geometry)
    {
        if(!mIsInitialized)
        {
            Toast.makeText(context, context.getString(R.string.error_layer_not_inited), Toast.LENGTH_SHORT).show();
            return;
        }
        //check custom form
        File form = new File(mPath, ConstantsUI.FILE_FORM);
        if(form.exists()){
            //show custom form
            Intent intent = new Intent(context, CustomModifyAttributesActivity.class);
            intent.putExtra(KEY_LAYER_ID, getId());
            intent.putExtra(KEY_FEATURE_ID, featureId);
            intent.putExtra(KEY_FORM_PATH, form);
            if(null != geometry)
                intent.putExtra(KEY_GEOMETRY, geometry);
            context.startActivity(intent);
        }
        else {
            //if not exist show standard form
            Intent intent = new Intent(context, ModifyAttributesActivity.class);
            intent.putExtra(KEY_LAYER_ID, getId());
            intent.putExtra(KEY_FEATURE_ID, featureId);
            if(null != geometry)
                intent.putExtra(KEY_GEOMETRY, geometry);
            context.startActivity(intent);
        }
    }


    @Override
    protected void reportError(final String error)
    {
        Intent msg = new Intent(ConstantsUI.MESSAGE_INTENT);
        msg.putExtra(ConstantsUI.KEY_MESSAGE, error);
        getContext().sendBroadcast(msg);
        super.reportError(error);
    }
}

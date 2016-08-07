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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.AttributesActivity;
import com.nextgis.maplibui.activity.FormBuilderModifyAttributesActivity;
import com.nextgis.maplibui.activity.ModifyAttributesActivity;
import com.nextgis.maplibui.activity.VectorLayerSettingsActivity;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;

import java.io.File;

import static com.nextgis.maplibui.util.ConstantsUI.KEY_FEATURE_ID;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_FORM_PATH;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_GEOMETRY;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_GEOMETRY_CHANGED;
import static com.nextgis.maplibui.util.ConstantsUI.KEY_LAYER_ID;


/**
 * A UI for vector layer
 */
public class VectorLayerUI
        extends VectorLayer
        implements IVectorLayerUI
{

    public VectorLayerUI(
            Context context,
            File path)
    {
        super(context, path);
    }


    @Override
    public Drawable getIcon(Context context)
    {
        int color = ((SimpleFeatureRenderer) mRenderer).getStyle().getColor();
        return ControlHelper.getIconByVectorType(mContext, mGeometryType, color, R.drawable.ic_local_vector, false);
    }


    @Override
    public void changeProperties(Context context)
    {
        Intent settings = new Intent(context, VectorLayerSettingsActivity.class);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settings.putExtra(ConstantsUI.KEY_LAYER_ID, getId());
        context.startActivity(settings);
    }


    @Override
    public void showEditForm(
            Context context,
            long featureId,
            GeoGeometry geometry)
    {
        if (mFields == null) {
            Toast.makeText(
                    context, context.getString(R.string.error_layer_not_inited), Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        boolean isGeometryChanged = geometry != null;
        //get geometry
        if (geometry == null && featureId != Constants.NOT_FOUND) {
            geometry = getGeometryForId(featureId);
        }

        Intent intent;
        //check custom form
        File form = new File(mPath, ConstantsUI.FILE_FORM);
        if (form.exists()) {
            //show custom form
            intent = new Intent(context, FormBuilderModifyAttributesActivity.class);
            intent.putExtra(KEY_FORM_PATH, form);
        } else {
            //if not exist show standard form
            intent = new Intent(context, ModifyAttributesActivity.class);
        }

        intent.putExtra(KEY_LAYER_ID, getId());
        intent.putExtra(KEY_FEATURE_ID, featureId);
        intent.putExtra(KEY_GEOMETRY_CHANGED, isGeometryChanged);
        if (null != geometry)
            intent.putExtra(KEY_GEOMETRY, geometry);

        ((Activity) context).startActivityForResult(intent, MODIFY_REQUEST);
    }

    @Override
    public boolean delete() throws SQLiteException {
        File preference = new File(mContext.getApplicationInfo().dataDir, "shared_prefs");

        if (preference.exists() && preference.isDirectory()) {
            preference = new File(preference, getPath().getName() + ".xml");
            preference.delete();
        }

        return super.delete();
    }

    @Override
    public void showAttributes() {
        Intent settings = new Intent(mContext, AttributesActivity.class);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settings.putExtra(ConstantsUI.KEY_LAYER_ID, getId());
        mContext.startActivity(settings);
    }
}

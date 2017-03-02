/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.NGWLookupTable;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.AttributesActivity;
import com.nextgis.maplibui.activity.VectorLayerSettingsActivity;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.LayerUtil;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class NGWVectorLayerUI
        extends NGWVectorLayer
        implements IVectorLayerUI
{
    public NGWVectorLayerUI(
            Context context,
            File path)
    {
        super(context, path);
    }


    @Override
    public Drawable getIcon(Context context)
    {
        int color = ((SimpleFeatureRenderer) mRenderer).getStyle().getColor();
        boolean syncable = 0 == (getSyncType() & Constants.SYNC_NONE);
        return ControlHelper.getIconByVectorType(mContext, mGeometryType, color, R.drawable.ic_vector, syncable);
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
    public void showEditForm(Context context, long featureId, GeoGeometry geometry) {
        LayerUtil.showEditForm(this, context, featureId, geometry);
    }


    @Override
    protected void reportError(final String error)
    {
        Intent msg = new Intent(ConstantsUI.MESSAGE_INTENT);
        msg.putExtra(ConstantsUI.KEY_MESSAGE, error);
        getContext().sendBroadcast(msg);
        super.reportError(error);
    }

    @Override
    public boolean delete() {
        File form = new File(mPath, ConstantsUI.FILE_FORM);
        if (form.exists()) {
            try {
                ArrayList<String> lookupTableIds = LayerUtil.fillLookupTableIds(form);
                MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
                if (null == map)
                    throw new IllegalArgumentException("The map should extends MapContentProviderHelper or inherited");

                for (int i = 0; i < map.getLayerCount(); i++) {
                    if (map.getLayer(i) instanceof NGWVectorLayer) {
                        form = new File(map.getLayer(i).getPath(), ConstantsUI.FILE_FORM);
                        if (form.exists()) {
                            ArrayList<String> otherIds = LayerUtil.fillLookupTableIds(form);
                            lookupTableIds.removeAll(otherIds);
                        }
                    }
                }

                if (lookupTableIds.size() > 0)
                    for (int i = 0; i < map.getLayerCount(); i++) {
                        if (map.getLayer(i) instanceof NGWLookupTable) {
                            NGWLookupTable table = (NGWLookupTable) map.getLayer(i);
                            String id = table.getRemoteId() + "";
                            if (table.getAccountName().equals(mAccountName) && lookupTableIds.contains(id)) {
                                map.removeLayer(table);
                                table.delete();
                                i--;
                            }
                        }
                    }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
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

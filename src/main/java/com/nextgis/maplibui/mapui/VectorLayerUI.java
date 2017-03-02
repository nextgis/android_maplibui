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
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Connections;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.AttributesActivity;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.maplibui.activity.SelectNGWResourceActivity;
import com.nextgis.maplibui.activity.VectorLayerSettingsActivity;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.dialog.NGWResourcesListAdapter;
import com.nextgis.maplibui.dialog.SelectNGWResourceDialog;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.LayerUtil;

import java.io.File;


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
        int color = Color.RED;
        if (mRenderer != null && ((SimpleFeatureRenderer) mRenderer).getStyle() != null)
            color = ((SimpleFeatureRenderer) mRenderer).getStyle().getColor();

        return ControlHelper.getIconByVectorType(mContext, mGeometryType, color, R.drawable.ic_vector, false);
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

    public void sendToNGW(final NGActivity activity) {
        final SelectNGWResourceDialog selectAccountDialog = new SelectNGWResourceDialog();
        selectAccountDialog.setConnectionListener(new NGWResourcesListAdapter.OnConnectionListener() {
            @Override
            public void onConnectionSelected(final Connection connection) {
                Intent intent = new Intent(activity, SelectNGWResourceActivity.class);
                Connections connections = new Connections(activity.getString(R.string.ngw_accounts));
                connections.add(connection);
                intent.putExtra(SelectNGWResourceActivity.KEY_TASK, SelectNGWResourceActivity.TYPE_SELECT);
                intent.putExtra(SelectNGWResourceActivity.KEY_CONNECTIONS, connections);
                intent.putExtra(SelectNGWResourceActivity.KEY_RESOURCE_ID, connections.getChild(0).getId());
                intent.putExtra(SelectNGWResourceActivity.KEY_PUSH_ID, VectorLayerUI.this.getId());
                activity.startActivity(intent);
                selectAccountDialog.dismiss();
            }

            @Override
            public void onAddConnection() {
                selectAccountDialog.onAddAccount(mContext);
            }
        })
                .setTitle(mContext.getString(R.string.ngw_accounts))
                .setTheme(activity.getThemeId())
                .show(activity.getSupportFragmentManager(), "send_layer_to_ngw");
    }

}


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

import android.support.v4.app.FragmentActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.map.LayerFactory;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.NGWRasterLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplibui.CreateRemoteTMSLayerDialog;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.SelectNGWResourceDialog;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import static com.nextgis.maplib.util.Constants.*;

public class LayerFactoryUI
        extends LayerFactory
{


    public LayerFactoryUI(File mapPath)
    {
        super(mapPath);
    }

    public void createNewNGWLayer(
            final Context context,
            final LayerGroup groupLayer)
    {
        if(context instanceof FragmentActivity) {
            FragmentActivity fragmentActivity = (FragmentActivity)context;
            SelectNGWResourceDialog newFragment = new SelectNGWResourceDialog();
            newFragment.setTitle(context.getString(R.string.select_ngw_layer))
                       .setLayerGroup(groupLayer)
                       .setTypeMask(Connection.NGWResourceTypePostgisLayer | Connection.NGWResourceTypeVectorLayer | Connection.NGWResourceTypeRasterLayer)
                       .show(fragmentActivity.getSupportFragmentManager(), "create_ngw_layer");
        }
    }

    public void createNewRemoteTMSLayer(
            final Context context,
            final LayerGroup groupLayer)
    {
        if(context instanceof FragmentActivity) {
            FragmentActivity fragmentActivity = (FragmentActivity)context;
            CreateRemoteTMSLayerDialog newFragment = new CreateRemoteTMSLayerDialog();
            newFragment.setTitle(context.getString(R.string.create_tms_layer))
                       .setLayerGroup(groupLayer)
                       .show(fragmentActivity.getSupportFragmentManager(), "create_tms_layer");
        }
    }



    public ILayer createLayer(
            Context context,
            File path)
    {
        File config_file = new File(path, CONFIG);
        ILayer layer = null;

        try {
            String sData = FileUtil.readFromFile(config_file);
            JSONObject rootObject = new JSONObject(sData);
            int nType = rootObject.getInt(JSON_TYPE_KEY);

            switch (nType) {
                case LAYERTYPE_REMOTE_TMS:
                    layer = new RemoteTMSLayerUI(context, path);
                    break;
                case LAYERTYPE_NGW_RASTER:
                    layer = new NGWRasterLayerUI(context, path);
                    break;
            }
        } catch (IOException | JSONException e) {
            Log.d(TAG, e.getLocalizedMessage());
        }

        return layer;
    }

}

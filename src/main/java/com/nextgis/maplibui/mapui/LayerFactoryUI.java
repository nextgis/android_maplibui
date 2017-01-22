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

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Connections;
import com.nextgis.maplib.map.LayerFactory;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.NGWLookupTable;
import com.nextgis.maplib.map.NGWTrackLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.maplibui.activity.SelectNGWResourceActivity;
import com.nextgis.maplibui.dialog.CreateFromQMSLayerDialog;
import com.nextgis.maplibui.dialog.CreateLocalLayerDialog;
import com.nextgis.maplibui.dialog.NGWResourcesListAdapter;
import com.nextgis.maplibui.dialog.SelectNGWResourceDialog;
import com.nextgis.maplibui.fragment.LayerFillProgressDialogFragment;
import com.nextgis.maplibui.service.LayerFillService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static com.nextgis.maplib.util.Constants.CONFIG;
import static com.nextgis.maplib.util.Constants.JSON_TYPE_KEY;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_GROUP;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOCAL_TMS;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOCAL_VECTOR;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOOKUPTABLE;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_RASTER;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_TRACKS;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_VECTOR;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_WEBMAP;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_REMOTE_TMS;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_TRACKS;
import static com.nextgis.maplib.util.Constants.TAG;


public class LayerFactoryUI
        extends LayerFactory
{

    public void createNewNGWLayer(
            final Context context,
            final LayerGroup layerGroup)
    {
        if (context instanceof NGActivity) {
            AccountManager accountManager = AccountManager.get(context);
            Connections connections = SelectNGWResourceDialog.fillConnections(context, accountManager);
            if (connections.getChildrenCount() == 1) {
                startNGWResourceActivity(context, (Connection) connections.getChild(0), layerGroup);
            } else {
                NGActivity fragmentActivity = (NGActivity) context;
                final SelectNGWResourceDialog newFragment = new SelectNGWResourceDialog();
                newFragment.setLayerGroup(layerGroup)
                        .setTypeMask(
                                Connection.NGWResourceTypePostgisLayer |
                                        Connection.NGWResourceTypeVectorLayer |
                                        Connection.NGWResourceTypeRasterLayer |
                                        Connection.NGWResourceTypeWMSClient |
                                        Connection.NGWResourceTypeWebMap)
                        .setConnectionListener(new NGWResourcesListAdapter.OnConnectionListener() {
                            @Override
                            public void onConnectionSelected(Connection connection) {
                                startNGWResourceActivity(context, connection, layerGroup);
                                newFragment.dismiss();
                            }

                            @Override
                            public void onAddConnection() {
                                newFragment.onAddAccount(context);
                            }
                        })
                        .setTitle(context.getString(R.string.choose_layers))
                        .setTheme(fragmentActivity.getThemeId())
                        .show(fragmentActivity.getSupportFragmentManager(), "create_ngw_layer");
            }
        }
    }

    private void startNGWResourceActivity(Context context, Connection connection, LayerGroup layerGroup) {
        Intent intent = new Intent(context, SelectNGWResourceActivity.class);
        Connections connections = new Connections(context.getString(R.string.ngw_accounts));
        connections.add(connection);
        intent.putExtra(SelectNGWResourceActivity.KEY_TASK, SelectNGWResourceActivity.TYPE_ADD);
        intent.putExtra(SelectNGWResourceActivity.KEY_CONNECTIONS, connections);
        intent.putExtra(SelectNGWResourceActivity.KEY_RESOURCE_ID, connections.getChild(0).getId());
        intent.putExtra(SelectNGWResourceActivity.KEY_GROUP_ID, layerGroup.getId());
        context.startActivity(intent);
    }


    @Override
    public void createNewLocalTMSLayer(
            final Context context,
            final LayerGroup groupLayer,
            final Uri uri)
    {
        String ext = "zip";
        String layerName =
                FileUtil.getFileNameByUri(context, uri, context.getString(R.string.new_layer));
        final int lastPeriodPos = layerName.lastIndexOf('.');
        if (lastPeriodPos > 0) {
            ext = layerName.substring(lastPeriodPos).toLowerCase();
            layerName = layerName.substring(0, lastPeriodPos);
        }
        if (context instanceof NGActivity) {
            NGActivity fragmentActivity = (NGActivity) context;

            if (ext.equals(".ngrc")) {
                Intent intent = new Intent(context, LayerFillService.class);
                intent.setAction(LayerFillService.ACTION_ADD_TASK);
                intent.putExtra(LayerFillService.KEY_URI, uri);
                intent.putExtra(LayerFillService.KEY_INPUT_TYPE, LayerFillService.TMS_LAYER);
                intent.putExtra(LayerFillService.KEY_LAYER_GROUP_ID, groupLayer.getId());

                LayerFillProgressDialogFragment.startFill(intent);
                return;
            }

            AtomicReference<Uri> temp = new AtomicReference<>(uri);
            if (MapUtil.isZippedGeoJSON(context, temp)) {
                createNewVectorLayer(context, groupLayer, temp.get());
                return;
            }

            CreateLocalLayerDialog newFragment = new CreateLocalLayerDialog();
            newFragment.setLayerGroup(groupLayer)
                    .setLayerType(LayerFillService.TMS_LAYER)
                    .setUri(uri)
                    .setLayerName(layerName)
                    .setTitle(context.getString(R.string.create_tms_layer))
                    .setTheme(fragmentActivity.getThemeId())
                    .show(fragmentActivity.getSupportFragmentManager(), "create_tms_layer");
        }
    }


    @Override
    public void createNewVectorLayer(
            final Context context,
            final LayerGroup groupLayer,
            final Uri uri)
    {
        String layerName =
                FileUtil.getFileNameByUri(context, uri, context.getString(R.string.new_layer));
        final int lastPeriodPos = layerName.lastIndexOf('.');
        if (lastPeriodPos > 0) {
            layerName = layerName.substring(0, lastPeriodPos);
        }
        if (context instanceof NGActivity) {
            NGActivity fragmentActivity = (NGActivity) context;
            CreateLocalLayerDialog newFragment = new CreateLocalLayerDialog();
            newFragment.setLayerGroup(groupLayer)
                    .setLayerType(LayerFillService.VECTOR_LAYER)
                    .setUri(uri)
                    .setLayerName(layerName)
                    .setTitle(context.getString(R.string.create_vector_layer))
                    .setTheme(fragmentActivity.getThemeId())
                    .show(fragmentActivity.getSupportFragmentManager(), "create_vector_layer");
        }
    }


    @Override
    public void createNewVectorLayerWithForm(
            final Context context,
            final LayerGroup groupLayer,
            final Uri uri)
    {
        String layerName =
                FileUtil.getFileNameByUri(context, uri, context.getString(R.string.new_layer));
        final int lastPeriodPos = layerName.lastIndexOf('.');
        if (lastPeriodPos > 0) {
            layerName = layerName.substring(0, lastPeriodPos);
        }
        if (context instanceof NGActivity) {
            NGActivity fragmentActivity = (NGActivity) context;
            CreateLocalLayerDialog newFragment = new CreateLocalLayerDialog();
            newFragment.setLayerGroup(groupLayer)
                    .setLayerType(LayerFillService.VECTOR_LAYER_WITH_FORM)
                    .setUri(uri)
                    .setLayerName(layerName)
                    .setTitle(context.getString(R.string.create_vector_layer))
                    .setTheme(fragmentActivity.getThemeId())
                    .show(fragmentActivity.getSupportFragmentManager(), "create_vector_with_form_layer");
        }
    }


    public void createNewRemoteTMSLayer(
            final Context context,
            final LayerGroup groupLayer)
    {
        if (context instanceof NGActivity) {
            NGActivity fragmentActivity = (NGActivity) context;
            CreateFromQMSLayerDialog newFragment = new CreateFromQMSLayerDialog();
            newFragment.setLayerGroup(groupLayer)
                    .setTitle(context.getString(R.string.create_qms_layer))
                    .setTheme(fragmentActivity.getThemeId())
                    .show(fragmentActivity.getSupportFragmentManager(), "create_qms_layer");
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
                case LAYERTYPE_NGW_VECTOR:
                    layer = new NGWVectorLayerUI(context, path);
                    break;
                case LAYERTYPE_NGW_WEBMAP:
                    layer = new NGWWebMapLayerUI(context, path);
                    break;
                case LAYERTYPE_NGW_TRACKS:
                    layer = new NGWTrackLayer(context, path);
                    break;
                case LAYERTYPE_LOCAL_VECTOR:
                    layer = new VectorLayerUI(context, path);
                    break;
                case LAYERTYPE_LOCAL_TMS:
                    layer = new LocalTMSLayerUI(context, path);
                    break;
                case LAYERTYPE_GROUP:
                    layer = new LayerGroupUI(context, path, this);
                    break;
                case LAYERTYPE_TRACKS:
                    layer = new TrackLayerUI(context, path);
                    break;
                case LAYERTYPE_LOOKUPTABLE:
                    layer = new NGWLookupTable(context, path); // TODO: 26.07.15 Do we need UI for this?
                    break;
            }
        } catch (IOException | JSONException e) {
            Log.d(TAG, e.getLocalizedMessage());
        }

        return layer;
    }

    @Override
    public String getLayerTypeString(
            Context context,
            int type)
    {
        switch (type) {
            case LAYERTYPE_GROUP:
                return context.getString(R.string.layer_group);
            case LAYERTYPE_NGW_RASTER:
                return context.getString(R.string.layer_ngw_raster);
            case LAYERTYPE_NGW_VECTOR:
                return context.getString(R.string.layer_ngw_vector);
            case LAYERTYPE_REMOTE_TMS:
                return context.getString(R.string.layer_tms);
            case LAYERTYPE_LOCAL_VECTOR:
                return context.getString(R.string.layer_vector);
            case LAYERTYPE_LOCAL_TMS:
                return context.getString(R.string.layer_tms);
            case LAYERTYPE_LOOKUPTABLE:
                return context.getString(R.string.layer_lookuptable);
            case LAYERTYPE_NGW_WEBMAP:
                return context.getString(R.string.web_map);
            default:
                return context.getString(R.string.layer_na);
        }
    }
}

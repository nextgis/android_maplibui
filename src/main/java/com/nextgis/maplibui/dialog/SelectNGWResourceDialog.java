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

package com.nextgis.maplibui.dialog;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Connections;
import com.nextgis.maplib.datasource.ngw.INGWResource;
import com.nextgis.maplib.datasource.ngw.LayerWithStyles;
import com.nextgis.maplib.datasource.ngw.WebMap;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWRasterLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGWLoginActivity;
import com.nextgis.maplibui.fragment.LayerFillProgressDialogFragment;
import com.nextgis.maplibui.mapui.NGWRasterLayerUI;
import com.nextgis.maplibui.mapui.NGWWebMapLayerUI;
import com.nextgis.maplibui.service.LayerFillService;
import com.nextgis.maplibui.util.CheckState;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;


public class SelectNGWResourceDialog
        extends NGDialog
{
    protected LayerGroup mGroupLayer;
    protected int        mTypeMask;

    protected NGWResourcesListAdapter mListAdapter;
    protected AlertDialog             mDialog;
    protected AccountManager          mAccountManager;
    protected NGWResourcesListAdapter.OnConnectionListener mConnectionListener;

    protected final static String KEY_MASK        = "mask";
    protected final static String KEY_ID          = "id";
    protected final static String KEY_CONNECTIONS = "connections";
    protected final static String KEY_RESOURCE_ID = "resource_id";
    protected final static String KEY_STATES      = "states";

    protected final static int ADD_ACCOUNT_CODE = 777;


    public SelectNGWResourceDialog setLayerGroup(LayerGroup groupLayer)
    {
        mGroupLayer = groupLayer;
        return this;
    }


    public SelectNGWResourceDialog setTypeMask(int typeMask)
    {
        mTypeMask = typeMask;
        return this;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        super.onCreateDialog(savedInstanceState);
        mAccountManager = AccountManager.get(getActivity().getApplicationContext());
        Log.d(TAG, "SelectNGWDialog: AccountManager.get(" + getActivity().getApplicationContext() + ")");
        mListAdapter = new NGWResourcesListAdapter(getActivity());

        if (null == savedInstanceState) {
            //first launch, lets fill connections array
            Connections connections = fillConnections(getActivity(), mAccountManager);
            mListAdapter.setConnections(connections);
            mListAdapter.setCurrentResourceId(connections.getId());
            mListAdapter.setCheckState(new ArrayList<CheckState>());
        } else {
            mTypeMask = savedInstanceState.getInt(KEY_MASK);
            int id = savedInstanceState.getInt(KEY_ID);
            MapBase map = MapBase.getInstance();
            if (null != map) {
                ILayer iLayer = map.getLayerById(id);
                if (iLayer instanceof LayerGroup) {
                    mGroupLayer = (LayerGroup) iLayer;
                }
            }

            mListAdapter.setConnections((Connections) savedInstanceState.getParcelable(KEY_CONNECTIONS));
            mListAdapter.setCurrentResourceId(savedInstanceState.getInt(KEY_RESOURCE_ID));
            mListAdapter.setCheckState(savedInstanceState.<CheckState> getParcelableArrayList(KEY_STATES));
        }

        View view = View.inflate(mContext, R.layout.layout_resources, null);
        ListView dialogListView = (ListView) view.findViewById(R.id.listView);
        mListAdapter.setTypeMask(mTypeMask);
        dialogListView.setAdapter(mListAdapter);
        dialogListView.setOnItemClickListener(mListAdapter);

        LinearLayout pathView = (LinearLayout) view.findViewById(R.id.path);
        mListAdapter.setPathLayout(pathView);

//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, mDialogTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mTitle)
                .setIcon(R.drawable.ic_ngw)
                .setView(view)
                .setInverseBackgroundForced(true)
                .setPositiveButton(
                        R.string.add, new DialogInterface.OnClickListener()
                        {
                            public void onClick(
                                    DialogInterface dialog,
                                    int id)
                            {
                                createLayers(mContext);
                            }
                        })
                .setNegativeButton(R.string.cancel, null);

        // Create the AlertDialog object and return it
        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                mEnabledColor = mDialog.getButton(DialogInterface.BUTTON_POSITIVE).getTextColors().getDefaultColor();
                updateSelectButton();
            }
        });

        mListAdapter.setConnectionListener(mConnectionListener);

        return mDialog;
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (null != mGroupLayer)
            outState.putInt(KEY_ID, mGroupLayer.getId());

        if (null != mListAdapter) {
            outState.putInt(KEY_MASK, mTypeMask);
            outState.putInt(KEY_RESOURCE_ID, mListAdapter.getCurrentResourceId());
            outState.putParcelable(KEY_CONNECTIONS, mListAdapter.getConnections());
            outState.putParcelableArrayList(
                    KEY_STATES,
                    (ArrayList<? extends android.os.Parcelable>) mListAdapter.getCheckState());
        }
    }


    public NGDialog setConnectionListener(NGWResourcesListAdapter.OnConnectionListener connectionListener) {
        mConnectionListener = connectionListener;
        return this;
    }


    public static Connections fillConnections(Context context, AccountManager accountManager)
    {
        Connections connections = new Connections(context.getString(R.string.ngw_accounts));
        IGISApplication app = (IGISApplication) context.getApplicationContext();

        for (Account account : accountManager.getAccountsByType(app.getAccountsType())) {
            String url = app.getAccountUrl(account);
            String password = app.getAccountPassword(account);
            String login = app.getAccountLogin(account);
            connections.add(new Connection(account.name, login, password, url.toLowerCase()));
        }
        return connections;
    }


    public void onAddAccount(Context context)
    {
        Intent intent = new Intent(context, NGWLoginActivity.class);
        startActivityForResult(intent, ADD_ACCOUNT_CODE);
    }


    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data)
    {
        if (requestCode == ADD_ACCOUNT_CODE) {
            if (resultCode != Activity.RESULT_CANCELED) {
                //search new account and add it
                Connections connections = mListAdapter.getConnections();
                IGISApplication app = (IGISApplication) mContext.getApplicationContext();

                for (Account account : mAccountManager.getAccountsByType(app.getAccountsType())) {
                    boolean find = false;
                    for (int i = 0; i < connections.getChildrenCount(); i++) {
                        Connection connection = (Connection) connections.getChild(i);
                        if (null != connection && connection.getName().equals(account.name)) {
                            find = true;
                            break;
                        }
                    }

                    if (!find) {
                        String url = app.getAccountUrl(account);
                        String password = app.getAccountPassword(account);
                        String login = app.getAccountLogin(account);
                        connections.add(new Connection(account.name, login, password, url.toLowerCase()));
                        mListAdapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    public void updateSelectButton() {
        boolean active = mListAdapter.getCheckState().size() > 0;
        setEnabled(mDialog.getButton(AlertDialog.BUTTON_POSITIVE), active);
    }


    public void createLayers(Context context) {
        if (mGroupLayer == null)
            return;

        setEnabled(mDialog.getButton(AlertDialog.BUTTON_POSITIVE), false);
        setEnabled(mDialog.getButton(AlertDialog.BUTTON_NEGATIVE), false);

        List<CheckState> checkStates = mListAdapter.getCheckState();
        Connections connections = mListAdapter.getConnections();
        for (CheckState checkState : checkStates) {
            if (checkState.isCheckState1()) { //create raster

                INGWResource resource = connections.getResourceById(checkState.getId());
                if (resource instanceof LayerWithStyles) {
                    LayerWithStyles layer = (LayerWithStyles) resource;
                    //1. get first style
                    Connection connection = layer.getConnection();
                    //2. create tiles url
                    String layerURL = layer.getTMSUrl(0);

                    if (layerURL == null) {
                        Toast.makeText(
                                context, getString(R.string.error_layer_create),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!layerURL.startsWith("http")) {
                        layerURL = "http://" + layerURL;
                    }
                    //3. create layer
                    String layerName = layer.getName();

                    NGWRasterLayer newLayer;
                    if (resource instanceof WebMap) {
                        NGWWebMapLayerUI webmap = new NGWWebMapLayerUI(mGroupLayer.getContext(), mGroupLayer.createLayerStorage());
                        webmap.setChildren(((WebMap) resource).getChildren());
                        newLayer = webmap;
                    } else
                        newLayer = new NGWRasterLayerUI(mGroupLayer.getContext(), mGroupLayer.createLayerStorage());

                    newLayer.setName(layerName);
                    newLayer.setURL(layerURL);
                    newLayer.setTMSType(TMSTYPE_OSM);
                    newLayer.setVisible(true);
                    newLayer.setAccountName(connection.getName());
                    newLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
                    newLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

                    mGroupLayer.addLayer(newLayer);
                    mGroupLayer.save();
                }
            }

            if (checkState.isCheckState2()) { //create vector
                INGWResource resource = connections.getResourceById(checkState.getId());
                if (resource instanceof LayerWithStyles) {
                    LayerWithStyles layer = (LayerWithStyles) resource;
                    //1. get connection for url
                    Connection connection = layer.getConnection();
                    // create or connect to fill layer with features
                    Intent intent = new Intent(context, LayerFillService.class);
                    intent.setAction(LayerFillService.ACTION_ADD_TASK);
                    intent.putExtra(LayerFillService.KEY_NAME, layer.getName());
                    intent.putExtra(LayerFillService.KEY_ACCOUNT, connection.getName());
                    intent.putExtra(LayerFillService.KEY_REMOTE_ID, layer.getRemoteId());
                    intent.putExtra(LayerFillService.KEY_LAYER_GROUP_ID, mGroupLayer.getId());
                    intent.putExtra(LayerFillService.KEY_INPUT_TYPE, LayerFillService.NGW_LAYER);

                    LayerFillProgressDialogFragment.startFill(intent);
                }
            }
        }
        mGroupLayer.save();
    }

}

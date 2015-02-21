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

package com.nextgis.maplibui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Connections;
import com.nextgis.maplib.datasource.ngw.INGWResource;
import com.nextgis.maplib.datasource.ngw.LayerWithStyles;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplibui.mapui.NGWRasterLayerUI;
import com.nextgis.maplibui.mapui.NGWVectorLayerUI;
import com.nextgis.maplibui.util.CheckState;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.NGW_ACCOUNT_TYPE;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;


public class SelectNGWResourceDialog extends DialogFragment
{
    protected String mTitle;
    protected LayerGroup mGroupLayer;
    protected int mTypeMask;

    protected NGWResourcesListAdapter mListAdapter;
    protected AlertDialog mDialog;

    protected final static String KEY_TITLE = "title";
    protected final static String KEY_MASK = "mask";
    protected final static String KEY_ID = "id";
    protected final static String KEY_CONNECTIONS = "connections";
    protected final static String KEY_RESOURCEID = "resource_id";
    protected final static String KEY_STATES = "states";

    protected final static int ADDACCOUNT_CODE = 777;

    public SelectNGWResourceDialog setTitle(String title)
    {
        mTitle = title;
        return this;
    }


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
        LayoutInflater inflater = getActivity().getLayoutInflater();


        mListAdapter = new NGWResourcesListAdapter(this);

        if(null == savedInstanceState){
            //first launch, lets fill connections array
            Connections connections = fillConnections();
            mListAdapter.setConnections(connections);
            mListAdapter.setCurrentResourceId(connections.getId());
            mListAdapter.setCheckState(new ArrayList<CheckState>());
        }
        else{
            mTitle = savedInstanceState.getString(KEY_TITLE);
            mTypeMask = savedInstanceState.getInt(KEY_MASK);
            short id = savedInstanceState.getShort(KEY_ID);
            MapBase map = MapBase.getInstance();
            if(null != map){
                ILayer iLayer = map.getLayerById(id);
                if(iLayer instanceof LayerGroup)
                    mGroupLayer = (LayerGroup)iLayer;
            }

            mListAdapter.setConnections(
                    (Connections) savedInstanceState.getParcelable(KEY_CONNECTIONS));
            mListAdapter.setCurrentResourceId(savedInstanceState.getInt(KEY_RESOURCEID));
            mListAdapter.setCheckState(savedInstanceState.<CheckState> getParcelableArrayList(
                    KEY_STATES));
        }

        View view = inflater.inflate(R.layout.layout_resources, null);
        ListView dialogListView = (ListView) view.findViewById(R.id.listView);
        mListAdapter.setTypeMask(mTypeMask);
        dialogListView.setAdapter(mListAdapter);
        dialogListView.setOnItemClickListener(mListAdapter);

        LinearLayout pathView = (LinearLayout) view.findViewById(R.id.path);
        mListAdapter.setPathLayout(pathView);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitle)
               .setIcon(R.drawable.ic_ngw)
               .setView(view)
               .setPositiveButton(R.string.select, new DialogInterface.OnClickListener()
               {
                   public void onClick(
                           DialogInterface dialog,
                           int id)
                   {
                        createLayers();
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
               {
                   public void onClick(
                           DialogInterface dialog,
                           int id)
                   {
                       // User cancelled the dialog
                   }
               });
        // Create the AlertDialog object and return it
        mDialog = builder.create();
        return mDialog;
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString(KEY_TITLE, mTitle);
        outState.putInt(KEY_MASK, mTypeMask);
        outState.putShort(KEY_ID, mGroupLayer.getId());
        outState.putInt(KEY_RESOURCEID, mListAdapter.getCurrentResourceId());
        outState.putParcelable(KEY_CONNECTIONS, mListAdapter.getConnections());
        outState.putParcelableArrayList(KEY_STATES,
                                        (ArrayList<? extends android.os.Parcelable>) mListAdapter.getCheckState());
        super.onSaveInstanceState(outState);
    }

    protected Connections fillConnections(){
        Connections connections = new Connections(getString(R.string.accounts));
        final AccountManager accountManager = AccountManager.get(getActivity());
        for (Account account : accountManager.getAccountsByType(
                NGW_ACCOUNT_TYPE)) {
            String url = accountManager.getUserData(account, "url");
            String password = accountManager.getPassword(account);
            String login = accountManager.getUserData(account, "login");
            connections.add(new Connection(account.name, login, password, url));
        }
        return connections;
    }

    public void onAddAccount(){
        Intent intent = new Intent(getActivity(), NGWLoginActivity.class);
        startActivityForResult(intent, ADDACCOUNT_CODE);
    }


    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data)
    {
        if (requestCode == ADDACCOUNT_CODE) {
            if(resultCode != Activity.RESULT_CANCELED){
                //search new account and add it
                final AccountManager accountManager = AccountManager.get(getActivity());
                Connections connections = mListAdapter.getConnections();
                for (Account account : accountManager.getAccountsByType(
                        NGW_ACCOUNT_TYPE)) {
                    boolean find = false;
                    for(int i = 0; i < connections.getChildrenCount(); i++){
                        Connection connection = (Connection) connections.getChild(i);
                        if(null != connection && connection.getName().equals(account.name) ){
                            find = true;
                            break;
                        }
                    }

                    if(!find) {
                        String url = accountManager.getUserData(account, "url");
                        String password = accountManager.getPassword(account);
                        String login = accountManager.getUserData(account, "login");
                        connections.add(new Connection(account.name, login, password, url));
                        mListAdapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onStart()
    {
        super.onStart();
        updateSelectButton();
    }

    public void updateSelectButton()
    {
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(mListAdapter.getCheckState().size() > 0);
    }

    public void createLayers()
    {
        List<CheckState> checkStates = mListAdapter.getCheckState();
        Connections connections = mListAdapter.getConnections();
        for(CheckState checkState : checkStates){
            if(checkState.isCheckState1()) { //create raster

                INGWResource resource = connections.getResourceById(checkState.getId());
                if(resource instanceof LayerWithStyles){
                    LayerWithStyles layer = (LayerWithStyles)resource;
                    //1. get first style
                    Connection connection = layer.getConnection();
                    //2. create tiles url
                    String layerURL = layer.getTMSUrl(0);

                    if(layerURL == null){
                        Toast.makeText(getActivity(), getString(R.string.error_layer_create),
                                       Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(!layerURL.startsWith("http"))
                        layerURL = "http://" + layerURL;
                    //3. create layer
                    String layerName = layer.getName();

                    NGWRasterLayerUI newLayer = new NGWRasterLayerUI(mGroupLayer.getContext(), mGroupLayer.createLayerStorage());
                    newLayer.setName(layerName);
                    newLayer.setURL(layerURL);
                    newLayer.setTMSType(TMSTYPE_OSM);
                    newLayer.setVisible(true);
                    newLayer.setAccountName(connection.getName());
                    newLayer.setLogin(connection.getLogin());
                    newLayer.setPassword(connection.getPassword());

                    mGroupLayer.addLayer(newLayer);
                }
            }

            if(checkState.isCheckState2()){ //create vector
                INGWResource resource = connections.getResourceById(checkState.getId());
                if(resource instanceof LayerWithStyles){
                    LayerWithStyles layer = (LayerWithStyles)resource;
                    //1. get connection for url
                    Connection connection = layer.getConnection();

                    //3. create layer
                    String layerName = layer.getName();

                    final NGWVectorLayerUI newLayer = new NGWVectorLayerUI(mGroupLayer.getContext(), mGroupLayer.createLayerStorage());
                    newLayer.setName(layerName);
                    newLayer.setURL(connection.getURL());
                    newLayer.setRemoteId(layer.getRemoteId());
                    newLayer.setVisible(true);
                    newLayer.setAccountName(connection.getName());
                    newLayer.setLogin(connection.getLogin());
                    newLayer.setPassword(connection.getPassword());

                    mGroupLayer.addLayer(newLayer);

                    newLayer.downloadAsync();
                }
            }
        }
        mGroupLayer.save();

        Toast.makeText(getActivity(), getString(R.string.message_layer_created),
                       Toast.LENGTH_SHORT).show();
    }

}

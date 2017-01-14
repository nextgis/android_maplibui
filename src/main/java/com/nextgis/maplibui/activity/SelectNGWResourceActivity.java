/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016-2017 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.activity;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Connections;
import com.nextgis.maplib.datasource.ngw.INGWResource;
import com.nextgis.maplib.datasource.ngw.LayerWithStyles;
import com.nextgis.maplib.datasource.ngw.ResourceGroup;
import com.nextgis.maplib.datasource.ngw.WebMap;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWRasterLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.dialog.NGWResourcesListAdapter;
import com.nextgis.maplibui.fragment.LayerFillProgressDialogFragment;
import com.nextgis.maplibui.mapui.NGWRasterLayerUI;
import com.nextgis.maplibui.mapui.NGWWebMapLayerUI;
import com.nextgis.maplibui.service.LayerFillService;
import com.nextgis.maplibui.util.CheckState;
import com.nextgis.maplibui.util.NGWCreateNewResourceTask;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;

public class SelectNGWResourceActivity extends NGActivity implements View.OnClickListener {
    public final static int TYPE_ADD = 10;
    public final static int TYPE_SELECT = 11;

    public final static String KEY_CONNECTIONS = "connections";
    public final static String KEY_RESOURCE_ID = "resource_id";
    public final static String KEY_MASK        = "mask";
    public final static String KEY_TASK        = "type";
    public final static String KEY_GROUP_ID    = "group_id";
    public final static String KEY_PUSH_ID     = "local_id";
    protected final static String KEY_STATES   = "states";

    protected VectorLayer mLayer;
    protected LayerGroup mGroupLayer;
    protected NGWResourcesListAdapter mListAdapter;
    protected AccountManager mAccountManager;
    protected IGISApplication mApp;

    protected Toolbar mToolbar;
    protected Button mButton;

    protected int mTypeMask, mTask, mPushId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resources);
        setToolbar(R.id.main_toolbar);

        mApp = (IGISApplication) getApplication();
        mAccountManager = AccountManager.get(this);
        Log.d(TAG, "SelectNGWActivity: AccountManager.get(" + this + ")");

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        mButton = (Button) findViewById(R.id.button1);
        mButton.setOnClickListener(this);

        mListAdapter = new NGWResourcesListAdapter(this);
        mListAdapter.setShowAccounts(false);
        ListView dialogListView = (ListView) findViewById(R.id.listView);
        dialogListView.setAdapter(mListAdapter);
        dialogListView.setOnItemClickListener(mListAdapter);
        LinearLayout pathView = (LinearLayout) findViewById(R.id.path);
        mListAdapter.setPathLayout(pathView);

        if (mTask == TYPE_ADD) {
            mTypeMask = Connection.NGWResourceTypePostgisLayer |
                    Connection.NGWResourceTypeVectorLayer | Connection.NGWResourceTypeRasterLayer |
                    Connection.NGWResourceTypeWMSClient | Connection.NGWResourceTypeWebMap;
        } else {
            mTypeMask = Connection.NGWResourceTypeResourceGroup | Connection.NGWResourceTypePostgisLayer |
                    Connection.NGWResourceTypeVectorLayer | Connection.NGWResourceTypeRasterLayer |
                    Connection.NGWResourceTypeWMSClient | Connection.NGWResourceTypeWebMap;
        }

        int id = mPushId = NOT_FOUND;
        Bundle bundle = savedInstanceState;
        if (null == savedInstanceState)
            if (getIntent() != null)
                bundle = getIntent().getExtras();

        if (bundle != null) {
            mTask = bundle.getInt(KEY_TASK);
            id = bundle.getInt(KEY_GROUP_ID, id);
            mPushId = bundle.getInt(KEY_PUSH_ID, mPushId);
            mTypeMask = bundle.getInt(KEY_MASK, mTypeMask);
            mListAdapter.setConnections((Connections) bundle.getParcelable(KEY_CONNECTIONS));
            mListAdapter.setCurrentResourceId(bundle.getInt(KEY_RESOURCE_ID));

            ArrayList<CheckState> states = bundle.getParcelableArrayList(KEY_STATES);
            if (states == null)
                states = new ArrayList<>();
            mListAdapter.setCheckState(states);
        }

        MapBase map = MapBase.getInstance();
        if (null != map) {
            ILayer layer = map.getLayerById(id);
            if (layer instanceof LayerGroup)
                mGroupLayer = (LayerGroup) layer;

            layer = map.getLayerById(mPushId);
            if (layer instanceof VectorLayer && mPushId != NOT_FOUND)
                mLayer = (VectorLayer) layer;
        }

        mListAdapter.setShowCheckboxes(mTask == TYPE_ADD);
        mListAdapter.setTypeMask(mTypeMask);
        String instance = getConnection().getName();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mTask == TYPE_ADD ? R.string.import_ : R.string.export);
            getSupportActionBar().setSubtitle(instance);
        }
    }

    @Override
    public void onBackPressed() {
        if (!mListAdapter.isAccountsDisabled())
            mListAdapter.goUp();
        else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ngw_resource, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_new_group) {
            final EditText view = (EditText) View.inflate(this, R.layout.dialog_edittext, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.new_group_name).setView(view)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, null);

            final AlertDialog dialog = builder.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Editable text = view.getText();
                    if (text.length() < 1) {
                        Toast.makeText(v.getContext(), R.string.field_not_filled, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long id = getRemoteResourceId();
                    Connection connection = getConnection();
                    if (connection != null && id != NOT_FOUND) {
                        new NGWCreateNewResourceTask(SelectNGWResourceActivity.this, connection, id).setName(text.toString()).execute();
                        mListAdapter.refresh();
                    }

                    dialog.dismiss();
                }
            });
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_TASK, mTask);
        outState.putInt(KEY_MASK, mTypeMask);

        if (null != mGroupLayer)
            outState.putInt(KEY_GROUP_ID, mGroupLayer.getId());

        if(null != mListAdapter) {
            outState.putInt(KEY_RESOURCE_ID, mListAdapter.getCurrentResourceId());
            outState.putParcelable(KEY_CONNECTIONS, mListAdapter.getConnections());
            outState.putParcelableArrayList(KEY_STATES, (ArrayList<? extends android.os.Parcelable>) mListAdapter.getCheckState());
        }
    }

    public boolean createLayers() {
        if (mGroupLayer == null)
            return false;

        List<CheckState> checkStates = mListAdapter.getCheckState();
        if (checkStates.size() == 0) {
            Toast.makeText(this, R.string.nothing_selected, Toast.LENGTH_SHORT).show();
            return false;
        }

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
                        Toast.makeText(this, getString(R.string.error_layer_create), Toast.LENGTH_SHORT).show();
                        return false;
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
                    } else {
                        newLayer = new NGWRasterLayerUI(mGroupLayer.getContext(), mGroupLayer.createLayerStorage());
                        newLayer.getExtents().set(layer.getExtent());
                    }

                    newLayer.setName(layerName);
                    newLayer.setRemoteId(layer.getRemoteId());
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
                    Intent intent = new Intent(this, LayerFillService.class);
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
        return true;
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.button1) {
            switch (mTask) {
                case TYPE_ADD:
                    if (createLayers())
                        finish();
                    break;
                case TYPE_SELECT:
                    long id = getRemoteResourceId();
                    Connection connection = getConnection();
                    if (connection != null && mLayer != null && id != NOT_FOUND) {
                        new NGWCreateNewResourceTask(this, connection, id).setLayer(mLayer).execute();
                        finish();
                    }
                    break;
            }
        }
    }

    public Connection getConnection() {
        return (Connection) mListAdapter.getConnections().getChild(0);
    }

    public long getRemoteResourceId() {
        long id = NOT_FOUND;
        INGWResource resource = mListAdapter.getCurrentResource();

        if (resource instanceof ResourceGroup)
            id = ((ResourceGroup) resource).getRemoteId();
        else if (resource instanceof Connection)
            id = ((Connection) resource).getRootResource().getRemoteId();

        return id;
    }
}

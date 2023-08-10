/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import com.evrencoskun.tableview.TableView;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.adapter.attributes.ICellCLickListener;
import com.nextgis.maplibui.adapter.attributes.TableViewAdapter;
import com.nextgis.maplibui.adapter.attributes.TableViewListener;
import com.nextgis.maplibui.adapter.attributes.TableViewModel;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.fragment.BottomToolbar;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.TAG;

public class AttributesActivity extends NGActivity {
    protected VectorLayer mLayer;
    protected BottomToolbar mToolbar;

    protected  TableView mTableView;
    RelativeLayout mProgressBar;
    TextView progressText;

    boolean nullFeatureExist = false;
    protected Long mId;
    protected int mLayerId;
    protected BroadcastReceiver mReceiver;

    int selectedRow = -1;
    protected boolean mLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attributes);
        setToolbar(R.id.main_toolbar);

        mToolbar = (BottomToolbar) findViewById(R.id.bottom_toolbar);
        mTableView= findViewById(R.id.my_TableView);
        mProgressBar= findViewById(R.id.progressArea);
        progressText = findViewById(R.id.progressText);
        mToolbar.inflateMenu(R.menu.attributes_table);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int i = item.getItemId();
                if (i == R.id.menu_zoom) {
                    IGISApplication application = (IGISApplication) getApplication();
                    MapDrawable map = (MapDrawable) application.getMap();
                    if (null != map) {
                        if (mLayer.getGeometryType() == GeoConstants.GTPoint || mLayer.getGeometryType() == GeoConstants.GTMultiPoint)
                            map.zoomToExtent(mLayer.getFeature(mId).getGeometry().getEnvelope(), 18);
                        else
                            map.zoomToExtent(mLayer.getFeature(mId).getGeometry().getEnvelope());

                        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AttributesActivity.this).edit();
                        edit.putFloat(SettingsConstantsUI.KEY_PREF_ZOOM_LEVEL, map.getZoomLevel());
                        GeoPoint point = map.getMapCenter();
                        edit.putLong(SettingsConstantsUI.KEY_PREF_SCROLL_X, Double.doubleToRawLongBits(point.getX()));
                        edit.putLong(SettingsConstantsUI.KEY_PREF_SCROLL_Y, Double.doubleToRawLongBits(point.getY()));
                        edit.commit();
                    }

                    finish();
                    return true;
                } else if (i == R.id.menu_delete) {
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.container), getString(R.string.delete_item_done), Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {

                                }
                            })
                            .setCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar snackbar, int event) {
                                    super.onDismissed(snackbar, event);
                                    if (event == DISMISS_EVENT_MANUAL)
                                        return;
                                    if (event != DISMISS_EVENT_ACTION) {
                                        mLayer.deleteAddChanges(mId);

                                        if (selectedRow != -1) {
                                            mTableView.getAdapter().removeRow(selectedRow, true);
                                            selectedRow = -1;
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        mTableView.getSelectionHandler().clearSelection();
                                                    }catch (Exception ex){
                                                        Log.e(TAG, ex.getMessage());
                                                    }
                                                }
                                            }, 100);
                                        }
                                    }
                                }

                                @Override
                                public void onShown(Snackbar snackbar) {
                                    super.onShown(snackbar);
                                }
                            });

                    View view = snackbar.getView();
                    TextView textView = (TextView) view.findViewById(R.id.snackbar_text);
                    textView.setTextColor(ContextCompat.getColor(view.getContext(), com.nextgis.maplibui.R.color.color_white));
                    snackbar.show();
                    mToolbar.setVisibility(View.GONE);

                    return true;
                } else if (i == R.id.menu_edit) {
                    ((IVectorLayerUI) mLayer).showEditForm(AttributesActivity.this, mId, null);
                    return true;
                }

                return false;
            }
        });
        mToolbar.setNavigationIcon(R.drawable.ic_action_cancel_dark);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mToolbar.setVisibility(View.GONE);
                mTableView.getSelectionHandler().clearSelection();
            }
        });
        mToolbar.setVisibility(View.GONE);

        mLayerId = Constants.NOT_FOUND;
        if (savedInstanceState != null)
            mLayerId = savedInstanceState.getInt(ConstantsUI.KEY_LAYER_ID);
        else
            mLayerId = getIntent().getIntExtra(ConstantsUI.KEY_LAYER_ID, mLayerId);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //mTable.setAdapter(getAdapter());
                // update move to delete only one row in table - not re-load all items
            }
        };
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.NOTIFY_DELETE);
        intentFilter.addAction(Constants.NOTIFY_DELETE_ALL);
        registerReceiver(mReceiver, intentFilter);

        IGISApplication application = (IGISApplication) getApplication();
        MapBase map = application.getMap();

        if (null != map) {
            ILayer layer = map.getLayerById(mLayerId);
            if (null != layer && layer instanceof VectorLayer) {
                mLayer = (VectorLayer) layer;

                LoadBigData loadBigDataTask = new LoadBigData(this, mLayer, progressText);
                loadBigDataTask.execute();

                String [][] data = new String[0][0];
                String [][] data0row = new String[0][0];
                String [][] data0Column = new String[0][0];

                TableViewModel tableViewModel = new TableViewModel(data.length > 0 ? data[0].length : 0, data.length, data, data0row, data0Column);
                TableViewAdapter tableViewAdapter = new TableViewAdapter(tableViewModel);

                mTableView.setAdapter(tableViewAdapter);


                final ICellCLickListener iCellCLickListener = new ICellCLickListener() {
                    @Override
                    public void onCellClick(View view, int row) {
                        if (view != null && view instanceof AppCompatTextView) {
                            Long id = parseLong((String) view.getTag());
                            if (id != null) {
                                mId = id;
                                String featureName = String.format(getString(R.string.feature_n), id);
                                mToolbar.setTitle(featureName);
                                String labelField = mLayer.getPreferences().getString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, FIELD_ID);
                                if (!labelField.equals(FIELD_ID)) {
                                    Feature feature = mLayer.getFeature(id);
                                    if (feature != null) {
                                        mToolbar.setSubtitle(featureName);
                                        featureName = feature.getFieldValueAsString(labelField);
                                        mToolbar.setTitle(featureName);
                                    }
                                }
                                mToolbar.setVisibility(View.VISIBLE);
                            }
                        }
                        mTableView.setSelectedRow(row);
                        selectedRow = row;
                    }
                };

                final ICellCLickListener iColumnHeadCLickListener = new ICellCLickListener() {
                    @Override
                    public void onCellClick(View view, int row) {
                        if (mToolbar.getVisibility() == View.VISIBLE) {
                            mTableView.getSelectionHandler().clearSelection();
                            mToolbar.setVisibility(View.GONE);
                        }
                    }
                };

                mTableView.setTableViewListener(new TableViewListener(mTableView, iCellCLickListener, iColumnHeadCLickListener));
                tableViewAdapter.setAllItems(
                        tableViewModel.getColumnHeaderList(),
                        tableViewModel.getRowHeaderList(),
                        tableViewModel.getCellList());

                Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
                toolbar.setSubtitle(mLayer.getName());

            } else
                Toast.makeText(this, R.string.error_layer_not_inited, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ConstantsUI.KEY_LAYER_ID, mLayer.getId());
    }



    private Long parseLong(String string) {
        try {
            return Long.parseLong(string);
        } catch (Exception e) {
            return null;
        }
    }

    protected class LoadBigData extends AsyncTask<Void, Integer, String>{
        final Context mContext;
        final VectorLayer layer;

        String [][] data = new String[0][0];
        String [][] data0row = new String[0][0];
        String [][] data0Column = new String[0][0];

        final TextView progressText;

        public LoadBigData(
                Context context,
                VectorLayer layer,
                TextView progressText) {
            super();
            mContext = context;
            this.layer = layer;
            this.progressText = progressText;
        }

        @Override
        protected void onPreExecute()        {
            mLoading = true;
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            this.progressText.setText("Progress: " + String.valueOf(values [0]) + "%");
        }

        @Override
        protected String doInBackground(Void... voids) {
            publishProgress(0);
            List<Long> ids = mLayer.query(null);
            Map<Long, Feature> featureMap = mLayer.getFeatures();
            if (featureMap == null)
                return "";
            data = getData(featureMap, ids, false, false);
            data0row = getData(featureMap, ids, true, false);
            data0Column = getData(featureMap, ids, false, true);
            return "";
        }

        @Override
        protected void onPostExecute(String error)
        {
            mProgressBar.setVisibility(View.GONE);
            TableViewModel tableViewModel = new TableViewModel(data.length > 0 ? data[0].length : 0, data.length, data, data0row, data0Column);
            TableViewAdapter tableViewAdapter = new TableViewAdapter(tableViewModel);

            mTableView.setAdapter(tableViewAdapter);
            tableViewAdapter.setAllItems(
                    tableViewModel.getColumnHeaderList(),
                    tableViewModel.getRowHeaderList(),
                    tableViewModel.getCellList());

            Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
            toolbar.setSubtitle(mLayer.getName());

            mLoading = false;

            if (nullFeatureExist){
                nullFeatureExist = false;
                Toast.makeText(mContext, R.string.error_cache, Toast.LENGTH_LONG).show();
            }

            if (data.length == 0){
                Toast.makeText(mContext, "no data in layer", Toast.LENGTH_LONG).show();
            }
        }

        public String[][] getData(Map<Long, Feature> featureMap, final List<Long> ids, boolean get0row, boolean get0column ) {
            if (get0column){
                String[][] data0col = new String[ids.size()][1];
                for (int i = 0; i < ids.size(); i++) {
                    Feature feature = featureMap.get(ids.get(i));
                    if (feature != null)
                        data0col[i][0] = feature.getId() + "";
                }
                return data0col;
            }
            List<Field> fields = mLayer.getFields();
            int rows = ids.size();
            for (int i = 0; i < ids.size(); i++) {
                Feature feature = featureMap.get(ids.get(i));
                if (feature == null) {
                    nullFeatureExist = true;
                }
            }
            if (get0row)
                rows = 1;

            String[][] data = new String[rows][fields.size() + 1];
            if (rows == 0)
                return data;

            data[0][0] = FIELD_ID;
            if (get0row){
                for (int i = 0; i < fields.size(); i++)
                    data[0][i + 1] = fields.get(i).getAlias();
            }

            int percents10 = ids.size() / 100;
            boolean useProgress = percents10 >= 100;

            int counter = 0;
            int progress = 0;

            if (rows > 0 && !get0row) {
                for (int i = 0; i < ids.size(); i++) {
                    if (useProgress){
                        counter ++;
                        if (counter >= percents10){
                            progress = progress + 1;
                            counter = 0;
                            publishProgress(progress);
                        }
                    }
                    Feature feature = featureMap.get(ids.get(i));
                    if (feature != null ) {
                        data[i][0] = feature.getId() + "";
                        for (int j = 0; j < fields.size(); j++)
                            data[i][j + 1] = feature.getFieldValueAsString(fields.get(j).getName());
                    }
                }
            }
            return data;
        }
    }
}

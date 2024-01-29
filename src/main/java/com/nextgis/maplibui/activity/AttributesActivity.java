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

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.evrencoskun.tableview.TableView;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inqbarna.tablefixheaders.TableFixHeaders;
import com.inqbarna.tablefixheaders.adapters.BaseTableAdapter;
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
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.fragment.BottomToolbar;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.MatrixTableAdapter;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.maplibui.adapter.attributes.ICellCLickListener;
import com.nextgis.maplibui.adapter.attributes.TableViewAdapter;
import com.nextgis.maplibui.adapter.attributes.TableViewListener;
import com.nextgis.maplibui.adapter.attributes.TableViewModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.TAG;

public class AttributesActivity extends NGActivity {

    protected TableView mTableView;
    RelativeLayout mProgressBar;
    TextView progressText;

    boolean nullFeatureExist = false;

    protected VectorLayer mLayer;
    protected BottomToolbar mToolbar;
    protected Long mId;
    protected int mLayerId;


    String searchText = "";

    Handler handlerSearch;

    int selectedRow = -1;
    protected boolean mLoading;
    LoadBigData loadBigDataTask;

    List<Long> ids;

    Map<Long, Feature> featureMap;
    List<Field> fields;

    boolean firstLoadStart = true;
    String [][] data ;
    String [] data0row ;
    String [] data0Column;

     final Object syncAdapterChanges = new Object();

    //search part - result here
    String[][] dataResult;
    List<Long> idsResult;
    String[] dataColumnResult;

    int searchedResultLenght = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attributes);
        setToolbar(R.id.main_toolbar);

        handlerSearch = new Handler();

        mTableView= findViewById(R.id.my_TableView);
        mProgressBar= findViewById(R.id.progressArea);
        progressText = findViewById(R.id.progressText);

        mToolbar = (BottomToolbar) findViewById(R.id.bottom_toolbar);
        mToolbar.inflateMenu(R.menu.attributes_table);
        if (null != getSupportActionBar()) {
            //getSupportActionBar().add
        }
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
                                        try {
                                            onDeleteData(mId);
                                        } catch (Exception ex){

                                        }

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

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.menu_search).getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onPerformSearch(newText);
                return false;
            }
        });
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onPerformSearch(final String text){
        searchText = text; // remember current text
        //mTable.setAdapter(getAdapter(text));
        if (loadBigDataTask != null && !firstLoadStart)
            loadBigDataTask.cancel(true);

        if (!firstLoadStart) {
            loadBigDataTask = new LoadBigData(this, mLayer, progressText, text);
            loadBigDataTask.execute();
        } else{
            Toast.makeText(this, R.string.loading_data_inprogress, Toast.LENGTH_LONG).show();
        }
    }

    public void onDeleteData(long mId){

        String isString = String.valueOf(mId);

        if (data != null && data0Column != null && mId > -1 && data0Column.length > 0) {
            for (int i = 0; i < data0Column.length; i++) {
                if (data0Column[i].equals(isString)) {

                    for (int j = i; j < data0Column.length - 1; j++) {
                        data0Column[j] = data0Column[j + 1];
                        data[j] = data[j + 1];
                    }
                    data0Column[data0Column.length - 1] = "-1";

                    data[data0Column.length - 1] = new String[data0row.length];
                    for (int k = 0; k < data0row.length; k++)
                        data[data0Column.length - 1][k] = "";
                    break;
                }
            }
        } else {
            // no action - when no search was activated
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        final IGISApplication application = (IGISApplication) getApplication();
        final MapBase map = application.getMap();
        if (null != map) {
            final ILayer layer = map.getLayerById(mLayerId);
            if (null != layer && layer instanceof VectorLayer) {
                mLayer = (VectorLayer) layer;

                if (loadBigDataTask != null )
                    loadBigDataTask.cancel(true);

                loadBigDataTask = new LoadBigData(this, mLayer, progressText, "");
                loadBigDataTask.execute();


                final TableViewModel tableViewModel = new TableViewModel(0, 0, new String[0][], new String[0], new String[0]);
                final TableViewAdapter tableViewAdapter = new TableViewAdapter(tableViewModel);

                final ICellCLickListener iCellCLickListener = new ICellCLickListener() {
                    @Override
                    public void onCellClick(View view, int row) {
                        if (view != null && view instanceof AppCompatTextView) {
                            final Long id = parseLong((String) view.getTag());
                            if (id != null) {
                                mId = id;
                                final String featureName = String.format(getString(R.string.feature_n), id);
                                mToolbar.setTitle(featureName);
                                String labelField = mLayer.getPreferences().getString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, FIELD_ID);
                                if (!labelField.equals(FIELD_ID)) {
                                    final Feature feature = mLayer.getFeature(id);
                                    if (feature != null) {
                                        mToolbar.setSubtitle(featureName);
                                        final String featureNameTitle = feature.getFieldValueAsString(labelField);
                                        mToolbar.setTitle(featureNameTitle);
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

                synchronized (syncAdapterChanges) {
                    mTableView.setAdapter(tableViewAdapter);
                    tableViewAdapter.setAllItems(
                            tableViewModel.getColumnHeaderList(),
                            tableViewModel.getRowHeaderList(),
                            tableViewModel.getCellList());
                }

                final Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
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

    protected class LoadBigData extends AsyncTask<Void, Integer, String> {
        final WeakReference mContextRef;
        final VectorLayer layer;

        final TextView progressText;
        final String filterText;

        String [][] dataToShow ;
        String [] data0ColumnToShow ;

        public LoadBigData(
                final Context context,
                final VectorLayer layer,
                final TextView progressText,
                final String filterText) {
            super();
            mContextRef = new WeakReference(context);
            this.layer = layer;
            this.progressText = progressText;
            this.filterText = filterText;
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
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


            if (ids == null)
                ids = mLayer.query(null);
            if (featureMap == null)
                featureMap = mLayer.getFeatures();
            if (fields == null)
                fields = mLayer.getFields();

            if (featureMap == null)
                return "";

            if (data == null)
                data = getData(false, false, filterText);

            if (data0row == null)
                data0row = get0Row();

            if (data0Column == null)
                data0Column = get0Column();

            dataToShow = data;
            data0ColumnToShow = data0Column;

            try {
                Thread.sleep(300);
            } catch (Exception ex){

            }
            if (!TextUtils.isEmpty(filterText)){

                getDataFiltered(filterText, data, ids);

                dataToShow =  dataResult;
                data0ColumnToShow =  dataColumnResult;
            }
            return "";
        }

        @Override
        protected void onPostExecute(String error)
        {
            mProgressBar.setVisibility(View.GONE);
            final int lenght = data.length > 0 ? data[0].length : 0;
            int rowsSize = data.length;
            if (!TextUtils.isEmpty(filterText))
                rowsSize =  searchedResultLenght;
            final TableViewModel tableViewModel = new TableViewModel(lenght, rowsSize, dataToShow, data0row, data0ColumnToShow);
            final TableViewAdapter tableViewAdapter = new TableViewAdapter(tableViewModel);


            synchronized (syncAdapterChanges) {
                mTableView.setAdapter(tableViewAdapter);

                tableViewAdapter.setAllItems(
                    tableViewModel.getColumnHeaderList(),
                    tableViewModel.getRowHeaderList(),
                    tableViewModel.getCellList());
            }
            final Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
            toolbar.setSubtitle(mLayer.getName());

            mLoading = false;

            if (nullFeatureExist){
                nullFeatureExist = false;
                if (mContextRef.get() != null)
                    Toast.makeText((Activity)mContextRef.get(), R.string.error_cache, Toast.LENGTH_LONG).show();
            }

            if (data.length == 0){
                if (mContextRef.get() != null)
                    Toast.makeText((Activity)mContextRef.get(), "no data in layer", Toast.LENGTH_LONG).show();
            }
            if (firstLoadStart)
                firstLoadStart = false;
        }

        public String[] get0Row() {
            final String[] data = new String[fields.size() + 1];
            data[0] = FIELD_ID;
                for (int i = 0; i < fields.size(); i++)
                    data[i + 1] = fields.get(i).getAlias();
            return data;
        }

        public String[] get0Column(){
            final String[] data0col = new String[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                final Feature feature = featureMap.get(ids.get(i));
                if (feature != null)
                    data0col[i] = feature.getId() + "";
            }
            return data0col;
        }

        final public String[][] getData(boolean get0row, boolean get0column, final String filterString) {
            if (get0column){
                final String[][] data0col = new String[ids.size()][1];
                for (int i = 0; i < ids.size(); i++) {
                    final Feature feature = featureMap.get(ids.get(i));
                    if (feature != null)
                        data0col[i][0] = feature.getId() + "";
                }
                return data0col;
            }
            int rows = ids.size();
            for (int i = 0; i < ids.size(); i++) {
                final Feature feature = featureMap.get(ids.get(i));
                if (feature == null) {
                    nullFeatureExist = true;
                }
            }
            if (get0row)
                rows = 1;

            final String[][] data = new String[rows][fields.size() + 1];
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
                int iRow = 0;
                for (int i = 0; i < ids.size(); i++) {

                    if (isCancelled()){
                        break;
                    }
                    if (useProgress){
                        counter ++;
                        if (counter >= percents10){
                            progress = progress + 1;
                            counter = 0;
                            publishProgress(progress);
                        }
                    }
                    final Feature feature = featureMap.get(ids.get(i));
                    if (feature != null ) {
                        // filter part
                        if (TextUtils.isEmpty(filterString)){
                            data[i][0] = feature.getId() + "";
                            // no filter
                            for (int j = 0; j < fields.size(); j++) {
                                data[i][j + 1] = feature.getFieldValueAsString(fields.get(j).getName());
                            }
                        } else {
                            // filter
                            final String filterStringLower = filterString.toLowerCase();
                            boolean isProgressSearch = true;
                            for (int j = 0; j < fields.size() && isProgressSearch; j++){
                                final String element = feature.getFieldValueAsString(fields.get(j).getName());
                                if (!TextUtils.isEmpty(element) && element.toLowerCase().contains(filterStringLower))
                                    isProgressSearch = false;
                            }
                            if (!isProgressSearch) {

                                data[iRow ][0] = feature.getId() + "";
                                for (int j = 0; j < fields.size(); j++) {
                                    data[iRow ][j + 1] = feature.getFieldValueAsString(fields.get(j).getName());
                                }
                                iRow ++;
                            }
                        }
                    }
                }
            }
            return data;
        }

        public void getDataFiltered(final String filterString, String [][] dataSource, List<Long> idsSource) {
            int rows = idsSource.size();
            idsResult = new ArrayList<>();

            dataResult = new String[rows][fields.size() + 1];
            dataColumnResult = new String[rows];
            //clear result

            int percents10 = idsSource.size() / 100;
            boolean useProgress = percents10 >= 100;

            int counter = 0;
            int progress = 0;

            if (rows > 0 ) {
                int iRow = 0;
                for (int i = 0; i < idsSource.size(); i++) {
                    if (isCancelled()){
                        break;
                    }
                    if (useProgress){
                        counter ++;
                        if (counter >= percents10){
                            progress = progress + 1;
                            counter = 0;
                            publishProgress(progress);
                        }
                    }
                    // filter part
                    final String filterStringLower = filterString.toLowerCase();
                    boolean isProgressSearch = true;
                    for (int j = 0; j < fields.size() +1 && isProgressSearch; j++){
                        final String element = dataSource[i][j];
                        if (!TextUtils.isEmpty(element) && element.toLowerCase().contains(filterStringLower))
                            isProgressSearch = false;
                    }
                    if (!isProgressSearch) {
                        dataColumnResult[iRow] = data0Column[i] + "";
                        dataResult[iRow ][0] = data0Column[i];
                        idsResult.add(idsSource.get(i));
                        for (int j = 0; j < fields.size(); j++) {
                            dataResult[iRow ][j+1] = dataSource[i][j+1];
                        }
                        iRow ++;
                    }
                }
                searchedResultLenght = iRow;
            }
        }
    }

    private Long parseLong(String string) {
        try {
            return Long.parseLong(string);
        } catch (Exception e) {
            return null;
        }
    }
}

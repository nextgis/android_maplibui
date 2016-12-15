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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
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

import java.util.List;

import static com.nextgis.maplib.util.Constants.FIELD_ID;

public class AttributesActivity extends NGActivity {
    protected TableFixHeaders mTable;
    protected VectorLayer mLayer;
    protected BottomToolbar mToolbar;
    protected Long mId;
    protected int mLayerId;
    protected BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attributes);
        setToolbar(R.id.main_toolbar);

        mTable = (TableFixHeaders) findViewById(R.id.attributes);

        mToolbar = (BottomToolbar) findViewById(R.id.bottom_toolbar);
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
                mTable.setAdapter(getAdapter());
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
                mTable.setAdapter(getAdapter());
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

    public BaseTableAdapter getAdapter() {
        MatrixTableAdapter<String> adapter = new MatrixTableAdapter<>(this);

        if (mLayer == null)
            return adapter;

        List<Long> ids = mLayer.query(null);
        List<Field> fields = mLayer.getFields();
        int rows = ids.size() + 1;
        for (int i = 0; i < ids.size(); i++) {
            Feature feature = mLayer.getFeature(ids.get(i));
            if (feature == null) {
                rows = 1;
                Toast.makeText(this, R.string.error_cache, Toast.LENGTH_LONG).show();
                break;
            }
        }

        String[][] data = new String[rows][fields.size() + 1];
        data[0][0] = FIELD_ID;
        for (int i = 0; i < fields.size(); i++)
            data[0][i + 1] = fields.get(i).getAlias();

        if (rows > 1)
            for (int i = 0; i < ids.size(); i++) {
                Feature feature = mLayer.getFeature(ids.get(i));
                data[i + 1][0] = feature.getId() + "";
                for (int j = 0; j < fields.size(); j++)
                    data[i + 1][j + 1] = feature.getFieldValueAsString(fields.get(j).getName());
            }

        adapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view != null && view instanceof TextView) {
                    Long id = parseLong((String) view.getTag(R.id.text1));
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
            }
        });
        adapter.setInformation(data);
        return adapter;
    }

    private Long parseLong(String string) {
        try {
            return Long.parseLong(string);
        } catch (Exception e) {
            return null;
        }
    }
}

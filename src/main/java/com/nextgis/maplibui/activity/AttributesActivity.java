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

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.inqbarna.tablefixheaders.TableFixHeaders;
import com.inqbarna.tablefixheaders.adapters.BaseTableAdapter;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.MatrixTableAdapter;

import java.util.List;


public class AttributesActivity extends NGActivity {
    private TableFixHeaders mTable;
    private VectorLayer mLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attributes);
        setToolbar(R.id.main_toolbar);

        mTable = (TableFixHeaders) findViewById(R.id.attributes);

        int layerId = Constants.NOT_FOUND;
        if (savedInstanceState != null) {
            layerId = savedInstanceState.getInt(ConstantsUI.KEY_LAYER_ID);
        } else {
            layerId = getIntent().getIntExtra(ConstantsUI.KEY_LAYER_ID, layerId);
        }

        IGISApplication application = (IGISApplication) getApplication();
        MapBase map = application.getMap();

        if (null != map) {
            ILayer layer = map.getLayerById(layerId);
            if (null != layer && layer instanceof VectorLayer) {
                mLayer = (VectorLayer) layer;
                mTable.setAdapter(getAdapter());
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ConstantsUI.KEY_LAYER_ID, mLayer.getId());
    }

    public BaseTableAdapter getAdapter() {
        MatrixTableAdapter<String> adapter = new MatrixTableAdapter<>(this);

        if (mLayer == null)
            return adapter;

        List<Long> ids = mLayer.query(null);
        List<Field> fields = mLayer.getFields();
        String[][] data = new String[ids.size() + 1][fields.size() + 1];
        data[0][0] = getString(R.string.id);
        for (int i = 0; i < fields.size(); i++)
            data[0][i + 1] = fields.get(i).getAlias();

        for (int i = 0; i < ids.size(); i++) {
            Feature feature = mLayer.getFeature(ids.get(i));
            data[i + 1][0] = feature.getId() + "";
            for (int j = 0; j < fields.size(); j++)
                data[i + 1][j + 1] = feature.getFieldValueAsString(fields.get(j).getName());
        }

        adapter.setInformation(data);
        return adapter;
    }
}

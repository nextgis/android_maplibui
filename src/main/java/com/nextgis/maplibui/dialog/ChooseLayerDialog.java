/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ListView;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IChooseLayerResult;
import com.nextgis.maplibui.api.ILayerSelector;

import java.util.ArrayList;
import java.util.List;


/**
 * A dialog to choose layer and start attributes edit form
 */
public class ChooseLayerDialog
        extends NGDialog
        implements ILayerSelector
{
    protected List<ILayer>           mLayers;
    protected ChooseLayerListAdapter mListAdapter;
    protected int                    mCode;

    protected final static String KEY_LAYERS_IDS = "ids";
    protected final static String KEY_CODE       = "code";


    public ChooseLayerDialog setLayerList(List<ILayer> list)
    {
        mLayers = list;
        return this;
    }


    public ChooseLayerDialog setCode(int code)
    {
        mCode = code;
        return this;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        super.onCreateDialog(savedInstanceState);
        mListAdapter = new ChooseLayerListAdapter(this);

        if (null != savedInstanceState) {
            List<Integer> ids = savedInstanceState.getIntegerArrayList(KEY_LAYERS_IDS);
            IGISApplication app = (IGISApplication) mActivity.getApplication();
            MapBase map = app.getMap();
            mLayers = new ArrayList<>();
            for (Integer id : ids) {
                ILayer layer = map.getLayerById(id);
                mLayers.add(layer);
            }
            mCode = savedInstanceState.getInt(KEY_CODE);
        }

        View view = View.inflate(mContext, R.layout.layout_layers, null);
        ListView dialogListView = (ListView) view.findViewById(R.id.listView);
        dialogListView.setAdapter(mListAdapter);
        dialogListView.setOnItemClickListener(mListAdapter);

//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, mDialogTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mTitle).setView(view).setInverseBackgroundForced(true).setNegativeButton(
                R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(
                            DialogInterface dialog,
                            int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        ArrayList<Integer> ids = new ArrayList<>();
        for (ILayer layer : mLayers) {
            ids.add(layer.getId());
        }
        outState.putIntegerArrayList(KEY_LAYERS_IDS, ids);
        outState.putInt(KEY_CODE, mCode);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onLayerSelect(ILayer layer)
    {
        IChooseLayerResult activity = (IChooseLayerResult) mActivity;
        if (null != activity) {
            activity.onFinishChooseLayerDialog(mCode, layer);
        }

        dismiss();
    }


    @Override
    public List<ILayer> getLayers()
    {
        return mLayers;
    }


}

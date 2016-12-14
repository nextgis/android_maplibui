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

import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.fragment.LayerFillProgressDialogFragment;
import com.nextgis.maplibui.service.LayerFillService;

import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_NORMAL;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;


/**
 * The dialog to pick layer name and create the vector layer by input Uri
 */
public class CreateLocalLayerDialog
        extends NGDialog
{
    protected final static String KEY_ID         = "id";
    protected final static String KEY_URI        = "uri";
    protected final static String KEY_LAYER_TYPE = "layer_type";
    protected final static String KEY_TMS_TYPE   = "tms";
    protected final static String KEY_CACHE      = "cache";

    protected Uri        mUri;
    protected LayerGroup mGroupLayer;
    protected int        mLayerType;
    protected String     mLayerName;
    protected Spinner    mSpinner, mCache;


    public CreateLocalLayerDialog setLayerName(String layerName)
    {
        mLayerName = layerName;
        return this;
    }


    public CreateLocalLayerDialog setUri(Uri uri)
    {
        mUri = uri;
        return this;
    }


    public CreateLocalLayerDialog setLayerType(int layerType)
    {
        mLayerType = layerType;
        return this;
    }


    public CreateLocalLayerDialog setLayerGroup(LayerGroup groupLayer)
    {
        mGroupLayer = groupLayer;
        return this;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        super.onCreateDialog(savedInstanceState);
        if (null != savedInstanceState) {
            mLayerName = savedInstanceState.getString(LayerFillService.KEY_NAME);
            mUri = savedInstanceState.getParcelable(KEY_URI);
            int id = savedInstanceState.getInt(KEY_ID);
            mLayerType = savedInstanceState.getInt(KEY_LAYER_TYPE);

            MapBase map = MapBase.getInstance();
            if (null != map && mGroupLayer == null) {
                ILayer iLayer = map.getLayerById(id);
                if (iLayer instanceof LayerGroup) {
                    mGroupLayer = (LayerGroup) iLayer;
                }
            }
        }

        View view;
        if (mLayerType < 3) {
            view = View.inflate(mContext, R.layout.dialog_create_vector_layer, null);
        } else {
            view = View.inflate(mContext, R.layout.dialog_create_local_tms, null);
            mCache = (Spinner) view.findViewById(R.id.layer_cache);
            mCache.setSelection(2);

            final ArrayAdapter<CharSequence> adapter =
                    new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item);
            mSpinner = (Spinner) view.findViewById(R.id.layer_type);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);

            adapter.add(mContext.getString(R.string.tmstype_osm));
            adapter.add(mContext.getString(R.string.tmstype_normal));

            if (null != savedInstanceState) {
                mSpinner.setSelection(savedInstanceState.getInt(KEY_TMS_TYPE, 0));
                mCache.setSelection(savedInstanceState.getInt(KEY_CACHE, 0));
            }
        }

        final EditText layerName = (EditText) view.findViewById(R.id.layer_name);
        layerName.setText(mLayerName);
        layerName.setSelection(mLayerName.length());

//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, mDialogTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mTitle).setView(view)
                .setPositiveButton(
                        R.string.create, new DialogInterface.OnClickListener()
                        {
                            public void onClick(
                                    DialogInterface dialog,
                                    int whichButton)
                            {
                                mLayerName = layerName.getText().toString();
                                // create or connect to fill layer with features
                                Intent intent = new Intent(mContext, LayerFillService.class);
                                intent.setAction(LayerFillService.ACTION_ADD_TASK);
                                intent.putExtra(LayerFillService.KEY_URI, mUri);
                                intent.putExtra(LayerFillService.KEY_NAME, mLayerName);
                                intent.putExtra(LayerFillService.KEY_INPUT_TYPE, mLayerType);
                                intent.putExtra(LayerFillService.KEY_LAYER_GROUP_ID, mGroupLayer.getId());

                                if (mSpinner != null)
                                    intent.putExtra(LayerFillService.KEY_TMS_TYPE, getTmsType());

                                if (mCache != null)
                                    intent.putExtra(LayerFillService.KEY_TMS_CACHE, mCache.getSelectedItemPosition());

                                LayerFillProgressDialogFragment.startFill(intent);
                            }

                            private int getTmsType() {
                                return mSpinner.getSelectedItemPosition() == 0 ? TMSTYPE_OSM : TMSTYPE_NORMAL;
                            }
                        }
                )
                .setNegativeButton(R.string.cancel, null);
        // Create the AlertDialog object and return it
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putInt(KEY_ID, mGroupLayer.getId());
        outState.putString(LayerFillService.KEY_NAME, mLayerName);
        outState.putParcelable(KEY_URI, mUri);
        outState.putInt(KEY_LAYER_TYPE, mLayerType);
        if (mSpinner != null)
            outState.putInt(KEY_TMS_TYPE, mSpinner.getSelectedItemPosition());
        if (mCache != null)
            outState.putInt(KEY_CACHE, mCache.getSelectedItemPosition());

        super.onSaveInstanceState(outState);
    }
}

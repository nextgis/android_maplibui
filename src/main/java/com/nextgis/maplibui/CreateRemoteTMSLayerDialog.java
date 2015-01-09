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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;

import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_NORMAL;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;


public class CreateRemoteTMSLayerDialog extends DialogFragment
{
    protected String     mTitle;
    protected LayerGroup mGroupLayer;
    protected Spinner mSpinner;
    protected EditText mInput;
    protected EditText mUrl;

    protected final static String KEY_TITLE = "title";
    protected final static String KEY_ID    = "id";
    protected final static String KEY_NAME  = "name";
    protected final static String KEY_URL   = "url";
    protected final static String KEY_POSITION   = "pos";

    public CreateRemoteTMSLayerDialog setTitle(String title)
    {
        mTitle = title;
        return this;
    }


    public CreateRemoteTMSLayerDialog setLayerGroup(LayerGroup groupLayer)
    {
        mGroupLayer = groupLayer;
        return this;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final Context context = getActivity();
        final LinearLayout linearLayout = new LinearLayout(context);
        mInput = new EditText(context);
        mUrl = new EditText(context);
        final TextView stLayerName = new TextView(context);
        stLayerName.setText(context.getString(R.string.layer_name) + ":");

        final TextView stLayerUrl = new TextView(context);
        stLayerUrl.setText(context.getString(R.string.layer_url) + ":");

        final TextView stLayerType = new TextView(context);
        stLayerType.setText(context.getString(R.string.layer_type) + ":");

        final ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
        mSpinner = new Spinner(context);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        adapter.add(context.getString(R.string.tmstype_osm));
        adapter.add(context.getString(R.string.tmstype_normal));

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(stLayerName);
        linearLayout.addView(mInput);
        linearLayout.addView(stLayerUrl);
        linearLayout.addView(mUrl);
        linearLayout.addView(stLayerType);
        linearLayout.addView(mSpinner);

        if(null == savedInstanceState){
            mInput.setText(context.getResources().getText(R.string.osm));
            mUrl.setText(context.getResources().getText(R.string.osm_url));
            mSpinner.setSelection(0);
        }
        else{
            mInput.setText(savedInstanceState.getString(KEY_NAME));
            mUrl.setText(savedInstanceState.getString(KEY_URL));
            mSpinner.setSelection(savedInstanceState.getInt(KEY_POSITION));
            mTitle = savedInstanceState.getString(KEY_TITLE);
            short id = savedInstanceState.getShort(KEY_ID);
            MapBase map = MapBase.getInstance();
            if(null != map){
                ILayer iLayer = map.getLayerById(id);
                if(iLayer instanceof LayerGroup)
                    mGroupLayer = (LayerGroup)iLayer;
            }
        }

        final Context appContext = mGroupLayer.getContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitle)
               .setIcon(R.drawable.ic_remote_tms)
               .setView(linearLayout)
               .setPositiveButton(R.string.create, new DialogInterface.OnClickListener()
                {
                    public void onClick(
                            DialogInterface dialog,
                            int whichButton)
                    {
                        int tmsType = 0;
                        switch (mSpinner.getSelectedItemPosition()) {
                            case 0:
                                tmsType = TMSTYPE_OSM;
                                break;
                            case 1:
                                tmsType = TMSTYPE_NORMAL;
                                break;
                        }
                        String layerName = mInput.getText().toString();
                        String layerURL = mUrl.getText().toString();

                        //check if {x}, {y} or {z} present
                        if (!layerURL.contains("{x}") || !layerURL.contains("{y}") ||
                            !layerURL.contains("{z}")) {
                            Toast.makeText(context, R.string.error_invalid_url, Toast.LENGTH_SHORT)
                                 .show();
                            return;
                        }

                        //create new layer and store it and add it to the map
                        RemoteTMSLayerUI layer = new RemoteTMSLayerUI(appContext, mGroupLayer.cretateLayerStorage());
                        layer.setName(layerName);
                        layer.setURL(layerURL);
                        layer.setTMSType(tmsType);
                        layer.setVisible(true);

                        mGroupLayer.addLayer(layer);
                        mGroupLayer.save();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick(
                            DialogInterface dialog,
                            int whichButton)
                    {
                        // Do nothing.
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString(KEY_TITLE, mTitle);
        outState.putShort(KEY_ID, mGroupLayer.getId());
        outState.putString(KEY_NAME, mInput.getText().toString());
        outState.putString(KEY_URL, mUrl.getText().toString());
        outState.putInt(KEY_POSITION, mSpinner.getSelectedItemPosition());

        super.onSaveInstanceState(outState);
    }
}

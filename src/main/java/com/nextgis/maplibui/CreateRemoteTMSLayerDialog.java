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
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;

import java.util.regex.Pattern;

import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_NORMAL;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;


public class CreateRemoteTMSLayerDialog extends DialogFragment
{
    protected String     mTitle;
    protected LayerGroup mGroupLayer;
    protected Spinner mSpinner;
    protected EditText mInput;
    protected EditText mUrl;
    protected EditText mLogin;
    protected EditText mPassword;

    protected final static String KEY_TITLE = "title";
    protected final static String KEY_ID    = "id";
    protected final static String KEY_NAME  = "name";
    protected final static String KEY_URL   = "url";
    protected final static String KEY_POSITION   = "pos";
    protected final static String KEY_LOGIN   = "login";
    protected final static String KEY_PASSWORD   = "password";

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
        //fix: http://stackoverflow.com/questions/25684940/buttons-not-displayed-on-copy-paste-action-bar-associated-with-a-textview-in-dia/25703569
        context.setTheme(android.R.style.Theme_Holo_Light);

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.layout_create_tms, null);

        final ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
        mSpinner = (Spinner) view.findViewById(R.id.layer_type);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        adapter.add(context.getString(R.string.tmstype_osm));
        adapter.add(context.getString(R.string.tmstype_normal));

        mInput = (EditText) view.findViewById(R.id.layer_name);
        mUrl = (EditText) view.findViewById(R.id.layer_url);

        mLogin = (EditText) view.findViewById(R.id.login);
        mPassword = (EditText) view.findViewById(R.id.password);

        if(null == savedInstanceState){
            //mInput.setText(context.getResources().getText(R.string.osm));
            //mUrl.setText(context.getResources().getText(R.string.osm_url));
            //mSpinner.setSelection(0);
        }
        else{
            mInput.setText(savedInstanceState.getString(KEY_NAME));
            mUrl.setText(savedInstanceState.getString(KEY_URL));
            mLogin.setText(savedInstanceState.getString(KEY_LOGIN));
            mPassword.setText(savedInstanceState.getString(KEY_PASSWORD));
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitle)
               .setIcon(R.drawable.ic_remote_tms)
               .setView(view)
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
                        String layerURL = mUrl.getText().toString().trim();
                        String layerLogin = mLogin.getText().toString();
                        String layerPassword = mPassword.getText().toString();

                        //check if {x}, {y} or {z} present
                        if (!layerURL.contains("{x}") || !layerURL.contains("{y}") ||
                            !layerURL.contains("{z}")   ) {
                            Toast.makeText(context, R.string.error_invalid_url, Toast.LENGTH_SHORT)
                                 .show();
                            return;
                        }

                        if(!layerURL.startsWith("http"))
                            layerURL = "http://" + layerURL;

                        boolean isURL = URLUtil.isValidUrl(layerURL);

                        if (!isURL) {
                            Toast.makeText(context, R.string.error_invalid_url, Toast.LENGTH_SHORT)
                                 .show();
                            return;
                        }

                        //create new layer and store it and add it to the map
                        RemoteTMSLayerUI layer = new RemoteTMSLayerUI(mGroupLayer.getContext(), mGroupLayer.createLayerStorage());
                        layer.setName(layerName);
                        layer.setURL(layerURL);
                        layer.setLogin(layerLogin);
                        layer.setPassword(layerPassword);
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
        outState.putString(KEY_LOGIN, mLogin.getText().toString());
        outState.putString(KEY_PASSWORD, mPassword.getText().toString());

        super.onSaveInstanceState(outState);
    }
}

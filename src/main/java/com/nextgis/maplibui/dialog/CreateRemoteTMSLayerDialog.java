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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;
import com.nextgis.maplibui.util.ConstantsUI;

import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_NORMAL;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;


public class CreateRemoteTMSLayerDialog
        extends NGDialog
{
    protected LayerGroup mGroupLayer;
    protected Spinner    mSpinner, mCache;
    protected EditText   mInput;
    protected EditText   mUrl;
    protected EditText   mLogin;
    protected EditText   mPassword;

    protected final static String KEY_ID       = "id";
    protected final static String KEY_NAME     = "name";
    protected final static String KEY_URL      = "url";
    protected final static String KEY_POSITION = "pos";
    protected final static String KEY_CACHE    = "cache";
    protected final static String KEY_LOGIN    = "login";
    protected final static String KEY_PASSWORD = "password";


    public CreateRemoteTMSLayerDialog setLayerGroup(LayerGroup groupLayer)
    {
        mGroupLayer = groupLayer;
        return this;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        //final Context context = getActivity();
        //fix: http://stackoverflow.com/q/25684940
        //context.setTheme(android.R.style.Theme_Holo_Light);
        //context.setTheme(android.R.style.Theme_Light_NoTitleBar);
        //LayoutInflater inflater = getActivity().getLayoutInflater();
        super.onCreateDialog(savedInstanceState);
        View view = View.inflate(mContext, R.layout.dialog_create_tms, null);
        mCache = (Spinner) view.findViewById(R.id.layer_cache);
        mCache.setSelection(2);

        final ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item);
        mSpinner = (Spinner) view.findViewById(R.id.layer_type);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        adapter.add(mContext.getString(R.string.tmstype_osm));
        adapter.add(mContext.getString(R.string.tmstype_normal));

        mInput = (EditText) view.findViewById(R.id.layer_name);
        mUrl = (EditText) view.findViewById(R.id.layer_url);

        mLogin = (EditText) view.findViewById(R.id.login);
        mPassword = (EditText) view.findViewById(R.id.password);

        if (null != savedInstanceState) {
            mInput.setText(savedInstanceState.getString(KEY_NAME));
            mUrl.setText(savedInstanceState.getString(KEY_URL));
            mLogin.setText(savedInstanceState.getString(KEY_LOGIN));
            mPassword.setText(savedInstanceState.getString(KEY_PASSWORD));
            mSpinner.setSelection(savedInstanceState.getInt(KEY_POSITION));
            mCache.setSelection(savedInstanceState.getInt(KEY_CACHE));
            int id = savedInstanceState.getInt(KEY_ID);
            MapBase map = MapBase.getInstance();
            if (null != map) {
                ILayer iLayer = map.getLayerById(id);
                if (iLayer instanceof LayerGroup) {
                    mGroupLayer = (LayerGroup) iLayer;
                }
            }
        }

//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, mDialogTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mTitle).setView(view).setPositiveButton(
                R.string.create, new DialogInterface.OnClickListener()
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
                            !layerURL.contains("{z}")) {
                            Toast.makeText(mContext, R.string.error_invalid_url, Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        if (!layerURL.startsWith("http")) {
                            layerURL = "http://" + layerURL;
                        }

                        boolean isURL = URLUtil.isValidUrl(layerURL);

                        if (!isURL) {
                            Toast.makeText(mContext, R.string.error_invalid_url, Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        //create new layer and store it and add it to the map
                        RemoteTMSLayerUI layer = new RemoteTMSLayerUI(
                                mGroupLayer.getContext(), mGroupLayer.createLayerStorage());
                        layer.setName(layerName);
                        layer.setURL(layerURL);
                        layer.setLogin(layerLogin);
                        layer.setPassword(layerPassword);
                        layer.setTMSType(tmsType);
                        layer.setCacheSizeMultiply(mCache.getSelectedItemPosition());
                        layer.setVisible(true);
                        layer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
                        layer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

                        mGroupLayer.addLayer(layer);
                        mGroupLayer.save();
                    }
                })
                .setNeutralButton(R.string.track_list, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CreateFromQMSLayerDialog newFragment = new CreateFromQMSLayerDialog();
                        newFragment.setLayerGroup(mGroupLayer)
                                .setTitle(mContext.getString(R.string.create_qms_layer))
                                .setTheme(((NGActivity) getActivity()).getThemeId())
                                .show(getActivity().getSupportFragmentManager(), "create_qms_layer");
                    }
                })
                .setNegativeButton(R.string.cancel, null);

        // Create the AlertDialog object and return it
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        IGISApplication application = (IGISApplication) getActivity().getApplication();
        application.sendScreen(ConstantsUI.GA_DIALOG_TMS);

        return dialog;
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putInt(KEY_ID, mGroupLayer.getId());
        outState.putString(KEY_NAME, mInput.getText().toString());
        outState.putString(KEY_URL, mUrl.getText().toString());
        outState.putInt(KEY_POSITION, mSpinner.getSelectedItemPosition());
        outState.putInt(KEY_CACHE, mCache.getSelectedItemPosition());
        outState.putString(KEY_LOGIN, mLogin.getText().toString());
        outState.putString(KEY_PASSWORD, mPassword.getText().toString());

        super.onSaveInstanceState(outState);
    }
}

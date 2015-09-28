/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.mapui.LocalTMSLayerUI;
import com.nextgis.maplibui.mapui.NGWVectorLayerUI;
import com.nextgis.maplibui.mapui.VectorLayerUI;
import com.nextgis.maplibui.service.LayerFillService;
import com.nextgis.maplibui.util.ConstantsUI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_NORMAL;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;


/**
 * The dialog to pick layer name and create the vector layer by input Uri
 */
public class CreateLocalLayerDialog
        extends DialogFragment
{
    public final static int VECTOR_LAYER           = 1;
    public final static int VECTOR_LAYER_WITH_FORM = 2;
    public final static int TMS_LAYER              = 3;

    protected final static String KEY_TITLE      = "title";
    protected final static String KEY_NAME       = "name";
    protected final static String KEY_ID         = "id";
    protected final static String KEY_URI        = "uri";
    protected final static String KEY_LAYER_TYPE = "layer_type";
    protected final static String KEY_TMS_TYPE   = "tms_type";
    protected final static String KEY_POSITION   = "pos";

    protected String     mTitle;
    protected Uri        mUri;
    protected LayerGroup mGroupLayer;
    protected int        mLayerType;
    protected String     mLayerName;
    protected Spinner    mSpinner;

    protected final static String NGFP_FILE_META = "meta.json";
    protected final static String NGFP_FILE_DATA = "data.geojson";


    public CreateLocalLayerDialog setTitle(String title)
    {
        mTitle = title;
        return this;
    }


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
        final Context context = new ContextThemeWrapper(getActivity(), R.style.Theme_NextGIS_AppCompat_Light_Dialog);

        int tmsType = 0;
        if (null != savedInstanceState) {
            mLayerName = savedInstanceState.getString(KEY_NAME);
            mTitle = savedInstanceState.getString(KEY_TITLE);
            mUri = savedInstanceState.getParcelable(KEY_URI);
            int id = savedInstanceState.getInt(KEY_ID);
            mLayerType = savedInstanceState.getInt(KEY_LAYER_TYPE);
            tmsType = savedInstanceState.getInt(KEY_TMS_TYPE);

            MapBase map = MapBase.getInstance();
            if (null != map) {
                ILayer iLayer = map.getLayerById(id);
                if (iLayer instanceof LayerGroup) {
                    mGroupLayer = (LayerGroup) iLayer;
                }
            }

        }

        View view;
        if (mLayerType < 3) {
            view = View.inflate(context, R.layout.dialog_create_vector_layer, null);
        } else {
            view = View.inflate(context, R.layout.dialog_create_local_tms, null);

            final ArrayAdapter<CharSequence> adapter =
                    new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
            mSpinner = (Spinner) view.findViewById(R.id.layer_type);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);

            adapter.add(context.getString(R.string.tmstype_osm));
            adapter.add(context.getString(R.string.tmstype_normal));

            mSpinner.setSelection(tmsType);
        }

        final EditText layerName = (EditText) view.findViewById(R.id.layer_name);
        layerName.setText(mLayerName);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(mTitle)
                .setIcon(mLayerType < 3 ? R.drawable.ic_local_vector : R.drawable.ic_local_tms)
                .setView(view)
                .setPositiveButton(
                        R.string.create, new DialogInterface.OnClickListener()
                        {
                            private Activity mActivity;

                            public void onClick(
                                    DialogInterface dialog,
                                    int whichButton)
                            {
                                mActivity = getActivity();
                                mLayerName = layerName.getText().toString();
                                if (mLayerType == VECTOR_LAYER) {
                                    VectorLayerUI layer = new VectorLayerUI(
                                            mGroupLayer.getContext(), mGroupLayer.createLayerStorage());
                                    layer.setName(mLayerName);
                                    layer.setVisible(true);
                                    layer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
                                    layer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

                                    mGroupLayer.addLayer(layer);
                                    mGroupLayer.save();
                                    startService(layer);
                                }
                                else if(mLayerType == VECTOR_LAYER_WITH_FORM){
                                    new CreateTask(mActivity).execute(mLayerName);
                                }
                                else if(mLayerType == TMS_LAYER){
                                    int nType = mSpinner.getSelectedItemPosition();
                                    LocalTMSLayerUI layer = new LocalTMSLayerUI(
                                            mGroupLayer.getContext(), mGroupLayer.createLayerStorage());
                                    layer.setName(mLayerName);
                                    layer.setTMSType(nType == 0 ? TMSTYPE_OSM : TMSTYPE_NORMAL);
                                    layer.setVisible(true);
                                    layer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
                                    layer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

                                    mGroupLayer.addLayer(layer);
                                    mGroupLayer.save();
                                    startService(layer);
                                }
                            }

                            private void startService(ILayer layer) {
                                // create or connect to fill layer with features
                                Intent intent = new Intent(context, LayerFillService.class);
                                intent.setAction(LayerFillService.ACTION_ADD_TASK);
                                intent.putExtra(ConstantsUI.KEY_LAYER_ID, layer.getId());
                                intent.putExtra(LayerFillService.KEY_URI, mUri);
                                intent.putExtra(LayerFillService.KEY_INPUT_TYPE, layer.getType());

                                LayerFillProgressDialog progressDialog = new LayerFillProgressDialog(mActivity);
                                progressDialog.execute(intent);
                            }
                        }
                )
                .setNegativeButton(
                        R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog,
                                    int whichButton) {
                                // Do nothing.
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
        outState.putString(KEY_TITLE, mTitle);
        outState.putInt(KEY_ID, mGroupLayer.getId());
        outState.putString(KEY_NAME, mLayerName);
        outState.putParcelable(KEY_URI, mUri);
        outState.putInt(KEY_LAYER_TYPE, mLayerType);
        //todo: outState.putInt(KEY_TMS_TYPE, mT);
        if (null != mSpinner) {
            outState.putInt(KEY_POSITION, mSpinner.getSelectedItemPosition());
        }

        super.onSaveInstanceState(outState);
    }


    protected class CreateTask
            extends AsyncTask<String, String, Intent>
    {
        protected ProgressDialog    mProgressDialog;
        protected Activity          mActivity;
        protected String            mError;


        public CreateTask(Activity activity)
        {
            mActivity = activity;
        }


        @Override
        protected void onPreExecute()
        {
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setMessage(
                    mGroupLayer.getContext().getString(R.string.message_loading));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }


        @Override
        protected Intent doInBackground(String... names)
        {
            if (mLayerType == VECTOR_LAYER_WITH_FORM) {
                //create local layer from ngfb
                return createVectorLayerWithForm(mActivity, names[0], this);
            }
            return null;
        }


        @Override
        protected void onPostExecute(Intent layerFillIntent)
        {
            try { // if dialog is already detach from window catch exception
                mProgressDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }

            LayerFillProgressDialog progressDialog = new LayerFillProgressDialog(mActivity);
            progressDialog.execute(layerFillIntent);

            if (null != mError && mError.length() > 0) {
                Toast.makeText(mActivity, mError, Toast.LENGTH_SHORT).show();
            }
        }


        @Override
        protected void onProgressUpdate(String... values)
        {
            mProgressDialog.setMessage(values[0]);
        }


        public void setMax(int max)
        {
            mProgressDialog.setMax(max);
        }


        public void setProgress(int increment)
        {
            mProgressDialog.setProgress(increment);
        }


        public void setMessage(String string)
        {
            publishProgress(string);
        }


        public void setProgressStyle(int styleSpinner)
        {
            mProgressDialog.setProgressStyle(styleSpinner);
        }


        public void setIndeterminate(boolean b)
        {
            mProgressDialog.setIndeterminate(b);
        }


        protected Intent createVectorLayerWithForm(
                Context context,
                String name,
                CreateTask task)
        {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(mUri);
                if (inputStream != null) {

                    int nSize = inputStream.available();
                    int nIncrement = 0;
                    task.setMax(nSize);
                    byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];

                    File outputPath = mGroupLayer.createLayerStorage();
                    ZipInputStream zis = new ZipInputStream(inputStream);

                    ZipEntry ze;
                    while ((ze = zis.getNextEntry()) != null) {
                        FileUtil.unzipEntry(zis, ze, buffer, outputPath);
                        nIncrement += ze.getSize();
                        zis.closeEntry();
                        task.setProgress(nIncrement);
                    }
                    zis.close();

                    //read meta.json
                    File meta = new File(outputPath, NGFP_FILE_META);
                    String jsonText = FileUtil.readFromFile(meta);
                    JSONObject metaJson = new JSONObject(jsonText);

                    File dataFile = new File(outputPath, NGFP_FILE_DATA);
                    Intent intent = new Intent(context, LayerFillService.class);
                    intent.setAction(LayerFillService.ACTION_ADD_TASK);
                    //read if this local o remote source
                    boolean isNgwConnection = metaJson.has("ngw_connection") && !metaJson.isNull("ngw_connection");
                    if (isNgwConnection) {
                        FileUtil.deleteRecursive(dataFile);
                        JSONObject connection = metaJson.getJSONObject("ngw_connection");

                        //read url
                        String url = connection.getString("url");
                        if (!url.startsWith("http")) {
                            url = "http://" + url;
                        }

                        //read login
                        String login = connection.getString("login");
                        //read password
                        String password = connection.getString("password");
                        //read id
                        long resourceId = connection.getLong("id");
                        //check account exist and try to create

                        FileUtil.deleteRecursive(meta);

                        String accountName = "";
                        URI uri = new URI(url);

                        if (uri.getHost() != null && uri.getHost().length() > 0) {
                            accountName += uri.getHost();
                        }
                        if (uri.getPort() != 80 && uri.getPort() > 0) {
                            accountName += ":" + uri.getPort();
                        }
                        if (uri.getPath() != null && uri.getPath().length() > 0) {
                            accountName += uri.getPath();
                        }

                        IGISApplication app = (IGISApplication) context.getApplicationContext();
                        Account account = app.getAccount(accountName);
                        if (null == account) {
                            //create account
                            if (!app.addAccount(accountName, url, login, password, "ngw")) {
                                mError = mGroupLayer.getContext().getString(R.string.account_already_exists);
                                return null;
                            }
                        } else {
                            //compare login/password and report differences
                            boolean same = app.getAccountPassword(account).equals(password) &&
                                    app.getAccountLogin(account).equals(login);
                            if (!same) {
                                Intent msg = new Intent(ConstantsUI.MESSAGE_INTENT);
                                msg.putExtra(
                                        ConstantsUI.KEY_MESSAGE,
                                        mGroupLayer.getContext().getString(R.string.warning_different_credentials));
                                context.sendBroadcast(msg);
                            }
                        }

                        //create NGWVectorLayer
                        NGWVectorLayerUI layer =
                                new NGWVectorLayerUI(mGroupLayer.getContext(), outputPath);
                        layer.setName(name);
                        layer.setRemoteId(resourceId);
                        layer.setVisible(true);
                        layer.setAccountName(accountName);
                        layer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
                        layer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

                        mGroupLayer.addLayer(layer);
                        mGroupLayer.save();

                        // create or connect to fill layer with features
                        intent.putExtra(ConstantsUI.KEY_LAYER_ID, layer.getId());
                        intent.putExtra(LayerFillService.KEY_INPUT_TYPE, layer.getType());
                    } else {
                        // prevent overwrite meta.json by layer save routine
                        meta.renameTo(new File(meta.getParentFile(), LayerFillService.NGFP_META));

                        VectorLayerUI layer = new VectorLayerUI(mGroupLayer.getContext(), outputPath);
                        layer.setName(name);
                        layer.setVisible(true);
                        layer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
                        layer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

                        mGroupLayer.addLayer(layer);
                        mGroupLayer.save();

                        intent.putExtra(ConstantsUI.KEY_LAYER_ID, layer.getId());
                        intent.putExtra(LayerFillService.KEY_INPUT_TYPE, layer.getType());
                        intent.putExtra(LayerFillService.KEY_PATH, dataFile.toString());
                        intent.putExtra(LayerFillService.KEY_DELETE_SRC_FILE, true);
                    }

                    return intent;
                }
            } catch (JSONException | IOException | URISyntaxException | SecurityException e) {
                e.printStackTrace();
                mError = e.getLocalizedMessage();
                return null;
            }

            mError = mGroupLayer.getContext().getString(R.string.error_layer_create);
            return null;
        }

    }
}

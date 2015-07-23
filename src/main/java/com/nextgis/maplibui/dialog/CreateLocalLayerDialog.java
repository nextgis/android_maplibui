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
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.map.LayerFactory;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.mapui.LocalTMSLayerUI;
import com.nextgis.maplibui.mapui.NGWVectorLayerUI;
import com.nextgis.maplibui.mapui.VectorLayerUI;
import com.nextgis.maplibui.util.ConstantsUI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.nextgis.maplib.util.Constants.MAX_CONTENT_LENGTH;
import static com.nextgis.maplib.util.Constants.NGW_ACCOUNT_TYPE;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.*;


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

    protected final static String FILE_META = "meta.json";
    protected final static String FILE_DATA = "data.geojson";


    protected String     mTitle;
    protected Uri        mUri;
    protected LayerGroup mGroupLayer;
    protected int        mLayerType;
    protected String     mLayerName;
    protected Spinner    mSpinner;


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
            short id = savedInstanceState.getShort(KEY_ID);
            mLayerType = savedInstanceState.getShort(KEY_LAYER_TYPE);
            tmsType = savedInstanceState.getShort(KEY_TMS_TYPE);

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
            view = View.inflate(context, R.layout.layout_create_vector_layer, null);
        } else {
            view = View.inflate(context, R.layout.layout_create_local_tms, null);

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
                            public void onClick(
                                    DialogInterface dialog,
                                    int whichButton)
                            {
                                mLayerName = layerName.getText().toString();
                                if (mLayerType < 3) {
                                    new CreateTask(context).execute(mLayerName);
                                } else {
                                    int nType = mSpinner.getSelectedItemPosition();
                                    String sTmsType = nType == 0 ? "XYZ" : "TMS";
                                    new CreateTask(context).execute(mLayerName, sTmsType);
                                }
                            }
                        }


                )
                .setNegativeButton(
                        R.string.cancel, new DialogInterface.OnClickListener()
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
        outState.putString(KEY_NAME, mLayerName);
        outState.putParcelable(KEY_URI, mUri);
        outState.putInt(KEY_LAYER_TYPE, mLayerType);
        if (null != mSpinner) {
            outState.putInt(KEY_POSITION, mSpinner.getSelectedItemPosition());
        }

        super.onSaveInstanceState(outState);
    }


    protected String createTMSLayer(
            Context context,
            String name,
            String type,
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
                    unzipEntry(zis, ze, buffer, outputPath);
                    nIncrement += ze.getSize();
                    zis.closeEntry();
                    task.setProgress(nIncrement);
                }

                //create Layer
                LocalTMSLayerUI layer = new LocalTMSLayerUI(mGroupLayer.getContext(), outputPath);
                layer.setName(name);
                layer.setVisible(true);
                layer.setTMSType(type.equals("TMS") ? TMSTYPE_NORMAL : TMSTYPE_OSM);

                int nMaxLevel = 0;
                int nMinLevel = 512;
                final File[] zoomLevels = outputPath.listFiles();

                task.setMessage(mGroupLayer.getContext().getString(R.string.message_opening));
                task.setMax(zoomLevels.length);
                int counter = 0;

                for (File zoomLevel : zoomLevels) {

                    task.setProgress(counter++);
                    int nMaxX = 0;
                    int nMinX = 10000000;
                    int nMaxY = 0;
                    int nMinY = 10000000;

                    int nLevelZ = Integer.parseInt(zoomLevel.getName());
                    if (nLevelZ > nMaxLevel) {
                        nMaxLevel = nLevelZ;
                    }
                    if (nLevelZ < nMinLevel) {
                        nMinLevel = nLevelZ;
                    }
                    final File[] levelsX = zoomLevel.listFiles();

                    boolean bFirstTurn = true;
                    for (File inLevelX : levelsX) {

                        int nX = Integer.parseInt(inLevelX.getName());
                        if (nX > nMaxX) {
                            nMaxX = nX;
                        }
                        if (nX < nMinX) {
                            nMinX = nX;
                        }

                        final File[] levelsY = inLevelX.listFiles();

                        if (bFirstTurn) {
                            for (File inLevelY : levelsY) {
                                String sLevelY = inLevelY.getName();

                                //Log.d(TAG, sLevelY);
                                int nY = Integer.parseInt(
                                        sLevelY.replace(
                                                com.nextgis.maplib.util.Constants.TILE_EXT, ""));
                                if (nY > nMaxY) {
                                    nMaxY = nY;
                                }
                                if (nY < nMinY) {
                                    nMinY = nY;
                                }
                            }
                            bFirstTurn = false;
                        }
                    }
                    layer.addLimits(nLevelZ, nMaxX, nMaxY, nMinX, nMinY);
                }

                mGroupLayer.addLayer(layer);
                mGroupLayer.save();

                return null;
            }
        } catch (NumberFormatException | IOException | SecurityException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }

        return mGroupLayer.getContext().getString(R.string.error_layer_create);
    }


    protected String createVectorLayer(
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
                //read all geojson

                //TODO: use JsonReader
                BufferedReader streamReader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null) {
                    nIncrement += inputStr.length();
                    task.setProgress(nIncrement);
                    responseStrBuilder.append(inputStr);
                    if(responseStrBuilder.length() > MAX_CONTENT_LENGTH)
                        return mGroupLayer.getContext().getString(R.string.error_layer_create);
                }
                task.setMessage(mGroupLayer.getContext().getString(R.string.message_opening));

                VectorLayerUI layer = new VectorLayerUI(
                        mGroupLayer.getContext(), mGroupLayer.createLayerStorage());
                layer.setName(name);
                layer.setVisible(true);
                layer.setMinZoom(0);
                layer.setMaxZoom(100);

                JSONObject geoJSONObject = new JSONObject(responseStrBuilder.toString());
                String errorMessage = layer.createFromGeoJSON(geoJSONObject);
                if (TextUtils.isEmpty(errorMessage)) {
                    mGroupLayer.addLayer(layer);
                    mGroupLayer.save();
                }

                return errorMessage;
            }
        } catch (JSONException | IOException | SecurityException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }

        return mGroupLayer.getContext().getString(R.string.error_layer_create);
    }


    protected String createVectorLayerWithForm(
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
                    unzipEntry(zis, ze, buffer, outputPath);
                    nIncrement += ze.getSize();
                    zis.closeEntry();
                    task.setProgress(nIncrement);
                }
                zis.close();

                //read meta.json
                File meta = new File(outputPath, FILE_META);
                String jsonText = FileUtil.readFromFile(meta);
                JSONObject metaJson = new JSONObject(jsonText);

                //read if this local o remote source
                boolean isNgwConnection = metaJson.has("ngw_connection");
                if (isNgwConnection && !metaJson.isNull("ngw_connection")) {
                    File dataFile = new File(outputPath, FILE_DATA);
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
                    try {
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

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }

                    Account account = LayerFactory.getAccountByName(context, accountName);
                    final AccountManager am = AccountManager.get(context.getApplicationContext());
                    if (null == account) {
                        //create account
                        Bundle userData = new Bundle();
                        userData.putString("url", url);
                        userData.putString("login", login);
                        account = new Account(accountName, NGW_ACCOUNT_TYPE);
                        if (!am.addAccountExplicitly(account, password, userData)) {
                            return mGroupLayer.getContext().getString(
                                    R.string.account_already_exists);
                        }
                    } else {
                        //compare login/password and report differences
                        boolean same = am.getPassword(account).equals(password) &&
                                       am.getUserData(account, "login").equals(login);
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
                    layer.setMinZoom(0);
                    layer.setMaxZoom(100);

                    mGroupLayer.addLayer(layer);
                    mGroupLayer.save();

                    task.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    task.setIndeterminate(true);

                    return layer.download();
                } else {
                    VectorLayerUI layer = new VectorLayerUI(mGroupLayer.getContext(), outputPath);
                    layer.setName(name);
                    layer.setVisible(true);
                    layer.setMinZoom(0);
                    layer.setMaxZoom(100);

                    //TODO: use JsonReader
                    File dataFile = new File(outputPath, FILE_DATA);
                    String jsonContent = FileUtil.readFromFile(dataFile);

                    FileUtil.deleteRecursive(dataFile);

                    JSONObject geoJSONObject = new JSONObject(jsonContent);
                    JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GEOJSON_TYPE_FEATURES);
                    if (0 == geoJSONFeatures.length()) {
                        //create empty layer

                        //read fields
                        List<Field> fields =
                                NGWVectorLayer.getFieldsFromJson(metaJson.getJSONArray(NGWUtil.NGWKEY_FIELDS));
                        //read geometry type
                        String geomTypeString = metaJson.getString("geometry_type");
                        int geomType = GeoGeometryFactory.typeFromString(geomTypeString);

                        //read SRS -- not need as we will be fill layer with 3857
                        //JSONObject srs = metaJson.getJSONObject(NGWUtil.NGWKEY_SRS);
                        //int nSRS = srs.getInt(NGWUtil.NGWKEY_ID);

                        FileUtil.deleteRecursive(meta);

                        mGroupLayer.addLayer(layer);
                        mGroupLayer.save();

                        return layer.initialize(fields, new ArrayList<Feature>(), geomType);
                    } else {

                        //read fields
                        List<Field> fields =
                                NGWVectorLayer.getFieldsFromJson(metaJson.getJSONArray(NGWUtil.NGWKEY_FIELDS));
                        //read geometry type
                        String geomTypeString = metaJson.getString("geometry_type");
                        int geomType = GeoGeometryFactory.typeFromString(geomTypeString);

                        //read SRS -- not need as we will be fill layer with 3857
                        JSONObject srs = metaJson.getJSONObject("srs");
                        int nSRS = srs.getInt(NGWUtil.NGWKEY_ID);

                        FileUtil.deleteRecursive(meta);
                        String errorMessage =
                                layer.createFromGeoJSON(geoJSONObject, fields, geomType, nSRS);
                        if (TextUtils.isEmpty(errorMessage)) {
                            mGroupLayer.addLayer(layer);
                            mGroupLayer.save();
                        }
                        return errorMessage;
                    }
                }
            }
        } catch (JSONException | IOException | SQLiteException | SecurityException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }

        return mGroupLayer.getContext().getString(R.string.error_layer_create);
    }


    protected void unzipEntry(
            ZipInputStream zis,
            ZipEntry entry,
            byte[] buffer,
            File outputDir)
            throws IOException
    {
        String entryName = entry.getName();
        int pos = entryName.indexOf('/');
        String folderName = entryName.substring(0, pos);

        //for backward capability where the zip has root directory named "mapnik"
        if (!TextUtils.isDigitsOnly(folderName)) {
            if (pos != NOT_FOUND) {
                entryName = entryName.substring(pos, entryName.length());
            }
        }

        if (entry.isDirectory()) {
            FileUtil.createDir(new File(outputDir, entryName));
            return;
        }

        //for prevent searching by media library
        entryName = entryName.replace(".png", com.nextgis.maplib.util.Constants.TILE_EXT);
        entryName = entryName.replace(".jpg", com.nextgis.maplib.util.Constants.TILE_EXT);
        entryName = entryName.replace(".jpeg", com.nextgis.maplib.util.Constants.TILE_EXT);

        File outputFile = new File(outputDir, entryName);
        if (!outputFile.getParentFile().exists()) {
            FileUtil.createDir(outputFile.getParentFile());
        }
        FileOutputStream fout = new FileOutputStream(outputFile);
        FileUtil.copyStream(zis, fout, buffer, Constants.IO_BUFFER_SIZE);
        fout.close();
    }


    protected class CreateTask
            extends AsyncTask<String, String, String>
    {
        protected ProgressDialog mProgressDialog;
        protected Context        mContext;


        public CreateTask(Context context)
        {
            mContext = context;
        }


        @Override
        protected void onPreExecute()
        {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(
                    mGroupLayer.getContext().getString(R.string.message_loading));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();
        }


        @Override
        protected String doInBackground(String... names)
        {
            if (mLayerType == VECTOR_LAYER) {
                //create local layer from json
                return createVectorLayer(mContext, names[0], this);
            } else if (mLayerType == VECTOR_LAYER_WITH_FORM) {
                //create local layer from ngfb
                return createVectorLayerWithForm(mContext, names[0], this);
            } else if (mLayerType == TMS_LAYER) {
                //create local TMS layer
                return createTMSLayer(mContext, names[0], names[1], this);
            }
            return null;
        }


        @Override
        protected void onPostExecute(String error)
        {
            try { // if dialog is already detach from window catch exception
                mProgressDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (null != error && error.length() > 0) {
                Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(
                        mContext, mContext.getString(R.string.message_layer_created),
                        Toast.LENGTH_SHORT).show();
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
    }
}

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

import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.map.LayerFactory;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplibui.mapui.NGWVectorLayerUI;
import com.nextgis.maplibui.mapui.VectorLayerUI;
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

import static com.nextgis.maplib.util.Constants.NGW_ACCOUNT_TYPE;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_FEATURES;


/**
 * The dialog to pick layer name and create the vector layer by input Uri
 */
public class CreateVectorLayerDialog
        extends DialogFragment
{
    public final static    int    VECTOR_LAYER           = 1;
    public final static    int    VECTOR_LAYER_WITH_FORM = 2;

    protected final static String KEY_TITLE      = "title";
    protected final static String KEY_NAME       = "name";
    protected final static String KEY_ID         = "id";
    protected final static String KEY_URI        = "uri";
    protected final static String KEY_LAYER_TYPE = "layer_type";

    protected final static String FILE_META = "meta.json";
    protected final static String FILE_DATA = "data.geojson";


    protected String     mTitle;
    protected Uri        mUri;
    protected LayerGroup mGroupLayer;
    protected int        mLayerType;
    protected String     mLayerName;


    public CreateVectorLayerDialog setTitle(String title)
    {
        mTitle = title;
        return this;
    }


    public CreateVectorLayerDialog setLayerName(String layerName)
    {
        mLayerName = layerName;
        return this;
    }


    public CreateVectorLayerDialog setUri(Uri uri)
    {
        mUri = uri;
        return this;
    }


    public CreateVectorLayerDialog setLayerType(int layerType)
    {
        mLayerType = layerType;
        return this;
    }


    public CreateVectorLayerDialog setLayerGroup(LayerGroup groupLayer)
    {
        mGroupLayer = groupLayer;
        return this;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final Context context = getActivity();
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.layout_create_vector_layer, null);


        if (null == savedInstanceState) {
            //nothing to do
        } else {
            mLayerName = savedInstanceState.getString(KEY_NAME);
            mTitle = savedInstanceState.getString(KEY_TITLE);
            mUri = savedInstanceState.getParcelable(KEY_URI);
            short id = savedInstanceState.getShort(KEY_ID);
            mLayerType = savedInstanceState.getShort(KEY_LAYER_TYPE);
            MapBase map = MapBase.getInstance();
            if (null != map) {
                ILayer iLayer = map.getLayerById(id);
                if (iLayer instanceof LayerGroup) {
                    mGroupLayer = (LayerGroup) iLayer;
                }
            }

        }
        final EditText layerName = (EditText) view.findViewById(R.id.layer_name);
        layerName.setText(mLayerName);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(mTitle)
               .setIcon(R.drawable.ic_local_vector)
               .setView(view)
               .setPositiveButton(R.string.create, new DialogInterface.OnClickListener()
                                  {
                                      public void onClick(
                                              DialogInterface dialog,
                                              int whichButton)
                                      {
                                          mLayerName = layerName.getText().toString();
                                          new CreateTask(context).execute(mLayerName);
                                      }
                                  }


               )
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
        outState.putString(KEY_NAME, mLayerName);
        outState.putParcelable(KEY_URI, mUri);
        outState.putInt(KEY_LAYER_TYPE, mLayerType);

        super.onSaveInstanceState(outState);
    }

    protected String createVectorLayer(
            Context context,
            String name,
            ProgressDialog progressDialog){
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(mUri);
            if (inputStream != null) {
                int nSize = inputStream.available();
                int nIncrement = 0;
                progressDialog.setMax(nSize);
                //read all geojson
                BufferedReader streamReader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;
                progressDialog.show();
                while ((inputStr = streamReader.readLine()) != null) {
                    nIncrement += inputStr.length();
                    progressDialog.setProgress(nIncrement);
                    responseStrBuilder.append(inputStr);
                }
                progressDialog.setMessage(mGroupLayer.getContext().getString(
                        R.string.message_opening));

                VectorLayerUI layer = new VectorLayerUI(mGroupLayer.getContext(),
                                                        mGroupLayer.createLayerStorage());
                layer.setName(name);
                layer.setVisible(true);

                JSONObject geoJSONObject = new JSONObject(responseStrBuilder.toString());
                String errorMessage = layer.createFromGeoJSON(geoJSONObject);
                if(TextUtils.isEmpty(errorMessage)) {
                    mGroupLayer.addLayer(layer);
                    mGroupLayer.save();
                }

                return errorMessage;
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }

        return mGroupLayer.getContext().getString(R.string.error_layer_create);
    }

    protected String createVectorLayerWithForm(
            Context context,
            String name,
            ProgressDialog progressDialog){
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(mUri);
            if (inputStream != null) {

                int nSize = inputStream.available();
                int nIncrement = 0;
                progressDialog.setMax(nSize);

                File outputPath = mGroupLayer.createLayerStorage();
                ZipInputStream zis = new ZipInputStream(inputStream);

                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    unzipEntry(zis, ze, outputPath);
                    nIncrement += ze.getSize();
                    zis.closeEntry();
                    progressDialog.setProgress(nIncrement);
                }
                zis.close();

                //read meta.json
                File meta = new File(outputPath, FILE_META);
                String jsonText = FileUtil.readFromFile(meta);
                JSONObject metaJson = new JSONObject(jsonText);

                //read if this local o remote source
                boolean isNgwConnection = metaJson.has("ngw_connection");
                if(isNgwConnection){
                    File dataFile = new File(outputPath, FILE_DATA);
                    FileUtil.deleteRecursive(dataFile);

                    JSONObject connection = metaJson.getJSONObject("ngw_connection");
                    //read url
                    String url = connection.getString("url");
                    if(!url.startsWith("http"))
                        url = "http://" + url;

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

                        if (uri.getHost() != null && uri.getHost().length() > 0)
                            accountName += uri.getHost();
                        if (uri.getPort() != 80 && uri.getPort() > 0)
                            accountName += ":" + uri.getPort();
                        if (uri.getPath() != null && uri.getPath().length() > 0)
                            accountName += uri.getPath();

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }

                    Account account = LayerFactory.getAccountByName(context, accountName);
                    final AccountManager am = AccountManager.get(context);
                    if(null == account) {
                        //create account
                        Bundle userData = new Bundle();
                        userData.putString("url", url);
                        userData.putString("login", login);
                        account = new Account(accountName, NGW_ACCOUNT_TYPE);
                        if (!am.addAccountExplicitly(account, password, userData)) {
                            return mGroupLayer.getContext().getString(
                                    R.string.account_already_exists);
                        }
                    }
                    else{
                        //compare login/password and report differences
                        boolean same = am.getPassword(account).equals(password) &&
                                       am.getUserData(account, "login").equals(login);
                        if(!same){
                            Intent msg = new Intent(com.nextgis.maplibui.util.Constants.MESSAGE_INTENT);
                            msg.putExtra(com.nextgis.maplibui.util.Constants.KEY_WARNING, getString(R.string.warning_different_credentials));
                            context.sendBroadcast(msg);
                        }
                    }

                    //create NGWVectorLayer
                    NGWVectorLayerUI layer = new NGWVectorLayerUI(mGroupLayer.getContext(),
                                                                  outputPath);
                    layer.setName(name);
                    layer.setURL(url);
                    layer.setRemoteId(resourceId);
                    layer.setVisible(true);
                    layer.setAccountName(accountName);
                    layer.setLogin(am.getUserData(account, "login"));
                    layer.setPassword(am.getPassword(account));

                    mGroupLayer.addLayer(layer);
                    mGroupLayer.save();

                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.setIndeterminate(true);

                    return layer.download();
                }
                else{
                    VectorLayerUI layer = new VectorLayerUI(mGroupLayer.getContext(),
                                                            outputPath);
                    layer.setName(name);
                    layer.setVisible(true);

                    File dataFile = new File(outputPath, FILE_DATA);
                    String jsonContent = FileUtil.readFromFile(dataFile);

                    FileUtil.deleteRecursive(dataFile);

                    JSONObject geoJSONObject = new JSONObject(jsonContent);
                    JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GEOJSON_TYPE_FEATURES);
                    if (0 == geoJSONFeatures.length()){
                        //create empty layer

                        //read fields
                        List<Field> fields = NGWVectorLayer.getFieldsFromJson(metaJson.getJSONArray("fields"));
                        //read geometry type
                        String geomTypeString = metaJson.getString("geometry_type");
                        int geomType = GeoGeometryFactory.typeFromString(geomTypeString);
                        if(geomType < 4)
                            geomType += 3;

                        //read SRS -- not need as we will be fill layer with 3857
                        //JSONObject srs = metaJson.getJSONObject("srs");
                        //int nSRS = srs.getInt("id");

                        FileUtil.deleteRecursive(meta);

                        return layer.initialize(fields, new ArrayList<Feature>(), geomType);
                    }
                    else{
                        FileUtil.deleteRecursive(meta);
                        String errorMessage = layer.createFromGeoJSON(geoJSONObject);
                        if(TextUtils.isEmpty(errorMessage)) {
                            mGroupLayer.addLayer(layer);
                            mGroupLayer.save();
                        }
                        return errorMessage;
                    }
                }
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }

        return mGroupLayer.getContext().getString(R.string.error_layer_create);
    }

    protected void unzipEntry(ZipInputStream zis, ZipEntry entry, File outputDir) throws IOException {
        String entryName = entry.getName();

        //for backward capability where the zip haz root directory named "mapnik"
        entryName = entryName.replace("Mapnik/", "");
        entryName = entryName.replace("mapnik/", "");

        //for prevent searching by media library
        entryName = entryName.replace(".png", com.nextgis.maplib.util.Constants.TILE_EXT);
        entryName = entryName.replace(".jpg", com.nextgis.maplib.util.Constants.TILE_EXT);
        entryName = entryName.replace(".jpeg", com.nextgis.maplib.util.Constants.TILE_EXT);
        if (entry.isDirectory()) {
            FileUtil.createDir(new File(outputDir, entryName));
            return;
        }
        File outputFile = new File(outputDir, entryName);
        if (!outputFile.getParentFile().exists()) {
            FileUtil.createDir(outputFile.getParentFile());
        }
        FileOutputStream fout = new FileOutputStream(outputFile);
        int nCount;
        byte[] buffer = new byte[1024];
        while ((nCount = zis.read(buffer)) > 0) {
            fout.write(buffer, 0, nCount);
        }
        //fout.flush();
        fout.close();
    }

    protected class CreateTask
            extends AsyncTask<String, Void, String>
    {
        protected ProgressDialog mProgressDialog;
        protected Context mContext;


        public CreateTask(Context context)
        {
            mContext = context;
        }


        @Override
        protected void onPreExecute()
        {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(mGroupLayer.getContext().getString(R.string.message_loading));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();
        }


        @Override
        protected String doInBackground(String... names)
        {
            if (mLayerType == VECTOR_LAYER) {
                //create local layer from json
                return createVectorLayer(mContext, names[0], mProgressDialog);
            } else if (mLayerType == VECTOR_LAYER_WITH_FORM) {
                //create local layer from ngfb
                return createVectorLayerWithForm(mContext, names[0], mProgressDialog);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String error)
        {
            mProgressDialog.dismiss();
            if(null != error && error.length() > 0){
                Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(mContext, mContext.getString(R.string.message_layer_created),
                               Toast.LENGTH_SHORT).show();
            }
        }
    }
}

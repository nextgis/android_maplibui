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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplibui.mapui.VectorLayerUI;
import com.nextgis.maplibui.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                                          if (mLayerType == VECTOR_LAYER) {
                                              //create local layer from json
                                              createVectorLayer(mLayerName);
                                          } else if (mLayerType == VECTOR_LAYER_WITH_FORM) {
                                              //create local layer from ngfb
                                              createVectorLayerWithForm(mLayerName);
                                          }
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

    protected void createVectorLayer(String name){
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(mUri);
            if (inputStream != null) {
                ProgressDialog progressDialog = new ProgressDialog(getActivity());
                progressDialog.setMessage(getString(R.string.message_loading));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(true);
                progressDialog.show();
                int nSize = inputStream.available();
                int nIncrement = 0;
                progressDialog.setMax(nSize);
                //read all geojson
                BufferedReader streamReader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null) {
                    nIncrement += inputStr.length();
                    progressDialog.setProgress(nIncrement);
                    responseStrBuilder.append(inputStr);
                }
                progressDialog.setMessage(getString(R.string.message_opening));

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
                else{
                    Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
                }

                progressDialog.dismiss();

                return;
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getActivity(), getString(R.string.error_layer_create), Toast.LENGTH_SHORT).show();
    }

    protected void createVectorLayerWithForm(String name){
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(mUri);
            if (inputStream != null) {
                ProgressDialog progressDialog = new ProgressDialog(getActivity());
                progressDialog.setMessage(getString(R.string.message_loading));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(true);
                progressDialog.show();
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

                //1. read meta.json
                File meta = new File(outputPath, FILE_META);
                String jsonText = FileUtil.readFromFile(meta);
                JSONObject metaJson = new JSONObject(jsonText);

                //1.1 read if this local o remote source

                //2. if meta said that GeoJSON available

                //3. form not read at all
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getActivity(), getString(R.string.error_layer_create), Toast.LENGTH_SHORT).show();
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
}

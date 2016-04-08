/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;


public class CreateFromQMSLayerDialog extends NGDialog {
    protected final static String QMS_URL = "https://qms.nextgis.com/api/v1/geoservices/";
    protected final static String QMS_LIST_URL = QMS_URL + "?epsg=3857&format=json&type=tms";
    protected final static String QMS_DETAIL_APPENDIX = "/?format=json";

    protected final static String KEY_ID = "id";
    protected final static String KEY_NAME = "name";
    protected final static String KEY_URL = "url";
    protected final static String KEY_Z_MIN = "z_min";
    protected final static String KEY_Z_MAX = "z_max";

    protected LayerGroup mGroupLayer;
    protected ListView mLayers;
    protected LinearLayout mProgress;
    protected List<HashMap<String, Object>> mData;
    protected volatile List<Integer> mChecked;

    public CreateFromQMSLayerDialog setLayerGroup(LayerGroup groupLayer) {
        mGroupLayer = groupLayer;
        return this;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_ID, mGroupLayer.getId());
        super.onSaveInstanceState(outState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        if (null != savedInstanceState) {
            int id = savedInstanceState.getInt(KEY_ID);
            MapBase map = MapBase.getInstance();
            if (null != map) {
                ILayer iLayer = map.getLayerById(id);
                if (iLayer instanceof LayerGroup) {
                    mGroupLayer = (LayerGroup) iLayer;
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mTitle).setIcon(R.drawable.ic_remote_tms).setView(R.layout.list_content)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (mChecked.size() > 0) {
                            mLayers.setVisibility(View.GONE);
                            mProgress.setVisibility(View.VISIBLE);

                            for (int i = 0; i < mChecked.size(); i++)
                                new LoadLayer().execute(mChecked.get(i));
                        }
                    }
                })
                .setNeutralButton(R.string.new_tms, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CreateRemoteTMSLayerDialog newFragment = new CreateRemoteTMSLayerDialog();
                        newFragment.setLayerGroup(mGroupLayer)
                                .setTitle(mContext.getString(R.string.create_tms_layer))
                                .setTheme(((NGActivity) getActivity()).getThemeId())
                                .show(getActivity().getSupportFragmentManager(), "create_tms_layer");
                    }
                })
                .setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                mEnabledColor = dialog.getButton(DialogInterface.BUTTON_POSITIVE).getTextColors().getDefaultColor();
                setEnabled(dialog.getButton(DialogInterface.BUTTON_POSITIVE), false);

                mLayers = (ListView) dialog.findViewById(android.R.id.list);
                mProgress = (LinearLayout) dialog.findViewById(R.id.progressContainer);
                new LoadLayersList().execute();

                mLayers.setItemsCanFocus(false);
                mLayers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                boolean checked = ((CheckedTextView) view).isChecked();

                                if (checked) {
                                    if (!mChecked.contains(position))
                                        mChecked.add(position);
                                } else
                                    mChecked.remove(Integer.valueOf(position));

                                setEnabled(dialog.getButton(DialogInterface.BUTTON_POSITIVE), mChecked.size() > 0);
                            }
                        });
                    }
                });
            }
        });

        mData = new ArrayList<>();
        mChecked = new ArrayList<>();

        return dialog;
    }

    private class LoadLayer extends AsyncTask<Integer, Void, String> {
        Integer mLayerPosition;
        @Override
        protected String doInBackground(Integer... params) {
            try {
                mLayerPosition = params[0];
                int layerId = (Integer) mData.get(mLayerPosition).get(KEY_ID);
                return NetworkUtil.get(QMS_URL + layerId + QMS_DETAIL_APPENDIX, null, null);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            try {
                JSONObject remoteLayer = new JSONObject(response);

                int tmsType = TMSTYPE_OSM; // TODO ??
                String layerName = (String) mData.get(mLayerPosition).get(KEY_NAME);
                String layerURL = remoteLayer.getString(KEY_URL);
                float minZoom = remoteLayer.isNull(KEY_Z_MIN) ? GeoConstants.DEFAULT_MIN_ZOOM: (float) remoteLayer.getDouble(KEY_Z_MIN);
                float maxZoom = remoteLayer.isNull(KEY_Z_MAX) ? GeoConstants.DEFAULT_MAX_ZOOM: (float) remoteLayer.getDouble(KEY_Z_MAX);

                // do we need this checks? QMS should provide correct data
                //check if {x}, {y} or {z} present
                if (!layerURL.contains("{x}") || !layerURL.contains("{y}") || !layerURL.contains("{z}")) {
                    Toast.makeText(mContext, R.string.error_layer_create, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!layerURL.startsWith("http")) {
                    layerURL = "http://" + layerURL;
                }

                boolean isURL = URLUtil.isValidUrl(layerURL);

                if (!isURL) {
                    Toast.makeText(mContext, R.string.error_layer_create, Toast.LENGTH_SHORT).show();
                    return;
                }

                //create new layer and store it and add it to the map
                RemoteTMSLayerUI layer = new RemoteTMSLayerUI(mGroupLayer.getContext(), mGroupLayer.createLayerStorage());
                layer.setName(layerName);
                layer.setURL(layerURL);
                layer.setTMSType(tmsType);
                layer.setVisible(true);
                layer.setMinZoom(minZoom);
                layer.setMaxZoom(maxZoom);

                mGroupLayer.addLayer(layer);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mChecked.remove(mLayerPosition);

            if (mChecked.size() == 0)
                mGroupLayer.save();
        }
    }

    private class LoadLayersList extends AsyncTask<Void, Void, Void> {
        private SimpleAdapter mAdapter;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String response = NetworkUtil.get(QMS_LIST_URL, null, null);
                JSONArray layers = new JSONArray(response);

                for (int i = 0; i < layers.length(); i++) {
                    JSONObject layer = layers.getJSONObject(i);
                    HashMap<String, Object> data = new HashMap<>();
                    data.put(KEY_ID, layer.getInt(KEY_ID));
                    data.put(KEY_NAME, layer.getString(KEY_NAME));
                    mData.add(data);
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mAdapter = new SimpleAdapter(mContext, mData, R.layout.select_dialog_multichoice_material, new String[]{KEY_NAME}, new int[]{android.R.id.text1});
            mLayers.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            mLayers.setAdapter(mAdapter);

            mProgress.setVisibility(View.GONE);
            mLayers.setVisibility(View.VISIBLE);
        }
    }
}

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
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;
import com.nextgis.maplibui.util.ConstantsUI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_NORMAL;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;

public class CreateFromQMSLayerDialog extends NGDialog {
    protected final static String QMS_URL = "https://qms.nextgis.com/api/v1";
    protected final static String QMS_GEOSERVICE_URL = QMS_URL + "/geoservices/";
    protected final static String QMS_GEOSERVICE_LIST_URL = QMS_GEOSERVICE_URL + "?epsg=3857&format=json&type=tms";
    protected final static String QMS_DETAIL_APPENDIX = "/?format=json";
    protected final static String QMS_ICON_URL = QMS_URL + "/icons/";
    protected final static String QMS_ICON_APPENDIX = "?width={w}&height={h}";
    protected final static String QMS_ICON_CONTENT = "/content" + QMS_ICON_APPENDIX;

    protected final static String KEY_ID = "id";
    protected final static String KEY_NAME = "name";
    protected final static String KEY_ICON = "icon";
    protected final static String KEY_URL = "url";
    protected final static String KEY_Z_MIN = "z_min";
    protected final static String KEY_Z_MAX = "z_max";
    protected final static String Y_TOP = "y_origin_top";

    protected LayerGroup mGroupLayer;
    protected ListView mLayers;
    protected LinearLayout mProgress, mQMSLayers;
    protected ProgressBar mProgressBar;
    protected TextView mProgressInfo;
    protected SearchView mSearch;
    protected Button mPositive;
    protected boolean mRetry;
    protected List<HashMap<String, Object>> mData;
    protected volatile List<Integer> mChecked;
    protected NetworkUtil mNet;
    protected File mQMSIconsDir;

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
        mNet = new NetworkUtil(mContext);
        mQMSIconsDir = MapUtil.prepareTempDir(mContext, "qms_icons");

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
        builder.setTitle(mTitle).setView(R.layout.list_content)
                .setPositiveButton(R.string.add, null)
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

        mData = new ArrayList<>();
        mChecked = new ArrayList<>();

        IGISApplication application = (IGISApplication) getActivity().getApplication();
        application.sendScreen(ConstantsUI.GA_DIALOG_QMS);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null && mLayers == null) {
            mPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            mEnabledColor = mPositive.getTextColors().getDefaultColor();
            setEnabled(mPositive, false);

            mLayers = (ListView) dialog.findViewById(R.id.list);
            mProgress = (LinearLayout) dialog.findViewById(R.id.progressContainer);
            mQMSLayers = (LinearLayout) dialog.findViewById(R.id.qms_layers);
            mProgressBar = (ProgressBar) dialog.findViewById(R.id.progressBar);
            mProgressInfo = (TextView) dialog.findViewById(R.id.progressInfo);
            mSearch = (SearchView) dialog.findViewById(R.id.search);
            mSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    LayersAdapter adapter = (LayersAdapter) mLayers.getAdapter();
                    if (adapter != null) {
                        adapter.getFilter().filter(newText);
                        return true;
                    }

                    return false;
                }
            });
            new LoadLayersList().execute();

            mLayers.setItemsCanFocus(false);
            mLayers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            boolean checked = ((CheckedTextView) view).isChecked();
                            //noinspection unchecked
                            int id = (int) ((Map<String, Object>) mLayers.getAdapter().getItem(position)).get(KEY_ID);

                            if (checked) {
                                if (!mChecked.contains(id))
                                    mChecked.add(id);
                            } else
                                mChecked.remove(Integer.valueOf(id));

                            setEnabled(mPositive, mChecked.size() > 0);
                        }
                    });
                }
            });

            mPositive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mRetry) {
                        mRetry = false;
                        mProgressBar.setVisibility(View.VISIBLE);
                        mProgressInfo.setText(R.string.message_loading);
                        setEnabled(mPositive, false);
                        mPositive.setText(R.string.add);
                        new LoadLayersList().execute();
                    } else {
                        if (mChecked.size() > 0) {
                            mQMSLayers.setVisibility(View.GONE);
                            mProgress.setVisibility(View.VISIBLE);

                            for (int i = 0; i < mChecked.size(); i++)
                                new LoadLayer().execute(mChecked.get(i));
                        } else
                            Toast.makeText(mContext, R.string.nothing_selected, Toast.LENGTH_SHORT).show();

                        dialog.dismiss();
                    }
                }
            });
        }
    }

    private class LoadLayer extends AsyncTask<Integer, Void, HttpResponse> {
        private Integer mLayerId;

        @Override
        protected HttpResponse doInBackground(Integer... params) {
            if (!mNet.isNetworkAvailable())
                return new HttpResponse(NetworkUtil.ERROR_NETWORK_UNAVAILABLE);

            try {
                mLayerId = params[0];
                return NetworkUtil.get(QMS_GEOSERVICE_URL + mLayerId + QMS_DETAIL_APPENDIX, null, null, false);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new HttpResponse(NetworkUtil.ERROR_DOWNLOAD_DATA);
        }

        @Override
        protected void onPostExecute(HttpResponse response)
        {
            super.onPostExecute(response);

            if (response.isOk()) {
                try {
                    new JSONObject(response.getResponseBody());
                    createLayer(response.getResponseBody());
                } catch (JSONException ignored) {
                    Toast.makeText(mContext, R.string.qms_unavailable, Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(
                        mContext, NetworkUtil.getError(mContext, response.getResponseCode()),
                        Toast.LENGTH_SHORT).show();
            }

            mChecked.remove(mLayerId);

            if (mChecked.size() == 0)
                mGroupLayer.save();
        }

        private void createLayer(String response) {
            try {
                JSONObject remoteLayer = new JSONObject(response);
                String layerName = remoteLayer.getString(KEY_NAME);
                String layerURL = remoteLayer.getString(KEY_URL);
                float minZoom = remoteLayer.isNull(KEY_Z_MIN) ? GeoConstants.DEFAULT_MIN_ZOOM : (float) remoteLayer.getDouble(KEY_Z_MIN);
                float maxZoom = remoteLayer.isNull(KEY_Z_MAX) ? GeoConstants.DEFAULT_MAX_ZOOM : (float) remoteLayer.getDouble(KEY_Z_MAX);
                boolean yOrder = remoteLayer.optBoolean(Y_TOP, true);
                int tmsType = yOrder ? TMSTYPE_OSM : TMSTYPE_NORMAL;

                // do we need this checks? QMS should provide correct data
                //check if {x}, {y} or {z} present
                if (!layerURL.contains("{x}") || !layerURL.contains("{y}") || !layerURL.contains("{z}")) {
                    String error = layerName + ": " + mContext.getString(R.string.error_invalid_url);
                    Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!layerURL.startsWith("http")) {
                    layerURL = "http://" + layerURL;
                }

                boolean isURL = URLUtil.isValidUrl(layerURL);

                if (!isURL) {
                    String error = layerName + ": " + mContext.getString(R.string.error_invalid_url);
                    Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show();
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
        }
    }

    private class LoadLayersList extends AsyncTask<Void, Void, HttpResponse> {
        private SimpleAdapter mAdapter;

        @Override
        protected HttpResponse doInBackground(Void... params) {
            if (!mNet.isNetworkAvailable())
                return new HttpResponse(NetworkUtil.ERROR_NETWORK_UNAVAILABLE);

            try {
                return NetworkUtil.get(QMS_GEOSERVICE_LIST_URL, null, null, false);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new HttpResponse(NetworkUtil.ERROR_DOWNLOAD_DATA);
        }

        @Override
        protected void onPostExecute(HttpResponse response) {
            super.onPostExecute(response);

            if (response.isOk()) {
                try {
                    new JSONArray(response.getResponseBody());
                    createList(response.getResponseBody());
                } catch (JSONException ignored) {
                    Toast.makeText(mContext, R.string.qms_unavailable, Toast.LENGTH_SHORT).show();
                    showRetry();
                }
            } else {
                Toast.makeText(
                        mContext, NetworkUtil.getError(mContext, response.getResponseCode()),
                        Toast.LENGTH_SHORT).show();
                showRetry();
            }
        }

        private void showRetry() {
            mRetry = true;
            mProgressBar.setVisibility(View.GONE);
            mProgressInfo.setText(R.string.error_connect_failed);
            setEnabled(mPositive, true);
            mPositive.setText(R.string.retry);
        }

        private void createList(String response) {
            try {
                JSONArray layers = new JSONArray(response);
                for (int i = 0; i < layers.length(); i++) {
                    JSONObject layer = layers.getJSONObject(i);
                    HashMap<String, Object> data = new HashMap<>();
                    data.put(KEY_ID, layer.getInt(KEY_ID));
                    data.put(KEY_NAME, layer.getString(KEY_NAME));
                    data.put(KEY_ICON, layer.getString(KEY_ICON));
                    mData.add(data);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mAdapter = new LayersAdapter(mContext, mData, R.layout.item_qms_layer, new String[]{KEY_NAME}, new int[]{R.id.text1});
            mLayers.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            mLayers.setAdapter(mAdapter);

            mProgress.setVisibility(View.GONE);
            mQMSLayers.setVisibility(View.VISIBLE);
        }
    }

    protected class LayersAdapter extends SimpleAdapter implements Filterable {
        private final LayoutInflater mInflater;
        private List<? extends Map<String, ?>> mOriginal, mFiltered;
        private Filter mFilter;
        private int[] mTo;
        private String[] mFrom;
        private int mResource;
        private Map<String, LoadIcon> mIconsQueue;

        LayersAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
            mFiltered = mOriginal = data;
            mFrom = from;
            mTo = to;
            mResource = resource;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mIconsQueue = new HashMap<>();
        }

        @Override
        public int getCount() {
            return mFiltered.size();
        }

        @Override
        public Object getItem(int position) {
            return mFiltered.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(mInflater, position, convertView, parent, mResource);
        }

        private void bindView(int position, View view) {
            final Map dataSet = mFiltered.get(position);
            if (dataSet == null)
                return;

            final String[] from = mFrom;
            final int[] to = mTo;
            final int count = to.length;

            for (int i = 0; i < count; i++) {
                final View v = view.findViewById(to[i]);
                if (v != null) {
                    final Object data = dataSet.get(from[i]);
                    String text = data == null ? "" : data.toString();
                    if (text == null)
                        text = "";

                    if (v instanceof Checkable) {
                        if (v instanceof TextView) {
                            TextView textView = (TextView) v;
                            // Note: keep the instanceof TextView check at the bottom of these
                            // ifs since a lot of views are TextViews (e.g. CheckBoxes).
                            setViewText(textView, text);
                            setIcon(textView, null);

                            String id = (String) dataSet.get(KEY_ICON);
                            if (id == null || !MapUtil.isParsable(id))
                                id = "default";

                            File icon = new File(mQMSIconsDir, id);
                            if (icon.exists())
                                setIcon(textView, icon.getPath());
                            else if (mIconsQueue.keySet().contains(id))
                                mIconsQueue.get(id).addViewWithIcon(textView);
                            else {
                                LoadIcon task = new LoadIcon(icon.getPath(), textView);
                                mIconsQueue.put(id, task);
                                task.execute();
                            }
                        }
                    }
                }
            }
        }

        private View createViewFromResource(LayoutInflater inflater, int position, View convertView, ViewGroup parent, int resource) {
            View view;
            if (convertView == null)
                view = inflater.inflate(resource, parent, false);
            else
                view = convertView;

            bindView(position, view);
            return view;
        }

        @Override
        public Filter getFilter() {
            if (mFilter == null)
                mFilter = new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults result = new FilterResults();

                        if (!TextUtils.isEmpty(constraint) && constraint.length() > 1) {
                            constraint = constraint.toString().toLowerCase();
                            List<Map<String, ?>> founded = new ArrayList<>();

                            for (Map<String, ?> item : mOriginal)
                                if (item.get(KEY_NAME).toString().toLowerCase().contains(constraint))
                                    founded.add(item);

                            result.values = founded;
                            result.count = founded.size();
                        } else {
                            result.values = mOriginal;
                            result.count = mOriginal.size();
                        }

                        return result;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                        //noinspection unchecked
                        mFiltered = (List<Map<String, ?>>) results.values;
                        if (results.count > 0) {
                            notifyDataSetChanged();
                        } else {
                            notifyDataSetInvalidated();
                        }
                    }
                };

            return mFilter;
        }

        class LoadIcon extends AsyncTask<Void, Void, Void> {
            private File mFile;
            private String mPath;
            private Queue<TextView> mAssignedViews;

            LoadIcon(String path, TextView view) {
                mAssignedViews = new LinkedList<>();
                mAssignedViews.add(view);
                mPath = path;
                mFile = new File(mPath);
            }

            @Override
            protected Void doInBackground(Void... params) {
                if (!mFile.exists()) {
                    try {
                        float density = getContext().getResources().getDisplayMetrics().density;
                        int size = (int) (16 * density);
                        if (size < 16)
                            size = 16;
                        if (size > 64)
                            size = 64;

                        String iconUrl;
                        if (mFile.getName().equals("default"))
                            iconUrl = QMS_ICON_URL + mFile.getName() + QMS_ICON_APPENDIX;
                        else
                            iconUrl = QMS_ICON_URL + mFile.getName() + QMS_ICON_CONTENT;

                        iconUrl = iconUrl.replace("{w}", size + "").replace("{h}", size + "");
                        HttpURLConnection connection = NetworkUtil.getHttpConnection("GET", iconUrl, null, null);
                        if (connection != null) {
                            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                InputStream input = connection.getInputStream();
                                Bitmap icon = BitmapFactory.decodeStream(input);
                                FileOutputStream fos = new FileOutputStream(mFile);
                                icon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                fos.close();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if (mFile.exists())
                    while (mAssignedViews.size() > 0)
                        setIcon(mAssignedViews.poll(), mPath);

                mIconsQueue.remove(mFile.getName());
            }

            void addViewWithIcon(TextView textView) {
                mAssignedViews.add(textView);
            }
        }

        private void setIcon(TextView view, String path) {
            Drawable icon = Drawable.createFromPath(path);
            Drawable checkbox = view.getCompoundDrawables()[2];

            if (icon != null)
                icon.setBounds(0, 0, checkbox.getIntrinsicWidth(), checkbox.getIntrinsicHeight());

            view.setCompoundDrawables(icon, null, checkbox, null);
        }
    }
}

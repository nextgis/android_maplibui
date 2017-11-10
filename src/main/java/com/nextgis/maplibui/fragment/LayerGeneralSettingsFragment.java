/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016-2017 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.appyvet.materialrangebar.RangeBar;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.LayerSettingsActivity;
import com.nextgis.maplibui.util.ControlHelper;

public class LayerGeneralSettingsFragment extends Fragment {
    protected EditText mEditText;
    protected RangeBar mRangeBar;
    protected ILayer mLayer;
    protected LayerSettingsActivity mActivity;

    public LayerGeneralSettingsFragment() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getView() != null)
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    public Fragment setRoot(ILayer layer, LayerSettingsActivity activity) {
        mLayer = layer;
        mActivity = activity;
        return this;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mActivity.mLayerName = mEditText.getEditableText().toString();
        mActivity.mLayerMinZoom = mRangeBar.getLeftIndex();
        mActivity.mLayerMaxZoom = mRangeBar.getRightIndex();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_layer_general, container, false);
        if (mLayer == null)
            return v;

        TextView path = v.findViewById(R.id.layer_local_lath);
        path.setText(String.format(getString(R.string.layer_local_path), mLayer.getPath()));

        TextView remote = v.findViewById(R.id.layer_remote_path);
        String remoteUrl = null;
        if (mLayer instanceof NGWVectorLayer)
            remoteUrl = ((NGWVectorLayer) mLayer).getRemoteUrl();
        if (mLayer instanceof RemoteTMSLayer)
            remoteUrl = ((RemoteTMSLayer) mLayer).getURL();

        if (remoteUrl != null) {
            remote.setText(String.format(getString(R.string.layer_remote_path), remoteUrl));
            remote.setVisibility(View.VISIBLE);
        }

        mEditText = v.findViewById(R.id.layer_name);
        mEditText.setText(mLayer.getName());
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mActivity.setTitle(s.toString());
                mActivity.mLayerName = s.toString();
            }
        });

        // Gets the index value TextViews
        final TextView leftIndexValue = v.findViewById(R.id.leftIndexValue);
        final TextView rightIndexValue = v.findViewById(R.id.rightIndexValue);

        // Gets the RangeBar and set range
        mRangeBar = v.findViewById(R.id.rangebar);
        int nMinZoom = mActivity.mLayerMinZoom < mRangeBar.getRightIndex() ? (int) mActivity.mLayerMinZoom : mRangeBar.getRightIndex();
        int nMaxZoom = mActivity.mLayerMaxZoom < mRangeBar.getRightIndex() ? (int) mActivity.mLayerMaxZoom : mRangeBar.getRightIndex();
        final int maxZoom = GeoConstants.DEFAULT_MAX_ZOOM;
        nMinZoom = nMinZoom < 0 ? 0 : nMinZoom;
        nMaxZoom = nMaxZoom > maxZoom ? maxZoom : nMaxZoom;

        mRangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
                if (leftPinIndex < 0 || rightPinIndex > maxZoom) {
                    rangeBar.setRangePinsByIndices(leftPinIndex < 0 ? 0 : leftPinIndex, rightPinIndex > maxZoom ? maxZoom : rightPinIndex);
                    return;
                }

                mActivity.mLayerMinZoom = leftPinIndex;
                mActivity.mLayerMaxZoom = rightPinIndex;
                ControlHelper.setZoomText(getActivity(), leftIndexValue, R.string.min, leftPinIndex);
                ControlHelper.setZoomText(getActivity(), rightIndexValue, R.string.max, rightPinIndex);
            }
        });
        mRangeBar.setRangePinsByIndices(nMinZoom, nMaxZoom);

        if (mLayer instanceof VectorLayer) {
            final VectorLayer vectorLayer = (VectorLayer) mLayer;
            Button deleteFeatures = v.findViewById(R.id.delete_features);
            deleteFeatures.setVisibility(View.VISIBLE);
            deleteFeatures.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog builder = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.are_you_sure)
                            .setMessage(R.string.delete_features)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new DeleteFeaturesTask().execute(vectorLayer);
                                }
                            }).create();
                    builder.show();
                }
            });
        }

        return v;
    }
    public class DeleteFeaturesTask extends AsyncTask<VectorLayer, Integer, Void> implements IProgressor {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMessage(getString(R.string.waiting));
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(VectorLayer... layer) {
            layer[0].deleteAllFeatures(this);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (mProgressDialog != null) {
                mProgressDialog.setProgress(values[0]);
                if (values[1] > 0)
                    mProgressDialog.setMax(values[1]);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mActivity.onFeaturesCountChanged();
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }

        @Override
        public void setMax(int maxValue) {
            publishProgress(0, maxValue);
        }

        @Override
        public boolean isCanceled() {
            return mProgressDialog == null || !mProgressDialog.isShowing();
        }

        @Override
        public void setValue(int value) {
            publishProgress(value, 0);
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {

        }

        @Override
        public void setMessage(String message) {

        }
    }
}

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

package com.nextgis.maplibui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.edmodo.rangebar.RangeBar;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.LayerSettingsActivity;

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

        TextView path = (TextView) v.findViewById(R.id.layer_local_lath);
        path.setText(String.format(getString(R.string.layer_local_path), mLayer.getPath()));

        TextView remote = (TextView) v.findViewById(R.id.layer_remote_path);
        String remoteUrl = null;
        if (mLayer instanceof NGWVectorLayer)
            remoteUrl = ((NGWVectorLayer) mLayer).getRemoteUrl();
        if (mLayer instanceof RemoteTMSLayer)
            remoteUrl = ((RemoteTMSLayer) mLayer).getURL();

        if (remoteUrl != null) {
            remote.setText(String.format(getString(R.string.layer_remote_path), remoteUrl));
            remote.setVisibility(View.VISIBLE);
        }

        mEditText = (EditText) v.findViewById(R.id.layer_name);
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
                mActivity.mLayerName = s.toString();
            }
        });

        //set range
        // Gets the RangeBar
        mRangeBar = (RangeBar) v.findViewById(R.id.rangebar);
        int nMinZoom = mActivity.mLayerMinZoom < mRangeBar.getRightIndex() ? (int) mActivity.mLayerMinZoom : mRangeBar.getRightIndex();
        int nMaxZoom = mActivity.mLayerMaxZoom < mRangeBar.getRightIndex() ? (int) mActivity.mLayerMaxZoom : mRangeBar.getRightIndex();
        mRangeBar.setThumbIndices(nMinZoom, nMaxZoom);
        // Gets the index value TextViews
        final TextView leftIndexValue = (TextView) v.findViewById(R.id.leftIndexValue);
        leftIndexValue.setText(String.format(getString(R.string.min), nMinZoom));
        final TextView rightIndexValue = (TextView) v.findViewById(R.id.rightIndexValue);
        rightIndexValue.setText(String.format(getString(R.string.max), nMaxZoom));

        // Sets the display values of the indices
        mRangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int leftThumbIndex, int rightThumbIndex) {
                mActivity.mLayerMinZoom = leftThumbIndex;
                mActivity.mLayerMaxZoom = rightThumbIndex;
                leftIndexValue.setText(String.format(getString(R.string.min), leftThumbIndex));
                rightIndexValue.setText(String.format(getString(R.string.max), rightThumbIndex));
            }
        });

        return v;
    }
}

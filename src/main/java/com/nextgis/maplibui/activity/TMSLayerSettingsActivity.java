/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.display.TMSRenderer;
import com.nextgis.maplib.map.TMSLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.fragment.LayerGeneralSettingsFragment;
import com.nextgis.maplibui.util.ClearCacheTask;
import com.nextgis.maplibui.util.ControlHelper;

/**
 * TMS layer settings activity. Include common settings (layer name) and renderer settings.
 */
public class TMSLayerSettingsActivity
        extends LayerSettingsActivity {
    protected TMSLayer mRasterLayer;
    protected StyleFragment mStyleFragment;
    protected static boolean mClearCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mLayer == null)
            return;

        if (mLayer.getType() == Constants.LAYERTYPE_REMOTE_TMS ||
                mLayer.getType() == Constants.LAYERTYPE_LOCAL_TMS ||
                mLayer.getType() == Constants.LAYERTYPE_NGW_RASTER ||
                mLayer.getType() == Constants.LAYERTYPE_NGW_WEBMAP) {
            mRasterLayer = (TMSLayer) mLayer;
            mLayerMinZoom = mRasterLayer.getMinZoom();
            mLayerMaxZoom = mRasterLayer.getMaxZoom();
        }

        mClearCache = false;
    }

    @Override
    protected void saveSettings() {
        if (null == mRasterLayer)
            return;

        boolean changes = mStyleFragment.saveSettings();
        mRasterLayer.setName(mLayerName);
        changes = changes || mLayerMaxZoom != mRasterLayer.getMaxZoom() || mLayerMinZoom != mRasterLayer.getMinZoom();
        mRasterLayer.setMinZoom(mLayerMinZoom);
        mRasterLayer.setMaxZoom(mLayerMaxZoom);
        mRasterLayer.save();
        if (changes || mClearCache)
            mMap.setDirty(true);
    }

    @Override
    void addFragments() {
        mStyleFragment = new StyleFragment();
        mStyleFragment.setLayer(mLayer);
        mAdapter.addFragment(mStyleFragment, R.string.style);
        LayerGeneralSettingsFragment generalSettingsFragment = new LayerGeneralSettingsFragment();
        generalSettingsFragment.setRoot(mLayer, this);
        mAdapter.addFragment(generalSettingsFragment, R.string.general);
        CacheFragment cacheFragment = new CacheFragment();
        cacheFragment.setLayer(mLayer);
        mAdapter.addFragment(cacheFragment, R.string.cache);
    }

    public static class StyleFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {
        private TextView mAlphaLabel, mBrightnessLabel, mContrastLabel;
        private float mContrast, mBrightness;
        private int mAlpha;
        private boolean mForceToGrayScale;
        private TMSLayer mRasterLayer;

        public StyleFragment() {

        }

        boolean saveSettings() {
            TMSRenderer tmsRenderer = (TMSRenderer) mRasterLayer.getRenderer();
            boolean changes = false;
            if (null != tmsRenderer) {
                changes = tmsRenderer.getAlpha() != mAlpha || tmsRenderer.getBrightness() != mBrightness
                        || tmsRenderer.getContrast() != mContrast || tmsRenderer.isForceToGrayScale() != mForceToGrayScale;
                tmsRenderer.setContrastBrightness(mContrast, mBrightness, mForceToGrayScale);
                tmsRenderer.setAlpha(mAlpha);
            }

            return changes;
        }

        void setLayer(ILayer layer) {
            if (layer instanceof TMSLayer) {
                mRasterLayer = (TMSLayer) layer;
                TMSRenderer tmsRenderer = (TMSRenderer) mRasterLayer.getRenderer();
                if (null != tmsRenderer) {
                    mForceToGrayScale = tmsRenderer.isForceToGrayScale();
                    mContrast = tmsRenderer.getContrast();
                    mBrightness = tmsRenderer.getBrightness();
                    mAlpha = tmsRenderer.getAlpha();
                }
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_raster_layer_style, container, false);

            SwitchCompat switchCompat = (SwitchCompat) v.findViewById(R.id.make_grayscale);
            switchCompat.setChecked(mForceToGrayScale);
            switchCompat.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            mForceToGrayScale = isChecked;
                        }
                    });

            mContrastLabel = (TextView) v.findViewById(R.id.contrast_seek);
            SeekBar contrastPicker = (SeekBar) v.findViewById(R.id.contrastSeekBar);
            contrastPicker.setOnSeekBarChangeListener(this);
            contrastPicker.setProgress((int) mContrast * 10);

            mBrightnessLabel = (TextView) v.findViewById(R.id.brightness_seek);
            SeekBar brightnessPicker = (SeekBar) v.findViewById(R.id.brightnessSeekBar);
            brightnessPicker.setOnSeekBarChangeListener(this);
            brightnessPicker.setProgress((int) mBrightness + 255);

            mAlphaLabel = (TextView) v.findViewById(R.id.alpha_seek);
            SeekBar alphaPicker = (SeekBar) v.findViewById(R.id.alphaSeekBar);
            alphaPicker.setOnSeekBarChangeListener(this);
            alphaPicker.setProgress(mAlpha);

            return v;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }


        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int id = seekBar.getId();
            if (id == R.id.alphaSeekBar) {
                if (fromUser)
                    mAlpha = progress;

                mAlphaLabel.setText(ControlHelper.getPercentValue(getContext(), R.string.alpha, mAlpha));
            } else if (id == R.id.brightnessSeekBar) {
                if (fromUser)
                    mBrightness = progress - 255;

                mBrightnessLabel.setText((ControlHelper.getPercentValue(getContext(), R.string.brightness, mBrightness)));
            } else if (id == R.id.contrastSeekBar) {
                if (fromUser)
                    mContrast = (float) progress / 10;

                mContrastLabel.setText(String.format(getString(R.string.contrast), mContrast));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    public static class CacheFragment extends Fragment {
        protected TMSLayer mRasterLayer;

        public CacheFragment() {

        }

        void setLayer(ILayer layer) {
            if (layer instanceof TMSLayer)
                mRasterLayer = (TMSLayer) layer;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_raster_layer_cache, container, false);
            if (mRasterLayer == null)
                return v;

            Spinner cacheSizeMulti = (Spinner) v.findViewById(R.id.spinner);
            cacheSizeMulti.setSelection(mRasterLayer.getCacheSizeMultiply());
            cacheSizeMulti.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            mRasterLayer.setCacheSizeMultiply(position);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

            Button clearCache = (Button) v.findViewById(R.id.clear_cache);
            clearCache.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DialogInterface.OnDismissListener listener = new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            mClearCache = true;
                        }
                    };

                    new ClearCacheTask(getActivity(), listener).execute(mRasterLayer.getPath());
                }
            });

            return v;
        }
    }
}

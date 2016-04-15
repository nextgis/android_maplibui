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

package com.nextgis.maplibui.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.nextgis.maplib.display.TMSRenderer;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplib.map.TMSLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.fragment.LayerGeneralSettingsFragment;
import com.nextgis.maplibui.util.ControlHelper;


/**
 * TMS layer settings activity. Include common settings (layer name) and renderer settings.
 */
public class TMSLayerSettingsActivity
        extends LayerSettingsActivity {
    protected static TMSLayer mRasterLayer;

    protected static int mCacheSizeMulti;
    protected static float mContrast, mBrightness;
    protected static int mAlpha;
    protected static boolean mForceToGrayScale;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mLayer == null)
            return;

        if (mLayer.getType() == Constants.LAYERTYPE_REMOTE_TMS ||
                mLayer.getType() == Constants.LAYERTYPE_LOCAL_TMS ||
                mLayer.getType() == Constants.LAYERTYPE_NGW_RASTER) {
            mRasterLayer = (TMSLayer) mLayer;
            mCacheSizeMulti = mRasterLayer.getCacheSizeMultiply();
            mLayerMinZoom = mRasterLayer.getMinZoom();
            mLayerMaxZoom = mRasterLayer.getMaxZoom();

            // set color
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
    protected void saveSettings() {
        if (null == mRasterLayer)
            return;

        mRasterLayer.setCacheSizeMultiply(mCacheSizeMulti);

        TMSRenderer tmsRenderer = (TMSRenderer) mRasterLayer.getRenderer();
        if (null != tmsRenderer) {
            tmsRenderer.setContrastBrightness(mContrast, mBrightness, mForceToGrayScale);
            tmsRenderer.setAlpha(mAlpha);
        }

        mRasterLayer.setMinZoom(mLayerMinZoom);
        mRasterLayer.setMaxZoom(mLayerMaxZoom);

        mRasterLayer.save();
    }

    @Override
    void addFragments() {
        mAdapter.addFragment(new StyleFragment(), R.string.style);
        mAdapter.addFragment(new LayerGeneralSettingsFragment(), R.string.general);
        mAdapter.addFragment(new CacheFragment(), R.string.cache);
    }

    public static class StyleFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {
        private TextView mAlphaLabel, mBrightnessLabel, mContrastLabel;

        public StyleFragment() {

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
        public CacheFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_raster_layer_cache, container, false);

            Spinner cacheSizeMulti = (Spinner) v.findViewById(R.id.spinner);
            cacheSizeMulti.setSelection(mCacheSizeMulti);
            cacheSizeMulti.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            mCacheSizeMulti = position;
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

            return v;
        }
    }
}

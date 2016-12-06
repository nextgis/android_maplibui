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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import com.edmodo.rangebar.RangeBar;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.service.TileDownloadService;
import com.nextgis.maplibui.util.ConstantsUI;

/**
 * Dialog to select which zoom levels to download
 */
public class SelectZoomLevelsDialog
        extends DialogFragment {

    protected GeoEnvelope mEnvelope;
    protected int mLayerId;

    public GeoEnvelope getEnvelope() {
        return mEnvelope;
    }

    public SelectZoomLevelsDialog setEnvelope(GeoEnvelope envelope) {
        mEnvelope = envelope;
        return this;
    }

    public int getLayerId() {
        return mLayerId;
    }

    public SelectZoomLevelsDialog setLayerId(int layerId) {
        mLayerId = layerId;
        return this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (null != savedInstanceState) {
            mLayerId = savedInstanceState.getInt(ConstantsUI.KEY_LAYER_ID);
            double dfMinX = savedInstanceState.getDouble(TileDownloadService.KEY_MINX);
            double dfMinY = savedInstanceState.getDouble(TileDownloadService.KEY_MINY);
            double dfMaxX = savedInstanceState.getDouble(TileDownloadService.KEY_MAXX);
            double dfMaxY = savedInstanceState.getDouble(TileDownloadService.KEY_MAXY);
            mEnvelope = new GeoEnvelope(dfMinX, dfMaxX, dfMinY, dfMaxY);
        }

        final Context context = getActivity();
        View view = View.inflate(context, R.layout.dialog_select_zoom_levels, null);
        IGISApplication app = (IGISApplication) getActivity().getApplication();
        final MapDrawable map = (MapDrawable) app.getMap();
        int left = (int) map.getZoomLevel() - 1;
        int right = (int) map.getZoomLevel() + 1;

        // Gets the RangeBar
        final RangeBar rangebar = (RangeBar) view.findViewById(R.id.rangebar);
        rangebar.setThumbIndices(left, right);
        // Gets the index value TextViews
        final TextView leftIndexValue = (TextView) view.findViewById(R.id.leftIndexValue);
        final TextView rightIndexValue = (TextView) view.findViewById(R.id.rightIndexValue);
        leftIndexValue.setText(String.format(getString(R.string.min), left));
        rightIndexValue.setText(String.format(getString(R.string.max), right));

        // Sets the display values of the indices
        rangebar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int leftThumbIndex, int rightThumbIndex) {
                leftIndexValue.setText(String.format(getString(R.string.min), leftThumbIndex));
                rightIndexValue.setText(String.format(getString(R.string.max), rightThumbIndex));
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(String.format(getString(R.string.current_zoom), map.getZoomLevel())).setView(view).setPositiveButton(
                R.string.start, new DialogInterface.OnClickListener() {
                    public void onClick(
                            DialogInterface dialog,
                            int id) {
                        final int zoomFrom = rangebar.getLeftIndex();
                        final int zoomTo = rangebar.getRightIndex();
                        final int layerId = getLayerId();
                        final GeoEnvelope env = getEnvelope();

                        //start download service

                        ILayer layer = map.getLayerById(layerId);
                        if (null != layer) {
                            Intent intent = new Intent(getActivity(), TileDownloadService.class);
                            intent.setAction(TileDownloadService.ACTION_ADD_TASK);
                            intent.putExtra(ConstantsUI.KEY_LAYER_ID, layerId);
                            intent.putExtra(TileDownloadService.KEY_PATH, layer.getPath().getName());
                            intent.putExtra(TileDownloadService.KEY_ZOOM_FROM, zoomFrom);
                            intent.putExtra(TileDownloadService.KEY_ZOOM_TO, zoomTo);
                            intent.putExtra(TileDownloadService.KEY_MINX, env.getMinX());
                            intent.putExtra(TileDownloadService.KEY_MAXX, env.getMaxX());
                            intent.putExtra(TileDownloadService.KEY_MINY, env.getMinY());
                            intent.putExtra(TileDownloadService.KEY_MAXY, env.getMaxY());

                            getActivity().startService(intent);
                        }

                    }
                }).setNegativeButton(
                R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(
                            DialogInterface dialog,
                            int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(ConstantsUI.KEY_LAYER_ID, mLayerId);
        outState.putDouble(TileDownloadService.KEY_MINX, mEnvelope.getMinX());
        outState.putDouble(TileDownloadService.KEY_MAXX, mEnvelope.getMaxX());
        outState.putDouble(TileDownloadService.KEY_MINY, mEnvelope.getMinY());
        outState.putDouble(TileDownloadService.KEY_MAXY, mEnvelope.getMaxY());
        super.onSaveInstanceState(outState);
    }
}

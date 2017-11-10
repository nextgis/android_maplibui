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

package com.nextgis.maplibui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import com.appyvet.materialrangebar.RangeBar;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.TMSLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.service.TileDownloadService;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;

import java.util.Locale;

/**
 * Dialog to select which zoom levels to download
 */
public class SelectZoomLevelsDialog
        extends DialogFragment {
    final static String TILDA = "~";

    private TextView mTilesCount;
    private GeoEnvelope mEnvelope;
    private int mLayerId;
    private CountTilesTask mCountTask;

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
        final int maxZoom = GeoConstants.DEFAULT_MAX_ZOOM;
        left = left < 0 ? 0 : left;
        right = right > maxZoom ? maxZoom : right;

        // Get the index value TextViews
        mTilesCount = view.findViewById(R.id.tilesCount);
        final TextView leftIndexValue = view.findViewById(R.id.leftIndexValue);
        final TextView rightIndexValue = view.findViewById(R.id.rightIndexValue);

        // Get the RangeBar and set the display values of the indices
        final RangeBar rangebar = view.findViewById(R.id.rangebar);
        rangebar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
                if (leftPinIndex < 0 || rightPinIndex > maxZoom) {
                    rangeBar.setRangePinsByIndices(leftPinIndex < 0 ? 0 : leftPinIndex, rightPinIndex > maxZoom ? maxZoom : rightPinIndex);
                    return;
                }

                ControlHelper.setZoomText(getActivity(), leftIndexValue, R.string.min, leftPinIndex);
                ControlHelper.setZoomText(getActivity(), rightIndexValue, R.string.max, rightPinIndex);

                if (mCountTask != null)
                    mCountTask.cancel(true);

                mTilesCount.setText(getString(R.string.counting).toLowerCase());
                mCountTask = new CountTilesTask(map, leftPinIndex, rightPinIndex);
                mCountTask.execute();
            }
        });
        rangebar.setRangePinsByIndices(left, right);

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

    private long getTilesCount(AsyncTask task, MapBase map, int leftThumbIndex, int rightThumbIndex) {
        TMSLayer layer = (TMSLayer) map.getLayerById(getLayerId());
        GeoEnvelope envelope = getEnvelope();
        long total = 0;
        for (int zoom = leftThumbIndex; zoom <= rightThumbIndex; zoom++) {
            if (task != null && task.isCancelled())
                return total;

            total += MapUtil.getTileCount(task, envelope, zoom, layer.getTMSType());
        }

        return total;
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

    private class CountTilesTask extends AsyncTask<Void, Void, String> {
        private int mFrom, mTo;
        private MapBase mMap;

        CountTilesTask(MapBase map, int from, int to) {
            mMap = map;
            mFrom = from;
            mTo = to;
        }

        @Override
        protected String doInBackground(Void... params) {
            long total = getTilesCount(this, mMap, mFrom, mTo);
            String value = total + "";
            if (total >= 1000000000)
                value = String.format(Locale.getDefault(), "%s%.1f%s", TILDA, total / 1000000000f, getString(R.string.unit_billion));
            else if (total >= 1000000)
                value = String.format(Locale.getDefault(), "%s%.1f%s", TILDA, total / 1000000f, getString(R.string.unit_million));
            else if (total >= 100000)
                value = String.format(Locale.getDefault(), "%s%.1f%s", TILDA, total / 1000f, getString(R.string.unit_thousand));

            return value;
        }

        @Override
        protected void onPostExecute(String res) {
            mTilesCount.setText(String.format(getString(R.string.tiles_count), res));
        }
    }
}

package com.nextgis.maplibui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import com.edmodo.rangebar.RangeBar;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplibui.service.TileDownloadService;
import com.nextgis.maplibui.R;

/**
 * Dialog to select which zoom levels to download
 */
public class SelectZoomLevelsDialog
        extends DialogFragment {


    protected GeoEnvelope mEnvelope;
    protected short       mLayerId;

    protected static final String KEY_LAYER_ID = "layer_id";
    protected static final String KEY_MINX = "env_minx";
    protected static final String KEY_MAXX = "env_maxx";
    protected static final String KEY_MINY = "env_miny";
    protected static final String KEY_MAXY = "env_maxy";

    public GeoEnvelope getEnvelope() {
        return mEnvelope;
    }

    public SelectZoomLevelsDialog setEnvelope(GeoEnvelope envelope) {
        mEnvelope = envelope;
        return this;
    }

    public short getLayerId() {
        return mLayerId;
    }

    public SelectZoomLevelsDialog setLayerId(short layerId) {
        mLayerId = layerId;
        return this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (null != savedInstanceState) {
            mLayerId = savedInstanceState.getShort(KEY_LAYER_ID);
            double dfMinX = savedInstanceState.getDouble(KEY_MINX);
            double dfMinY = savedInstanceState.getDouble(KEY_MINY);
            double dfMaxX = savedInstanceState.getDouble(KEY_MAXX);
            double dfMaxY = savedInstanceState.getDouble(KEY_MAXY);
            mEnvelope = new GeoEnvelope(dfMinX, dfMaxX, dfMinY, dfMaxY);
        }

        final Context context = getActivity();
        View view = View.inflate(context, R.layout.dialog_select_zoom_levels, null);

        // Gets the RangeBar
        final RangeBar rangebar = (RangeBar) view.findViewById(R.id.rangebar);
        rangebar.setThumbIndices(8, 15);
        // Gets the index value TextViews
        final TextView leftIndexValue = (TextView) view.findViewById(R.id.leftIndexValue);
        leftIndexValue.setText("min: " + 8);
        final TextView rightIndexValue = (TextView) view.findViewById(R.id.rightIndexValue);
        rightIndexValue.setText("max: " + 15);

        // Sets the display values of the indices
        rangebar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int leftThumbIndex, int rightThumbIndex) {

                leftIndexValue.setText("min: " + leftThumbIndex);
                rightIndexValue.setText("max: " + rightThumbIndex);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.select_zoom_levels_to_download_title).setView(view).setPositiveButton(
                R.string.start, new DialogInterface.OnClickListener()
                {
                    public void onClick(
                            DialogInterface dialog,
                            int id)
                    {
                        final int zoomFrom = rangebar.getLeftIndex();
                        final int zoomTo = rangebar.getRightIndex();
                        final short layerId = getLayerId();
                        final GeoEnvelope env = getEnvelope();

                        //start download service
                        ServiceConnection connection = new ServiceConnection() {

                            @Override
                            public void onServiceConnected(ComponentName className, IBinder service) {

                                TileDownloadService boundService = ((TileDownloadService.LocalBinder)service).getService();
                                boundService.addTask(layerId, env, zoomFrom, zoomTo);

                                getActivity().unbindService(this);
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName name) {

                            }
                        };

                        getActivity().bindService(new Intent(getActivity(),
                                TileDownloadService.class), connection, Context.BIND_AUTO_CREATE);

                    }
                }).setNegativeButton(
                R.string.cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick(
                            DialogInterface dialog,
                            int id)
                    {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putShort(KEY_LAYER_ID, mLayerId);
        outState.putDouble(KEY_MINX, mEnvelope.getMinX());
        outState.putDouble(KEY_MAXX, mEnvelope.getMaxX());
        outState.putDouble(KEY_MINY, mEnvelope.getMinY());
        outState.putDouble(KEY_MAXY, mEnvelope.getMaxY());
        super.onSaveInstanceState(outState);
    }
}

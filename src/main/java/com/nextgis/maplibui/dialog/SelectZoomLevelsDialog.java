package com.nextgis.maplibui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.edmodo.rangebar.RangeBar;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplibui.R;

/**
 * Dialog to select which zoom levels to download
 */
public class SelectZoomLevelsDialog
        extends DialogFragment {

    protected GeoEnvelope mEnvelope;
    protected short       mId;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
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
}

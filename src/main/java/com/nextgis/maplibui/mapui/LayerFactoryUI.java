/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplibui.mapui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.LayerFactory;

import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.IOException;

public class LayerFactoryUI extends LayerFactory{

    public static Layer createNewRemoteTMSLayer(){
        final LinearLayout linearLayout = new LinearLayout(map.getContext());
        final EditText input = new EditText(map.getContext());
        input.setText(layerName);

        final EditText url = new EditText(map.getContext());
        url.setText(layerUrl);

        final TextView stLayerName = new TextView(map.getContext());
        stLayerName.setText(map.getContext().getString(R.string.layer_name) + ":");

        final TextView stLayerUrl = new TextView(map.getContext());
        stLayerUrl.setText(map.getContext().getString(R.string.layer_url) + ":");

        final TextView stLayerType = new TextView(map.getContext());
        stLayerType.setText(map.getContext().getString(R.string.layer_type) + ":");

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                map.getContext(), android.R.layout.simple_spinner_item);
        final Spinner spinner = new Spinner(map.getContext());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        adapter.add(map.getContext().getString(R.string.tmstype_osm));
        adapter.add(map.getContext().getString(R.string.tmstype_normal));
        adapter.add(map.getContext().getString(R.string.tmstype_ngw));

        if (type == TMSTYPE_OSM) {
            spinner.setSelection(0);
        } else {
            spinner.setSelection(1);
        }

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(stLayerName);
        linearLayout.addView(input);
        linearLayout.addView(stLayerUrl);
        linearLayout.addView(url);
        linearLayout.addView(stLayerType);
        linearLayout.addView(spinner);

        new AlertDialog.Builder(map.getContext())
                .setTitle(bCreate
                        ? R.string.input_layer_properties
                        : R.string.change_layer_properties)
//                .setMessage(message)
                .setView(linearLayout)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int tmsType = 0;
                        switch (spinner.getSelectedItemPosition()) {
                            case 0:
                            case 1:
                                tmsType = TMSTYPE_OSM;
                                break;
                            case 2:
                            case 3:
                                tmsType = TMSTYPE_NORMAL;
                                break;
                        }

                        if (bCreate) {
                            String sErr = map.getContext().getString(R.string.error_occurred);

                            try {
                                create(map, input.getText().toString(),
                                        url.getText().toString(), tmsType);
                                return;

                            } catch (FileNotFoundException e) {
                                Log.d(TAG, "Exception: " + e.getLocalizedMessage());
                                sErr += ": " + e.getLocalizedMessage();
                            } catch (JSONException e) {
                                Log.d(TAG, "Exception: " + e.getLocalizedMessage());
                                sErr += ": " + e.getLocalizedMessage();
                            } catch (IOException e) {
                                Log.d(TAG, "Exception: " + e.getLocalizedMessage());
                                sErr += ": " + e.getLocalizedMessage();
                            }

                            //if we here something wrong occurred
                            Toast.makeText(map.getContext(), sErr, Toast.LENGTH_SHORT).show();

                        } else {
                            layer.setName(input.getText().toString());
                            layer.setTMSType(tmsType);
                            map.onLayerChanged(layer);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                        Toast.makeText(map.getContext(), R.string.error_cancel_by_user,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}

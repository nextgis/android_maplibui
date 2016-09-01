/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.util;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplibui.R;

import org.json.JSONException;
import org.json.JSONObject;

public class NGWCreateNewLayerTask extends AsyncTask<Void, Void, String> {
    private Connection mConnection;
    private VectorLayer mLayer;

    public NGWCreateNewLayerTask(Connection connection, VectorLayer layer) {
        mConnection = connection;
        mLayer = layer;
    }

    @Override
    protected String doInBackground(Void... voids) {
        if (mConnection.connect())
            return NGWUtil.createNewLayer(mConnection, mLayer);

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        Context context = mLayer.getContext();
        String message = context.getString(R.string.error_layer_create);
        if (result != null) {
            try {
                JSONObject obj = new JSONObject(result);
                message = obj.optString(Constants.JSON_MESSAGE_KEY, message);
                Long id = obj.getLong(Constants.JSON_ID_KEY);
                mLayer.toNGW(id, mConnection.getName());
                message = context.getString(R.string.message_layer_created);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}

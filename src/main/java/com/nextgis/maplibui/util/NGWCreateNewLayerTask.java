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
import android.util.Pair;
import android.widget.Toast;

import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class NGWCreateNewLayerTask extends AsyncTask<Void, Void, String> {
    private Connection mConnection;
    private VectorLayer mLayer;
    private Context mContext;
    private Pair<Integer, Integer> mVer;

    public NGWCreateNewLayerTask(Connection connection, VectorLayer layer) {
        mConnection = connection;
        mLayer = layer;
        mContext = mLayer.getContext();
    }

    @Override
    protected String doInBackground(Void... voids) {
        if (mConnection.connect()) {
            mVer = null;
            try {
                AccountUtil.AccountData accountData = AccountUtil.getAccountData(mContext, mConnection.getName());
                if (null == accountData.url)
                    return "404";

                mVer = NGWUtil.getNgwVersion(accountData.url, accountData.login, accountData.password);
            } catch (IllegalStateException e) {
                return "401";
            } catch (JSONException | IOException | NumberFormatException e) {
                e.printStackTrace();
            }

            return NGWUtil.createNewLayer(mConnection, mLayer);
        }

        return "0";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        String message;
        if (!MapUtil.isParsable(result)) {
            try {
                JSONObject obj = new JSONObject(result);
                Long id = obj.getLong(Constants.JSON_ID_KEY);
                mLayer.toNGW(id, mConnection.getName(), mVer);
                result = "-999";
            } catch (JSONException e) {
                result = "500";
                e.printStackTrace();
            }
        }

        switch (result) {
            case "-999":
                message = mContext.getString(R.string.message_layer_created);
                break;
            default:
                message = NetworkUtil.getError(mContext, result);
                break;
        }

        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }
}

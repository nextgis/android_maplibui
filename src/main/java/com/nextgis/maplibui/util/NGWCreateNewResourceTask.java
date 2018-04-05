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
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class NGWCreateNewResourceTask extends AsyncTask<Void, Void, HttpResponse> {
    private Connection mConnection;
    private VectorLayer mLayer;
    private Context mContext;
    private Pair<Integer, Integer> mVer;
    private long mParentId;
    private String mName;

    public NGWCreateNewResourceTask(Context context, Connection connection, long parentId) {
        mContext = context;
        mParentId = parentId;
        mConnection = connection;
    }

    public NGWCreateNewResourceTask setLayer(VectorLayer layer) {
        mLayer = layer;
        return this;
    }

    public NGWCreateNewResourceTask setName(String name) {
        mName = name;
        return this;
    }

    @Override
    protected HttpResponse doInBackground(Void... voids) {
        if (mConnection.connect(false)) {
            mVer = null;
            try {
                AccountUtil.AccountData accountData = AccountUtil.getAccountData(mContext, mConnection.getName());
                if (null == accountData.url)
                    return new HttpResponse(404);

                mVer = NGWUtil.getNgwVersion(accountData.url, accountData.login, accountData.password);
            } catch (IllegalStateException e) {
                return new HttpResponse(401);
            } catch (JSONException | IOException | NumberFormatException e) {
                e.printStackTrace();
            }

            if (mLayer != null)
                return NGWUtil.createNewLayer(mConnection, mLayer, mParentId, null);
            else
                return NGWUtil.createNewGroup(mContext, mConnection, mParentId, mName, null);
        }

        return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);
    }

    @Override
    protected void onPostExecute(HttpResponse result)
    {
        super.onPostExecute(result);

        String message;
        if (result.isOk()) {
            try {
                JSONObject obj = new JSONObject(result.getResponseBody());
                Long id = obj.getLong(Constants.JSON_ID_KEY);
                if (mLayer != null)
                    mLayer.toNGW(id, mConnection.getName(), Constants.SYNC_ALL, mVer);
                result.setResponseCode(-999);
            } catch (JSONException e) {
                result.setResponseCode(500);
                e.printStackTrace();
            }
        }

        switch (result.getResponseCode()) {
            case -999:
                message = mContext.getString(mLayer == null
                                             ? R.string.message_group_created
                                             : R.string.message_layer_created);
                break;
            default:
                message = NetworkUtil.getError(mContext, result.getResponseCode());
                break;
        }

        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }
}

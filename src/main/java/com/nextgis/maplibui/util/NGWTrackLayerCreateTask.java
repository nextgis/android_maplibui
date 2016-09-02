/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.util;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Resource;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWTrackLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplibui.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NGWTrackLayerCreateTask extends AsyncTask<Void, Void, Boolean> {
    private Connection mConnection;
    private Context mContext;

    public NGWTrackLayerCreateTask(Context context, Connection connection) {
        mConnection = connection;
        mContext = context;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        Map<String, Resource> keys = new HashMap<>();
        keys.put(mConnection.getLogin(), null);
        if (mConnection.connect(Constants.NGW_ACCOUNT_GUEST.equals(mConnection.getLogin()))) {
            NGWUtil.getResourceByKey(mConnection, keys);
            Resource resource = keys.get(mConnection.getLogin());
            if (resource != null) {
                MapBase map = MapBase.getInstance();
                NGWTrackLayer layer = new NGWTrackLayer(mContext, map.createLayerStorage());
                layer.setName(mContext.getString(R.string.tracks));
                layer.setRemoteId(resource.getRemoteId());
                layer.setAccountName(mConnection.getName());
                layer.setVisible(false);
                try {
                    layer.createFromNGW(null);
                    map.addLayer(layer);
                    map.save();
                    return true;
                } catch (NGException | IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        Toast.makeText(mContext, result ? R.string.sync_enabled : R.string.sync_enable_error, Toast.LENGTH_SHORT).show();
    }
}

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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

import com.nextgis.maplib.api.IProgressor;
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

public class NGWTrackLayerCreateTask extends AsyncTask<Void, Void, Boolean> implements IProgressor {
    private ProgressDialog mProgress;
    private Connection mConnection;
    private Activity mActivity;
    private boolean mIsCancelled;

    public NGWTrackLayerCreateTask(Activity activity, Connection connection) {
        mConnection = connection;
        mActivity = activity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgress = new ProgressDialog(mActivity);
        mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mIsCancelled = true;
            }
        });
        mProgress.setMessage(mActivity.getString(R.string.message_loading));
        ControlHelper.lockScreenOrientation(mActivity);
        mProgress.show();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        Map<String, Resource> keys = new HashMap<>();
        keys.put(mConnection.getLogin(), null);
        if (mConnection.connect(Constants.NGW_ACCOUNT_GUEST.equals(mConnection.getLogin()))) {
            NGWUtil.getResourceByKey(mActivity, mConnection, keys);

            if (mIsCancelled)
                return false;

            Resource resource = keys.get(mConnection.getLogin());
            if (resource != null) {
                MapBase map = MapBase.getInstance();
                NGWTrackLayer layer = new NGWTrackLayer(mActivity.getApplicationContext(), map.createLayerStorage());
                layer.setName(mActivity.getString(R.string.tracks));
                layer.setRemoteId(resource.getRemoteId());
                layer.setAccountName(mConnection.getName());
                layer.setVisible(false);
                try {
                    layer.createFromNGW(this);

                    if (mIsCancelled) {
                        layer.delete();
                        return false;
                    }

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
        mProgress.dismiss();
        ControlHelper.unlockScreenOrientation(mActivity);
        Toast.makeText(mActivity, mIsCancelled ? R.string.canceled : result ? R.string.sync_enabled : R.string.sync_enable_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setMax(int maxValue) {

    }

    @Override
    public boolean isCanceled() {
        return mIsCancelled;
    }

    @Override
    public void setValue(int value) {

    }

    @Override
    public void setIndeterminate(boolean indeterminate) {

    }

    @Override
    public void setMessage(final String message) {
        if (mProgress.isShowing())
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setMessage(message);
                }
            });
    }
}

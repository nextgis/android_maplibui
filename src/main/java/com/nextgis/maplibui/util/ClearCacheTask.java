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

import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplibui.R;

import java.io.File;

public class ClearCacheTask extends AsyncTask<File, Integer, Void> {
    private Activity mActivity;
    private ProgressDialog mProgressDialog;
    private DialogInterface.OnDismissListener mListener;

    public ClearCacheTask(Activity activity, DialogInterface.OnDismissListener listener) {
        mActivity = activity;
        mListener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (!mActivity.isFinishing()) {
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMessage(mActivity.getString(R.string.waiting));
            mProgressDialog.show();
        }
    }

    @Override
    protected Void doInBackground(File... path) {
        if (path.length > 0) {
            if (path[0].exists() && path[0].isDirectory()) {
                File[] data = path[0].listFiles();
                int c = 0;
                for (File file : data) {
                    publishProgress(++c, data.length);
                    if (file.isDirectory() && MapUtil.isParsable(file.getName()))
                        FileUtil.deleteRecursive(file);
                }
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (mProgressDialog != null) {
            mProgressDialog.setMax(values[1]);
            mProgressDialog.setProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            if (mListener != null)
                mListener.onDismiss(null);
        }
    }
}
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

package com.nextgis.maplibui.fragment;

import android.accounts.Account;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.service.LayerFillService;

// http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
public class LayerFillProgressDialogFragment extends Fragment {
    private static Activity mActivity;
    private static BroadcastReceiver mLayerFillReceiver;
    private static ProgressDialog mProgressDialog;
    private static boolean mIsShowing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();

        IntentFilter intentFilter = new IntentFilter(LayerFillService.ACTION_UPDATE);
        mActivity.registerReceiver(mLayerFillReceiver, intentFilter);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (mLayerFillReceiver != null)
            mActivity.unregisterReceiver(mLayerFillReceiver);

        mProgressDialog = null;
    }

    public static void startFill(Intent intent) {
        LayerFillProgressDialog dialog = new LayerFillProgressDialog();
        dialog.execute(intent);
    }

    private static void createProgressDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mProgressDialog = new ProgressDialog(mActivity, android.R.style.Theme_Material_Light_Dialog_Alert);
        else
            mProgressDialog = new ProgressDialog(mActivity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mProgressDialog.setProgressNumberFormat(null);

        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                mActivity.getString(R.string.menu_visibility_off), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mIsShowing = false;
                    }
                });
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                mActivity.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intentStop = new Intent(mActivity, LayerFillService.class);
                        intentStop.setAction(LayerFillService.ACTION_STOP);
                        mActivity.startService(intentStop);
                    }
                });
    }

    private static class LayerFillProgressDialog extends AsyncTask<Object, Intent, Boolean> {
        private boolean mIsFinished;

        @Override
        protected Boolean doInBackground(Object[] params) {
            if (params.length == 1 && params[0] instanceof Intent) {
                Intent intent = (Intent) params[0];
                mActivity.startService(intent);

                mLayerFillReceiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        publishProgress(intent);
                    }
                };

                IntentFilter intentFilter = new IntentFilter(LayerFillService.ACTION_UPDATE);
                mActivity.registerReceiver(mLayerFillReceiver, intentFilter);

                while (!mIsFinished)
                    SystemClock.sleep(500);

                return true;
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Intent... values) {
            super.onProgressUpdate(values);

            final Intent intent = values[0];
            short serviceStatus = intent.getShortExtra(LayerFillService.KEY_STATUS, (short) 0);
            String title = intent.getStringExtra(LayerFillService.KEY_TITLE);

            if (mProgressDialog == null) {
                createProgressDialog();
                setDialogInfo(title, title);

                if (mIsShowing)
                    mProgressDialog.show();
            }

            switch (serviceStatus) {
                case LayerFillService.STATUS_START:
                    mProgressDialog.setIndeterminate(true);
                    mProgressDialog.show();
                    mIsShowing = true;
                    break;
                case LayerFillService.STATUS_UPDATE:
                    final String message = intent.getStringExtra(LayerFillService.KEY_MESSAGE);
                    if (TextUtils.isEmpty(message))
                        return;

                    mProgressDialog.setIndeterminate(false);
                    setDialogInfo(title, message);
                    mProgressDialog.setMax(intent.getIntExtra(LayerFillService.KEY_TOTAL, 0));
                    mProgressDialog.setProgress(intent.getIntExtra(LayerFillService.KEY_PROGRESS, 0));
                    break;
                case LayerFillService.STATUS_STOP:
                    if (intent.getIntExtra(LayerFillService.KEY_TOTAL, 0) == 0) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                        mActivity.unregisterReceiver(mLayerFillReceiver);
                        mLayerFillReceiver = null;
                        mIsFinished = true;
                    }

                    boolean canceled = intent.getBooleanExtra(LayerFillService.KEY_CANCELLED, false);
                    String toast = mActivity.getString(com.nextgis.maplibui.R.string.message_layer_created);
                    boolean success = intent.getBooleanExtra(LayerFillService.KEY_RESULT, false);
                    if (!success) {
                        if (canceled)
                            toast = mActivity.getString(com.nextgis.maplibui.R.string.canceled);
                        else
                            toast = intent.getStringExtra(LayerFillService.KEY_MESSAGE);
                    }

                    if (intent.hasExtra(LayerFillService.KEY_MESSAGE))
                        Toast.makeText(mActivity, toast, Toast.LENGTH_LONG).show();

                    boolean isNgwSync = intent.getBooleanExtra(LayerFillService.KEY_SYNC, false);
                    if (success && !canceled && isNgwSync) {
                        int id = intent.getIntExtra(LayerFillService.KEY_REMOTE_ID, -1);
                        final IGISApplication app = (IGISApplication) mActivity.getApplication();
                        final NGWVectorLayer ngwLayer = (NGWVectorLayer) app.getMap().getLayerById(id);
                        final Account account = app.getAccount(ngwLayer.getAccountName());

                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                        builder.setTitle(R.string.sync_dialog_title).setMessage(R.string.sync_dialog_message)
                                .setPositiveButton(R.string.auto, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        NGWSettingsFragment.setAccountSyncEnabled(account, app.getAuthority(), true);
                                        ngwLayer.setSyncType(Constants.SYNC_ALL);
                                        ngwLayer.save();
                                    }
                                })
                                .setNeutralButton(R.string.skip, null)
                                .setNegativeButton(R.string.manual, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        ngwLayer.setSyncType(Constants.SYNC_ALL);
                                        ngwLayer.save();
                                    }
                                });

                        AlertDialog dialog = builder.show();
                        dialog.setCanceledOnTouchOutside(false);
                    }
                    break;
                case LayerFillService.STATUS_SHOW:
                    if (!mProgressDialog.isShowing()) {
                        createProgressDialog();
                        setDialogInfo(title, title);
                        mProgressDialog.show();
                        mIsShowing = true;
                    }
                    break;
            }
        }

        private void setDialogInfo(final String title, final String message) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.setTitle(title);
                    mProgressDialog.setMessage(message);
                }
            });
        }
    }
}

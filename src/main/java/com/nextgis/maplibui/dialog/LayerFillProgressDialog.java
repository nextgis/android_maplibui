package com.nextgis.maplibui.dialog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.widget.Toast;

import com.nextgis.maplibui.R;
import com.nextgis.maplibui.service.LayerFillService;

public class LayerFillProgressDialog extends AsyncTask<Object, Intent, Boolean> {
    private ProgressDialog mProgressDialog;
    private Activity mActivity;
    private BroadcastReceiver mLayerFillReceiver;
    private boolean mIsFinished;

    public LayerFillProgressDialog(Activity activity) {
        mActivity = activity;
        createProgressDialog();
    }

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
        switch (serviceStatus) {
            case LayerFillService.STATUS_START:
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.setTitle(intent.getStringExtra(LayerFillService.KEY_TITLE));
                        mProgressDialog.setMessage(intent.getStringExtra(LayerFillService.KEY_TITLE));
                    }
                });
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.show();
                break;
            case LayerFillService.STATUS_UPDATE:
                mProgressDialog.setIndeterminate(false);
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.setMessage(intent.getStringExtra(LayerFillService.KEY_MESSAGE));
                    }
                });
                mProgressDialog.setMax(intent.getIntExtra(LayerFillService.KEY_TOTAL, 0));
                mProgressDialog.setProgress(intent.getIntExtra(LayerFillService.KEY_PROGRESS, 0));
                break;
            case LayerFillService.STATUS_STOP:
                mProgressDialog.dismiss();
                int toast = intent.getBooleanExtra(LayerFillService.KEY_MESSAGE, false) ? R.string.canceled : R.string.message_layer_created;
                Toast.makeText(mActivity, mActivity.getString(toast), Toast.LENGTH_SHORT).show();
                mActivity.unregisterReceiver(mLayerFillReceiver);
                mIsFinished = true;
                break;
            case LayerFillService.STATUS_SHOW:
                if (!mProgressDialog.isShowing()) {
                    createProgressDialog();
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.setTitle(intent.getStringExtra(LayerFillService.KEY_TITLE));
                            mProgressDialog.setMessage(intent.getStringExtra(LayerFillService.KEY_TITLE));
                        }
                    });
                    mProgressDialog.show();
                }
                break;
        }
    }

    private void createProgressDialog() {
        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                mActivity.getString(R.string.menu_visibility_off), (DialogInterface.OnClickListener) null);
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
}

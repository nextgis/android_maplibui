package com.nextgis.maplibui.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplibui.R;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Service for filling layers with data
 */
public class LayerFillService extends Service implements IProgressor {
    protected NotificationManager mNotifyManager;
    protected static final int FILL_NOTIFICATION_ID = 9;
    protected ThreadPoolExecutor mThreadPool;
    protected NotificationCompat.Builder mBuilder;

    public static final String ACTION_STOP = "FILL_LAYER_STOP";
    public static final String ACTION_ADD_TASK = "ADD_FILL_LAYER_TASK";
    public static final String KEY_URI = "uri";
    public static final String KEY_INPUT_TYPE = "input_type";
    public static final String KEY_HAS_FORM = "has_form";

    // TODO: 31.08.15 Add support for filling zip tile cache, vector from GeoJSON, vector from NGFP, vector from NGW

    @Override
    public void onCreate() {
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Bitmap largeIcon =
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_notification_download);

        Intent intentStop = new Intent(this, TileDownloadService.class);
        intentStop.setAction(ACTION_STOP);
        PendingIntent stopService = PendingIntent.getService(this, 0, intentStop,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_notification_download).setLargeIcon(largeIcon)
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.tracks_stop),
                        stopService);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LayerFillService", "Received start id " + startId + ": " + intent);

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void setMax(int maxValue) {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setValue(int value) {

    }

    @Override
    public void setIndeterminate(boolean indeterminate) {

    }

    @Override
    public void setMessage(String message) {

    }
}

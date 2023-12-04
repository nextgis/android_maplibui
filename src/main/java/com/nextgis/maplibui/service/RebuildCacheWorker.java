package com.nextgis.maplibui.service;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import java.util.concurrent.TimeUnit;

public class RebuildCacheWorker extends Worker  {

    public RebuildCacheWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void schedule(Context context, int layerId) {
        Log.d(Constants.TAG, "RebuildCacheWorker schedule with ");

        Constraints constraints = new Constraints.Builder()
                .build();
        Data myData = new Data.Builder()
                .putInt("layerid", layerId)
                .build();

        WorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(RebuildCacheWorker.class)
                        .setConstraints(constraints)
                        .setInputData(myData)
                        .setInitialDelay(1, TimeUnit.SECONDS)
                        .build();
        WorkManager
                .getInstance(context)
                .enqueue(syncWorkRequest);
        Log.d(Constants.TAG, "start worker");
    }

    @NonNull
    @Override
    public Result doWork() {

        int layerid = getInputData().getInt("layerid", -1);
        Log.d(Constants.TAG, "RebuildCacheWorker doWork() with " +  layerid);

        if (layerid != -1){
            VectorLayer mLayer;

            try {
                mLayer = (VectorLayer) MapBase.getInstance().getLayerById(layerid);
            } catch (Exception ex){
                mLayer = null;
                Log.e("error", ex.getMessage());
            }
//            String notifyTitle;
//            if (getPackageName().equals("com.nextgis.mobile")) {
//                notifyTitle = getString(R.string.rebuild_cache);
//                notifyTitle += ": " + mCurrentTasks + "/" + mTotalTasks;
//            } else {
//                notifyTitle = getString(R.string.updating_data);
//            }

//            mBuilder.setWhen(System.currentTimeMillis())
//                    .setContentTitle(notifyTitle)
//                    .setTicker(notifyTitle);
//            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

//            Process.setThreadPriority(Constants.DEFAULT_DOWNLOAD_THREAD_PRIORITY);
            if (mLayer != null)
                mLayer.rebuildCache(null);

        }
        return Result.success();
    }
}

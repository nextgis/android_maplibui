//package com.nextgis.maplibui.service
//
//import android.content.Context
//import android.util.Log
//import androidx.work.Constraints
//import androidx.work.Constraints.Builder
//import androidx.work.Data
//import androidx.work.OneTimeWorkRequestBuilder
//import androidx.work.WorkManager
//import androidx.work.WorkRequest
//import androidx.work.Worker
//import androidx.work.WorkerParameters
//import com.nextgis.maplib.map.MapBase
//import com.nextgis.maplib.map.VectorLayer
//import com.nextgis.maplib.util.Constants
//import java.util.concurrent.TimeUnit
//
//
//class RebuildCacheWorkerKT(context: Context, workerParams: WorkerParameters)
//    : Worker(context, workerParams) {
//
//    companion object {
//
//        fun schedule(context: Context?, layerId: Int) {
//            val constraints: Constraints = Builder()
//                .build()
//            val myData = Data.Builder()
//                .putInt("layerid", layerId)
//                .build()
//            val syncWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<RebuildCacheWorkerKT>()
//                .setConstraints(constraints)
//                .setInputData(myData)
//                .setInitialDelay(1, TimeUnit.SECONDS)
//                .build()
//            WorkManager
//                .getInstance(context!!)
//                .enqueue(syncWorkRequest)
//            Log.d(Constants.TAG, "start worker")
//        }
//    }
//
//    override fun doWork(): Result {
//        val layerid = inputData.getInt("layerid", -1)
//        if (layerid != -1) {
//            var mLayer: VectorLayer?
//            try {
//                mLayer = MapBase.getInstance().getLayerById(layerid) as VectorLayer
//            } catch (ex: Exception) {
//                mLayer = null
//                Log.e("error", ex.message!!)
//            }
//            var notifyTitle: String
//            //            if (getPackageName().equals("com.nextgis.mobile")) {
////                notifyTitle = getString(R.string.rebuild_cache);
////                notifyTitle += ": " + mCurrentTasks + "/" + mTotalTasks;
////            } else {
////                notifyTitle = getString(R.string.updating_data);
////            }
//
////            mBuilder.setWhen(System.currentTimeMillis())
////                    .setContentTitle(notifyTitle)
////                    .setTicker(notifyTitle);
////            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
//
////            Process.setThreadPriority(Constants.DEFAULT_DOWNLOAD_THREAD_PRIORITY);
//            mLayer?.rebuildCache(null)
//        }
//
//
//
//
//        return Result.success()    }
//}
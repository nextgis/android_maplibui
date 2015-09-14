/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.ConstantsUI;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import static com.nextgis.maplib.util.Constants.DRAWING_SEPARATE_THREADS;
import static com.nextgis.maplib.util.Constants.KEEP_ALIVE_TIME;
import static com.nextgis.maplib.util.Constants.KEEP_ALIVE_TIME_UNIT;

/**
 * The service to batch download tiles
 */
public class TileDownloadService extends Service{
    protected List<DownloadTask> mQueue;
    protected NotificationManager mNotifyManager;
    protected static final int TILE_DOWNLOAD_NOTIFICATION_ID = 7;
    protected ThreadPoolExecutor mThreadPool;
    protected NotificationCompat.Builder mBuilder;

    public static final String KEY_MINX = "env_minx";
    public static final String KEY_MAXX = "env_maxx";
    public static final String KEY_MINY = "env_miny";
    public static final String KEY_MAXY = "env_maxy";
    public static final String KEY_ZOOM_FROM = "zoom_from";
    public static final String KEY_ZOOM_TO = "zoom_to";
    public static final String ACTION_STOP = "TILE_DOWNLOAD_STOP";
    public static final String ACTION_ADD_TASK = "ADD_TILE_DOWNLOAD_TASK";

    @Override
    public void onCreate() {
        mNotifyManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
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

        mQueue = new LinkedList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("TileDownloadService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.

        if (intent != null) {
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_ADD_TASK:
                        int layerId = intent.getIntExtra(ConstantsUI.KEY_LAYER_ID, Constants.NOT_FOUND);
                        double dfMinX = intent.getDoubleExtra(KEY_MINX, 0);
                        double dfMinY = intent.getDoubleExtra(KEY_MINY, 0);
                        double dfMaxX = intent.getDoubleExtra(KEY_MAXX, GeoConstants.MERCATOR_MAX);
                        double dfMaxY = intent.getDoubleExtra(KEY_MAXY, GeoConstants.MERCATOR_MAX);
                        int zoomFrom = intent.getIntExtra(KEY_ZOOM_FROM, 0);
                        int zoomTo = intent.getIntExtra(KEY_ZOOM_TO, 18);

                        GeoEnvelope env = new GeoEnvelope(dfMinX, dfMaxX, dfMinY, dfMaxY);
                        addTask(layerId, env, zoomFrom, zoomTo);
                        return START_STICKY;
                    case ACTION_STOP:
                        mQueue.clear();
                        cancelDownload();
                        break;
                }

                startDownload();
            }
        }
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void addTask(int layerId, GeoEnvelope env, int zoomFrom, int zoomTo) {
        DownloadTask task = new DownloadTask(layerId, env, zoomFrom, zoomTo);
        mQueue.add(task);

        if(mQueue.size() == 1){
            startDownload();
        }
    }

    protected void startDownload(){
        if(mQueue.isEmpty()){
            mNotifyManager.cancel(TILE_DOWNLOAD_NOTIFICATION_ID);
            stopSelf();
            return;
        }

        MapBase map = MapBase.getInstance();
        if(null == map)
            return;

        DownloadTask task = mQueue.remove(0);
        ILayer layer = map.getLayerById(task.getLayerId());
        if(layer instanceof RemoteTMSLayer) {
            final RemoteTMSLayer tmsLayer = (RemoteTMSLayer) layer;
            String notifyTitle = getString(R.string.download_tiles) + " (" + tmsLayer.getName() + ")";

            mBuilder.setWhen(System.currentTimeMillis())
                    .setContentTitle(notifyTitle);
            mNotifyManager.notify(TILE_DOWNLOAD_NOTIFICATION_ID, mBuilder.build());

            final List<TileItem> tiles = new LinkedList<>();
            int zoomCount = task.getZoomTo() + 1 - task.getZoomFrom();
            for (int zoom = task.getZoomFrom(); zoom < task.getZoomTo() + 1; zoom++) {
                tiles.addAll(MapUtil.getTileItems(task.getEnvelope(), zoom, tmsLayer.getTMSType()));
                mBuilder.setProgress(zoomCount, zoom, false)
                        .setContentText(getString(R.string.form_tiles_list));
                mNotifyManager.notify(TILE_DOWNLOAD_NOTIFICATION_ID, mBuilder.build());
            }

            int threadCount = DRAWING_SEPARATE_THREADS;

            mThreadPool = new ThreadPoolExecutor(
                    threadCount, threadCount, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
                    new LinkedBlockingQueue<Runnable>(), new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(
                        Runnable r,
                        ThreadPoolExecutor executor) {
                    try {
                        executor.getQueue().put(r);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        //throw new RuntimeException("Interrupted while submitting task", e);
                    }
                }
            });

            int tilesSize = tiles.size();
            List<Future> futures = new ArrayList<>(tilesSize);

            for (int i = 0; i < tilesSize; ++i) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                final TileItem tile = tiles.get(i);

                futures.add(
                        mThreadPool.submit(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        android.os.Process.setThreadPriority(
                                                Constants.DEFAULT_DRAW_THREAD_PRIORITY);
                                        tmsLayer.downloadTile(tile);
                                    }
                                }));
            }

            // wait for download ending
            int nStep = futures.size() / Constants.DRAW_NOTIFY_STEP_PERCENT;
            if (nStep == 0)
                nStep = 1;
            for (int i = 0, futuresSize = futures.size(); i < futuresSize; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                try {
                    Future future = futures.get(i);
                    future.get(); // wait for task ending

                    if (i % nStep == 0) {
                        mBuilder.setProgress(futures.size(), i, false)
                                .setContentText(getString(R.string.download_in_progress));
                        // Displays the progress bar for the first time.
                        mNotifyManager.notify(TILE_DOWNLOAD_NOTIFICATION_ID, mBuilder.build());
                    }

                } catch (CancellationException | InterruptedException e) {
                    //e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            // skip non tms layer
        }

        startDownload();
    }

    public void cancelDownload()
    {
        if (mThreadPool != null) {
            //synchronized (lock) {
            mThreadPool.shutdownNow();
            try {
                mThreadPool.awaitTermination(Constants.TERMINATE_TIME, Constants.KEEP_ALIVE_TIME_UNIT);
                //mDrawThreadPool.purge();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
            //}
        }
    }

    public class DownloadTask{
        protected int mLayerId;
        protected GeoEnvelope mEnvelope;
        protected int mZoomFrom;
        protected int mZoomTo;

        public DownloadTask(int layerId, GeoEnvelope envelope, int zoomFrom, int zoomTo) {
            mEnvelope = envelope;
            mLayerId = layerId;
            mZoomFrom = zoomFrom;
            mZoomTo = zoomTo;
        }

        public GeoEnvelope getEnvelope() {
            return mEnvelope;
        }

        public int getLayerId() {
            return mLayerId;
        }

        public int getZoomFrom() {
            return mZoomFrom;
        }

        public int getZoomTo() {
            return mZoomTo;
        }
    }
}

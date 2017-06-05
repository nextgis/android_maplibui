/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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
import android.os.Build;
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
import com.nextgis.maplibui.util.NotificationHelper;

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
public class TileDownloadService extends Service {
    protected List<DownloadTask> mQueue;
    protected NotificationManager mNotifyManager;
    protected static final int TILE_DOWNLOAD_NOTIFICATION_ID = 7;
    protected ThreadPoolExecutor mThreadPool;
    protected NotificationCompat.Builder mBuilder;

    public static final String KEY_MINX = "env_minx";
    public static final String KEY_MAXX = "env_maxx";
    public static final String KEY_MINY = "env_miny";
    public static final String KEY_MAXY = "env_maxy";
    public static final String KEY_PATH = "path";
    public static final String KEY_ZOOM_FROM = "zoom_from";
    public static final String KEY_ZOOM_TO = "zoom_to";
    public static final String KEY_ZOOM_LIST = "zoom_list";
    public static final String ACTION_STOP = "TILE_DOWNLOAD_STOP";
    public static final String ACTION_ADD_TASK = "ADD_TILE_DOWNLOAD_TASK";

    protected boolean mCanceled;

    @Override
    public void onCreate() {
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Bitmap largeIcon = NotificationHelper.getLargeIcon(R.drawable.ic_notification_download, getResources());

        Intent intentStop = new Intent(this, TileDownloadService.class);
        intentStop.setAction(ACTION_STOP);
        PendingIntent stopService = PendingIntent.getService(this, 0, intentStop, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_notification_download).setLargeIcon(largeIcon)
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel), stopService);

        mQueue = new LinkedList<>();
        mCanceled = false;
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
                        String layerPathName = intent.getStringExtra(KEY_PATH);
                        double dfMinX = intent.getDoubleExtra(KEY_MINX, 0);
                        double dfMinY = intent.getDoubleExtra(KEY_MINY, 0);
                        double dfMaxX = intent.getDoubleExtra(KEY_MAXX, GeoConstants.MERCATOR_MAX);
                        double dfMaxY = intent.getDoubleExtra(KEY_MAXY, GeoConstants.MERCATOR_MAX);
                        GeoEnvelope env = new GeoEnvelope(dfMinX, dfMaxX, dfMinY, dfMaxY);

                        if (intent.hasExtra(KEY_ZOOM_FROM) && intent.hasExtra(KEY_ZOOM_TO)) {
                            int zoomFrom = intent.getIntExtra(KEY_ZOOM_FROM, 0);
                            int zoomTo = intent.getIntExtra(KEY_ZOOM_TO, 18);
                            addTask(layerPathName, env, zoomFrom, zoomTo);
                        } else if (intent.hasExtra(KEY_ZOOM_LIST)) {
                            List<Integer> zoomList = intent.getIntegerArrayListExtra(KEY_ZOOM_LIST);
                            addTask(layerPathName, env, zoomList);
                        }

                        return START_STICKY;
                    case ACTION_STOP:
                        if (Constants.DEBUG_MODE)
                            Log.d(Constants.TAG, "Cancel download queue");
                        mQueue.clear();
                        mCanceled = true;
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

    protected void addTask(String layerPathName, GeoEnvelope env, int zoomFrom, int zoomTo) {
        List<Integer> zoomList = new ArrayList<>(zoomTo - zoomFrom + 1);
        for (int zoom = zoomFrom; zoom < zoomTo + 1; ++zoom) {
            zoomList.add(zoom);
        }
        addTask(layerPathName, env, zoomList);
    }

    protected void addTask(String layerPathName, GeoEnvelope env, List<Integer> zoomList) {
        DownloadTask task = new DownloadTask(layerPathName, env, zoomList);
        mQueue.add(task);
        mCanceled = false;

        if (mQueue.size() == 1) {
            startDownload();
        }
    }

    protected void startDownload() {
        if (Constants.DEBUG_MODE)
            Log.d(Constants.TAG, "Tile download queue size " + mQueue.size());

        if (mQueue.isEmpty()) {
            mNotifyManager.cancel(TILE_DOWNLOAD_NOTIFICATION_ID);
            stopSelf();
            return;
        }

        final DownloadTask task = mQueue.remove(0);
        new Thread() {
            @Override
            public void run() {
                download(task);
                startDownload();
            }
        }.start();
    }

    private void download(DownloadTask task) {
        MapBase map = MapBase.getInstance();
        if (null == map)
            return;

        ILayer layer = map.getLayerByPathName(task.getLayerPathName());
        if (null != layer && layer instanceof RemoteTMSLayer) { // process only tms layers
            final RemoteTMSLayer tmsLayer = (RemoteTMSLayer) layer;
            String notifyTitle = getString(R.string.download_tiles);

            mBuilder.setWhen(System.currentTimeMillis()).setContentTitle(notifyTitle);
            mNotifyManager.notify(TILE_DOWNLOAD_NOTIFICATION_ID, mBuilder.build());

            final List<TileItem> tiles = new LinkedList<>();
            int zoomCount = task.getZoomList().size();
            for (Integer zoom : task.getZoomList()) {
                tiles.addAll(MapUtil.getTileItems(task.getEnvelope(), zoom, tmsLayer.getTMSType()));

                if (mCanceled)
                    break;

                mBuilder.setProgress(zoomCount, zoom, false).setContentText(getString(R.string.form_tiles_list));
                mNotifyManager.notify(TILE_DOWNLOAD_NOTIFICATION_ID, mBuilder.build());

                if (tiles.size() > Constants.MAX_TILES_COUNT)
                    break;
            }

            int threadCount = DRAWING_SEPARATE_THREADS;
            int coreCount = Runtime.getRuntime().availableProcessors();

            // FIXME more than 1 pool size causing strange behaviour on 6.0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                coreCount = 1;

            mThreadPool = new ThreadPoolExecutor(
                    coreCount, threadCount, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
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
                if (mCanceled)
                    break;

                final TileItem tile = tiles.get(i);
                futures.add(mThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(Constants.DEFAULT_DRAW_THREAD_PRIORITY);
                        tmsLayer.downloadTile(tile, false);
                    }
                }));
            }

            //in separate thread

            // wait for download ending
            int nStep = futures.size() / Constants.DRAW_NOTIFY_STEP_PERCENT;
            if (nStep == 0)
                nStep = 1;

            for (int i = 0, futuresSize = futures.size(); i < futuresSize; i++) {
                if (mCanceled)
                    break;

                try {
                    Future future = futures.get(i);
                    future.get(); // wait for task ending

                    if (i % nStep == 0) {
                        mBuilder.setProgress(futuresSize, i, false).setContentText(getString(R.string.processing) + " " + tmsLayer.getName());
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
    }

    public void cancelDownload() {
        if (mThreadPool != null) {
            //synchronized (lock) {
            mThreadPool.shutdownNow();
            try {
                mThreadPool.awaitTermination(2000, Constants.KEEP_ALIVE_TIME_UNIT);
                //mThreadPool.purge();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }

            if (Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "Canceled download queue. Active count: " + mThreadPool.getActiveCount() + " queue size " + mQueue.size());
        }
    }

    public class DownloadTask {
        String mLayerPathName;
        GeoEnvelope mEnvelope;
        List<Integer> mZoomList;

        DownloadTask(String layerPathName, GeoEnvelope envelope, List<Integer> zoomList) {
            mEnvelope = envelope;
            mLayerPathName = layerPathName;
            mZoomList = zoomList;
        }

        GeoEnvelope getEnvelope() {
            return mEnvelope;
        }

        String getLayerPathName() {
            return mLayerPathName;
        }

        List<Integer> getZoomList()
        {
            return mZoomList;
        }
    }
}

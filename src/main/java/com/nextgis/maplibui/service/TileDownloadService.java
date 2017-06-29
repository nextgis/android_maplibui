/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
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
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
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
public class TileDownloadService
        extends Service
{
    protected static final int TILE_DOWNLOAD_NOTIFICATION_ID = 7;

    public static final String KEY_MINX        = "env_minx";
    public static final String KEY_MAXX        = "env_maxx";
    public static final String KEY_MINY        = "env_miny";
    public static final String KEY_MAXY        = "env_maxy";
    public static final String KEY_PATH        = "path";
    public static final String KEY_ZOOM_FROM   = "zoom_from";
    public static final String KEY_ZOOM_TO     = "zoom_to";
    public static final String KEY_ZOOM_LIST   = "zoom_list";
    public static final String ACTION_STOP     = "TILE_DOWNLOAD_STOP";
    public static final String ACTION_ADD_TASK = "ADD_TILE_DOWNLOAD_TASK";

    protected NotificationManager        mNotifyManager;
    protected NotificationCompat.Builder mBuilder;

    protected Queue<DownloadTask> mQueue;
    protected Thread              mDownloadThread;

    protected final Object  mErrorLock       = new Object();
    protected       boolean mIsDownloadError = false;

    // Thread.currentThread().isInterrupted() is not work, so we use mIsDownloadInterrupted.
    protected volatile boolean mIsDownloadInterrupted = false;

    @Override
    public void onCreate()
    {
        super.onCreate();
        //android.os.Debug.waitForDebugger();

        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "TileDownloadService.onCreate() is starting");
        }

        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent intentStop = getStopIntent();
        intentStop.setAction(ACTION_STOP);
        PendingIntent stopService =
                PendingIntent.getService(this, 0, intentStop, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap largeIcon = NotificationHelper.getLargeIcon(R.drawable.ic_notification_download,
                getResources());

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_notification_download)
                .setLargeIcon(largeIcon)
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.cancel), stopService);

        mQueue = new ConcurrentLinkedQueue<>();
    }

    // For overriding in subclasses
    protected Intent getStopIntent()
    {
        return new Intent(this, TileDownloadService.class);
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId)
    {
        Log.i("TileDownloadService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.

        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "TileDownloadService.onStartCommand() is starting");
        }

        if (intent != null) {
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_ADD_TASK:
                        if (Constants.DEBUG_MODE) {
                            Log.d(
                                    Constants.TAG,
                                    "TileDownloadService.onStartCommand(), ACTION_ADD_TASK");
                        }
                        addDownloadTask(intent);
                        break;

                    case ACTION_STOP:
                        if (Constants.DEBUG_MODE) {
                            Log.d(
                                    Constants.TAG,
                                    "TileDownloadService.onStartCommand(), ACTION_STOP");
                        }
                        cancelDownload();
                        stopSelf();
                        break;
                }
            }
        }
        return START_STICKY;
    }

    protected void addDownloadTask(Intent intent)
    {
        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "Add task to download queue");
        }
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
    }

    protected void cancelDownload()
    {
        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "Cancel download queue");
        }
        mNotifyManager.cancel(TILE_DOWNLOAD_NOTIFICATION_ID);
        mQueue.clear();
        if (mDownloadThread != null && mDownloadThread.isAlive()) {
            mDownloadThread.interrupt();
            mDownloadThread = null;
            mIsDownloadInterrupted = true;
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "TileDownloadService.cancelDownload(), interrupted service");
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        cancelDownload();
        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "TileDownloadService.onDestroy(), service is stopped");
        }
        super.onDestroy();
    }

    protected void addTask(
            String layerPathName,
            GeoEnvelope env,
            int zoomFrom,
            int zoomTo)
    {
        List<Integer> zoomList = new ArrayList<>(zoomTo - zoomFrom + 1);
        for (int zoom = zoomFrom; zoom < zoomTo + 1; ++zoom) {
            zoomList.add(zoom);
        }
        addTask(layerPathName, env, zoomList);
    }

    protected void addTask(
            String layerPathName,
            GeoEnvelope env,
            List<Integer> zoomList)
    {
        DownloadTask task = new DownloadTask(layerPathName, env, zoomList);
        mQueue.add(task);

        if (mDownloadThread == null) {
            if (Constants.DEBUG_MODE) {
                Log.d(
                        Constants.TAG,
                        "TileDownloadService.addTask(), create and run download thread");
            }
            mDownloadThread = createDownloadThread();
            mDownloadThread.start();
        }
    }

    private Thread createDownloadThread()
    {
        mIsDownloadInterrupted = false;
        return new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if (Constants.DEBUG_MODE) {
                    Log.d(Constants.TAG, "TileDownloadService.mDownloadThread, started");
                }

                while (!mQueue.isEmpty()) {
                    if (mIsDownloadInterrupted) {
                        break;
                    }
                    if (Constants.DEBUG_MODE) {
                        Log.d(Constants.TAG, "Tile download queue size " + mQueue.size());
                    }
                    DownloadTask task = mQueue.poll();
                    download(task);
                }

                stopSelf();

                if (Constants.DEBUG_MODE) {
                    Log.d(Constants.TAG, "TileDownloadService.stopSelf() is performed");
                    Log.d(Constants.TAG, "TileDownloadService.mDownloadThread, stopped");
                }
            }
        });
    }

    // For overriding in subclasses
    protected List<TileItem> getTileItems(
            GeoEnvelope bounds,
            double zoom,
            RemoteTMSLayer tmsLayer)
    {
        return MapUtil.getTileItems(bounds, zoom, tmsLayer.getTMSType());
    }

    protected void download(DownloadTask task)
    {
        synchronized (mErrorLock) {
            mIsDownloadError = false;
        }

        MapBase map = MapBase.getInstance();
        if (null == map) {
            return;
        }

        map.load(); // Reload map for new added layers from the main app process

        ILayer layer = map.getLayerByPathName(task.getLayerPathName());

        if (null != layer && layer instanceof RemoteTMSLayer) { // process only tms layers
            String notifyTitle = getString(R.string.download_tiles);
            mBuilder.setContentTitle(notifyTitle).setWhen(System.currentTimeMillis());
            mNotifyManager.notify(TILE_DOWNLOAD_NOTIFICATION_ID, mBuilder.build());

            final RemoteTMSLayer tmsLayer = (RemoteTMSLayer) layer;
            final List<TileItem> tiles = new LinkedList<>();
            int zoomCount = task.getZoomList().size();

            for (Integer zoom : task.getZoomList()) {
                if (mIsDownloadInterrupted) {
                    if (Constants.DEBUG_MODE) {
                        Log.d(
                                Constants.TAG,
                                "TileDownloadService.mDownloadThread is interrupted, point 01");
                    }
                    return;
                }

                tiles.addAll(getTileItems(task.getEnvelope(), zoom, tmsLayer));

                mBuilder.setProgress(zoomCount, zoom, false)
                        .setContentText(getString(R.string.form_tiles_list));
                mNotifyManager.notify(TILE_DOWNLOAD_NOTIFICATION_ID, mBuilder.build());

                if (tiles.size() > Constants.MAX_TILES_COUNT) {
                    break;
                }
            }

            int threadCount = DRAWING_SEPARATE_THREADS;
            int coreCount = Runtime.getRuntime().availableProcessors();

            // FIXME: More than 1 pool size causing strange behaviour on 6.0.
            // FIXME: Check this again after 29.06.2017.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                coreCount = 1;
            }

            ThreadPoolExecutor threadPool =
                    new ThreadPoolExecutor(coreCount, threadCount, KEEP_ALIVE_TIME,
                            KEEP_ALIVE_TIME_UNIT, new LinkedBlockingQueue<Runnable>(),
                            new RejectedExecutionHandler()
                            {
                                @Override
                                public void rejectedExecution(
                                        Runnable r,
                                        ThreadPoolExecutor executor)
                                {
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
                boolean isError = isDownloadError();
                if (isError || mIsDownloadInterrupted) {
                    if (Constants.DEBUG_MODE) {
                        Log.d(
                                Constants.TAG,
                                "TileDownloadService.mDownloadThread is interrupted, point 02, isDownloadError: "
                                        + isError);
                    }
                    break;
                }

                final TileItem tile = tiles.get(i);
                futures.add(threadPool.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        android.os.Process.setThreadPriority(
                                Constants.DEFAULT_DRAW_THREAD_PRIORITY);

                        if (!downloadTile(tmsLayer, tile)) {
                            synchronized (mErrorLock) {
                                mIsDownloadError = true;
                            }
                            if (Constants.DEBUG_MODE) {
                                Log.d(
                                        Constants.TAG,
                                        "TileDownloadService.mDownloadThread, downloadTile() with error, layer: "
                                                + tmsLayer.getName() + ", tile: "
                                                + tile.toString());
                            }
                        }
                    }
                }));
            }

            // in separate thread

            // wait for download ending
            int nStep = futures.size() / Constants.DRAW_NOTIFY_STEP_PERCENT;
            if (nStep == 0) {
                nStep = 1;
            }

            for (int i = 0, futuresSize = futures.size(); i < futuresSize; ++i) {
                boolean isError = isDownloadError();
                if (isError || mIsDownloadInterrupted) {
                    if (Constants.DEBUG_MODE) {
                        Log.d(
                                Constants.TAG,
                                "TileDownloadService.mDownloadThread is interrupted, point 03, isDownloadError: "
                                        + isError);
                    }
                    break;
                }

                try {
                    Future future = futures.get(i);
                    future.get(); // wait for task ending

                    if (i % nStep == 0) {
                        mBuilder.setProgress(futuresSize, i, false)
                                .setContentText(
                                        getString(R.string.processing) + " " + tmsLayer.getName());
                        // Displays the progress bar for the first time.
                        mNotifyManager.notify(TILE_DOWNLOAD_NOTIFICATION_ID, mBuilder.build());
                    }

                } catch (CancellationException | InterruptedException e) {
                    //e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            threadPool.shutdownNow(); // Cancel currently executing tasks
            try {
                // Wait a while for tasks to respond to being cancelled
                if (!threadPool.awaitTermination(2000, Constants.KEEP_ALIVE_TIME_UNIT)) {
                    if (Constants.DEBUG_MODE) {
                        Log.d(
                                Constants.TAG,
                                "TileDownloadService.mDownloadThread, threadPool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                // (Re-)Cancel if current thread also interrupted
                threadPool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        } else {
            if (Constants.DEBUG_MODE) {
                if (layer == null) {
                    Log.d(
                            Constants.TAG,
                            "TileDownloadService.mDownloadThread, layer == null, exit");
                    return;
                }
                Log.d(
                        Constants.TAG,
                        "TileDownloadService.mDownloadThread, layer type is not TMS, layer name: "
                                + layer.getName() + ", type: " + layer.getType() + ", exit");
            }
        }
    }

    // For overriding in subclasses
    protected boolean downloadTile(
            RemoteTMSLayer tmsLayer,
            TileItem tile)
    {
        return tmsLayer.downloadTile(tile, false);
    }

    // For overriding in subclasses
    protected boolean isDownloadError()
    {
        return false;

        // Example for error checking
        //boolean isError;
        //synchronized (mErrorLock) {
        //    isError = mIsDownloadError;
        //}
        //return isError;
    }

    public class DownloadTask
    {
        String        mLayerPathName;
        GeoEnvelope   mEnvelope;
        List<Integer> mZoomList;

        DownloadTask(
                String layerPathName,
                GeoEnvelope envelope,
                List<Integer> zoomList)
        {
            mEnvelope = envelope;
            mLayerPathName = layerPathName;
            mZoomList = zoomList;
        }

        GeoEnvelope getEnvelope()
        {
            return mEnvelope;
        }

        String getLayerPathName()
        {
            return mLayerPathName;
        }

        List<Integer> getZoomList()
        {
            return mZoomList;
        }
    }
}

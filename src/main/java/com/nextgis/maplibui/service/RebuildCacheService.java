/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016, 2018-2019 NextGIS, info@nextgis.com
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
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.VectorLayerSettingsActivity;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.NotificationHelper;

import java.util.LinkedList;
import java.util.List;

import static com.nextgis.maplibui.util.NotificationHelper.createBuilder;

public class RebuildCacheService extends Service implements IProgressor {
    public static final String ACTION_ADD_TASK = "REBUILD_CACHE_ADD_TASK";
    public static final String ACTION_REMOVE_TASK = "REBUILD_CACHE_REMOVE_TASK";
    public static final String ACTION_STOP = "REBUILD_CACHE_STOP";
    public static final String ACTION_SHOW = "REBUILD_CACHE_SHOW";
    public static final String ACTION_UPDATE = "REBUILD_CACHE_UPDATE_PROGRESS";
    public static final String KEY_PROGRESS = "progress";
    public static final String KEY_MAX = "max";

    protected NotificationManager mNotifyManager;
    protected List<Integer> mQueue;
    protected static final int NOTIFICATION_ID = 99;
    protected NotificationCompat.Builder mBuilder;
    protected Intent mProgressIntent;

    protected VectorLayer mLayer;
    protected int mProgressMax, mCurrentTasks;
    protected long mLastUpdate = 0;
    protected boolean mIsRunning, mIsCanceled, mRemoveCurrent;

    @Override
    public void onCreate() {
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mProgressIntent = new Intent(ACTION_UPDATE);
        Intent intent = new Intent(this, RebuildCacheService.class);
        intent.setAction(ACTION_STOP);
        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent stop = PendingIntent.getService(this, 0, intent, flag);
        intent.setAction(ACTION_SHOW);
        PendingIntent show = PendingIntent.getService(this, 0, intent, flag);
        mBuilder = createBuilder(this, R.string.rebuild_cache);

        int icon = R.drawable.ic_notification_rebuild_cache;
        Bitmap largeIcon = NotificationHelper.getLargeIcon(icon, getResources());
        mBuilder.setSmallIcon(icon).setLargeIcon(largeIcon);
        icon = R.drawable.ic_action_cancel_dark;

        mBuilder.setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(show)
                .addAction(icon, getString(android.R.string.cancel), stop);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String title = getString(R.string.rebuild_cache);
            mBuilder.setWhen(System.currentTimeMillis()).setContentTitle(title).setTicker(title);
            startForeground(NOTIFICATION_ID, mBuilder.build());
        }

        mQueue = new LinkedList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && !TextUtils.isEmpty(action)) {
                String key = ConstantsUI.KEY_LAYER_ID;
                switch (action) {
                    case ACTION_ADD_TASK:
                        int layerIdAdd = intent.getIntExtra(key, Constants.NOT_FOUND);
                        mQueue.add(layerIdAdd);

                        if (!mIsRunning)
                            startNextTask();

                        return START_STICKY;
                    case ACTION_REMOVE_TASK:
                        int layerIdRemove = intent.getIntExtra(key, Constants.NOT_FOUND);
                        mCurrentTasks--;

                        if (!mQueue.contains(layerIdRemove))
                            if (mLayer != null && mLayer.getId() == layerIdRemove)
                                mRemoveCurrent = true;
                            else
                                mQueue.remove(layerIdRemove);
                        return START_STICKY;
                    case ACTION_STOP:
                        mQueue.clear();
                        mIsCanceled = true;
                        break;
                    case ACTION_SHOW:
                        Intent settings = new Intent(this, VectorLayerSettingsActivity.class);
                        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        settings.putExtra(key, mLayer != null ? mLayer.getId() : Constants.NOT_FOUND);
                        startActivity(settings);
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    protected void stopService() {
        mCurrentTasks = 0;
        mProgressIntent.putExtra(KEY_PROGRESS, 0);
        sendBroadcast(mProgressIntent);
        mLayer = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            stopForeground(true);
        else
            mNotifyManager.cancel(NOTIFICATION_ID);

        stopSelf();
    }

    protected void startNextTask() {
        if (mQueue.isEmpty()) {
            stopService();
            return;
        }

        mIsCanceled = false;
        final IProgressor progressor = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mLayer = (VectorLayer) MapBase.getInstance().getLayerById(mQueue.remove(0));
                mIsRunning = true;
                mCurrentTasks++;
                String notifyTitle = getString(R.string.rebuild_cache);
                notifyTitle += ": " + mCurrentTasks + "/" + mQueue.size() + 1;

                mBuilder.setWhen(System.currentTimeMillis())
                        .setContentTitle(notifyTitle)
                        .setTicker(notifyTitle);
                mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

                Process.setThreadPriority(Constants.DEFAULT_DOWNLOAD_THREAD_PRIORITY);
                if (mLayer != null)
                    mLayer.rebuildCache(progressor);

                mIsRunning = mRemoveCurrent = false;
                startNextTask();
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void setMax(int maxValue) {
        mProgressMax = maxValue;
    }

    @Override
    public boolean isCanceled() {
        return mIsCanceled || mRemoveCurrent;
    }

    @Override
    public void setValue(int value) {
        if (mLastUpdate + ConstantsUI.NOTIFICATION_DELAY < System.currentTimeMillis()) {
            mLastUpdate = System.currentTimeMillis();
            mBuilder.setProgress(mProgressMax, value, false);
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        }

        int id = mLayer != null ? mLayer.getId() : Constants.NOT_FOUND;
        mProgressIntent.putExtra(KEY_PROGRESS, value)
                .putExtra(KEY_MAX, mProgressMax)
                .putExtra(ConstantsUI.KEY_LAYER_ID, id);
        sendBroadcast(mProgressIntent);
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {

    }

    @Override
    public void setMessage(String message) {

    }
}

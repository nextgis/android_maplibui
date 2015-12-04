/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.service;

import android.accounts.Account;
import android.accounts.AccountsException;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.TMSLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.GeoJSONUtil;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.mapui.LocalTMSLayerUI;
import com.nextgis.maplibui.mapui.NGWVectorLayerUI;
import com.nextgis.maplibui.mapui.VectorLayerUI;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.NotificationHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for filling layers with data
 */
public class LayerFillService extends Service implements IProgressor {
    protected NotificationManager mNotifyManager;
    protected List<LayerFillTask> mQueue;
    protected static final int FILL_NOTIFICATION_ID = 9;
    protected NotificationCompat.Builder mBuilder;

    public final static int VECTOR_LAYER           = 1;
    public final static int VECTOR_LAYER_WITH_FORM = 2;
    public final static int TMS_LAYER              = 3;
    public final static int NGW_LAYER              = 4;

    public static final String ACTION_STOP = "FILL_LAYER_STOP";
    public static final String ACTION_ADD_TASK = "ADD_FILL_LAYER_TASK";
    public static final String ACTION_SHOW = "SHOW_PROGRESS_DIALOG";
    public static final String ACTION_UPDATE = "UPDATE_FILL_LAYER_PROGRESS";
    public static final String KEY_STATUS = "status";
    public static final String KEY_PROGRESS = "progress";
    public static final String KEY_TOTAL = "count";
    public static final String KEY_TITLE = "title";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_URI = "uri";
    public static final String KEY_PATH = "path";
    public static final String KEY_LAYER_PATH = "layer_path";
    public static final String KEY_REMOTE_ID = "remote_id";
    public static final String KEY_ACCOUNT = "account";
    public static final String KEY_NAME = "name";
    public static final String KEY_INPUT_TYPE = "input_type";
    public static final String KEY_DELETE_SRC_FILE = "delete_source_file";
    public static final String KEY_LAYER_GROUP_ID = "layer_group_id";
    public static final String KEY_TMS_TYPE   = "tms_type";
    public static final String NGFP_META = "ngfp_meta.json";
    protected final static String NGFP_FILE_META = "meta.json";
    protected final static String NGFP_FILE_DATA = "data.geojson";


    public static final short STATUS_START = 0;
    public static final short STATUS_UPDATE = 1;
    public static final short STATUS_STOP = 2;
    public static final short STATUS_SHOW = 3;

    protected int mProgressMax;
    protected int mProgressValue;
    protected LayerGroup mLayerGroup;
    protected long mLastUpdate = 0;
    protected String mProgressMessage;
    protected boolean mIndeterminate;
    protected boolean mIsCanceled;
    protected boolean mIsRunning;
    protected Handler mHandler;
    protected Intent mProgressIntent;

    protected static final String BUNDLE_MSG_KEY = "error_message";

    @Override
    public void onCreate() {
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Bitmap largeIcon = NotificationHelper.getLargeIcon(
                R.drawable.ic_notification_download, getResources());

        mProgressIntent = new Intent(ACTION_UPDATE);
        Intent intent = new Intent(this, LayerFillService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent stopService = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.setAction(ACTION_SHOW);
        PendingIntent showProgressDialog = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_notification_download).setLargeIcon(largeIcon)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(showProgressDialog)
                .addAction(R.drawable.ic_action_cancel_dark, getString(R.string.tracks_stop), stopService);
        mIsCanceled = false;

        mQueue = new LinkedList<>();
        mIsRunning = false;
        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle resultData = msg.getData();
                Toast.makeText(LayerFillService.this, resultData.getString(BUNDLE_MSG_KEY), Toast.LENGTH_LONG).show();
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LayerFillService", "Received start id " + startId + ": " + intent);
        if (intent != null) {
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_ADD_TASK:
                        int layerGroupId = intent.getIntExtra(KEY_LAYER_GROUP_ID, Constants.NOT_FOUND);
                        mLayerGroup = (LayerGroup) MapBase.getInstance().getLayerById(layerGroupId);
                        
                        Bundle extra = intent.getExtras();
                        int layerType = extra.getInt(KEY_INPUT_TYPE, Constants.NOT_FOUND);

                        switch (layerType) {
                            case VECTOR_LAYER:
                                mQueue.add(new VectorLayerFillTask(extra));
                                break;
                            case VECTOR_LAYER_WITH_FORM:
                                mQueue.add(new UnzipForm(extra));
                                break;
                            case TMS_LAYER:
                                mQueue.add(new LocalTMSFillTask(extra));
                                break;
                            case NGW_LAYER:
                                mQueue.add(new NGWVectorLayerFillTask(extra));
                                break;
                        }

                        if(!mIsRunning){
                            startNextTask();
                        }

                        return START_STICKY;
                    case ACTION_STOP:
                        mQueue.clear();
                        mIsCanceled = true;
                        break;
                    case ACTION_SHOW:
                        mProgressIntent.putExtra(KEY_STATUS, STATUS_SHOW).putExtra(KEY_TITLE, mBuilder.mContentTitle);
                        sendBroadcast(mProgressIntent);
                        break;
                }
            }
        }
        return START_STICKY;
    }

    protected void startNextTask(){
        if(mQueue.isEmpty()){
            mNotifyManager.cancel(FILL_NOTIFICATION_ID);
            mProgressIntent.putExtra(KEY_STATUS, STATUS_STOP);
            mProgressIntent.putExtra(KEY_MESSAGE, mIsCanceled);
            sendBroadcast(mProgressIntent);
            stopSelf();
            return;
        }

        mIsCanceled = false;
        final  IProgressor progressor = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mIsRunning = true;
                LayerFillTask task = mQueue.remove(0);
                String notifyTitle = task.getDescription();

                mBuilder.setWhen(System.currentTimeMillis())
                        .setContentTitle(notifyTitle)
                        .setTicker(notifyTitle);
                mNotifyManager.notify(FILL_NOTIFICATION_ID, mBuilder.build());
                mProgressIntent.putExtra(KEY_STATUS, STATUS_START).putExtra(KEY_TITLE, notifyTitle);
                sendBroadcast(mProgressIntent);

                Process.setThreadPriority(Constants.DEFAULT_DOWNLOAD_THREAD_PRIORITY);
                progressor.setValue(0);
                task.execute(progressor);

                if (!mIsCanceled) {
                    mLayerGroup.addLayer(task.getLayer());
                    mLayerGroup.save();
                } else
                    task.cancel();
                
                mIsRunning = false;
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
        return mIsCanceled;
    }

    @Override
    public void setValue(int value) {
        mProgressValue = value;
        updateNotify();
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        mIndeterminate = indeterminate;
        updateNotify();
    }

    @Override
    public void setMessage(String message) {
        mProgressMessage = message;
        updateNotify();
    }

    protected void updateNotify(){
        if (mLastUpdate + ConstantsUI.NOTIFICATION_DELAY < System.currentTimeMillis()) {
            mLastUpdate = System.currentTimeMillis();
            mBuilder.setProgress(mProgressMax, mProgressValue, mIndeterminate)
                    .setContentText(mProgressMessage);
            // Displays the progress bar for the first time.
            mNotifyManager.notify(FILL_NOTIFICATION_ID, mBuilder.build());
        }

        mProgressIntent.putExtra(KEY_STATUS, STATUS_UPDATE).putExtra(KEY_TOTAL, mProgressMax)
                .putExtra(KEY_PROGRESS, mProgressValue).putExtra(KEY_MESSAGE, mProgressMessage);
        sendBroadcast(mProgressIntent);
    }

    private void notifyError(String error) {
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_MSG_KEY, error);

        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * task classes
     */

    private abstract class LayerFillTask{
        protected String mLayerName;
        protected File mLayerPath;
        protected Uri mUri;
        protected Layer mLayer;

        public LayerFillTask(Bundle bundle) {
            mUri = bundle.getParcelable(KEY_URI);
            mLayerName = bundle.getString(KEY_NAME);
            mLayerPath = bundle.containsKey(KEY_LAYER_PATH) ?
                    (File) bundle.getSerializable(KEY_LAYER_PATH) :
                    mLayerGroup.createLayerStorage();
        }

        protected void initLayer() {
            mLayer.setName(mLayerName);
            mLayer.setVisible(true);
            mLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
            mLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);
        }

        public abstract void execute(IProgressor progressor);

        public String getDescription(){
            if(null == mLayer)
                return "";
            return getString(R.string.process_layer) + " " + mLayer.getName();
        }

        public ILayer getLayer() {
            return mLayer;
        }

        public void cancel() {
            if (mLayer != null)
                mLayer.delete();
            else if (mLayerPath != null)
                FileUtil.deleteRecursive(mLayerPath);
        }
    }

    private class VectorLayerFillTask extends LayerFillTask{
        public VectorLayerFillTask(Bundle bundle) {
            super(bundle);
            mLayer = new VectorLayerUI(mLayerGroup.getContext(), mLayerPath);
            initLayer();
        }

        @Override
        public void execute(IProgressor progressor) {
            try {
                VectorLayer vectorLayer = (VectorLayer) mLayer;
                if(null == vectorLayer)
                    return;

                vectorLayer.createFromGeoJson(mUri, progressor);
            } catch (IOException | JSONException | SQLiteException | NGException | ClassCastException e) {
                e.printStackTrace();
                if(null != progressor){
                    progressor.setMessage(e.getLocalizedMessage());
                }
                notifyError(mProgressMessage);
            }
        }
    }

    private class UnzipForm extends LayerFillTask {

        public UnzipForm(Bundle bundle) {
            super(bundle);
        }

        @Override
        public void execute(IProgressor progressor) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(mUri);
                if (inputStream != null) {
                    int nSize = inputStream.available();
                    int nIncrement = 0;
                    byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
                    progressor.setMax(nSize);
                    progressor.setMessage(getString(R.string.message_loading));

                    ZipInputStream zis = new ZipInputStream(inputStream);
                    ZipEntry ze;
                    while ((ze = zis.getNextEntry()) != null) {
                        if (isCanceled())
                            return;

                        FileUtil.unzipEntry(zis, ze, buffer, mLayerPath);
                        nIncrement += ze.getSize();
                        zis.closeEntry();
                        progressor.setValue(nIncrement);
                    }
                    zis.close();

                    //read meta.json
                    File meta = new File(mLayerPath, NGFP_FILE_META);
                    String jsonText = FileUtil.readFromFile(meta);
                    JSONObject metaJson = new JSONObject(jsonText);
                    File dataFile = new File(mLayerPath, NGFP_FILE_DATA);
                    Bundle extra = new Bundle();
                    extra.putSerializable(KEY_LAYER_PATH, mLayerPath);
                    extra.putString(KEY_NAME, mLayerName);

                    //read if this local o remote source
                    boolean isNgwConnection = metaJson.has("ngw_connection") && !metaJson.isNull("ngw_connection");
                    if (isNgwConnection) {
                        FileUtil.deleteRecursive(dataFile);
                        JSONObject connection = metaJson.getJSONObject("ngw_connection");

                        //read url
                        String url = connection.getString("url");
                        if (!url.startsWith("http")) {
                            url = "http://" + url;
                        }

                        //read login
                        String login = connection.getString("login");
                        //read password
                        String password = connection.getString("password");
                        //read id
                        long resourceId = connection.getLong("id");
                        //check account exist and try to create

                        FileUtil.deleteRecursive(meta);

                        String accountName = "";
                        URI uri = new URI(url);

                        if (uri.getHost() != null && uri.getHost().length() > 0) {
                            accountName += uri.getHost();
                        }
                        if (uri.getPort() != 80 && uri.getPort() > 0) {
                            accountName += ":" + uri.getPort();
                        }
                        if (uri.getPath() != null && uri.getPath().length() > 0) {
                            accountName += uri.getPath();
                        }

                        IGISApplication app = (IGISApplication) getApplicationContext();
                        Account account = app.getAccount(accountName);
                        if (null == account) {
                            //create account
                            if (!app.addAccount(accountName, url, login, password, "ngw")) {
                                throw new AccountsException(getString(R.string.account_already_exists));
                            }
                        } else {
                            //compare login/password and report differences
                            boolean same = app.getAccountPassword(account).equals(password) &&
                                    app.getAccountLogin(account).equals(login);
                            if (!same) {
                                Intent msg = new Intent(ConstantsUI.MESSAGE_INTENT);
                                msg.putExtra(ConstantsUI.KEY_MESSAGE, getString(R.string.warning_different_credentials));
                                sendBroadcast(msg);
                            }
                        }

                        extra.putLong(KEY_REMOTE_ID, resourceId);
                        extra.putString(KEY_ACCOUNT, accountName);

                        if (!isCanceled())
                            mQueue.add(new NGWVectorLayerFillTask(extra));
                    } else {
                        // prevent overwrite meta.json by layer save routine
                        meta.renameTo(new File(meta.getParentFile(), LayerFillService.NGFP_META));

                        extra.putSerializable(LayerFillService.KEY_PATH, dataFile);
                        extra.putBoolean(LayerFillService.KEY_DELETE_SRC_FILE, true);

                        if (!isCanceled())
                            mQueue.add(new VectorLayerFormFillTask(extra));
                    }
                }
            } catch (AccountsException | JSONException | IOException | URISyntaxException | SecurityException e) {
                e.printStackTrace();
                if(null != progressor){
                    progressor.setMessage(e.getLocalizedMessage());
                }
                notifyError(mProgressMessage);
            }
        }
    }

    private class VectorLayerFormFillTask extends LayerFillTask {
        protected File mPath;
        protected boolean mDeletePath;

        public VectorLayerFormFillTask(Bundle bundle) {
            super(bundle);
            mPath = (File) bundle.getSerializable(KEY_PATH);
            mDeletePath = bundle.getBoolean(KEY_DELETE_SRC_FILE, false);
            mLayer = new VectorLayerUI(mLayerGroup.getContext(), mLayerPath);
            initLayer();
        }

        @Override
        public void execute(IProgressor progressor) {
            try {
                VectorLayer vectorLayer = (VectorLayer) mLayer;
                if (null == vectorLayer)
                    return;
                File meta = new File(mPath.getParentFile(), NGFP_META);

                if (meta.exists()) {
                    String jsonText = FileUtil.readFromFile(meta);
                    JSONObject metaJson = new JSONObject(jsonText);
                    //read fields
                    List<Field> fields =
                            NGWUtil.getFieldsFromJson(metaJson.getJSONArray(NGWUtil.NGWKEY_FIELDS));
                    //read geometry type
                    String geomTypeString = metaJson.getString("geometry_type");
                    int geomType = GeoGeometryFactory.typeFromString(geomTypeString);
                    vectorLayer.create(geomType, fields);

                    if (GeoJSONUtil.isGeoJsonHasFeatures(mPath)) {
                        //read SRS -- not need as we will be fill layer with 3857
                        JSONObject srs = metaJson.getJSONObject("srs");
                        int nSRS = srs.getInt(NGWUtil.NGWKEY_ID);
                        vectorLayer.fillFromGeoJson(mPath, nSRS, progressor);
                    }

                    FileUtil.deleteRecursive(meta);
                } else
                    vectorLayer.createFromGeoJson(mPath, progressor);
            } catch (IOException | JSONException | SQLiteException | NGException | ClassCastException e) {
                e.printStackTrace();
                if (null != progressor) {
                    progressor.setMessage(e.getLocalizedMessage());
                }
                notifyError(mProgressMessage);
            }

            if (mDeletePath)
                FileUtil.deleteRecursive(mPath);
        }
    }

    private class LocalTMSFillTask extends LayerFillTask{
        public LocalTMSFillTask(Bundle bundle) {
            super(bundle);
            mLayer = new LocalTMSLayerUI(mLayerGroup.getContext(), mLayerPath);
            ((LocalTMSLayerUI) mLayer).setTMSType(bundle.getInt(KEY_TMS_TYPE));
            initLayer();
        }

        @Override
        public void execute(IProgressor progressor) {
            try {
                TMSLayer tmsLayer = (TMSLayer) mLayer;
                if(null == tmsLayer)
                    return;
                tmsLayer.fillFromZip(mUri, progressor);
            } catch (IOException | NumberFormatException | SecurityException | NGException | ClassCastException e) {
                e.printStackTrace();
                if(null != progressor){
                    progressor.setMessage(e.getLocalizedMessage());
                }
                notifyError(mProgressMessage);
            }
        }
    }

    private class NGWVectorLayerFillTask extends LayerFillTask{
        public NGWVectorLayerFillTask(Bundle bundle) {
            super(bundle);
            mLayer = new NGWVectorLayerUI(mLayerGroup.getContext(), mLayerPath);
            ((NGWVectorLayerUI) mLayer).setRemoteId(bundle.getLong(KEY_REMOTE_ID));
            ((NGWVectorLayerUI) mLayer).setAccountName(bundle.getString(KEY_ACCOUNT));
            initLayer();
        }

        @Override
        public void execute(IProgressor progressor) {
            try {
                NGWVectorLayer ngwVectorLayer = (NGWVectorLayer) mLayer;
                if(null == ngwVectorLayer)
                    return;
                ngwVectorLayer.createFromNGW(progressor);
            } catch (JSONException | IOException | SQLiteException | NGException | ClassCastException e) {
                e.printStackTrace();
                if(null != progressor){
                    progressor.setMessage(e.getLocalizedMessage());
                }
                notifyError(mProgressMessage);
            }
        }
    }
}

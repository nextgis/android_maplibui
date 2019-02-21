/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2019 NextGIS, info@nextgis.com
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
import android.os.Build;
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
import com.nextgis.maplib.map.NGWLookupTable;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.TMSLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.GeoJSONUtil;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.mapui.LocalTMSLayerUI;
import com.nextgis.maplibui.mapui.NGWVectorLayerUI;
import com.nextgis.maplibui.mapui.VectorLayerUI;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.LayerUtil;
import com.nextgis.maplibui.util.NotificationHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.nextgis.maplibui.util.NotificationHelper.createBuilder;

/**
 * Service for filling layers with data
 */
public class LayerFillService extends Service implements IProgressor {
    protected NotificationManager mNotifyManager;
    protected List<LayerFillTask> mQueue;
    protected static final int FILL_NOTIFICATION_ID = 9;
    protected NotificationCompat.Builder mBuilder;
    protected String mNotifyTitle;

    public final static int VECTOR_LAYER           = 1;
    public final static int VECTOR_LAYER_WITH_FORM = 2;
    public final static int TMS_LAYER              = 3;
    public final static int NGW_LAYER              = 4;

    public static final String ACTION_STOP = "com.nextgis.maplibui.FILL_LAYER_STOP";
    public static final String ACTION_ADD_TASK = "com.nextgis.maplibui.ADD_FILL_LAYER_TASK";
    public static final String ACTION_SHOW = "com.nextgis.maplibui.SHOW_PROGRESS_DIALOG";
    public static final String ACTION_UPDATE = "com.nextgis.maplibui.UPDATE_FILL_LAYER_PROGRESS";
    public static final String KEY_STATUS = "status";
    public static final String KEY_PROGRESS = "progress";
    public static final String KEY_TOTAL = "count";
    public static final String KEY_TITLE = "title";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_CANCELLED = "cancel";
    public static final String KEY_RESULT = "result";
    public static final String KEY_SYNC = "sync";
    public static final String KEY_URI = "uri";
    public static final String KEY_PATH = "path";
    public static final String KEY_LAYER_PATH = "layer_path";
    public static final String KEY_MIN_ZOOM = "min_zoom";
    public static final String KEY_MAX_ZOOM = "max_zoom";
    public static final String KEY_VISIBLE = "visible";
    public static final String KEY_REMOTE_ID = "remote_id";
    public static final String KEY_LOOKUP_ID = "lookup_id";
    public static final String KEY_ACCOUNT = "account";
    public static final String KEY_NAME = "name";
    public static final String KEY_INPUT_TYPE = "input_type";
    public static final String KEY_DELETE_SRC_FILE = "delete_source_file";
    public static final String KEY_LAYER_GROUP_ID = "layer_group_id";
    public static final String KEY_TMS_TYPE   = "tms_type";
    public static final String KEY_TMS_CACHE   = "tms_cache";
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
        int icon = R.drawable.ic_notification_download;
        Bitmap largeIcon = NotificationHelper.getLargeIcon(icon, getResources());

        mProgressIntent = new Intent(ACTION_UPDATE);
        Intent intent = new Intent(this, LayerFillService.class);
        intent.setAction(ACTION_STOP);
        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent stop = PendingIntent.getService(this, 0, intent, flag);
        intent.setAction(ACTION_SHOW);
        PendingIntent show = PendingIntent.getService(this, 0, intent, flag);

        mBuilder = createBuilder(this, R.string.start_fill_layer);
        mBuilder.setSmallIcon(icon).setLargeIcon(largeIcon)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(show)
                .addAction(R.drawable.ic_action_cancel_dark, getString(R.string.tracks_stop), stop);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String title = getString(R.string.start_fill_layer);
            mBuilder.setWhen(System.currentTimeMillis()).setContentTitle(title).setTicker(title);
            startForeground(FILL_NOTIFICATION_ID, mBuilder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LayerFillService", "Received start id " + startId + ": " + intent);
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && !TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_ADD_TASK:
                        int layerGroupId = intent.getIntExtra(KEY_LAYER_GROUP_ID, Constants.NOT_FOUND);
                        mLayerGroup = (LayerGroup) MapBase.getInstance().getLayerById(layerGroupId);

                        int layerType = intent.getIntExtra(KEY_INPUT_TYPE, Constants.NOT_FOUND);
                        Bundle extra = intent.getExtras();

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
                        startNextTask();
                        break;
                    case ACTION_SHOW:
                        mProgressIntent.putExtra(KEY_STATUS, STATUS_SHOW).putExtra(KEY_TITLE, mNotifyTitle);
                        sendBroadcast(mProgressIntent);
                        break;
                }
            }
        }
        return START_STICKY;
    }

    protected void startNextTask(){
        if (mQueue.isEmpty()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                stopForeground(true);
            else
                mNotifyManager.cancel(FILL_NOTIFICATION_ID);

            stopSelf();
            return;
        }

        mIsRunning = true;
        final  IProgressor progressor = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mIsCanceled)
                    return;

                LayerFillTask task = mQueue.remove(0);
                mNotifyTitle = task.getDescription();

                mBuilder.setWhen(System.currentTimeMillis())
                        .setContentTitle(mNotifyTitle)
                        .setTicker(mNotifyTitle);
                mNotifyManager.notify(FILL_NOTIFICATION_ID, mBuilder.build());

                if (mProgressIntent.getExtras() != null)
                    mProgressIntent.getExtras().clear();

                mProgressIntent.putExtra(KEY_STATUS, STATUS_START).putExtra(KEY_TITLE, mNotifyTitle);
                sendBroadcast(mProgressIntent);

                Process.setThreadPriority(Constants.DEFAULT_DOWNLOAD_THREAD_PRIORITY);
                progressor.setValue(0);
                boolean result = task.execute(progressor);

                if (!(task instanceof UnzipForm))
                    mProgressIntent.putExtra(KEY_MESSAGE, mProgressMessage);

                mProgressIntent.putExtra(KEY_STATUS, STATUS_STOP);
                mProgressIntent.putExtra(KEY_CANCELLED, mIsCanceled);
                mProgressIntent.putExtra(KEY_RESULT, result && !mIsCanceled);
                mProgressIntent.putExtra(KEY_TOTAL, mQueue.size());

                if (result) {
                    mLayerGroup.addLayer(task.getLayer());
                    mLayerGroup.save();
                } else
                    task.cancel();

                if (task instanceof NGWVectorLayerFillTask) {
                    mProgressIntent.putExtra(KEY_SYNC, ((NGWVectorLayerFillTask) task).showSyncDialog());
                    mProgressIntent.putExtra(KEY_ACCOUNT, ((NGWVectorLayerFillTask) task).getAccountName());
                    mProgressIntent.putExtra(KEY_REMOTE_ID, task.getLayer().getId());
                }

                sendBroadcast(mProgressIntent);
                mProgressIntent.removeExtra(KEY_STATUS);
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

        if (mProgressIntent.getExtras() != null)
            mProgressIntent.getExtras().clear();
        if (mProgressMessage != null)
            mProgressIntent.putExtra(KEY_MESSAGE, mProgressMessage);
        else
            mProgressIntent.removeExtra(KEY_MESSAGE);

        mProgressIntent.putExtra(KEY_STATUS, STATUS_UPDATE).putExtra(KEY_TOTAL, mProgressMax)
                .putExtra(KEY_PROGRESS, mProgressValue);
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
        String mLayerName;
        File mLayerPath;
        float mMinZoom, mMaxZoom;
        boolean mVisible;
        Uri mUri;
        protected Layer mLayer;

        LayerFillTask(Bundle bundle) {
            mUri = bundle.getParcelable(KEY_URI);
            mLayerName = bundle.getString(KEY_NAME);
            mLayerPath = bundle.containsKey(KEY_LAYER_PATH) ?
                    (File) bundle.getSerializable(KEY_LAYER_PATH) :
                    mLayerGroup.createLayerStorage();
            mMinZoom = bundle.getFloat(KEY_MIN_ZOOM, GeoConstants.DEFAULT_MIN_ZOOM);
            mMaxZoom = bundle.getFloat(KEY_MAX_ZOOM, GeoConstants.DEFAULT_MAX_ZOOM);
            mVisible = bundle.getBoolean(KEY_VISIBLE, true);
        }

        void initLayer() {
            mLayer.setName(mLayerName);
            mLayer.setVisible(mVisible);
            mLayer.setMinZoom(mMinZoom);
            mLayer.setMaxZoom(mMaxZoom);
        }

        public abstract boolean execute(IProgressor progressor);

        public String getDescription(){
            if(null == mLayer)
                return "";
            return getString(R.string.processing) + " " + mLayer.getName();
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

        void setError(Exception e, IProgressor progressor) {
            String logMsg = e.getLocalizedMessage();
            if (null != logMsg) {
                if (null != progressor)
                    progressor.setMessage(logMsg);
                setMessage(logMsg);
            }
        }
    }

    private class VectorLayerFillTask extends LayerFillTask{
        VectorLayerFillTask(Bundle bundle) {
            super(bundle);
            mLayer = new VectorLayerUI(mLayerGroup.getContext(), mLayerPath);
            initLayer();
        }

        @Override
        public boolean execute(IProgressor progressor) {
            try {
                VectorLayer vectorLayer = (VectorLayer) mLayer;
                if(null == vectorLayer)
                    return false;

                vectorLayer.createFromGeoJson(mUri, progressor);
            } catch (IOException | JSONException | SQLiteException | NGException | ClassCastException e) {
                e.printStackTrace();
                setError(e, progressor);
                notifyError(mProgressMessage);
                return false;
            }

            return true;
        }
    }

    private class UnzipForm extends LayerFillTask {
        boolean mSync;
        long mRemoteId;
        String mAccount;

        UnzipForm(Bundle bundle) {
            super(bundle);
            mSync = bundle.getBoolean(KEY_SYNC, true);
            mRemoteId = bundle.getLong(KEY_REMOTE_ID, -1);
            mAccount = bundle.getString(KEY_ACCOUNT, "");
        }

        @Override
        public boolean execute(IProgressor progressor) {
            try {
                InputStream inputStream;
                String url = mUri.toString();
                if (NetworkUtil.isValidUri(url)) {
                    URLConnection connection = new URL(url).openConnection();
                    try {
                        AccountUtil.AccountData accountData = AccountUtil.getAccountData(getApplicationContext(), mAccount);
                        String basicAuth = NetworkUtil.getHTTPBaseAuth(accountData.login, accountData.password);
                        if (null != basicAuth)
                            connection.setRequestProperty ("Authorization", basicAuth);
                    } catch (IllegalStateException ignored) {}
//                    inputStream = new URL(url).openStream();
                    inputStream = connection.getInputStream();
                } else
                    inputStream = getContentResolver().openInputStream(mUri);

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
                            return false;

                        FileUtil.unzipEntry(zis, ze, buffer, mLayerPath);
                        nIncrement += ze.getSize();
                        zis.closeEntry();
                        progressor.setValue(nIncrement);
                    }
                    zis.close();
                    progressor.setMessage(null);

                    //read meta.json
                    File meta = new File(mLayerPath, NGFP_FILE_META);
                    // prevent overwrite meta.json by layer save routine
                    //noinspection ResultOfMethodCallIgnored
                    meta.renameTo(meta = new File(meta.getParentFile(), LayerFillService.NGFP_META));
                    String jsonText = FileUtil.readFromFile(meta);
                    JSONObject metaJson = new JSONObject(jsonText);
                    File dataFile = new File(mLayerPath, NGFP_FILE_DATA);
                    Bundle extra = new Bundle();
                    extra.putSerializable(KEY_LAYER_PATH, mLayerPath);
                    extra.putString(KEY_NAME, mLayerName);

                    long resourceId = mRemoteId;
                    String accountName = mAccount;
                    //read if this local or remote source
                    boolean isNgwConnection = !metaJson.isNull(ConstantsUI.JSON_NGW_CONNECTION_KEY);
                    if (isNgwConnection) {
                        JSONObject connection = metaJson.getJSONObject(ConstantsUI.JSON_NGW_CONNECTION_KEY);
                        //read url
                        url = connection.getString("url");
                        if (!url.startsWith("http")) {
                            url = "http://" + url;
                        }
                        //read login
                        String login = connection.getString("login");
                        //read password
                        String password = connection.getString("password");
                        //read id
                        resourceId = connection.getLong("id");

                        metaJson.remove(ConstantsUI.JSON_NGW_CONNECTION_KEY);
                        FileUtil.writeToFile(meta, metaJson.toString());

                        //check account exist and try to create
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
                                throw new AccountsException(getString(R.string.ngw_account_already_exists));
                            }
                        } else {
                            //compare login/password and report differences
                            String savedPassword = app.getAccountPassword(account);
                            String savedLogin = app.getAccountLogin(account);
                            boolean same = false;
                            if (savedPassword != null && savedLogin != null)
                                same = savedPassword.equals(password) && savedLogin.equals(login);
                            else {
                                if (savedLogin == null)
                                    same = login == null;
                                if (savedPassword == null)
                                    same = password == null;
                            }

                            if (!same) {
                                Intent msg = new Intent(ConstantsUI.MESSAGE_INTENT);
                                msg.putExtra(ConstantsUI.KEY_MESSAGE, getString(R.string.ngw_different_credentials));
                                sendBroadcast(msg);
                            }
                        }
                    }

                    isNgwConnection = isNgwConnection || mRemoteId > -1;
                    if (isNgwConnection) {
                        FileUtil.deleteRecursive(dataFile);
                        File form = new File(mLayerPath, ConstantsUI.FILE_FORM);
                        ArrayList<String> lookupTableIds = LayerUtil.fillLookupTableIds(form);

                        extra.putStringArrayList(KEY_LOOKUP_ID, lookupTableIds);
                        extra.putLong(KEY_REMOTE_ID, resourceId);
                        extra.putString(KEY_ACCOUNT, accountName);
                        extra.putBoolean(KEY_SYNC, mSync);

                        if (!isCanceled())
                            mQueue.add(new NGWVectorLayerFillTask(extra));
                    } else {
                        extra.putSerializable(LayerFillService.KEY_PATH, dataFile);
                        extra.putBoolean(LayerFillService.KEY_DELETE_SRC_FILE, true);

                        if (!isCanceled())
                            mQueue.add(new VectorLayerFormFillTask(extra));
                    }
                }
            } catch (AccountsException | JSONException | IOException | URISyntaxException | RuntimeException e) {
                e.printStackTrace();
                setError(e, progressor);
                notifyError(mProgressMessage);
                return false;
            }

            return true;
        }
    }

    private class VectorLayerFormFillTask extends LayerFillTask {
        File mPath;
        boolean mDeletePath;

        VectorLayerFormFillTask(Bundle bundle) {
            super(bundle);
            mPath = (File) bundle.getSerializable(KEY_PATH);
            mDeletePath = bundle.getBoolean(KEY_DELETE_SRC_FILE, false);
            mLayer = new VectorLayerUI(mLayerGroup.getContext(), mLayerPath);
            initLayer();
        }

        @Override
        public boolean execute(IProgressor progressor) {
            try {
                VectorLayer vectorLayer = (VectorLayer) mLayer;
                if (null == vectorLayer)
                    return false;
                File meta = new File(mPath.getParentFile(), NGFP_META);

                if (meta.exists()) {
                    String jsonText = FileUtil.readFromFile(meta);
                    JSONObject metaJson = new JSONObject(jsonText);
                    //read fields
                    List<Field> fields = NGWUtil.getFieldsFromJson(metaJson.getJSONArray(NGWUtil.NGWKEY_FIELDS));
                    //read geometry type
                    String geomTypeString = metaJson.getString("geometry_type");
                    int geomType = GeoGeometryFactory.typeFromString(geomTypeString);
                    vectorLayer.create(geomType, fields);

                    if (GeoJSONUtil.isGeoJsonHasFeatures(mPath)) {
                        //read SRS -- not need as we will be fill layer with 3857
                        JSONObject srs = metaJson.getJSONObject(NGWUtil.NGWKEY_SRS);
                        int nSRS = srs.getInt(NGWUtil.NGWKEY_ID);
                        vectorLayer.fillFromGeoJson(mPath, nSRS, progressor);
                    }
                } else
                    vectorLayer.createFromGeoJson(mPath, progressor); // should never get there
            } catch (IOException | JSONException | SQLiteException | NGException | ClassCastException e) {
                e.printStackTrace();
                setError(e, progressor);
                notifyError(mProgressMessage);
                return false;
            }

            if (mDeletePath)
                FileUtil.deleteRecursive(mPath);

            return true;
        }
    }

    private class LocalTMSFillTask extends LayerFillTask{
        boolean mIsNgrc;

        LocalTMSFillTask(Bundle bundle) {
            super(bundle);
            mLayer = new LocalTMSLayerUI(mLayerGroup.getContext(), mLayerPath);
            mIsNgrc = !bundle.containsKey(KEY_TMS_TYPE);
            ((LocalTMSLayerUI) mLayer).setCacheSizeMultiply(bundle.getInt(KEY_TMS_CACHE));

            if (!mIsNgrc) { // it's zip
                ((LocalTMSLayerUI) mLayer).setTMSType(bundle.getInt(KEY_TMS_TYPE));
                initLayer();
            } else
                mLayerName = mUri.getLastPathSegment();
        }

        @Override
        public boolean execute(IProgressor progressor) {
            try {
                TMSLayer tmsLayer = (TMSLayer) mLayer;
                if (null == tmsLayer)
                    return false;

                if (mIsNgrc)
                    tmsLayer.fillFromNgrc(mUri, progressor);
                else
                    tmsLayer.fillFromZip(mUri, progressor);
            } catch (IOException | NGException | RuntimeException e) {
                e.printStackTrace();
                setError(e, progressor);
                notifyError(mProgressMessage);
                return false;
            }

            return true;
        }

        @Override
        public String getDescription() {
            return mIsNgrc ? mLayerName : super.getDescription();
        }
    }

    private class NGWVectorLayerFillTask extends LayerFillTask{
        private ArrayList<String> mLookupIds = new ArrayList<>();
        private boolean mShowSyncDialog;

        NGWVectorLayerFillTask(Bundle bundle) {
            super(bundle);
            mLayer = new NGWVectorLayerUI(mLayerGroup.getContext(), mLayerPath);
            ((NGWVectorLayerUI) mLayer).setRemoteId(bundle.getLong(KEY_REMOTE_ID));
            ((NGWVectorLayerUI) mLayer).setAccountName(bundle.getString(KEY_ACCOUNT));
            initLayer();

            if (bundle.containsKey(KEY_LOOKUP_ID))
                mLookupIds = bundle.getStringArrayList(KEY_LOOKUP_ID);

            mShowSyncDialog = bundle.getBoolean(KEY_SYNC, false);

            for (int i = 0; i < mLayerGroup.getLayerCount(); i++) {
                if (mLayerGroup.getLayer(i) instanceof NGWLookupTable) {
                    NGWLookupTable table = (NGWLookupTable) mLayerGroup.getLayer(i);
                    String id = table.getRemoteId() + "";
                    if (table.getAccountName().equals(bundle.getString(KEY_ACCOUNT)) && mLookupIds.contains(id))
                        mLookupIds.remove(id);
                }
            }
        }

        @Override
        public boolean execute(IProgressor progressor) {
            try {
                NGWVectorLayer ngwVectorLayer = (NGWVectorLayer) mLayer;
                if (null == ngwVectorLayer)
                    return false;

                for (String id : mLookupIds) {
                    NGWLookupTable table = new NGWLookupTable(mLayer.getContext(), mLayerGroup.createLayerStorage());
                    table.setAccountName(((NGWVectorLayer) mLayer).getAccountName());
                    table.setRemoteId(Long.parseLong(id));
                    table.setSyncType(Constants.SYNC_ALL);
                    table.setName(getText(R.string.layer_lookuptable) + " #" + id);
                    table.fillFromNGW(null);
                    mLayerGroup.addLayer(table);
                }

                ngwVectorLayer.createFromNGW(progressor);
            } catch (JSONException | IOException | SQLiteException | NGException | ClassCastException e) {
                e.printStackTrace();
                setError(e, progressor);
                notifyError(mProgressMessage);
                return false;
            }

            return true;
        }

        boolean showSyncDialog() {
            return mShowSyncDialog;
        }

        String getAccountName() {
            return ((NGWVectorLayerUI) mLayer).getAccountName();
        }
    }
}

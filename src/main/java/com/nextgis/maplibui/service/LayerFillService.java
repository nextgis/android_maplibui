package com.nextgis.maplibui.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.TMSLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoJSONUtil;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.ConstantsUI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Service for filling layers with data
 */
public class LayerFillService extends Service implements IProgressor {
    protected NotificationManager mNotifyManager;
    protected List<LayerFillTask> mQueue;
    protected static final int FILL_NOTIFICATION_ID = 9;
    protected NotificationCompat.Builder mBuilder;

    public static final String ACTION_STOP = "FILL_LAYER_STOP";
    public static final String ACTION_ADD_TASK = "ADD_FILL_LAYER_TASK";
    public static final String ACTION_SHOW = "SHOW_PROGRESS_DIALOG";
    public static final String ACTION_UPDATE = "UPDATE_FILL_LAYER_PROGRESS";
    public static final String KEY_STATUS = "status";
    public static final String KEY_PROGRESS = "progress";
    public static final String KEY_TOTAL = "count";
    public static final String KEY_TITLE = "title";
    public static final String KEY_TEXT = "message";
    public static final String KEY_URI = "uri";
    public static final String KEY_PATH = "path";
    public static final String KEY_INPUT_TYPE = "input_type";
    public static final String KEY_DELETE_SRC_FILE = "delete_source_file";
    public static final String NGFP_META = "ngfp_meta.json";

    public static final short STATUS_START = 0;
    public static final short STATUS_UPDATE = 1;
    public static final short STATUS_STOP = 2;
    public static final short STATUS_SHOW = 3;

    protected int mProgressMax;
    protected int mProgressValue;
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
        Bitmap largeIcon =
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_notification_download);

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
                        int layerId = intent.getIntExtra(ConstantsUI.KEY_LAYER_ID, Constants.NOT_FOUND);
                        int layerType = intent.getIntExtra(LayerFillService.KEY_INPUT_TYPE, Constants.NOT_FOUND);
                        Uri uri = intent.getParcelableExtra(LayerFillService.KEY_URI);
                        String sPath = intent.getStringExtra(LayerFillService.KEY_PATH);
                        File path = null;
                        if(!TextUtils.isEmpty(sPath))
                            path = new File(sPath);
                        boolean deleteSourceFile = intent.getBooleanExtra(LayerFillService.KEY_DELETE_SRC_FILE, false);

                        if(layerType == Constants.LAYERTYPE_LOCAL_VECTOR){
                            if(null != uri)
                                addVectorLayerTask(layerId, uri);
                            else if(null != path)
                                addVectorLayerTask(layerId, path, deleteSourceFile);
                        }
                        else if(layerType == Constants.LAYERTYPE_NGW_VECTOR){
                            addNGWVectorLayerTask(layerId);
                        }
                        else if(layerType == Constants.LAYERTYPE_LOCAL_TMS){
                            addTMSTask(layerId, uri);
                        }

                        if(!mIsRunning){
                            startNextTask();
                        }

                        return START_STICKY;
                    case ACTION_STOP:
                        mIsCanceled = true;
                        mQueue.clear();
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

    private void addTMSTask(int layerId, Uri uri) {
        mQueue.add(new LocalTMSFillTask(layerId, uri));
    }

    private void addNGWVectorLayerTask(int layerId) {
        mQueue.add(new NGWVectorLayerFillTask(layerId));
    }

    private void addVectorLayerTask(int layerId, Uri uri) {
        mQueue.add(new VectorLayerFillTask(layerId, uri));
    }

    private void addVectorLayerTask(int layerId, File path, boolean deleteSource) {
        mQueue.add(new VectorLayerFileFillTask(layerId, path, deleteSource));
    }

    protected void startNextTask(){
        if(mQueue.isEmpty()){
            mNotifyManager.cancel(FILL_NOTIFICATION_ID);
            mProgressIntent.putExtra(KEY_STATUS, STATUS_STOP);
            sendBroadcast(mProgressIntent);
            stopSelf();
            return;
        }

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

                android.os.Process.setThreadPriority( Constants.DEFAULT_DOWNLOAD_THREAD_PRIORITY );
                task.execute(progressor);

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
        mBuilder.setProgress(mProgressMax, mProgressValue, mIndeterminate)
                .setContentText(mProgressMessage);
        // Displays the progress bar for the first time.
        mNotifyManager.notify(FILL_NOTIFICATION_ID, mBuilder.build());
        mProgressIntent.putExtra(KEY_STATUS, STATUS_UPDATE).putExtra(KEY_TOTAL, mProgressMax)
                .putExtra(KEY_PROGRESS, mProgressValue).putExtra(KEY_TEXT, mProgressMessage);
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
        protected Uri mUri;
        protected ILayer mLayer;

        public LayerFillTask(int layerId, Uri uri) {
            mUri = uri;

            MapBase map = MapBase.getInstance();
            if(null != map){
                mLayer = map.getLayerById(layerId);
            }
        }

        public abstract void execute(IProgressor progressor);

        public String getDescription(){
            if(null == mLayer)
                return "";
            return getString(R.string.proceed_layer) + " " + mLayer.getName();
        }
    }

    private class VectorLayerFillTask extends LayerFillTask{

        public VectorLayerFillTask(int layerId, Uri uri) {
            super(layerId, uri);
        }

        @Override
        public void execute(IProgressor progressor) {
            VectorLayer vectorLayer = (VectorLayer) mLayer;
            if(null == vectorLayer)
                return;
            try {
                vectorLayer.createFromGeoJson(mUri, progressor);
            } catch (IOException | JSONException | SQLiteException | NGException e) {
                e.printStackTrace();
                if(null != progressor){
                    progressor.setMessage(e.getLocalizedMessage());
                }
                notifyError(mProgressMessage);
            }
        }
    }

    private class VectorLayerFileFillTask extends LayerFillTask {

        protected File mPath;
        protected boolean mDeletePath;

        public VectorLayerFileFillTask(int layerId, File path, boolean deletePath) {
            super(layerId, null);
            mPath = path;
            mDeletePath = deletePath;
        }

        @Override
        public void execute(IProgressor progressor) {
            VectorLayer vectorLayer = (VectorLayer) mLayer;
            if (null == vectorLayer)
                return;
            File meta = new File(mPath.getParentFile(), NGFP_META);
            try {
                if (GeoJSONUtil.isGeoJsonHasFeatures(mPath)) {
                    if (meta.exists()) {
                        String jsonText = FileUtil.readFromFile(meta);
                        JSONObject metaJson = new JSONObject(jsonText);
                        //read fields
                        List<Field> fields =
                                NGWUtil.getFieldsFromJson(metaJson.getJSONArray(NGWUtil.NGWKEY_FIELDS));
                        //read geometry type
                        String geomTypeString = metaJson.getString("geometry_type");
                        int geomType = GeoGeometryFactory.typeFromString(geomTypeString);

                        //read SRS -- not need as we will be fill layer with 3857
                        JSONObject srs = metaJson.getJSONObject("srs");
                        int nSRS = srs.getInt(NGWUtil.NGWKEY_ID);

                        vectorLayer.create(geomType, fields);
                        vectorLayer.fillFromGeoJson(mPath, nSRS, progressor);

                        FileUtil.deleteRecursive(meta);
                    } else {
                        vectorLayer.createFromGeoJson(mPath, progressor);
                        if (mDeletePath)
                            FileUtil.deleteRecursive(mPath);
                    }
                } else {
                    String jsonText = FileUtil.readFromFile(meta);
                    JSONObject metaJson = new JSONObject(jsonText);
                    //read fields
                    List<Field> fields =
                            NGWUtil.getFieldsFromJson(metaJson.getJSONArray(NGWUtil.NGWKEY_FIELDS));
                    //read geometry type
                    String geomTypeString = metaJson.getString("geometry_type");
                    int geomType = GeoGeometryFactory.typeFromString(geomTypeString);

                    vectorLayer.create(geomType, fields);

                    FileUtil.deleteRecursive(meta);
                }
            } catch (IOException | JSONException | SQLiteException | NGException e) {
                e.printStackTrace();
                if (null != progressor) {
                    progressor.setMessage(e.getLocalizedMessage());
                }
                notifyError(mProgressMessage);
            }
        }
    }

    private class LocalTMSFillTask extends LayerFillTask{

        public LocalTMSFillTask(int layerId, Uri uri) {
            super(layerId, uri);
        }

        @Override
        public void execute(IProgressor progressor) {
            TMSLayer tmsLayer = (TMSLayer) mLayer;
            if(null == tmsLayer)
                return;
            try {
                tmsLayer.fillFromZip(mUri, progressor);
            } catch (IOException | NumberFormatException | SecurityException | NGException e) {
                e.printStackTrace();
                if(null != progressor){
                    progressor.setMessage(e.getLocalizedMessage());
                }
                notifyError(mProgressMessage);
            }
        }
    }

    private class NGWVectorLayerFillTask extends LayerFillTask{
        public NGWVectorLayerFillTask(int layerId) {
            super(layerId, null);
        }

        @Override
        public void execute(IProgressor progressor) {
            NGWVectorLayer ngwVectorLayer = (NGWVectorLayer) mLayer;
            if(null == ngwVectorLayer)
                return;
            try {
                ngwVectorLayer.createFromNGW(progressor);
            } catch (JSONException | IOException | SQLiteException | NGException e) {
                e.printStackTrace();
                if(null != progressor){
                    progressor.setMessage(e.getLocalizedMessage());
                }
                notifyError(mProgressMessage);
            }
        }
    }
}

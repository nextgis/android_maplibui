package com.nextgis.maplibui.mapui;
import static android.content.Context.MODE_MULTI_PROCESS;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplib.util.SettingsConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackWorker  extends Worker {
    public static final String TEMP_PREFERENCES = "tracks_temp";
    private static final String TRACK_URI = "track_uri";
    public static final String ACTION_SYNC = "com.nextgis.maplibui.TRACK_SYNC";
    public static final String ACTION_STOP = "com.nextgis.maplibui.TRACK_STOP";
    private static final String ACTION_SPLIT = "com.nextgis.maplibui.TRACK_SPLIT";
    private static final int TRACK_NOTIFICATION_ID = 1;
    //    public static final String HOST = "http://dev.nextgis.com/tracker-dev1-hub";
    public static final String HOST = "https://track.nextgis.com";
    public static final String URL = "/ng-mobile";

    public TrackWorker(@NonNull Context context,
                     @NonNull WorkerParameters params){
        super(context, params);
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        WorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(TrackWorker.class)
                        .setConstraints(constraints)
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
            Log.d(Constants.TAG, "start worker doWork");
        sync();
        return Result.success();
    }

    private void sync() throws SQLiteException {


        String authority = ((IGISApplication)getApplicationContext()).getAuthority();
        String tracks = TrackLayer.TABLE_TRACKS;
        Uri mContentUriTracks = Uri.parse("content://" + authority + "/" + tracks);
        String pointsStr = TrackLayer.TABLE_TRACKPOINTS;
        Uri mContentUriTrackPoints = Uri.parse("content://" + authority + "/" + pointsStr);


        String name = getApplicationContext().getPackageName() + "_preferences";
        SharedPreferences mSharedPreferences = getApplicationContext().getSharedPreferences(name, MODE_MULTI_PROCESS);

        if (mSharedPreferences.getBoolean(SettingsConstants.KEY_PREF_TRACK_SEND, false)) {
            ContentResolver resolver = getApplicationContext().getContentResolver();
            String selection = TrackLayer.FIELD_SENT + " = 0";
            String sort = TrackLayer.FIELD_TIMESTAMP + " ASC";
            Cursor points = null;
            try {
                Log.d(Constants.TAG, "worker start point query");

                points = resolver.query(mContentUriTrackPoints, null, selection, null, sort);
            } catch (Exception ignored) {
                Log.d(Constants.TAG, "worker start point query EXCEPTION " + ignored.getMessage());
            }
            if (points != null) {
                Log.d(Constants.TAG, "worker points != null");

                List<String> ids = new ArrayList<>();
                if (points.moveToFirst()) {
                    Log.d(Constants.TAG, "worker points points.moveToFirst()");

                    GeoPoint point = new GeoPoint();
                    int lon = points.getColumnIndex(TrackLayer.FIELD_LON);
                    int lat = points.getColumnIndex(TrackLayer.FIELD_LAT);
                    int ele = points.getColumnIndex(TrackLayer.FIELD_ELE);
                    int fix = points.getColumnIndex(TrackLayer.FIELD_FIX);
                    int sat = points.getColumnIndex(TrackLayer.FIELD_SAT);
                    int acc = points.getColumnIndex(TrackLayer.FIELD_ACCURACY);
                    int speed = points.getColumnIndex(TrackLayer.FIELD_SPEED);
                    int time = points.getColumnIndex(TrackLayer.FIELD_TIMESTAMP);
                    JSONArray payload = new JSONArray();

                    int counter = 0;
                    do {
                        JSONObject item = new JSONObject();
                        try {
                            point.setCoordinates(points.getDouble(lon), points.getDouble(lat));
                            point.setCRS(GeoConstants.CRS_WEB_MERCATOR);
                            point.project(GeoConstants.CRS_WGS84);
                            item.put("lt", point.getY());
                            item.put("ln", point.getX());
                            item.put("ts", points.getLong(time) / 1000);
                            item.put("a", points.getDouble(ele));
                            item.put("s", points.getInt(sat));
                            item.put("ft", points.getString(fix).equals("3d") ? 3 : 2);
                            item.put("sp", points.getDouble(speed) * 18 / 5);
                            item.put("ha", points.getDouble(acc));
                            payload.put(item);
                            ids.add(points.getString(time));
                            counter++;

                            if (counter >= 100) {
                                Log.d(Constants.TAG, "worker points >= 100");

                                post(payload.toString(), getApplicationContext(), ids, mSharedPreferences, mContentUriTrackPoints);
                                payload = new JSONArray();
                                ids.clear();
                                counter = 0;
                            }
                        } catch (Exception ignored) {
                            Log.d(Constants.TAG, "worker EXCEPTION "  + ignored.getMessage());
                        }
                    } while (points.moveToNext());

                    if (counter > 0) {
                        try {
                            post(payload.toString(), getApplicationContext(), ids, mSharedPreferences, mContentUriTrackPoints);
                        } catch (Exception ignored) {
                            Log.d(Constants.TAG, "worker EXCEPTION "  + ignored.getMessage());
                        }
                    }
                }  else {
                    Log.d(Constants.TAG, "worker points NOT points.moveToFirst()");
                }
                points.close();
            }
        }
    }

    private void post(String payload, Context context, List<String> ids, SharedPreferences mSharedPreferences, Uri mContentUriTrackPoints) throws IOException {
        Log.d(Constants.TAG, "worker start post");

        String base = mSharedPreferences.getString("tracker_hub_url", HOST);
        String url = String.format("%s/%s/packet", base + URL, getUid(context));
//        Log.d(Constants.TAG, "Post to " + url);
        HttpResponse response = NetworkUtil.post(url, payload, null, null, false);
//        Log.d(Constants.TAG, "Response is " + response.getResponseCode());
        if (!response.isOk()) {
            Log.d(Constants.TAG, "worker post response is not ok " + response.getResponseBody());

            return;
        }

        ContentValues cv = new ContentValues();
        cv.put(TrackLayer.FIELD_SENT, 1);
        String where = TrackLayer.FIELD_TIMESTAMP + " in (" + MapUtil.makePlaceholders(ids.size()) + ")";
        String[] timestamps = ids.toArray(new String[0]);
        try {
            context.getContentResolver().update(mContentUriTrackPoints, cv, where, timestamps);
        } catch (SQLiteException ignored) {
            Log.d(Constants.TAG, "worker update sended points excaption " + ignored.getMessage());
        }
    }

    @SuppressLint("HardwareIds")
    public static String getUid(Context context) {
        String uuid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return String.format("%X", uuid.hashCode());
    }
}

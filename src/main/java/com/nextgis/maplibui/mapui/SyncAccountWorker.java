package com.nextgis.maplibui.mapui;

import static android.content.Context.MODE_MULTI_PROCESS;

import static com.nextgis.maplib.datasource.ngw.SyncAdapter.ACTION_LPATH;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.hypertrack.hyperlog.HyperLog;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.GISApplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SyncAccountWorker  extends Worker {



    public static final String ACTION_STOP = "com.nextgis.maplibui.TRACK_STOP";
    //public static final String URL = "/ng-mobile";

    public static final String WORKER_ACCOUNT_NAME = "account_name_worker";

    public SyncAccountWorker(@NonNull Context context,
                       @NonNull WorkerParameters params){
        super(context, params);
    }

    public static void removeSchedule(Context context, String accountName) {
        WorkManager.getInstance(context).cancelUniqueWork("sync_account_" + accountName);
    }

    public static void schedule(Context context, String accountName, long secUpdateInterval) {

        Log.e("SYNC2S", "schedule for " + accountName + " with " + secUpdateInterval);

        Data data = new Data.Builder()
                .putString(WORKER_ACCOUNT_NAME, accountName)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                            SyncAccountWorker.class,
                            secUpdateInterval,TimeUnit.SECONDS)
                        .setInputData(data)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        "sync_account_" + accountName,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        request);
    }

    @NonNull
    @Override
    public Result doWork() {
        String accountName = getInputData().getString(WORKER_ACCOUNT_NAME);
        Log.e("SYNC2S", "doWork " + accountName );

        if (TextUtils.isEmpty(accountName))
            return Result.failure();

        SyncResult syncResult = new SyncResult();
        SyncAdapter syncAdapter = new SyncAdapter(getApplicationContext(), true);

        // get account
        final AccountManager accountManager = AccountManager.get(getApplicationContext());
        final IGISApplication application = (IGISApplication) getApplicationContext();


        Account selectedAccount = null;
        for (Account account : accountManager.getAccountsByType(application.getAccountsType()))
            if (account.name.equals(accountName)){
                selectedAccount = account;
                break;
            }


        if (selectedAccount == null)
            return Result.failure();

        Bundle bundle = new Bundle();

        Log.e("SYNC2S", "doWork performSync for " + accountName );

        HyperLog.v(Constants.TAG, "xxx SyncAccountWorker syncAdapter.onPerformSync for" + selectedAccount.name );
        syncAdapter.onPerformSync(selectedAccount,
                    bundle,
                    ((GISApplication)getApplicationContext()).getAuthority(),
                    null, syncResult);
        return Result.success();
    }



}

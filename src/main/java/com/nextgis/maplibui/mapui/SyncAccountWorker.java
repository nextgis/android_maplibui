package com.nextgis.maplibui.mapui;

import static com.nextgis.maplib.datasource.ngw.SyncAdapter.SYNC_CANCELED;
import static com.nextgis.maplib.datasource.ngw.SyncAdapter.SYNC_CHANGES;
import static com.nextgis.maplib.datasource.ngw.SyncAdapter.SYNC_FINISH;
import static com.nextgis.maplib.datasource.ngw.SyncAdapter.SYNC_START;
import static com.nextgis.maplibui.util.NotificationHelper.createBuilder;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;

import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.hypertrack.hyperlog.HyperLog;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.util.NotificationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class SyncAccountWorker  extends Worker {

    private static final ReentrantLock LOCK = new ReentrantLock();

    public static final String ACTION_STOP = "com.nextgis.maplibui.TRACK_STOP";
    //public static final String URL = "/ng-mobile";

    public static final String WORKER_ACCOUNT_NAME = "account_name_worker";

    public static final String ALL_ACCOUNT_STRINGS = "allaccounts";

    public static final String SYNC_ALL = "account_sync_all"; // sync all - manual sync press

    public SyncAccountWorker(@NonNull Context context,
                       @NonNull WorkerParameters params){
        super(context, params);
    }

    public static void removeSchedule(Context context, String accountName) {
        WorkManager.getInstance(context).cancelUniqueWork("sync_account_" + accountName);
    }


    public static void scheduleOneTime(Context context) {

        Data data = new Data.Builder()
                .putString(WORKER_ACCOUNT_NAME, ALL_ACCOUNT_STRINGS)
                .putBoolean(SYNC_ALL, true)
                .build();

        OneTimeWorkRequest manualRequest =
                new OneTimeWorkRequest.Builder(SyncAccountWorker.class)
                        .setInputData(data)
                        //.setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        "my_worker",
                        ExistingWorkPolicy.KEEP,
                        manualRequest
                );
    }

    public static void schedulePeriodic(Context context, String accountName, long secUpdateInterval) {

        Log.d("SYNC2S", "schedulePeriodic for " + accountName + " with " + secUpdateInterval);

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

        if (!LOCK.tryLock()) {
            return Result.success();
        }

        try {
            String accountName = getInputData().getString(WORKER_ACCOUNT_NAME);
            Log.d("SYNC2S", "doWork " + accountName);

            if (TextUtils.isEmpty(accountName))
                return Result.failure();

            SyncResult syncResult = new SyncResult();
            SyncAdapter syncAdapter = new SyncAdapter(getApplicationContext(), true);

            // get account
            final AccountManager accountManager = AccountManager.get(getApplicationContext());
            final IGISApplication application = (IGISApplication) getApplicationContext();


            boolean syncAll = false;
            if (getInputData().hasKeyWithValueOfType(SYNC_ALL, Boolean.class)
                    && getInputData().getBoolean(SYNC_ALL, false))
                syncAll = true;

            List<Account> accountList = new ArrayList<>();

            for (Account account : accountManager.getAccountsByType(application.getAccountsType())) {
                if (syncAll)
                    accountList.add(account);
                else {
                    if (account.name.equals(accountName)) {
                        accountList.add(account);
                        break;
                    }
                }
            }

            if (accountList.size() == 0)
                return Result.failure();

            Bundle bundle = new Bundle();
            Log.d("SYNC2S", "doWork performSync for " + accountName);

            ((IGISApplication)getApplicationContext()).sendNotification(getApplicationContext(), SYNC_START, null);

            for (Account selectedAccount : accountList) {
                HyperLog.v(Constants.TAG, "xxx SyncAccountWorker syncAdapter.onPerformSync for" + selectedAccount.name);
                syncAdapter.onPerformSync(selectedAccount,
                        bundle,
                        ((GISApplication) getApplicationContext()).getAuthority(),
                        null, syncResult);
            }


            String mError = "";
            if (syncResult.stats.numIoExceptions > 0)
                mError += getApplicationContext().getString(com.nextgis.maplib.R.string.sync_error_io);
            if (syncResult.stats.numParseExceptions > 0) {
                if (mError.length() > 0)
                    mError += "\r\n";
                mError += getApplicationContext().getString(com.nextgis.maplib.R.string.sync_error_parse);
            }
            if (syncResult.stats.numAuthExceptions > 0) {
                if (mError.length() > 0)
                    mError += "\r\n";
                mError += getApplicationContext().getString(com.nextgis.maplib.R.string.error_auth_and_forbidden);
            }
            if (syncResult.stats.numConflictDetectedExceptions > 0) {
                if (mError.length() > 0)
                    mError += "\r\n";
                mError += getApplicationContext().getString(com.nextgis.maplib.R.string.sync_error_conflict);
            }
            if (syncResult.stats.numInserts > 0) {
                if (mError.length() > 0)
                    mError += "\r\n";
                mError += getApplicationContext().getString(com.nextgis.maplib.R.string.sync_error_insert);
            }
            if (syncResult.stats.numUpdates > 0) {
                if (mError.length() > 0)
                    mError += "\r\n";
                mError += getApplicationContext().getString(com.nextgis.maplib.R.string.sync_error_change);
            }
            if (syncResult.stats.numDeletes > 0) {
                if (mError.length() > 0)
                    mError += "\r\n";
                mError += getApplicationContext().getString(com.nextgis.maplib.R.string.sync_error_delete);
            }
            if (syncResult.stats.numEntries > 0) {
                if (mError.length() > 0)
                    mError += "\r\n";
                mError += getApplicationContext().getString(com.nextgis.maplib.R.string.sync_error_server);
            }
            if (syncResult.stats.numSkippedEntries > 0) {
                if (mError.length() > 0)
                    mError += "\r\n";
                mError += getApplicationContext().getString(com.nextgis.maplib.R.string.sync_error_oom);
            }


//            if (isCanceled())
//                ((IGISApplication)getApplicationContext()).sendNotification(getApplicationContext(), SYNC_CANCELED, null);
            if (syncResult.hasError())
                ((IGISApplication)getApplicationContext()).sendNotification(getApplicationContext(), SYNC_CHANGES, mError);
            else
                ((IGISApplication)getApplicationContext()).sendNotification(getApplicationContext(), SYNC_FINISH, null);

            return Result.success();
        } finally {
            LOCK.unlock();
        }
    }
}

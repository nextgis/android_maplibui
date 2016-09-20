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

package com.nextgis.maplibui.activity;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.Table;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.fragment.NGWSettingsFragment;
import com.nextgis.maplibui.util.ControlHelper;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_SYNC_PERIOD;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_SYNC_PERIOD_SEC_LONG;


public class NGWSettingsActivity
        extends NGPreferenceActivity
        implements OnAccountsUpdateListener {
    protected static final String ACCOUNT_ACTION = "com.nextgis.maplibui.ACCOUNT";
    protected static final String KEY_SYNC       = "synchronization";

    protected AccountManager mAccountManager;
    protected final Handler mHandler = new Handler();
    protected String mAction;

    protected static String mDeleteAccountSummary;
    protected static String mDeleteAccountWarnMsg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStrings();
        checkAccountManager();
        createView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void checkAccountManager() {
        if (null == mAccountManager) {
            mAccountManager = AccountManager.get(getApplicationContext());
            Log.d(TAG, "NGWSettingsActivity: AccountManager.get(" + getApplicationContext() + ")");
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        checkAccountManager();
        Header header;

        if (null != mAccountManager) {
            for (Account account : mAccountManager.getAccountsByType(
                    Constants.NGW_ACCOUNT_TYPE)) {
                header = new Header();
                header.title = account.name;
                header.fragment = NGWSettingsFragment.class.getName();

                Bundle bundle = new Bundle();
                bundle.putParcelable("account", account);
                header.fragmentArguments = bundle;
                target.add(header);
            }

            //add "Add account" header
            header = new Header();
            header.title = getString(R.string.add_account);
            header.intent = new Intent(this, NGWLoginActivity.class);
            target.add(header);

            //add "New account" header
            header = new Header();
            header.title = getString(R.string.new_account);
            header.intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://my.nextgis.com"));
            target.add(header);
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public Header onGetInitialHeader() {
        if (null != mAccountManager) {
            if (mAccountManager.getAccountsByType(Constants.NGW_ACCOUNT_TYPE).length > 0) {
                return super.onGetInitialHeader();
            }
        }
        //in Android 5.0 or higher need to have one header with fragment
        Header header = new Header();
        header.fragment = NGWSettingsFragment.class.getName();
        return header;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public Header onGetNewHeader() {
        if (!onIsHidingHeaders()) {
            return onGetInitialHeader();
        }
        return super.onGetNewHeader();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (null != mAccountManager) {
            mAccountManager.addOnAccountsUpdatedListener(this, mHandler, true);
        }
    }


    @Override
    public void onPause() {
        if (null != mAccountManager) {
            mAccountManager.removeOnAccountsUpdatedListener(this);
        }
        super.onPause();
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return NGWSettingsFragment.class.getName().equals(fragmentName);
    }


    // for overriding in a subclass
    protected void setStrings() {
        mDeleteAccountSummary = getString(R.string.delete_account_summary);
        mDeleteAccountWarnMsg = getString(R.string.delete_account_warn_msg);
    }


    // for overriding in a subclass
    protected void createView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
            mAction = getIntent().getAction();
            if (mAction != null && mAction.equals(ACCOUNT_ACTION)) {
                Account account = getIntent().getParcelableExtra("account");
                fillAccountPreferences(screen, account);
            } else {
                fillPreferences(screen);
            }

            setPreferenceScreen(screen);
        }
    }


    protected void fillPreferences(PreferenceScreen screen) {
        if (null != mAccountManager) {
            for (Account account : mAccountManager.getAccountsByType(
                    Constants.NGW_ACCOUNT_TYPE)) {
                Preference preference = new Preference(this);
                preference.setTitle(account.name);

                Bundle bundle = new Bundle();
                bundle.putParcelable("account", account);
                Intent intent = new Intent(this, NGWSettingsActivity.class);
                intent.putExtras(bundle);
                intent.setAction(ACCOUNT_ACTION);

                preference.setIntent(intent);

                if (null != screen) {
                    screen.addPreference(preference);
                }
            }
            //add "Add account" preference
            Preference addAccount = new Preference(this);
            addAccount.setTitle(R.string.add_account);
            Intent intent = new Intent(this, NGWLoginActivity.class);
            addAccount.setIntent(intent);

            //add "New account" preference
            Preference newAccount = new Preference(this);
            newAccount.setTitle(R.string.new_account);
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse("http://my.nextgis.com"));
            newAccount.setIntent(browser);

            if (null != screen) {
                screen.addPreference(addAccount);
                screen.addPreference(newAccount);
            }
        }
    }


    public void fillAccountPreferences(
            PreferenceScreen screen,
            final Account account) {
        final IGISApplication application = (IGISApplication) getApplicationContext();

        // add sync settings group
        PreferenceCategory syncCategory = new PreferenceCategory(this);
        syncCategory.setTitle(R.string.sync);
        screen.addPreference(syncCategory);

        // add auto sync property
        addAutoSyncProperty(application, account, syncCategory);

        // add time for periodic sync
        addPeriodicSyncTime(application, account, syncCategory);

        // add account layers
        addAccountLayers(screen, application, account);

        // add actions group
        PreferenceCategory actionCategory = new PreferenceCategory(this);
        actionCategory.setTitle(R.string.actions);
        screen.addPreference(actionCategory);

        // add "Edit account" action
        addEditAccountAction(application, account, actionCategory);

        // add delete account action
        addDeleteAccountAction(application, account, actionCategory);
    }


    // for overriding in a subclass
    protected void addAutoSyncProperty(
            final IGISApplication application,
            final Account account,
            PreferenceCategory syncCategory) {
        final String accountNameHash = "_" + account.name.hashCode();
        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.PREFERENCES, Constants.MODE_MULTI_PROCESS);

        CheckBoxPreference enablePeriodicSync = new CheckBoxPreference(this);
        enablePeriodicSync.setKey(KEY_SYNC);
        enablePeriodicSync.setPersistent(false);
        enablePeriodicSync.setTitle(R.string.auto_sync);

        boolean isAccountSyncEnabled = isAccountSyncEnabled(account, application.getAuthority());
        enablePeriodicSync.setChecked(isAccountSyncEnabled);
        enablePeriodicSync.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            Object newValue) {
                        boolean isChecked = (boolean) newValue;
                        setAccountSyncEnabled(account, application.getAuthority(), isChecked);
                        return true;
                    }
                });

        long timeStamp =
                sharedPreferences.getLong(SettingsConstants.KEY_PREF_LAST_SYNC_TIMESTAMP + accountNameHash, 0);
        if (isAccountSyncEnabled && timeStamp > 0) {
            enablePeriodicSync.setSummary(ControlHelper.getSyncTime(this, timeStamp));
        } else {
            enablePeriodicSync.setSummary(R.string.auto_sync_summary);
        }
        syncCategory.addPreference(enablePeriodicSync);
    }


    // for overriding in a subclass
    public static boolean isAccountSyncEnabled(
            Account account,
            String authority) {
        return null != account && ContentResolver.getSyncAutomatically(account, authority);
    }


    // for overriding in a subclass
    public static void setAccountSyncEnabled(
            Account account,
            String authority,
            boolean isEnabled)
    {
        if (null == account) {
            return;
        }

        ContentResolver.setSyncAutomatically(account, authority, isEnabled);
    }


    // for overriding in a subclass
    protected void addPeriodicSyncTime(
            final IGISApplication application,
            final Account account,
            PreferenceCategory syncCategory) {

        String prefValue = "" + Constants.DEFAULT_SYNC_PERIOD;
        List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, application.getAuthority());
        if(null != syncs && !syncs.isEmpty()) {
            for(PeriodicSync sync : syncs) {
                Bundle bundle = sync.extras;
                long period = bundle.getLong(KEY_PREF_SYNC_PERIOD_SEC_LONG, Constants.NOT_FOUND);
                if(period > 0){
                    prefValue = "" + period;
                    break;
                }
            }
        }

        final CharSequence[] keys = getPeriodTitles(this);
        final CharSequence[] values = getPeriodValues();

        final ListPreference timeInterval = new ListPreference(this);
        timeInterval.setKey(KEY_PREF_SYNC_PERIOD);
        timeInterval.setTitle(R.string.sync_interval);
        timeInterval.setDialogTitle(R.string.sync_set_interval);
        timeInterval.setEntries(keys);
        timeInterval.setEntryValues(values);

        // set default values
        timeInterval.setValueIndex(4);
        timeInterval.setSummary(keys[4]);

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(prefValue)) {
                timeInterval.setValueIndex(i);
                timeInterval.setSummary(keys[i]);
                break;
            }
        }

        timeInterval.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            Object newValue) {
                        long interval = Long.parseLong((String) newValue);

                        for (int i = 0; i < values.length; i++) {
                            if (values[i].equals(newValue)) {
                                timeInterval.setSummary(keys[i]);
                                break;
                            }
                        }

                        Bundle bundle = new Bundle();
                        bundle.putLong(KEY_PREF_SYNC_PERIOD_SEC_LONG, interval);

                        if (interval == NOT_FOUND) {
                            ContentResolver.removePeriodicSync(
                                    account, application.getAuthority(), bundle);
                        } else {

                            ContentResolver.addPeriodicSync(
                                    account, application.getAuthority(), bundle, interval);
                        }

                        return true;
                    }
                });

        syncCategory.addPreference(timeInterval);
        // TODO 3.0
        // crash on Android 2.3.6
//        timeInterval.setDependency(KEY_SYNC);
    }


    // for overriding in a subclass
    protected void addAccountLayers(
            PreferenceScreen screen,
            final IGISApplication application,
            Account account) {
        List<INGWLayer> layers = getLayersForAccount(application, account);
        if (!layers.isEmpty()) {

            PreferenceCategory layersCategory = null;

            for (INGWLayer layer : layers) {

                if (!(layer instanceof NGWVectorLayer)) {
                    continue;
                }

                final NGWVectorLayer ngwLayer = (NGWVectorLayer) layer;

                if (!ngwLayer.isSyncable()) {
                    continue;
                }

                CheckBoxPreference layerSync = new CheckBoxPreference(this);
                layerSync.setTitle(ngwLayer.getName());
                layerSync.setChecked(0 == (ngwLayer.getSyncType() & Constants.SYNC_NONE));
                //layerSync.setKey("" + ngwLayer.getId());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                        ngwLayer instanceof ILayerUI) {
                    ILayerUI layerUI = (ILayerUI) ngwLayer;
                    layerSync.setIcon(layerUI.getIcon(this));
                }

                layerSync.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(
                                    Preference preference,
                                    Object newValue) {
                                boolean isChecked = (boolean) newValue;
                                if (isChecked) {
                                    ngwLayer.setSyncType(Constants.SYNC_ALL);

                                } else {
                                    ngwLayer.setSyncType(Constants.SYNC_NONE);
                                }
                                ngwLayer.save();
                                return true;
                            }
                        });

                if (null == layersCategory) {
                    layersCategory = new PreferenceCategory(this);
                    layersCategory.setTitle(R.string.sync_layers);
                    layersCategory.setSummary(R.string.sync_layers_summary);
                    screen.addPreference(layersCategory);
                }
                layersCategory.addPreference(layerSync);
            }
        }
    }


    // for overriding in a subclass
    protected void addEditAccountAction(
            final IGISApplication application,
            final Account account,
            PreferenceCategory actionCategory) {
        Preference preferenceEdit = new Preference(this);
        preferenceEdit.setTitle(R.string.edit_account);
        preferenceEdit.setSummary(R.string.edit_account_summary);

        String url = application.getAccountUrl(account);
        String login = application.getAccountLogin(account);

        Intent intent = new Intent(this, NGWLoginActivity.class);
        intent.putExtra(NGWLoginActivity.FOR_NEW_ACCOUNT, false);
        intent.putExtra(NGWLoginActivity.ACCOUNT_URL_TEXT, url);
        intent.putExtra(NGWLoginActivity.ACCOUNT_LOGIN_TEXT, login);
        intent.putExtra(NGWLoginActivity.CHANGE_ACCOUNT_URL, false);
        intent.putExtra(NGWLoginActivity.CHANGE_ACCOUNT_LOGIN, true);
        preferenceEdit.setIntent(intent);

        actionCategory.addPreference(preferenceEdit);
    }


    // for overriding in a subclass
    protected Preference addDeleteAccountAction(
            final IGISApplication application,
            final Account account,
             PreferenceCategory actionCategory) {
        final boolean[] wasCurrentSyncActive = {false};

        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            private ProgressDialog mProgressDialog;

            @Override
            public void onReceive(
                    Context context,
                    Intent intent) {
                Log.d(Constants.TAG, "NGWSettingsActivity - broadcastReceiver.onReceive()");

                if (mProgressDialog == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        mProgressDialog = new ProgressDialog(NGWSettingsActivity.this, android.R.style.Theme_Material_Light_Dialog_Alert);
                    else
                        mProgressDialog = new ProgressDialog(NGWSettingsActivity.this);
                }

                if (!mProgressDialog.isShowing()) {
                    Log.d(Constants.TAG, "NGWSettingsActivity - show ProgressDialog");

                    mProgressDialog.setTitle(R.string.waiting);
                    mProgressDialog.setMessage(getString(R.string.wait_sync_stopping));
                    mProgressDialog.setIndeterminate(true);
                    mProgressDialog.show();
                }

                String action;
                if (wasCurrentSyncActive[0]) {
                    if (null == intent) {
                        return;
                    }
                    action = intent.getAction();

                } else {
                    action = SyncAdapter.SYNC_CANCELED;
                }

                switch (action) {
                    case SyncAdapter.SYNC_START:
                        break;

                    case SyncAdapter.SYNC_FINISH:
                        break;

                    case SyncAdapter.SYNC_CANCELED:
                        Log.d(Constants.TAG, "NGWSettingsActivity - sync status - SYNC_CANCELED");

                        wasCurrentSyncActive[0] = false;

                        application.removeAccount(account);

                        Log.d(Constants.TAG, "NGWSettingsActivity - account is removed");

                        deleteAccountLayers(application, account);
                        Log.d(Constants.TAG, "NGWSettingsActivity - account layers are deleted");

                        unregisterReceiver(this);
                        mProgressDialog.dismiss();

                        onDeleteAccount();
                        break;

                    case SyncAdapter.SYNC_CHANGES:
                        break;
                }
            }
        };

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(NGWSettingsActivity.this);
        dialogBuilder.setIcon(R.drawable.ic_action_warning_light)
                .setTitle(R.string.delete_account_ask)
                .setMessage(mDeleteAccountWarnMsg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.ok, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(
                                    DialogInterface dialog,
                                    int which)
                            {
                                Log.d(Constants.TAG, "NGWSettingsActivity - OK pressed");

                                IntentFilter intentFilter = new IntentFilter();
                                intentFilter.addAction(SyncAdapter.SYNC_CANCELED);
                                registerReceiver(broadcastReceiver, intentFilter);

                                wasCurrentSyncActive[0] = AccountUtil.isSyncActive(
                                        account, application.getAuthority());

                                ContentResolver.removePeriodicSync(
                                        account, application.getAuthority(), Bundle.EMPTY);
                                ContentResolver.setSyncAutomatically(
                                        account, application.getAuthority(), false);

                                ContentResolver.cancelSync(account, application.getAuthority());

                                Log.d(
                                        Constants.TAG,
                                        "NGWSettingsActivity - ContentResolver.cancelSync() is performed");

                                broadcastReceiver.onReceive(NGWSettingsActivity.this, null);
                            }
                        });

        Preference preferenceDelete = new Preference(this);
        preferenceDelete.setTitle(R.string.delete_account);
        preferenceDelete.setSummary(mDeleteAccountSummary);

        if (actionCategory.addPreference(preferenceDelete)) {
            preferenceDelete.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference preference) {
                            dialogBuilder.show();
                            return true;
                        }
                    });

            return preferenceDelete;
        }

        return null;
    }


    public static void updateAccountLayersCacheData(
            final IGISApplication application,
            Account account) {
        List<INGWLayer> layers = getLayersForAccount(application, account);

        for (INGWLayer layer : layers) {
            layer.setAccountCacheData();
        }
    }


    protected void deleteAccountLayers(
            final IGISApplication application,
            Account account) {
        List<INGWLayer> layers = getLayersForAccount(application, account);

        for (INGWLayer layer : layers) {
            ((Table) layer).delete();
        }

        application.getMap().save();

        if (null != mOnDeleteAccountListener) {
            mOnDeleteAccountListener.onDeleteAccount(account);
        }
    }


    protected static List<INGWLayer> getLayersForAccount(
            final IGISApplication application,
            Account account) {
        List<INGWLayer> out = new ArrayList<>();

        MapContentProviderHelper.getLayersByAccount(application.getMap(), account.name, out);

        return out;
    }


    // for overriding in a subclass
    protected void onDeleteAccount() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            finish();
        } else {
            if (onIsHidingHeaders()) {
                finish();
            } else {
                invalidateHeaders();
            }
        }
    }


    @Override
    public void onAccountsUpdated(Account[] accounts) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            if (mAction == null || !mAction.equals(ACCOUNT_ACTION)) {
                PreferenceScreen screen = getPreferenceScreen();
                if (null != screen) {
                    screen.removeAll();
                    fillPreferences(screen);
                    //onContentChanged();
                }
            }
        } else {
            invalidateHeaders();
            setSelection(0);
        }
    }


    protected OnDeleteAccountListener mOnDeleteAccountListener;


    public void setOnDeleteAccountListener(OnDeleteAccountListener onDeleteAccountListener) {
        mOnDeleteAccountListener = onDeleteAccountListener;
    }


    public interface OnDeleteAccountListener {
        void onDeleteAccount(Account account);
    }

    public static CharSequence[] getPeriodTitles(Context context) {
        return new CharSequence[]{
                context.getString(R.string.five_minutes),
                context.getString(R.string.ten_minutes),
                context.getString(R.string.fifteen_minutes),
                context.getString(R.string.thirty_minutes),
                context.getString(R.string.one_hour),
                context.getString(R.string.two_hours)};
    }

    public static CharSequence[] getPeriodValues() {
        return new CharSequence[]{"300", "600", "900", "1800", "3600", "7200"};
    }
}

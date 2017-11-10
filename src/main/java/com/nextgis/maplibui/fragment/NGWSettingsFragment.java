/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.fragment;

import android.accounts.Account;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGPreferenceActivity;
import com.nextgis.maplibui.activity.NGWLoginActivity;
import com.nextgis.maplibui.activity.NGWSettingsActivity;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplibui.util.SettingsConstantsUI.ACTION_PREFS_NGW;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_SYNC_PERIOD;


public class NGWSettingsFragment
        extends NGPreferenceSettingsFragment
{
    protected static final String KEY_SYNC = "synchronization";

    protected boolean mChangeTitle = false;

    protected static String mDeleteAccountSummary;
    protected static String mDeleteAccountWarnMsg;

    protected OnDeleteAccountListener mOnDeleteAccountListener;


    @Override
    public void createPreferences(PreferenceScreen screen)
    {
        mChangeTitle = !mAction.equals(ACTION_PREFS_NGW);
        fillPreferences(screen);
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setStrings();
    }


    // for overriding in a subclass
    protected void setStrings()
    {
        mDeleteAccountSummary = getString(R.string.ngw_account_delete_summary);
        mDeleteAccountWarnMsg = getString(R.string.ngw_account_delete_warn_msg);
    }


    // for overriding in a subclass
    protected void fillPreferences(PreferenceGroup screen)
    {
        if (mAction != null && mAction.equals(SettingsConstantsUI.ACTION_ACCOUNT)) {
            Account account = getArguments().getParcelable(NGWSettingsActivity.KEY_ACCOUNT);
            fillAccountPreferences(screen, account);
        }
    }


    public void fillAccountPreferences(
            PreferenceGroup screen,
            final Account account)
    {
        final IGISApplication appContext = (IGISApplication) mStyledContext.getApplicationContext();

        // add sync settings group
        PreferenceCategory syncCategory = new PreferenceCategory(mStyledContext);
        syncCategory.setTitle(R.string.sync);
        screen.addPreference(syncCategory);

        // add auto sync property
        addAutoSyncProperty(appContext, account, syncCategory);

        // add time for periodic sync
        addPeriodicSyncTime(appContext, account, syncCategory);

        // add account layers
        addAccountLayers(screen, appContext, account);

        // add actions group
        PreferenceCategory actionCategory = new PreferenceCategory(mStyledContext);
        actionCategory.setTitle(R.string.actions);
        screen.addPreference(actionCategory);

        // add "Edit account" action
        addEditAccountAction(appContext, account, actionCategory);

        // add delete account action
        addDeleteAccountAction(appContext, account, actionCategory);

// TODO: for isMultiPane() change title of Settings fragment, not of Activity
//        if (mChangeTitle && !NGPreferenceActivity.isMultiPane(mActivity)) {
        if (mChangeTitle) {
            ActionBar ab = mActivity.getSupportActionBar();
            if (null != ab) {
                ab.setTitle(account.name);
            }
        }
    }


    // for overriding in a subclass
    protected void addAutoSyncProperty(
            final IGISApplication application,
            final Account account,
            PreferenceGroup syncCategory)
    {
        final String accountNameHash = "_" + account.name.hashCode();
        SharedPreferences sharedPreferences =
                mStyledContext.getSharedPreferences(Constants.PREFERENCES,
                        Constants.MODE_MULTI_PROCESS);

        CheckBoxPreference enablePeriodicSync = new CheckBoxPreference(mStyledContext);
        enablePeriodicSync.setKey(KEY_SYNC);
        enablePeriodicSync.setPersistent(false);
        enablePeriodicSync.setTitle(R.string.auto_sync);

        boolean isAccountSyncEnabled = isAccountSyncEnabled(account, application.getAuthority());
        enablePeriodicSync.setChecked(isAccountSyncEnabled);
        enablePeriodicSync.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(
                    Preference preference,
                    Object newValue)
            {
                boolean isChecked = (boolean) newValue;
                setAccountSyncEnabled(account, application.getAuthority(), isChecked);
                return true;
            }
        });

        long timeStamp = sharedPreferences.getLong(
                com.nextgis.maplib.util.SettingsConstants.KEY_PREF_LAST_SYNC_TIMESTAMP
                        + accountNameHash, 0);
        if (isAccountSyncEnabled && timeStamp > 0) {
            enablePeriodicSync.setSummary(ControlHelper.getSyncTime(mStyledContext, timeStamp));
        } else {
            enablePeriodicSync.setSummary(R.string.auto_sync_summary);
        }
        syncCategory.addPreference(enablePeriodicSync);
    }


    // for overriding in a subclass
    public static boolean isAccountSyncEnabled(
            Account account,
            String authority)
    {
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
            PreferenceGroup syncCategory)
    {

        String prefValue = "" + Constants.DEFAULT_SYNC_PERIOD;
        List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, application.getAuthority());
        if (null != syncs && !syncs.isEmpty()) {
            for (PeriodicSync sync : syncs) {
                Bundle bundle = sync.extras;
                String value = bundle.getString(KEY_PREF_SYNC_PERIOD);
                if (value != null) {
                    prefValue = value;
                    break;
                }
            }
        }

        final CharSequence[] keys = getPeriodTitles(mStyledContext);
        final CharSequence[] values = getPeriodValues();

        final ListPreference timeInterval = new ListPreference(mStyledContext);
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

        timeInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(
                    Preference preference,
                    Object newValue)
            {
                String value =(String) newValue;
                long interval = Long.parseLong(value);

                for (int i = 0; i < values.length; i++) {
                    if (values[i].equals(newValue)) {
                        timeInterval.setSummary(keys[i]);
                        break;
                    }
                }

                Bundle bundle = new Bundle();
                bundle.putString(KEY_PREF_SYNC_PERIOD, value);

                if (interval == NOT_FOUND) {
                    ContentResolver.removePeriodicSync(account, application.getAuthority(), bundle);
                } else {
                    ContentResolver.addPeriodicSync(account, application.getAuthority(), bundle, interval);
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
            PreferenceGroup screen,
            final IGISApplication application,
            Account account)
    {
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

                CheckBoxPreference layerSync = new CheckBoxPreference(mStyledContext);
                layerSync.setTitle(ngwLayer.getName());
                layerSync.setChecked(0 == (ngwLayer.getSyncType() & Constants.SYNC_NONE));
                //layerSync.setKey("" + ngwLayer.getId());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                        && ngwLayer instanceof ILayerUI) {
                    ILayerUI layerUI = (ILayerUI) ngwLayer;
                    layerSync.setIcon(layerUI.getIcon(mStyledContext));
                }

                layerSync.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            Object newValue)
                    {
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
                    layersCategory = new PreferenceCategory(mStyledContext);
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
            PreferenceGroup actionCategory)
    {
        Preference preferenceEdit = new Preference(mStyledContext);
        preferenceEdit.setTitle(R.string.edit_account);
        preferenceEdit.setSummary(R.string.edit_account_summary);

        String url = application.getAccountUrl(account);
        String login = application.getAccountLogin(account);

        Intent intent = new Intent(mStyledContext, NGWLoginActivity.class);
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
            PreferenceGroup actionCategory)
    {
        final boolean[] wasCurrentSyncActive = {false};

        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
        {
            private ProgressDialog mProgressDialog;


            @Override
            public void onReceive(
                    Context context,
                    Intent intent)
            {
                Log.d(Constants.TAG, "NGWSettingsActivity - broadcastReceiver.onReceive()");

                if (mProgressDialog == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mProgressDialog = new ProgressDialog(mStyledContext,
                                android.R.style.Theme_Material_Light_Dialog_Alert);
                    } else {
                        mProgressDialog = new ProgressDialog(mStyledContext);
                    }
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

                        mStyledContext.unregisterReceiver(this);
                        mProgressDialog.dismiss();

                        onDeleteAccount();
                        break;

                    case SyncAdapter.SYNC_CHANGES:
                        break;
                }
            }
        };

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mStyledContext);
        dialogBuilder.setIcon(R.drawable.ic_action_warning_light)
                .setTitle(getString(R.string.ngw_account_delete) + "?")
                .setMessage(mDeleteAccountWarnMsg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(
                            DialogInterface dialog,
                            int which)
                    {
                        Log.d(Constants.TAG, "NGWSettingsActivity - OK pressed");

                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(SyncAdapter.SYNC_CANCELED);
                        mStyledContext.registerReceiver(broadcastReceiver, intentFilter);

                        wasCurrentSyncActive[0] =
                                AccountUtil.isSyncActive(account, application.getAuthority());

                        ContentResolver.removePeriodicSync(account, application.getAuthority(), Bundle.EMPTY);
                        ContentResolver.setSyncAutomatically(account, application.getAuthority(), false);

                        ContentResolver.cancelSync(account, application.getAuthority());

                        Log.d(
                                Constants.TAG,
                                "NGWSettingsActivity - ContentResolver.cancelSync() is performed");

                        broadcastReceiver.onReceive(mStyledContext, null);
                    }
                });

        Preference preferenceDelete = new Preference(mStyledContext);
        preferenceDelete.setTitle(R.string.ngw_account_delete);
        preferenceDelete.setSummary(mDeleteAccountSummary);

        if (actionCategory.addPreference(preferenceDelete)) {
            preferenceDelete.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                public boolean onPreferenceClick(Preference preference)
                {
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
            Account account)
    {
        List<INGWLayer> layers = getLayersForAccount(application, account);

        for (INGWLayer layer : layers) {
            layer.setAccountCacheData();
        }
    }


    protected void deleteAccountLayers(
            final IGISApplication application,
            Account account)
    {
        List<INGWLayer> layers = getLayersForAccount(application, account);

        for (INGWLayer layer : layers) {
            ((Layer) layer).delete();
        }

        application.getMap().save();

        if (null != mOnDeleteAccountListener) {
            mOnDeleteAccountListener.onDeleteAccount(account);
        }
    }


    protected static List<INGWLayer> getLayersForAccount(
            final IGISApplication application,
            Account account)
    {
        List<INGWLayer> out = new ArrayList<>();
        if (application == null || account == null) {
            return out;
        }

        MapContentProviderHelper.getLayersByAccount(application.getMap(), account.name, out);
        return out;
    }


    // for overriding in a subclass
    protected void onDeleteAccount()
    {
        if (!NGPreferenceActivity.isMultiPane(getActivity())) {
            getFragmentManager().popBackStack();
        } else {
            mActivity.invalidatePreferences();
        }
        mActivity.replaceSettingsFragment(null);
        if (!NGPreferenceActivity.isMultiPane(getActivity()))
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.onBackPressed();
                }
            }, 1000);
    }


    public void setOnDeleteAccountListener(OnDeleteAccountListener onDeleteAccountListener)
    {
        mOnDeleteAccountListener = onDeleteAccountListener;
    }


    public interface OnDeleteAccountListener
    {
        void onDeleteAccount(Account account);

    }


    public static CharSequence[] getPeriodTitles(Context context)
    {
        return new CharSequence[] {
                context.getString(R.string.five_minutes),
                context.getString(R.string.ten_minutes),
                context.getString(R.string.fifteen_minutes),
                context.getString(R.string.thirty_minutes),
                context.getString(R.string.one_hour),
                context.getString(R.string.two_hours)};
    }


    public static CharSequence[] getPeriodValues()
    {
        return new CharSequence[] {"300", "600", "900", "1800", "3600", "7200"};
    }
}

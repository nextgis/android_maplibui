/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.api.ILayerUI;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.nextgis.maplib.util.Constants.NGW_ACCOUNT_TYPE;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_SYNC_PERIOD;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_SYNC_PERIOD_LONG;


public class NGWSettingsActivity
        extends PreferenceActivity
        implements OnAccountsUpdateListener
{
    protected static final String  ACCOUNT_ACTION = "com.nextgis.maplibui.ACCOUNT";

    protected AccountManager mAccountManager;
    protected final Handler mHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (null == mAccountManager) {
            mAccountManager = AccountManager.get(this);
        }

        ViewGroup root = ((ViewGroup) findViewById(android.R.id.content));
        LinearLayout content = (LinearLayout) root.getChildAt(0);
        LinearLayout toolbarContainer =
                (LinearLayout) View.inflate(this, R.layout.activity_settings, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        Toolbar toolbar = (Toolbar) toolbarContainer.findViewById(R.id.main_toolbar);
        toolbar.getBackground().setAlpha(255);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        NGWSettingsActivity.this.finish();
                    }
                });

        String action = getIntent().getAction();
        if (action != null && action.equals(ACCOUNT_ACTION)) {
            Account account = getIntent().getParcelableExtra("account");
            fillAccountPreferences(getPreferenceScreen(), account);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            fillPreferences();
        }
    }


    public void fillAccountPreferences(
            PreferenceScreen screen,
            final Account account)
    {
        //add sync settings group
        PreferenceCategory syncCategory = new PreferenceCategory(this);
        syncCategory.setTitle(R.string.sync);
        screen.addPreference(syncCategory);

        //add auto sync property
        CheckBoxPreference enablePeriodicSync = new CheckBoxPreference(this);
        enablePeriodicSync.setTitle(R.string.auto_sync);

        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.PREFERENCES, Context.MODE_MULTI_PROCESS);

        final IGISApplication application = (IGISApplication) getApplicationContext();
        boolean isAccountSyncEnabled =
                ContentResolver.getSyncAutomatically(account, application.getAuthority());
        enablePeriodicSync.setChecked(isAccountSyncEnabled);
        enablePeriodicSync.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener()
                {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            Object o)
                    {
                        boolean isChecked = (boolean) o;
                        ContentResolver.setSyncAutomatically(
                                account, application.getAuthority(), isChecked);
                        return true;
                    }
                });
        long timeStamp =
                sharedPreferences.getLong(SettingsConstants.KEY_PREF_LAST_SYNC_TIMESTAMP, 0);
        if (isAccountSyncEnabled && timeStamp > 0) {
            enablePeriodicSync.setSummary(
                    getString(R.string.last_sync_time) + ": " +
                    new SimpleDateFormat().format(new Date(timeStamp)));
        } else {
            enablePeriodicSync.setSummary(R.string.auto_sync_summary);
        }
        syncCategory.addPreference(enablePeriodicSync);

        //add time for periodic sync
        final ListPreference timeInterval = new ListPreference(this);
        timeInterval.setTitle(R.string.sync_interval);
        final CharSequence[] values = {"-1", "600", "900", "1800", "3600", "7200"};
        final CharSequence[] keys = {
                getString(R.string.system_default),
                getString(R.string.ten_minutes),
                getString(R.string.fifteen_minutes),
                getString(R.string.thirty_minutes),
                getString(R.string.one_hour),
                getString(R.string.two_hours)};
        timeInterval.setEntries(keys);
        timeInterval.setEntryValues(values);
        timeInterval.setDefaultValue(getString(R.string.system_default));
        timeInterval.setKey(KEY_PREF_SYNC_PERIOD);

        final SharedPreferences sharedPreferencesA =
                PreferenceManager.getDefaultSharedPreferences(this);
        String value = "" + sharedPreferencesA.getLong(KEY_PREF_SYNC_PERIOD_LONG, NOT_FOUND);
        for (int i = 0; i < 6; i++) {
            if (values[i].equals(value)) {
                timeInterval.setValueIndex(i);
                //timeInterval.setValue((String) values[i]);
                timeInterval.setSummary(keys[i]);
                break;
            }
        }
        timeInterval.setDialogTitle(R.string.sync_set_interval);
        timeInterval.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener()
                {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            Object o)
                    {
                        long interval = Long.parseLong((String) o);
                        for (int i = 0; i < 6; i++) {
                            if (values[i].equals(o)) {
                                timeInterval.setSummary(keys[i]);
                                break;
                            }
                        }

                        if (interval == NOT_FOUND) {
                            ContentResolver.removePeriodicSync(
                                    account, application.getAuthority(), Bundle.EMPTY);
                        } else {
                            ContentResolver.addPeriodicSync(
                                    account, application.getAuthority(), Bundle.EMPTY, interval);
                        }

                        //set KEY_PREF_SYNC_PERIOD_LONG
                        return sharedPreferencesA.edit()
                                .putLong(KEY_PREF_SYNC_PERIOD_LONG, interval)
                                .commit();
                    }
                });
        syncCategory.addPreference(timeInterval);

        addAccountLayers(screen, account);

        PreferenceCategory actionCategory = new PreferenceCategory(this);
        actionCategory.setTitle(R.string.actions);
        screen.addPreference(actionCategory);

        Preference preferenceDelete = new Preference(this);
        preferenceDelete.setTitle(R.string.delete_account);
        preferenceDelete.setSummary(R.string.delete_account_summary);
        if (actionCategory.addPreference(preferenceDelete)) {
            preferenceDelete.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener()
                    {
                        public boolean onPreferenceClick(Preference preference)
                        {
                            IGISApplication application = (IGISApplication) getApplicationContext();
                            ContentResolver.cancelSync(account, application.getAuthority());

                            final AccountManager accountManager =
                                    AccountManager.get(NGWSettingsActivity.this);
                            accountManager.removeAccount(account, null, new Handler());

                            List<INGWLayer> layers =
                                    getLayersForAccount(NGWSettingsActivity.this, account);

                            for (INGWLayer layer : layers) {
                                ((Layer) layer).delete();
                            }

                            application.getMap().save();

                            if (null != mOnDeleteAccountListener) {
                                mOnDeleteAccountListener.onDeleteAccount(account);
                            }

                            onBackPressed();
                            return true;
                        }
                    });
        }
    }


    protected void addAccountLayers(
            PreferenceScreen screen,
            Account account)
    {
        List<INGWLayer> layers = getLayersForAccount(this, account);
        if (!layers.isEmpty()) {
            PreferenceCategory layersCategory = new PreferenceCategory(this);
            layersCategory.setTitle(R.string.sync_layers);
            layersCategory.setSummary(R.string.sync_layers_summary);
            screen.addPreference(layersCategory);

            for (INGWLayer layer : layers) {

                if (!(layer instanceof NGWVectorLayer)) {
                    continue;
                }

                final NGWVectorLayer ngwLayer = (NGWVectorLayer) layer;

                CheckBoxPreference layerSync = new CheckBoxPreference(this);
                layerSync.setTitle(ngwLayer.getName());
                layerSync.setChecked(0 == (ngwLayer.getSyncType() & Constants.SYNC_NONE));
                //layerSync.setKey("" + ngwLayer.getId());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                    ngwLayer instanceof ILayerUI) {
                    ILayerUI layerUI = (ILayerUI) ngwLayer;
                    layerSync.setIcon(layerUI.getIcon());
                }

                layerSync.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener()
                        {
                            @Override
                            public boolean onPreferenceChange(
                                    Preference preference,
                                    Object o)
                            {
                                boolean isChecked = (boolean) o;
                                if (isChecked) {
                                    ngwLayer.setSyncType(Constants.SYNC_ALL);

                                } else {
                                    ngwLayer.setSyncType(Constants.SYNC_NONE);
                                }
                                ngwLayer.save();
                                return true;
                            }
                        });

                layersCategory.addPreference(layerSync);
            }
        }
    }


    protected OnDeleteAccountListener mOnDeleteAccountListener;


    public void setOnDeleteAccountListener(OnDeleteAccountListener onDeleteAccountListener)
    {
        mOnDeleteAccountListener = onDeleteAccountListener;
    }


    public interface OnDeleteAccountListener
    {
        public void onDeleteAccount(Account account);
    }


    protected static List<INGWLayer> getLayersForAccount(
            Context context,
            Account account)
    {
        List<INGWLayer> out = new ArrayList<>();

        IGISApplication application = (IGISApplication) context.getApplicationContext();
        MapContentProviderHelper.getLayersByAccount(application.getMap(), account.name, out);

        return out;
    }


    protected void fillPreferences()
    {
        if (null != mAccountManager) {
            for (Account account : mAccountManager.getAccountsByType(
                    NGW_ACCOUNT_TYPE)) {
                Preference preference = new Preference(this);
                preference.setTitle(account.name);

                Bundle bundle = new Bundle();
                bundle.putParcelable("account", account);
                Intent intent = new Intent(this, NGWSettingsActivity.class);
                intent.putExtras(bundle);
                intent.setAction(ACCOUNT_ACTION);

                preference.setIntent(intent);

                getPreferenceScreen().addPreference(preference);
            }
            //add "Add account" preference
            Preference preference = new Preference(this);
            preference.setTitle(R.string.add_account);
            preference.setSummary(R.string.add_account_summary);
            Intent intent = new Intent(this, NGWLoginActivity.class);
            preference.setIntent(intent);

            getPreferenceScreen().addPreference(preference);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target)
    {
        if (null == mAccountManager) {
            mAccountManager = AccountManager.get(this);
        }

        if (null != mAccountManager) {
            for (Account account : mAccountManager.getAccountsByType(
                    NGW_ACCOUNT_TYPE)) {
                Header header = new Header();
                header.title = account.name;
                header.fragment = com.nextgis.maplibui.NGWSettingsFragment.class.getName();

                Bundle bundle = new Bundle();
                bundle.putParcelable("account", account);
                header.fragmentArguments = bundle;
                target.add(header);
            }
            //add "Add account" header
            Header header = new Header();
            header.title = getString(R.string.add_account);
            header.summary = getString(R.string.add_account_summary);
            header.intent = new Intent(this, NGWLoginActivity.class);
            target.add(header);
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();
        if (null != mAccountManager) {
            mAccountManager.addOnAccountsUpdatedListener(this, mHandler, true);
        }
    }


    @Override
    public void onPause()
    {
        if (null != mAccountManager) {
            mAccountManager.removeOnAccountsUpdatedListener(this);
        }
        super.onPause();
    }


    @Override
    public void onAccountsUpdated(Account[] accounts)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            getPreferenceScreen().removeAll();
            fillPreferences();
            //onContentChanged();
        } else {
            invalidateHeaders();
        }
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        return NGWSettingsFragment.class.getName().equals(fragmentName);
    }
}

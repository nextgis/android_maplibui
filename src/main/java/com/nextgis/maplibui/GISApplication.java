/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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

package com.nextgis.maplibui;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Application;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.PermissionUtil;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.mapui.LayerFactoryUI;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.nextgis.maplib.util.Constants.MAP_EXT;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_SYNC_PERIODICALLY;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_SYNC_PERIOD_SEC_LONG;

/**
 * This is a base application class. Each application should inherited their base application from
 * this class.
 *
 * The main application class stored some singleton objects.
 *
 * @author Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 */
public abstract class GISApplication extends Application
        implements IGISApplication {

    protected MapDrawable mMap;
    protected GpsEventSource mGpsEventSource;
    protected SharedPreferences mSharedPreferences;
    protected AccountManager mAccountManager;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mGpsEventSource = new GpsEventSource(this);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mAccountManager = AccountManager.get(getApplicationContext());

        getMap();

        boolean mIsDarkTheme = mSharedPreferences.getString(SettingsConstantsUI.KEY_PREF_THEME, "light").equals("dark");
        setTheme(getThemeId(mIsDarkTheme));

        if (mSharedPreferences.getBoolean(SettingsConstantsUI.KEY_PREF_APP_FIRST_RUN, true)) {
            onFirstRun();
            SharedPreferences.Editor edit = mSharedPreferences.edit();
            edit.putBoolean(SettingsConstantsUI.KEY_PREF_APP_FIRST_RUN, false);
            edit.commit();
        }

        //turn on periodic sync. Can be set for each layer individually, but this is simpler
        if (mSharedPreferences.getBoolean(KEY_PREF_SYNC_PERIODICALLY, true)) {
            long period =
                    mSharedPreferences.getLong(KEY_PREF_SYNC_PERIOD_SEC_LONG, Constants.DEFAULT_SYNC_PERIOD); //1 hour

            if(-1 == period)
                period = Constants.DEFAULT_SYNC_PERIOD;

            Bundle params = new Bundle();
            params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
            params.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
            params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);

            SyncAdapter.setSyncPeriod(this, params, period);
        }
    }

    protected int getThemeId(boolean isDark){
        if(isDark)
            return R.style.Theme_NextGIS_AppCompat_Dark;
        else
            return R.style.Theme_NextGIS_AppCompat_Light;
    }

    @Override
    public MapBase getMap()
    {
        if (null != mMap) {
            return mMap;
        }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        File defaultPath = getExternalFilesDir(KEY_PREF_MAP);
        if (defaultPath == null) {
            defaultPath = new File(getFilesDir(), KEY_PREF_MAP);
        }

        String mapPath = mSharedPreferences.getString(SettingsConstants.KEY_PREF_MAP_PATH, defaultPath.getPath());
        String mapName = mSharedPreferences.getString(SettingsConstantsUI.KEY_PREF_MAP_NAME, "default");

        File mapFullPath = new File(mapPath, mapName + MAP_EXT);

        final Bitmap bkBitmap = BitmapFactory.decodeResource(
                getResources(), R.drawable.bk_tile);
        mMap = new MapDrawable(bkBitmap, this, mapFullPath, new LayerFactoryUI());
        mMap.setName(mapName);
        mMap.load();

        return mMap;
    }

    public Bitmap getMapBackground() {
        int backgroundResId;
        switch (mSharedPreferences.getString(SettingsConstantsUI.KEY_PREF_MAP_BG, "neutral")) {
            case "light":
                backgroundResId = com.nextgis.maplibui.R.drawable.bk_tile_light;
                break;
            case "dark":
                backgroundResId = com.nextgis.maplibui.R.drawable.bk_tile_dark;
                break;
            default:
                backgroundResId = com.nextgis.maplibui.R.drawable.bk_tile;
                break;
        }

        return BitmapFactory.decodeResource(getResources(), backgroundResId);
    }

    @Override
    public Account getAccount(String accountName)
    {
        if(!PermissionUtil.hasPermission(this, Manifest.permission.GET_ACCOUNTS)){
            return null;
        }

        if (mAccountManager == null) {
            if(Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "mAccountManager is NULL");
            return null;
        }
        try {
            for (Account account : mAccountManager.getAccountsByType(Constants.NGW_ACCOUNT_TYPE)) {
                if (account == null) {
                    if(Constants.DEBUG_MODE)
                        Log.d(Constants.TAG, "account for type " + Constants.NGW_ACCOUNT_TYPE + " is NULL");
                    continue;
                }
                if(Constants.DEBUG_MODE)
                    Log.d(Constants.TAG, "getAccount check account: " + account.toString());
                if (account.name.equals(accountName)) {
                    return account;
                }
            }
        }
        catch (SecurityException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public AccountManagerFuture<Boolean> removeAccount(Account account) {
        AccountManagerFuture<Boolean> bool = new AccountManagerFuture<Boolean>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Boolean getResult() throws OperationCanceledException, IOException, AuthenticatorException {
                return null;
            }

            @Override
            public Boolean getResult(long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
                return null;
            }
        };

        if(!PermissionUtil.hasPermission(this, ConstantsUI.PERMISSION_MANAGE_ACCOUNTS)){
            return bool;
        }

        if (mAccountManager == null)
            return bool;

        try {
            return mAccountManager.removeAccount(account, null, new Handler());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return bool;
    }

    @Override
    public String getAccountUrl(Account account) {
        return getAccountUserData(account, "url");
    }


    @Override
    public String getAccountLogin(Account account) {
        return getAccountUserData(account, "login");
    }

    @Override
    public String getAccountPassword(Account account)
    {
        if(!PermissionUtil.hasPermission(this, ConstantsUI.PERMISSION_AUTHENTICATE_ACCOUNTS)){
            return "";
        }

        if (mAccountManager == null)
            return "";

        try {
            return mAccountManager.getPassword(account);
        } catch (SecurityException e) {
            e.printStackTrace();
            return "";
        }
    }


    @Override
    public GpsEventSource getGpsEventSource()
    {
        return mGpsEventSource;
    }

    /**
     * Executed then application first run. One can create some data here (some layers, etc.).
     */
    protected void onFirstRun()
    {

    }


    @Override
    public boolean addAccount(String name, String url, String login, String password, String token) {
        if(!PermissionUtil.hasPermission(this, ConstantsUI.PERMISSION_AUTHENTICATE_ACCOUNTS)){
            return false;
        }

        if (mAccountManager == null)
            return false;

        final Account account = new Account(name, Constants.NGW_ACCOUNT_TYPE);

        Bundle userData = new Bundle();
        userData.putString("url", url.trim());
        userData.putString("login", login);

        try {
            boolean accountAdded = mAccountManager.addAccountExplicitly(account, password, userData);
            if (accountAdded)
                mAccountManager.setAuthToken(account, account.type, token);

            return accountAdded;
        }
        catch (SecurityException e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void setPassword(String name, String value) {
        if(!PermissionUtil.hasPermission(this, ConstantsUI.PERMISSION_AUTHENTICATE_ACCOUNTS)){
            return;
        }

        Account account = getAccount(name);
        if (null != account) {
            mAccountManager.setPassword(account, value);
        }
    }

    @Override
    public void setUserData(String name, String key, String value) {
        if(!PermissionUtil.hasPermission(this, ConstantsUI.PERMISSION_AUTHENTICATE_ACCOUNTS)){
            return;
        }

        Account account = getAccount(name);
        if (null != account) {
            mAccountManager.setUserData(account, key, value);
        }
    }

    @Override
    public String getAccountUserData(Account account, String key) {
        if(!PermissionUtil.hasPermission(this, ConstantsUI.PERMISSION_AUTHENTICATE_ACCOUNTS)){
            return "";
        }

        if (mAccountManager == null)
            return "";

        return mAccountManager.getUserData(account, key);
    }
}

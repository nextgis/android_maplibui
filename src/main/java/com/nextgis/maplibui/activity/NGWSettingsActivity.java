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

package com.nextgis.maplibui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.fragment.NGPreferenceSettingsFragment;
import com.nextgis.maplibui.fragment.NGPreferenceHeaderFragment;
import com.nextgis.maplibui.fragment.NGWSettingsFragment;
import com.nextgis.maplibui.fragment.NGWSettingsHeaderFragment;
import com.nextgis.maplibui.util.ConstantsUI;

import static com.nextgis.maplib.util.Constants.TAG;


public class NGWSettingsActivity
        extends NGPreferenceActivity
        implements OnAccountsUpdateListener
{
    public static final String KEY_ACCOUNT    = "account";

    protected AccountManager mAccountManager;

    protected final Handler mHandler = new Handler();


    @Override
    protected String getPreferenceHeaderFragmentTag()
    {
        return ConstantsUI.FRAGMENT_NGW_HEADER_SETTINGS;
    }


    @Override
    protected NGPreferenceHeaderFragment getNewPreferenceHeaderFragment()
    {
        return new NGWSettingsHeaderFragment();
    }


    @Override
    protected String getPreferenceSettingsFragmentTag()
    {
        return ConstantsUI.FRAGMENT_NGW_SETTINGS;
    }


    @Override
    protected NGPreferenceSettingsFragment getNewPreferenceSettingsFragment(String subScreenKey)
    {
        return new NGWSettingsFragment();
    }


    @Override
    public String getTitleString()
    {
        return getString(R.string.ngw_accounts);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (null == mAccountManager) {
            Context appContext = getApplicationContext();
            mAccountManager = AccountManager.get(appContext);
            Log.d(TAG, "NGWSettingsActivity: AccountManager.get(" + appContext + ")");
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
        invalidatePreferences();
    }
}

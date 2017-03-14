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
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGWLoginActivity;
import com.nextgis.maplibui.activity.NGWSettingsActivity;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import static com.nextgis.maplib.util.Constants.TAG;


public class NGWSettingsHeaderFragment
        extends NGPreferenceHeaderFragment
{
    protected AccountManager mAccountManager;


    @Override
    protected void createPreferences(PreferenceScreen screen)
    {
        if (null == mAccountManager) {
            Context appContext = mActivity.getApplicationContext();
            mAccountManager = AccountManager.get(appContext);
            Log.d(TAG, "NGWSettingsHeaderFragment: AccountManager.get(" + appContext + ")");
        }

        fillHeaders(screen);
    }


    protected void fillHeaders(PreferenceGroup screen)
    {
        IGISApplication app = (IGISApplication) mActivity.getApplication();
        if (null != mAccountManager) {
            for (final Account account : mAccountManager.getAccountsByType(app.getAccountsType())) {

                Preference preference = new Preference(mStyledContext);
                preference.setTitle(account.name);
                preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
                {
                    @Override
                    public boolean onPreferenceClick(Preference preference)
                    {
                        Bundle args = new Bundle();
                        args.putString(
                                PreferenceFragmentCompat.ARG_PREFERENCE_ROOT,
                                SettingsConstantsUI.ACTION_ACCOUNT);
                        args.putParcelable(NGWSettingsActivity.KEY_ACCOUNT, account);

                        mActivity.replaceSettingsFragment(args);
                        return true;
                    }
                });

                if (null != screen) {
                    screen.addPreference(preference);
                }
            }

            // add "Add account" preference
            Preference preference = new Preference(mStyledContext);
            preference.setTitle(R.string.ngw_account_add);
            Intent intent = new Intent(mStyledContext, NGWLoginActivity.class);
            preference.setIntent(intent);

            if (null != screen) {
                screen.addPreference(preference);
            }
        }
    }
}

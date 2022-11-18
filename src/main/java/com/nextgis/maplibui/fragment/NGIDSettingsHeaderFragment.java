/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2017-2018, 2020 NextGIS, info@nextgis.com
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGIDLoginActivity;
import com.nextgis.maplibui.util.NGIDUtils;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import static com.nextgis.maplibui.util.NGIDUtils.isLoggedIn;

public class NGIDSettingsHeaderFragment
        extends NGPreferenceHeaderFragment
{
    protected SharedPreferences mPreferences;


    @Override
    protected void createPreferences(PreferenceScreen screen)
    {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        fillHeaders(screen);
    }


    protected void fillHeaders(PreferenceGroup screen)
    {
        boolean isLoggedIn = isLoggedIn(mPreferences);
        Preference preference = new Preference(mStyledContext);
        String url = mPreferences != null ? mPreferences.getString("ngid_url", NGIDUtils.NGID_MY) : NGIDUtils.NGID_MY;
        String braces = url.replace("https://", "").replace("http://", "");
        braces = NetworkUtil.trimSlash(braces);

        if (isLoggedIn) {
            preference.setTitle(getString(R.string.ngid_my, braces));
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    Bundle args = new Bundle();
                    args.putString(
                            PreferenceFragmentCompat.ARG_PREFERENCE_ROOT,
                            SettingsConstantsUI.ACTION_ACCOUNT);
                    mActivity.replaceSettingsFragment(args);
                    return true;
                }
            });

        } else {
            preference.setTitle(R.string.login);
            Intent intent = new Intent(mStyledContext, NGIDLoginActivity.class);
            preference.setIntent(intent);
        }

        //add "New account" preference
        Preference newAccount = new Preference(mStyledContext);
        newAccount.setTitle(R.string.ngid_account_new);
        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        newAccount.setIntent(browser);

        if (null != screen) {
            screen.addPreference(newAccount);
            screen.addPreference(preference);
        }
    }
}

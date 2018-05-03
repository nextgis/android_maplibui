/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2018 NextGIS, info@nextgis.com
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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGPreferenceActivity;
import com.nextgis.maplibui.util.NGIDUtils;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import static com.nextgis.maplibui.util.NGIDUtils.signOut;

public class NGIDSettingsFragment
        extends NGPreferenceSettingsFragment
{
    protected SharedPreferences mPreferences;


    @Override
    public void createPreferences(PreferenceScreen screen)
    {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        fillPreferences(screen);
    }


    // for overriding in a subclass
    protected void fillPreferences(PreferenceGroup screen)
    {
        if (mAction != null && mAction.equals(SettingsConstantsUI.ACTION_ACCOUNT)) {
            addPreferencesFromResource(R.xml.preferences_ngid);
            fillAccountPreferences(screen);
        }
    }


    public void fillAccountPreferences(PreferenceGroup screen)
    {
        String notDefined = getString(R.string.not_set);
        String value = mPreferences.getString(NGIDUtils.PREF_USERNAME, null);
        screen.findPreference(NGIDUtils.PREF_USERNAME)
                .setSummary(TextUtils.isEmpty(value) ? notDefined : value);
        value = mPreferences.getString(NGIDUtils.PREF_EMAIL, null);
        screen.findPreference(NGIDUtils.PREF_EMAIL)
                .setSummary(TextUtils.isEmpty(value) ? notDefined : value);
        value = mPreferences.getString(NGIDUtils.PREF_FIRST_NAME, null);
        screen.findPreference(NGIDUtils.PREF_FIRST_NAME)
                .setSummary(TextUtils.isEmpty(value) ? notDefined : value);
        value = mPreferences.getString(NGIDUtils.PREF_LAST_NAME, null);
        screen.findPreference(NGIDUtils.PREF_LAST_NAME)
                .setSummary(TextUtils.isEmpty(value) ? notDefined : value);
        screen.findPreference("sign_out")
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
                {
                    @Override
                    public boolean onPreferenceClick(Preference preference)
                    {
                        signOut(mPreferences, getContext());

                        if (!NGPreferenceActivity.isMultiPane(getActivity())) {
                            mActivity.onBackPressed();
                            mActivity.onBackPressed();
                        } else {
                            mActivity.invalidatePreferences();
                        }
                        mActivity.replaceSettingsFragment(null);
                        return false;
                    }
                });
    }
}

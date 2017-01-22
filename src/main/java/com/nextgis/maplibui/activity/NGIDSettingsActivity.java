/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2017 NextGIS, info@nextgis.com
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


import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.MenuItem;

import com.nextgis.maplibui.R;
import com.nextgis.maplibui.fragment.NGIDSettingsFragment;
import com.nextgis.maplibui.util.NGIDUtils;

import java.util.List;

public class NGIDSettingsActivity extends NGPreferenceActivity {
    protected static final String ACCOUNT_ACTION = "com.nextgis.maplibui.ACCOUNT";
    SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        boolean isLoggedIn = !TextUtils.isEmpty(mPreferences.getString(NGIDUtils.PREF_ACCESS_TOKEN, ""));
        Header header = new Header();
        if (isLoggedIn) {
            header.title = getString(R.string.ngid_my);
            header.fragment = NGIDSettingsFragment.class.getName();
        } else {
            header.title = getString(R.string.login);
            header.intent = new Intent(this, NGIDLoginActivity.class);
//            header.fragment = NGIDLoginFragment.class.getName();
        }

        target.add(header);

        //add "New account" header
        header = new Header();
        header.title = getString(R.string.ngid_account_new);
        header.intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://my.nextgis.com"));
        target.add(header);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    protected void createView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
            String action = getIntent().getAction();
            if (action != null && action.equals(ACCOUNT_ACTION)) {
                addPreferencesFromResource(R.xml.preferences_ngid);
                fillAccountPreferences(screen);
                setPreferenceScreen(screen);
            } else {
                fillPreferences(screen);
                setPreferenceScreen(screen);
            }
        }
    }

    protected void fillPreferences(PreferenceScreen screen) {
        boolean isLoggedIn = !TextUtils.isEmpty(mPreferences.getString(NGIDUtils.PREF_ACCESS_TOKEN, ""));
        Preference preference = new Preference(this);

        if (isLoggedIn) {
            preference.setTitle(R.string.ngid_my);
            Intent intent = new Intent(this, NGIDSettingsActivity.class);
            intent.setAction(ACCOUNT_ACTION);
            preference.setIntent(intent);
        } else {
            preference.setTitle(R.string.login);
            Intent intent = new Intent(this, NGIDLoginActivity.class);
            preference.setIntent(intent);
        }

        //add "New account" preference
        Preference newAccount = new Preference(this);
        newAccount.setTitle(R.string.ngid_account_new);
        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse("http://my.nextgis.com"));
        newAccount.setIntent(browser);

        if (null != screen) {
            screen.addPreference(newAccount);
            screen.addPreference(preference);
        }
    }

    public void fillAccountPreferences(PreferenceScreen screen) {
        screen.findPreference("username").setSummary(mPreferences.getString("username", null));
        screen.findPreference("email").setSummary(mPreferences.getString("email", null));
        screen.findPreference("first_name").setSummary(mPreferences.getString("first_name", null));
        screen.findPreference("last_name").setSummary(mPreferences.getString("last_name", null));
    }
}

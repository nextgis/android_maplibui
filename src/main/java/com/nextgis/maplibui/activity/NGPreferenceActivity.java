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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.view.MenuItem;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.fragment.NGPreferenceSettingsFragment;
import com.nextgis.maplibui.fragment.NGPreferenceHeaderFragment;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.util.ArrayList;
import java.util.List;


/**
 * Base class for NextGIS preferences activity
 */
// http://stackoverflow.com/questions/32070186/how-to-use-the-v7-v14-preference-support-library
// http://developer.android.com/intl/ru/reference/android/support/v7/preference/PreferenceFragmentCompat.html
// http://stackoverflow.com/a/32540395
public abstract class NGPreferenceActivity
        extends NGActivity
        implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback
{
    protected List<OnInvalidatePreferencesListener> mListeners;


    protected abstract String getPreferenceHeaderFragmentTag();

    protected abstract NGPreferenceHeaderFragment getNewPreferenceHeaderFragment();

    protected abstract String getPreferenceSettingsFragmentTag();

    protected abstract NGPreferenceSettingsFragment getNewPreferenceSettingsFragment(String subScreenKey);

    public abstract String getTitleString();


    protected void setCurrentThemePref()
    {
        // do nothing
    }


    protected void refreshCurrentTheme()
    {
        // do nothing
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mListeners = new ArrayList<>();

        setContentView(R.layout.activity_settings);
        setToolbar(R.id.main_toolbar);

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener()
        {
            public void onBackStackChanged()
            {
                FragmentManager fm = getSupportFragmentManager();
                NGPreferenceSettingsFragment settings =
                        (NGPreferenceSettingsFragment) fm.findFragmentByTag(
                                getPreferenceSettingsFragmentTag());

                String title;
                if (null != settings) {
                    title = settings.getFragmentTitle();
                } else {
                    title = getTitleString();
                }
                ActionBar ab = getSupportActionBar();
                if (null != ab && title != null)
                    ab.setTitle(title);
            }
        });

        // Create the fragment only when the activity is created for the first time,
        // ie. not after orientation changes
        if (savedInstanceState == null) {
            replaceHeadersFragment();

            if (isMultiPane(this)) {
                String action = getIntent().getAction();
                if (null == action) {
                    action = SettingsConstantsUI.ACTION_PREFS_GENERAL;
                }
                Bundle args = new Bundle();
                args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, action);

                replaceSettingsFragment(args);
            }
        }
    }


    @Override
    public boolean onPreferenceStartScreen(
            PreferenceFragmentCompat preferenceFragmentCompat,
            PreferenceScreen preferenceScreen)
    {
        if (isMultiPane(this)) {
            setTitle(preferenceScreen);
        }

        onStartSubScreen(preferenceScreen);
        return true;
    }


    public void setTitle(PreferenceScreen preferenceScreen)
    {
        // TODO: for isMultiPane() change title of Settings fragment, not of Activity
        setTitle(preferenceScreen.getTitle());
    }


    protected void onStartSubScreen(PreferenceScreen preferenceScreen)
    {
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        args.putString(ConstantsUI.PREF_SCREEN_TITLE, preferenceScreen.getTitle().toString());
        replaceSettingsFragment(args);
    }


    @Override
    protected void onStart()
    {
        super.onStart();
        invalidatePreferences();
    }


    public void invalidatePreferences()
    {
        for (OnInvalidatePreferencesListener listener : mListeners) {
            listener.onInvalidatePreferences();
        }
    }


    public void replaceHeadersFragment()
    {
        FragmentManager fm = getSupportFragmentManager();

        NGPreferenceHeaderFragment fragment =
                (NGPreferenceHeaderFragment) fm.findFragmentByTag(getPreferenceHeaderFragmentTag());
        if (null != fragment) {
            removeListener(fragment);
        }

        String tag = getPreferenceHeaderFragmentTag();
        fragment = getNewPreferenceHeaderFragment();
        addListener(fragment);

        FragmentTransaction ft = fm.beginTransaction();
        if (isMultiPane(this)) {
            ft.replace(R.id.header_fragment, fragment, tag);
        } else {
            ft.replace(R.id.setting_fragment, fragment, tag);
        }
        ft.commit();
    }


    public void replaceSettingsFragment(Bundle args)
    {
        FragmentManager fm = getSupportFragmentManager();

        NGPreferenceSettingsFragment fragment = (NGPreferenceSettingsFragment) fm.findFragmentByTag(
                getPreferenceSettingsFragmentTag());
        if (null != fragment) {
            removeListener(fragment);
        }

        String subScreenKey = null;
        if (null != args) {
            subScreenKey = args.getString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT);
        }

        String tag = getPreferenceSettingsFragmentTag();
        fragment = getNewPreferenceSettingsFragment(subScreenKey);
        addListener(fragment);

        if (null != args) {
            fragment.setArguments(args);
        }

        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.setting_fragment, fragment, tag);
        if (!isMultiPane(this)) {
            ft.addToBackStack(tag);
        }
        ft.commit();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public static boolean isMultiPane(Activity activity)
    {
        return activity.findViewById(R.id.header_fragment) != null;
    }


    public void addListener(OnInvalidatePreferencesListener listener)
    {
        if (mListeners != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }


    public void removeListener(OnInvalidatePreferencesListener listener)
    {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }


    public interface OnInvalidatePreferencesListener
    {
        void onInvalidatePreferences();
    }
}

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

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import com.nextgis.maplibui.activity.NGPreferenceActivity;


public abstract class NGPreferenceFragment
        extends PreferenceFragmentCompat
        implements NGPreferenceActivity.OnInvalidatePreferencesListener
{
    protected Context              mStyledContext;
    protected NGPreferenceActivity mActivity;


    protected abstract void createPreferences(PreferenceScreen screen);


    @Override
    public void onCreatePreferences(
            Bundle savedInstanceState,
            String rootKey)
    {
        mActivity = (NGPreferenceActivity) getActivity();
        mStyledContext = getPreferenceManager().getContext();

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(mStyledContext);
        // https://groups.google.com/d/msg/android-developers/nbRw6OzWXYY/N2ckkMXmzhoJ
        // 1st setPreferenceScreen() then others
        setPreferenceScreen(screen);

        createPreferences(screen);
    }


    @Override
    public void onInvalidatePreferences()
    {
        PreferenceScreen screen = getPreferenceScreen();
        if (null != screen) {
            screen.removeAll();
            createPreferences(screen);
        }
    }
}

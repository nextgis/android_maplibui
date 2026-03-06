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

import android.os.Bundle;

import com.nextgis.maplibui.activity.NGPreferenceActivity;
import com.nextgis.maplibui.util.ConstantsUI;


public abstract class NGPreferenceSettingsFragment
        extends NGPreferenceFragment
{
    protected String mAction;


    @Override
    public void onCreatePreferences(
            Bundle savedInstanceState,
            String rootKey)
    {
        mAction = rootKey;
        if (null == rootKey) {
            return;
        }

        super.onCreatePreferences(savedInstanceState, rootKey);
    }


    public String getFragmentTitle()
    {
        Bundle args = getArguments();
        if (null != args) {
            return args.getString(ConstantsUI.PREF_SCREEN_TITLE);
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (getActivity() instanceof NGPreferenceActivity)
            ((NGPreferenceActivity)getActivity()).removeListener(this);
    }
}

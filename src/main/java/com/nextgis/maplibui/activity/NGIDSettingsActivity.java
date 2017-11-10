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

import com.nextgis.maplibui.R;
import com.nextgis.maplibui.fragment.NGIDSettingsFragment;
import com.nextgis.maplibui.fragment.NGIDSettingsHeaderFragment;
import com.nextgis.maplibui.fragment.NGPreferenceSettingsFragment;
import com.nextgis.maplibui.fragment.NGPreferenceHeaderFragment;
import com.nextgis.maplibui.util.ConstantsUI;


public class NGIDSettingsActivity
        extends NGPreferenceActivity
{
    @Override
    protected String getPreferenceHeaderFragmentTag()
    {
        return ConstantsUI.FRAGMENT_NGID_HEADER_SETTINGS;
    }


    @Override
    protected NGPreferenceHeaderFragment getNewPreferenceHeaderFragment()
    {
        return new NGIDSettingsHeaderFragment();
    }


    @Override
    protected String getPreferenceSettingsFragmentTag()
    {
        return ConstantsUI.FRAGMENT_NGID_SETTINGS;
    }


    @Override
    protected NGPreferenceSettingsFragment getNewPreferenceSettingsFragment(String subScreenKey)
    {
        return new NGIDSettingsFragment();
    }


    @Override
    public String getTitleString()
    {
        return getString(R.string.ngid_settings);
    }
}

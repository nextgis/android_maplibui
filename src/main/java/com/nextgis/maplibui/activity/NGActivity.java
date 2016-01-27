/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.SettingsConstantsUI;


/**
 * A base activity for NextGIS derived activities
 */
public class NGActivity
        extends AppCompatActivity
{
    protected final static String KEY_CURRENT_THEME = "current_theme";

    protected SharedPreferences mPreferences;
    protected boolean           mIsDarkTheme;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mIsDarkTheme =
                mPreferences.getString(SettingsConstantsUI.KEY_PREF_THEME, "light").equals("dark");
        setCurrentThemePref();
        setTheme(getThemeId());
        super.onCreate(savedInstanceState);
    }


    public int getThemeId()
    {
        return mIsDarkTheme
               ? R.style.Theme_NextGIS_AppCompat_Dark
               : R.style.Theme_NextGIS_AppCompat_Light;
    }


    // for overriding in a subclass
    protected void setCurrentThemePref()
    {
        mPreferences.edit().putString(KEY_CURRENT_THEME, mIsDarkTheme ? "dark" : "light").commit();
    }


    // for overriding in a subclass
    protected void refreshCurrentTheme()
    {
        String newTheme = mPreferences.getString(SettingsConstantsUI.KEY_PREF_THEME, "light");
        String currentTheme = mPreferences.getString(KEY_CURRENT_THEME, "light");

        if (!newTheme.equals(currentTheme)) {
            refreshActivityView();
        }
    }


    /**
     * This hook is called whenever an item in your options menu is selected. The default
     * implementation simply returns false to have the normal processing happen (calling the item's
     * Runnable or sending a message to its Handler as appropriate).  You can use this method for
     * any items for which you would like to do processing without those other facilities. <p/>
     * <p>Derived classes should call through to the base class for it to perform the default menu
     * handling.</p>
     *
     * @param item
     *         The menu item that was selected.
     *
     * @return boolean Return false to allow normal menu processing to proceed, true to consume it
     * here.
     *
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isHomeEnabled()) {
                    finish();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    protected boolean isHomeEnabled()
    {
        return true;
    }


    protected void setToolbar(int toolbarId)
    {
        Toolbar toolbar = (Toolbar) findViewById(toolbarId);
        if (null == toolbar) {
            return;
        }
        toolbar.getBackground().setAlpha(getToolbarAlpha());
        setSupportActionBar(toolbar);

        if (null != getSupportActionBar()) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    protected int getToolbarAlpha()
    {
        return 255; // not transparent
    }


    @Override
    protected void onResume()
    {
        refreshCurrentTheme();
        super.onResume();
    }


    public void refreshActivityView()
    {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }


    public void updateMenuView()
    {
        supportInvalidateOptionsMenu();
    }
}

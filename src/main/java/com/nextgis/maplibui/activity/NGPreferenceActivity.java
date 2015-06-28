/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.activity;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.SettingsConstantsUI;


/**
 * Base class for NextGIS preferences activity
 */
public class NGPreferenceActivity
        extends PreferenceActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        boolean bDarkTheme = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsConstantsUI.KEY_PREF_DARKTHEME, false);
        setTheme(getThemeId(bDarkTheme));

        super.onCreate(savedInstanceState);

        ViewGroup root = ((ViewGroup) findViewById(android.R.id.content));
        if (null != root) {
            View content = root.getChildAt(0);
            if (null != content) {

                // create ContextThemeWrapper from the original Activity Context with the custom theme
                final Context contextThemeWrapper = new ContextThemeWrapper(this, getThemeId(bDarkTheme));

                LinearLayout toolbarContainer = (LinearLayout) View.inflate(
                        contextThemeWrapper, R.layout.activity_settings, null);

                root.removeAllViews();
                toolbarContainer.addView(content);
                root.addView(toolbarContainer);

                Toolbar toolbar = (Toolbar) toolbarContainer.findViewById(R.id.main_toolbar);
                toolbar.getBackground().setAlpha(255);
                toolbar.setTitle(getTitle());
                toolbar.setNavigationIcon(R.drawable.ic_action_home_light);

                toolbar.setNavigationOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                NGPreferenceActivity.this.finish();
                            }
                        });


            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected int getThemeId(boolean isDark){
        if(isDark)
            return com.nextgis.maplibui.R.style.Theme_NextGIS_AppCompat_Dark;
        else
            return com.nextgis.maplibui.R.style.Theme_NextGIS_AppCompat_Light;
    }
}
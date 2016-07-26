/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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
import android.support.annotation.MenuRes;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;

import com.nextgis.maplibui.util.ControlHelper;


/**
 * Bottom toolbar
 */
public class BottomToolbar
        extends Toolbar
{

    protected boolean mIsMenuInitialized;


    public BottomToolbar(Context context)
    {
        super(context);
        mIsMenuInitialized = false;
    }


    public BottomToolbar(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
        mIsMenuInitialized = false;
    }


    public BottomToolbar(
            Context context,
            AttributeSet attrs,
            int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        mIsMenuInitialized = false;
    }

    @Override
    public void inflateMenu(@MenuRes int resId) {
        super.inflateMenu(resId);
        Menu menu = getMenu();
        MenuItem item = menu.getItem(0);
        int size = item.getIcon().getIntrinsicWidth() + ControlHelper.dpToPx(30, getResources());
        int width = getWidth();

        for (int i = 0; i < menu.size(); i++) {
            item = menu.getItem(i);
            if (size * (i + 2) < width)
                MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
            else
                break;
        }
    }
}

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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

package com.nextgis.maplibui;

import android.content.Context;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.widget.ActionMenuPresenter;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.View;


/**
 * Bottom toolbar
 */
public class BottomToolbar extends Toolbar {

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
    public Menu getMenu()
    {
        MenuBuilder menuBuilder = (MenuBuilder) super.getMenu();
        if(!mIsMenuInitialized) {
            ActionMenuPresenter presenter = new ActionMenuPresenter(getContext());
            presenter.setWidthLimit(getContext().getResources().getDisplayMetrics().widthPixels,
                                    true);
            presenter.setItemLimit(Integer.MAX_VALUE);
            menuBuilder.addMenuPresenter(presenter, new ContextThemeWrapper(getContext(), getPopupTheme()));

            ActionMenuView menuView = null;
            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);
                if (view instanceof ActionMenuView) {
                    menuView = (ActionMenuView) view;
                    break;
                }
            }
            presenter.setMenuView(menuView);

            /* center menu buttons in toolbar
            ViewGroup.LayoutParams params = menuView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            menuView.setLayoutParams(params);
            */
            mIsMenuInitialized = true;
        }
        return menuBuilder;
    }
}

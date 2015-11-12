/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;

import com.nextgis.maplibui.R;

public class NGDialog extends DialogFragment {
    protected final static String KEY_TITLE = "title";

    protected Context mContext;
    protected Activity mActivity;
    protected String mTitle;
    protected int mTheme, mDialogTheme;

    public NGDialog setTitle(String title) {
        mTitle = title;
        return this;
    }

    public NGDialog setTheme(int themeId) {
        mTheme = themeId;
        return this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mActivity = getActivity();
        mContext = new ContextThemeWrapper(mActivity, mTheme);

        int[] attrs = {android.R.attr.alertDialogStyle};
        TypedArray ta = mContext.obtainStyledAttributes(mTheme, attrs);
        mDialogTheme = ta.getResourceId(0, R.style.Theme_NextGIS_AppCompat_Light_Dialog);
        ta.recycle();

        if (null != savedInstanceState)
            mTitle = savedInstanceState.getString(KEY_TITLE);

        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_TITLE, mTitle);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Context getContext()
    {
        return mActivity;
    }
}

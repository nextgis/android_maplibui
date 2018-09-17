/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016, 2018 NextGIS, info@nextgis.com
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
import android.widget.Button;

import com.nextgis.maplibui.R;

public class NGDialog extends DialogFragment {
    protected final static String KEY_TITLE = "title";
    protected final static String KEY_THEME = "theme";

    protected Context mContext;
    protected Activity mActivity;
    protected String mTitle;
    protected int mEnabledColor, mDisabledColor;
    protected int mTheme, mDialogTheme;

    public NGDialog setTitle(String title) {
        mTitle = title;
        return this;
    }

    public NGDialog setTheme(int themeId) {
        mTheme = themeId;
        return this;
    }

    protected void setEnabled(Button button, boolean state) {
        button.setEnabled(state);
        button.setTextColor(state ? mEnabledColor : mDisabledColor);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mActivity = getActivity();
        mContext = new ContextThemeWrapper(mActivity, mTheme);
        mDisabledColor = getResources().getColor(R.color.color_grey_400);

        int[] attrs = {android.R.attr.alertDialogStyle};
        TypedArray ta = mContext.obtainStyledAttributes(mTheme, attrs);
        mDialogTheme = ta.getResourceId(0, R.style.Theme_NextGIS_AppCompat_Light_Dialog);
        ta.recycle();

        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString(KEY_TITLE);
            setTheme(savedInstanceState.getInt(KEY_THEME));
        }

        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_TITLE, mTitle);
        outState.putInt(KEY_THEME, mTheme);
    }

    @Override
    public Context getContext()
    {
        return mActivity;
    }
}

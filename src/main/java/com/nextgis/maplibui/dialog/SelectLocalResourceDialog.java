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

package com.nextgis.maplibui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ISelectResourceDialog;

import java.io.File;
import java.util.ArrayList;


/**
 * Local folder/file select dialog
 */
public class SelectLocalResourceDialog
        extends DialogFragment
        implements ISelectResourceDialog
{
    protected String                    mTitle;
    protected int                       mTypeMask;
    protected boolean                   mCanSelectMulti;
    protected boolean                   mCanWrite;
    protected File                      mPath;
    protected LocalResourcesListAdapter mListAdapter;

    protected AlertDialog mDialog;

    protected final static String KEY_TITLE       = "title";
    protected final static String KEY_MASK        = "mask";
    protected final static String KEY_STATES      = "states";
    protected final static String KEY_CANMULTISEL = "can_multiselect";
    protected final static String KEY_WRITABLE    = "can_write";
    protected final static String KEY_PATH        = "path";


    public SelectLocalResourceDialog setTitle(String title)
    {
        mTitle = title;
        return this;
    }


    public SelectLocalResourceDialog setTypeMask(int typeMask)
    {
        mTypeMask = typeMask;
        return this;
    }


    public SelectLocalResourceDialog setCanSelectMultiple(boolean can)
    {
        mCanSelectMulti = can;
        return this;
    }


    public SelectLocalResourceDialog setWritable(boolean can)
    {
        mCanWrite = can;
        return this;
    }


    public SelectLocalResourceDialog setPath(File path)
    {
        mPath = path;
        return this;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final Context context = new ContextThemeWrapper(getActivity(), R.style.Theme_NextGIS_AppCompat_Light_Dialog);

        mListAdapter = new LocalResourcesListAdapter(this);

        if (null == savedInstanceState) {
            //first launch, lets fill connections array
            mListAdapter.setCheckState(new ArrayList<String>());
        } else {
            mTitle = savedInstanceState.getString(KEY_TITLE);
            mTypeMask = savedInstanceState.getInt(KEY_MASK);
            mCanSelectMulti = savedInstanceState.getBoolean(KEY_CANMULTISEL);
            mCanWrite = savedInstanceState.getBoolean(KEY_WRITABLE);
            mListAdapter.setCheckState(savedInstanceState.getStringArrayList(KEY_STATES));
            mPath = (File) savedInstanceState.getSerializable(KEY_PATH);
        }

        View view = View.inflate(context, R.layout.layout_resources, null);
        ListView dialogListView = (ListView) view.findViewById(R.id.listView);
        mListAdapter.setTypeMask(mTypeMask);
        mListAdapter.setCurrentPath(mPath);
        mListAdapter.setCanSelectMulti(mCanSelectMulti);
        mListAdapter.setCanWrite(mCanWrite);
        dialogListView.setAdapter(mListAdapter);
        dialogListView.setOnItemClickListener(mListAdapter);

        LinearLayout pathView = (LinearLayout) view.findViewById(R.id.path);
        mListAdapter.setPathLayout(pathView);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(mTitle).setView(view).setInverseBackgroundForced(true).setPositiveButton(
                R.string.select, new DialogInterface.OnClickListener()
                {
                    public void onClick(
                            DialogInterface dialog,
                            int id)
                    {

                    }
                }).setNegativeButton(
                R.string.cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick(
                            DialogInterface dialog,
                            int id)
                    {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString(KEY_TITLE, mTitle);
        outState.putInt(KEY_MASK, mTypeMask);
        outState.putBoolean(KEY_CANMULTISEL, mCanSelectMulti);
        outState.putBoolean(KEY_WRITABLE, mCanWrite);
        outState.putStringArrayList(KEY_STATES, (ArrayList<String>) mListAdapter.getCheckState());
        outState.putSerializable(KEY_PATH, mListAdapter.getCurrentPath());
        super.onSaveInstanceState(outState);
    }


    @Override
    public Context getContext()
    {
        return getActivity();
    }


    @Override
    public void updateButtons()
    {
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(mListAdapter.getCheckState().size() > 0);
    }
}

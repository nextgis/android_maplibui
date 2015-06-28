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
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IChooseColorResult;

import java.util.List;


/**
 * Color picker
 */
public class ChooseColorDialog
        extends DialogFragment
        implements AdapterView.OnItemClickListener
{
    protected String                      mTitle;
    protected List<Pair<Integer, String>> mColors;
    protected ChooseColorListAdapter      mColorsListAdapter;

    protected final static String KEY_TITLE  = "title";
    protected final static String KEY_COLORS = "color";


    public ChooseColorDialog setTitle(String title)
    {
        mTitle = title;
        return this;
    }


    public ChooseColorDialog setColors(List<Pair<Integer, String>> colors)
    {
        mColors = colors;
        return this;
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString(KEY_TITLE, mTitle);
        outState.putSerializable(KEY_COLORS, (java.io.Serializable) mColors);
        super.onSaveInstanceState(outState);
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final Context context = new ContextThemeWrapper(getActivity(), R.style.Theme_NextGIS_AppCompat_Light_Dialog);


        if (null != savedInstanceState) {
            mTitle = savedInstanceState.getString(KEY_TITLE);
            mColors = (List<Pair<Integer, String>>) savedInstanceState.getSerializable(KEY_COLORS);
        }

        mColorsListAdapter = new ChooseColorListAdapter(getActivity(), mColors);

        View view = View.inflate(context, R.layout.layout_layers, null);
        ListView dialogListView = (ListView) view.findViewById(R.id.listView);
        dialogListView.setAdapter(mColorsListAdapter);
        dialogListView.setOnItemClickListener(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(mTitle).setView(view).setInverseBackgroundForced(true).setNegativeButton(
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
    public void onItemClick(
            AdapterView<?> parent,
            View view,
            int position,
            long id)
    {
        //send event to parent
        IChooseColorResult activity = (IChooseColorResult) getActivity();
        if (null != activity) {
            TextView tv = (TextView) view.findViewById(R.id.color_name);
            String colorName = (String) tv.getText();
            for (Pair<Integer, String> entry : mColors) {
                if (colorName.equals(entry.second)) {
                    activity.onFinishChooseColorDialog(entry.first);
                    break;
                }
            }
        }

        dismiss();
    }

}

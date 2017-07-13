/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016-2017 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.mapui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.map.NGWWebMapLayer;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.TMSLayerSettingsActivity;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.dialog.SelectZoomLevelsDialog;
import com.nextgis.maplibui.util.ClearCacheTask;
import com.nextgis.maplibui.util.ConstantsUI;

import java.io.File;

public class NGWWebMapLayerUI extends NGWWebMapLayer implements ILayerUI {
    public NGWWebMapLayerUI(Context context, File path) {
        super(context, path);
    }

    @Override
    public Drawable getIcon(Context context) {
        return ContextCompat.getDrawable(mContext, R.drawable.ic_ngw_webmap);
    }

    @Override
    public void changeProperties(Context context) {
        Intent settings = new Intent(context, TMSLayerSettingsActivity.class);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settings.putExtra(ConstantsUI.KEY_LAYER_ID, getId());
        context.startActivity(settings);
    }

    public void downloadTiles(Context context, GeoEnvelope env) {
        FragmentActivity fragmentActivity = (FragmentActivity) context;
        SelectZoomLevelsDialog newFragment = new SelectZoomLevelsDialog();
        newFragment.setEnvelope(env).setLayerId(getId()).
                show(fragmentActivity.getSupportFragmentManager(), "select_zoom_levels");
    }

    public void showLayersDialog(final MapView map, final Activity activity) {
        CharSequence[] names = new CharSequence[mChildren.size()];
        final boolean[] visible = new boolean[mChildren.size()];
        for (int i = 0; i < mChildren.size(); i++) {
            names[i] = mChildren.get(i).getName();
            visible[i] = mChildren.get(i).isVisible();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.track_list)
                .setMultiChoiceItems(names, visible, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        mChildren.get(i).setVisible(b);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String oldUrl = getURL();
                        String newUrl = updateURL();

                        if (!oldUrl.equals(newUrl)) {
                            DialogInterface.OnDismissListener listener = new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    if (mBitmapCache != null)
                                        mBitmapCache.clear();

                                    map.drawMapDrawable();
                                }
                            };

                            new ClearCacheTask(activity, listener).execute(getPath());
                            save();
                        }
                    }
                });

        builder.show();
    }

}

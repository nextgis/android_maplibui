/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016, 2018-2019 NextGIS, info@nextgis.com
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

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.service.TrackerService;
import com.nextgis.maplibui.util.LayerUtil;
import com.nextgis.maplibui.util.TrackView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import yuku.ambilwarna.AmbilWarnaDialog;

import static com.nextgis.maplib.map.TrackLayer.getSelection;
import static com.nextgis.maplibui.service.TrackerService.isTrackerServiceRunning;

public class TracksActivity extends NGActivity implements ActionMode.Callback {
    private final static String BUNDLE_ACTION_MODE = "IS_IN_ACTION_MODE";

    private Uri mContentUriTracks, mContentUriTrackPoints;
    private TrackView mTracks;
    private ActionMode mActionMode;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);
        setToolbar(R.id.main_toolbar);

        mTracks = findViewById(R.id.lv_tracks);
        mTracks.setOnDataChangeListener(new TrackView.OnDataChangeListener() {
            @Override
            public void onDataChanged() {
                checkItemsCount();
            }
        });
        mTracks.setOnCheckedChangeListener(new TrackView.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged() {
                int count = mTracks.getSelectedItemsCount();
                if (count == 0 && mActionMode != null)
                    mActionMode.finish();

                if (count > 0 && mActionMode == null)
                    mActionMode = startSupportActionMode(TracksActivity.this);

                if (mActionMode != null)
                    mActionMode.setTitle("" + count);
            }
        });

        IGISApplication application = (IGISApplication) getApplication();
        String authority = application.getAuthority();
        mContentUriTracks = Uri.parse("content://" + authority + "/" + TrackLayer.TABLE_TRACKS);
        mContentUriTrackPoints = Uri.parse("content://" + authority + "/" + TrackLayer.TABLE_TRACKPOINTS);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_ACTION_MODE, mActionMode != null);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getBoolean(BUNDLE_ACTION_MODE)) {
            mActionMode = startSupportActionMode(this);
            mActionMode.setTitle("" + mTracks.getSelectedItemsCount());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tracks, menu);
        return true;
    }

    private void showStats() {
        mProgress = new ProgressDialog(this);
        mProgress.setTitle(R.string.stats);
        mProgress.setMessage(getString(R.string.preparing));
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();
        getStats();
    }

    private void getStats() {
        ContentResolver resolver = getContentResolver();
        String sort = TrackLayer.FIELD_TIMESTAMP + " ASC";
        Cursor points = resolver.query(mContentUriTrackPoints, null, null, null, sort);
        int total = 0, sent = 0;
        DateFormat df = SimpleDateFormat.getDateTimeInstance();
        String last = "-";
        if (points != null) {
            total = points.getCount();
            if (points.moveToLast()) {
                int id = points.getColumnIndex(TrackLayer.FIELD_TIMESTAMP);
                long lastL = points.getLong(id);
                last = df.format(new Date(lastL));
            }
            points.close();
        }

        String selection = TrackLayer.FIELD_SENT + " = 1";
        points = resolver.query(mContentUriTrackPoints, null, selection, null, sort);
        if (points != null) {
            sent = points.getCount();
            points.close();
        }

        if (mProgress != null)
            mProgress.dismiss();

        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle(R.string.stats)
                .setMessage(getString(R.string.trackpoints_stats, total, sent, last))
                .setPositiveButton(R.string.ok, null).create();
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_info) {
            showStats();
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.action_tracks, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.menu_delete) {
            Intent trackerService = new Intent(getApplicationContext(), TrackerService.class);

            if (isTrackerServiceRunning(getApplicationContext())) {
                stopService(trackerService);
                Toast.makeText(getApplicationContext(), R.string.unclosed_track_deleted, Toast.LENGTH_SHORT).show();
            }

            String selection = getSelection(mTracks.getSelectedItemsCount());
            String[] args = mTracks.getSelectedItemsIds();
            getContentResolver().delete(mContentUriTracks, selection, args);
            closeActionMode();
        } else if (id == R.id.menu_select_all) {
            mTracks.selectAll();
            actionMode.setTitle("" + mTracks.getSelectedItemsCount());
        } else if (id == R.id.menu_visibility_on) {
            changeVisibility(true);
        } else if (id == R.id.menu_visibility_off) {
            changeVisibility(false);
        } else if (id == R.id.menu_share) {
            String[] args = mTracks.getSelectedItemsIds();
            LayerUtil.shareTrackAsGPX(this, "NextGIS Mobile", args);
        } else if (id == R.id.menu_color) {
            int initColor = 0;
            if (mTracks.getSelectedItemsCount() == 1)
                initColor = TrackLayer.getColor(this, mContentUriTracks, Long.parseLong(mTracks.getSelectedItemsIds()[0]));

            AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, initColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    changeColor(color);
                }

                @Override
                public void onCancel(AmbilWarnaDialog dialog) { }
            });
            dialog.show();
        }

        return true;
    }

    protected void changeVisibility(boolean visible) {
        ContentValues cv = new ContentValues();
        cv.put(TrackLayer.FIELD_VISIBLE, visible);
        update(cv);
    }


    protected void changeColor(int color) {
        ContentValues cv = new ContentValues();
        cv.put(TrackLayer.FIELD_COLOR, color);
        update(cv);
    }

    protected void update(ContentValues cv) {
        String selection = getSelection(mTracks.getSelectedItemsCount());
        String[] args = mTracks.getSelectedItemsIds();
        getContentResolver().update(mContentUriTracks, cv, selection, args);
    }

    protected void closeActionMode() {
        mTracks.unselectAll();

        if (mActionMode != null)
            mActionMode.finish();
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        mTracks.unselectAll();
        mActionMode = null;
    }

    private void checkItemsCount() {
        if (mTracks.getCount() == 0)
            findViewById(R.id.tv_empty_list).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.tv_empty_list).setVisibility(View.GONE);
    }
}

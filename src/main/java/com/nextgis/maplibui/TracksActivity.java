/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
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

import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.service.TrackerService;

import java.util.ArrayList;
import java.util.List;


public class TracksActivity
        extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor>
{
    private static final int TRACKS_ID = 0;

    private Uri                 mContentUriTracks;
    private SimpleCursorAdapter mSimpleCursorAdapter;
    private List<String>        mIds;
    private ListView            mTracks;
    private ActionMode          mActionMode;
    private ActionMode.Callback mActionCallback;
    private boolean mSelectState = false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tracks);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.getBackground().setAlpha(255);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        IGISApplication application = (IGISApplication) getApplication();

        mContentUriTracks = Uri.parse(
                "content://" + application.getAuthority() + "/" + TrackLayer.TABLE_TRACKS);

        mIds = new ArrayList<>();

        String[] from = new String[] {TrackLayer.FIELD_NAME, TrackLayer.FIELD_VISIBLE};
        int[] to = new int[] {R.id.tv_name, R.id.iv_visibility};

        mSimpleCursorAdapter =
                new SimpleCursorAdapter(this, R.layout.layout_track_row, null, from, to,
                                        CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        mSimpleCursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
        {
            public boolean setViewValue(
                    final View view,
                    Cursor cursor,
                    int columnIndex)
            {
                String id = cursor.getString(0);
                final View rootView = (View) view.getParent();
                rootView.setTag(id);

                if (view instanceof TextView) {
                    ((TextView) view).setText(cursor.getString(columnIndex));
                }

                if (view instanceof ImageView) {
                    int visibleColumnID = cursor.getColumnIndex(TrackLayer.FIELD_VISIBLE);
                    boolean isVisible = cursor.getInt(visibleColumnID) != 0;

                    setImage(view, isVisible);
                    view.setTag(isVisible);
                    view.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            boolean isVisible = !(boolean) v.getTag();
                            updateRecord(isVisible, (String) rootView.getTag());

                            setImage(v, isVisible);
                            v.setTag(isVisible);
                        }
                    });

                    CheckBox cb = (CheckBox) rootView.findViewById(R.id.cb_item);
                    cb.setChecked(mIds.contains(id));
                    cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                    {
                        @Override
                        public void onCheckedChanged(
                                CompoundButton buttonView,
                                boolean isChecked)
                        {
                            updateSelectedItems(isChecked, (String) rootView.getTag());
                        }
                    });
                }

                return true;
            }


            private void setImage(
                    View view,
                    boolean visibility)
            {
                ((ImageView) view).setImageResource(visibility
                                                    ? R.drawable.ic_action_visibility_on
                                                    : R.drawable.ic_action_visibility_off);
            }
        });

        mTracks = (ListView) findViewById(R.id.lv_tracks);
        mTracks.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        mTracks.setItemsCanFocus(false);

        mTracks.setAdapter(mSimpleCursorAdapter);

        mActionCallback = new ActionMode.Callback()
        {
            @Override
            public boolean onCreateActionMode(
                    ActionMode actionMode,
                    Menu menu)
            {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.menu_tracks, menu);
                getSupportActionBar().hide();
                return true;
            }


            @Override
            public boolean onPrepareActionMode(
                    ActionMode actionMode,
                    Menu menu)
            {
                return false;
            }


            @Override
            public boolean onActionItemClicked(
                    ActionMode actionMode,
                    MenuItem menuItem)
            {
                int id = menuItem.getItemId();

                if (id == R.id.menu_delete) {
                    if (mIds.size() > 0) {
                        Intent trackerService = new Intent(getApplicationContext(), TrackerService.class);

                        if (isTrackerServiceRunning(getApplicationContext())) {
                            stopService(trackerService);
                            Toast.makeText(getApplicationContext(), R.string.unclosed_track_deleted, Toast.LENGTH_SHORT).show();
                        }

                        String selection = TrackLayer.FIELD_ID + " IN (" + makePlaceholders() + ")";
                        String[] args = mIds.toArray(new String[mIds.size()]);
                        getContentResolver().delete(mContentUriTracks, selection, args);
                        mIds.clear();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.nothing_selected, Toast.LENGTH_SHORT).show();
                    }

                    actionMode.finish();
                } else if (id == R.id.menu_select_all) {
                    mSelectState = !mSelectState;
                    setSelection();
                } else if (id == R.id.menu_visibility_on || id == R.id.menu_visibility_off) {
                    boolean isShow = id == R.id.menu_visibility_on;
                    SparseBooleanArray checkedItems = mTracks.getCheckedItemPositions();

                    for (int i = 0; i < mTracks.getAdapter().getCount(); i++) {
                        if (checkedItems.get(i)) {
                            ImageView view =
                                    (ImageView) mTracks.getAdapter().getView(i, null, mTracks).findViewById(R.id.iv_visibility);

                            if (isShow) {
                                if (!(boolean) view.getTag())
                                    view.performClick();
                            } else {
                                if ((boolean) view.getTag())
                                    view.performClick();
                            }
                        }
                    }

                    actionMode.finish();
                }

                return true;
            }


            @Override
            public void onDestroyActionMode(ActionMode actionMode)
            {
                mSelectState = false;
                setSelection();
                mActionMode = null;
                getSupportActionBar().show();
            }
        };

        mTracks.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id)
            {
                if (mActionMode == null)
                    mActionMode = getSupportActionBar().startActionMode(mActionCallback);

                CheckBox cb = (CheckBox) view.findViewById(R.id.cb_item);
                boolean isChecked = !cb.isChecked();
                cb.setChecked(isChecked);
                mTracks.setItemChecked(position, isChecked);
            }
        });

        getSupportLoaderManager().initLoader(TRACKS_ID, null, this);
    }


    private void setSelection()
    {
        int childrenCount = mTracks.getCount();

        for (int i = 0; i < childrenCount; i++) {
            View parent = mTracks.getAdapter().getView(i, null, mTracks);
            CheckBox view = (CheckBox) parent.findViewById(R.id.cb_item);

            if (mSelectState != view.isChecked())
                mTracks.performItemClick(parent, i, i);
        }

        mTracks.invalidateViews();
    }


    private void updateRecord(
            boolean visibility,
            String id)
    {
        ContentValues cv = new ContentValues();
        cv.put(TrackLayer.FIELD_VISIBLE, visibility);
        getContentResolver().update(Uri.withAppendedPath(mContentUriTracks, id), cv, null, null);
    }


    private void updateSelectedItems(
            boolean isChecked,
            String id)
    {
        if (isChecked) {
            if (!mIds.contains(id))
                mIds.add(id);
        } else {
            mIds.remove(id);
        }

        if (mActionMode != null) {
            if (mIds.size() == 0)
                mActionMode.finish();
            else
                mActionMode.setTitle(mIds.size() + getString(R.string.cab_selected));
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private String makePlaceholders()
    {
        int size = mIds.size();
        StringBuilder sb = new StringBuilder(size * 2 - 1);
        sb.append("?");

        for (int i = 1; i < size; i++) {
            sb.append(",?");
        }

        return sb.toString();
    }


    @Override
    public Loader<Cursor> onCreateLoader(
            int id,
            Bundle args)
    {
        String[] proj = new String[] {
                TrackLayer.FIELD_ID, TrackLayer.FIELD_NAME, TrackLayer.FIELD_VISIBLE};
//        String selection =
//                TrackLayer.FIELD_END + " IS NOT NULL AND " + TrackLayer.FIELD_END + " != ''";

        return new CursorLoader(this, mContentUriTracks, proj, null, null, null);
    }


    @Override
    public void onLoadFinished(
            Loader<Cursor> loader,
            Cursor data)
    {
        mSimpleCursorAdapter.swapCursor(data);

        if (data.getCount() == 0) {
            findViewById(R.id.tv_empty_list).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.tv_empty_list).setVisibility(View.GONE);
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        mSimpleCursorAdapter.swapCursor(null);
    }


    public static boolean isTrackerServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (TrackerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

}

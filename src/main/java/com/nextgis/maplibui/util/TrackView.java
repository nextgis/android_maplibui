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

package com.nextgis.maplibui.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplibui.R;

import java.util.ArrayList;

public class TrackView extends ListView implements LoaderManager.LoaderCallbacks<Cursor> {
    protected static final String BUNDLE_SELECTED_ITEMS_ID = "selected_items";

    private static final int TRACKS_ID = 0;

    private Context mContext;
    private ArrayList<String> mSelectedIds;
    private Drawable mVisibilityOn, mVisibilityOff;

    private TrackAdapter mTrackAdapter;
    private Uri mContentUriTracks;

    private OnCheckedChangeListener mCheckedChangeListener;
    private OnDataChangeListener mDataChangeListener;

    public interface OnCheckedChangeListener {
        void onCheckedChanged();
    }

    public interface OnDataChangeListener {
        void onDataChanged();
    }

    public TrackView(Context context) {
        super(context);
        init(context);
    }

    public TrackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TrackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mSelectedIds = new ArrayList<>();

        int[] attrs = new int[]{R.attr.ic_action_visibility_on, R.attr.ic_action_visibility_off};
        TypedArray ta = mContext.obtainStyledAttributes(attrs);
        mVisibilityOn = ta.getDrawable(0);
        mVisibilityOff = ta.getDrawable(1);
        ta.recycle();

        setItemsCanFocus(false);
        setChoiceMode(CHOICE_MODE_MULTIPLE);

        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            IGISApplication application = (IGISApplication) activity.getApplication();
            mContentUriTracks = Uri.parse("content://" + application.getAuthority() + "/" + TrackLayer.TABLE_TRACKS);

            String[] from = new String[]{TrackLayer.FIELD_NAME, TrackLayer.FIELD_VISIBLE};
            int[] to = new int[]{R.id.tv_name, R.id.iv_visibility};
            mTrackAdapter = new TrackAdapter(
                    context, R.layout.row_track, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            setAdapter(mTrackAdapter);

            activity.getSupportLoaderManager().initLoader(TRACKS_ID, null, this);
        }

        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedTextView name = (CheckedTextView) view.findViewById(R.id.tv_name);
                name.setChecked(!name.isChecked());

                if (name.isChecked())
                    mSelectedIds.add(id + "");
                else
                    mSelectedIds.remove(id + "");

                if (mCheckedChangeListener != null)
                    mCheckedChangeListener.onCheckedChanged();
            }
        });
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putStringArrayList(BUNDLE_SELECTED_ITEMS_ID, mSelectedIds);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            if (bundle.containsKey(BUNDLE_SELECTED_ITEMS_ID))
                mSelectedIds = bundle.getStringArrayList(BUNDLE_SELECTED_ITEMS_ID);

            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
            return;
        }

        super.onRestoreInstanceState(state);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = new String[]{TrackLayer.FIELD_ID, TrackLayer.FIELD_NAME, TrackLayer.FIELD_VISIBLE};
        return new CursorLoader(mContext, mContentUriTracks, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mTrackAdapter.swapCursor(data);

        if (mDataChangeListener != null)
            mDataChangeListener.onDataChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mTrackAdapter.swapCursor(null);

        if (mDataChangeListener != null)
            mDataChangeListener.onDataChanged();
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.mCheckedChangeListener = listener;
    }

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        this.mDataChangeListener = listener;
    }

    public void selectAll() {
        ArrayList<String> ids = new ArrayList<>();
        Cursor data = mTrackAdapter.getCursor();

        if (data.moveToFirst())
            do
                ids.add(data.getString(0));
            while (data.moveToNext());

        mSelectedIds.clear();
        mSelectedIds.addAll(ids);

        for (int i = 0; i < getChildCount(); i++)
            ((CheckedTextView) getChildAt(i).findViewById(R.id.tv_name)).setChecked(true);
    }

    public void unselectAll() {
        mSelectedIds.clear();

        for (int i = 0; i < getChildCount(); i++)
            ((CheckedTextView) getChildAt(i).findViewById(R.id.tv_name)).setChecked(false);
    }

    public String[] getSelectedItemsIds() {
        return mSelectedIds.toArray(new String[mSelectedIds.size()]);
    }

    public int getSelectedItemsCount() {
        return mSelectedIds.size();
    }

    public class TrackAdapter extends SimpleCursorAdapter {
        private Context mContext;

        public TrackAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            mContext = context;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            final Integer id = cursor.getInt(0);
            final ImageView visibility = (ImageView) view.findViewById(R.id.iv_visibility);
            visibility.setImageDrawable(cursor.getInt(2) != 0 ? mVisibilityOn : mVisibilityOff);
            visibility.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isVisible = visibility.getDrawable().equals(mVisibilityOn);
                    updateRecord(id, !isVisible);
                }
            });

            CheckedTextView name = (CheckedTextView) view.findViewById(R.id.tv_name);
            name.setChecked(mSelectedIds.contains(id + ""));
            name.setText(cursor.getString(1));
        }

        private void updateRecord(int id, boolean visibility) {
            ContentValues cv = new ContentValues();
            cv.put(TrackLayer.FIELD_VISIBLE, visibility);
            mContext.getContentResolver().update(Uri.withAppendedPath(mContentUriTracks, id + ""), cv, null, null);
        }
    }
}

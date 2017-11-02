/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2017 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.control;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import com.keenfin.easypicker.PhotoPicker;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.api.IFormControl;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_MAX_PHOTO_KEY;

public class PhotoGallery extends PhotoPicker implements IFormControl {
    private static final String BUNDLE_DELETED_IMAGES = "deleted_images";
    private long mFeatureId = NOT_FOUND;
    private VectorLayer mLayer;
    private Map<String, Integer> mAttaches = new HashMap<>();
    private PhotoAdapter mAdapter;
    private List<Integer> mDeletedImages = new ArrayList<>();;

    public PhotoGallery(Context context) {
        super(context);
    }

    public PhotoGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PhotoGallery(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void init(JSONObject element,
                     List<Field> fields,
                     Bundle savedState,
                     Cursor featureCursor,
                     SharedPreferences preferences) throws JSONException {
        mAdapter = (PhotoAdapter) getAdapter();

        if (element != null) {
            JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

            if (attributes.has(JSON_MAX_PHOTO_KEY)) {
                int maxPhotos = attributes.getInt(JSON_MAX_PHOTO_KEY);
                setMaxPhotos(maxPhotos);
            }
        }

        if (mLayer != null && mFeatureId != NOT_FOUND && mAdapter.getItemCount() < 2) { // feature exists
            IGISApplication app = (IGISApplication) ((Activity) getContext()).getApplication();
            getAttaches(app, mLayer, mFeatureId, mAttaches, true);
        }
    }

    public static void getAttaches(IGISApplication app, VectorLayer layer, long featureId, Map<String, Integer> map, boolean excludeSign) {
        Uri uri = Uri.parse("content://" + app.getAuthority() + "/" +
                layer.getPath().getName() + "/" + featureId + "/" + Constants.URI_ATTACH);
        MatrixCursor attachCursor = (MatrixCursor) layer.query(uri,
                new String[]{VectorLayer.ATTACH_DATA, VectorLayer.ATTACH_ID},
                FIELD_ID + " = " + featureId, null, null, null);

        if (attachCursor.moveToFirst()) {
            do {
                if (excludeSign && attachCursor.getInt(1) == Integer.MAX_VALUE)
                    continue;

                map.put(attachCursor.getString(0), attachCursor.getInt(1));
            } while (attachCursor.moveToNext());
        }

        attachCursor.close();
    }

    public void init(VectorLayer layer, long featureId) {
        mLayer = layer;
        mFeatureId = featureId;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mLayer != null && mFeatureId != NOT_FOUND && mAdapter.getItemCount() < 2) {
            ArrayList<String> images = new ArrayList<>();

            for (String attach : mAttaches.keySet())
                if (!mDeletedImages.contains(mAttaches.get(attach)))
                    images.add(attach);

            restoreImages(images);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = (Bundle) super.onSaveInstanceState();
        bundle.putIntegerArrayList(BUNDLE_DELETED_IMAGES, new ArrayList<>(getDeletedAttaches()));

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            if (bundle.containsKey(BUNDLE_DELETED_IMAGES)) {
                ArrayList<Integer> deletedImages = bundle.getIntegerArrayList(BUNDLE_DELETED_IMAGES);

                if (deletedImages != null)
                    mDeletedImages.addAll(deletedImages);
            }
        }

        super.onRestoreInstanceState(state);
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {
    }

    @Override
    public boolean isShowLast() {
        return false;
    }

    @Override
    public String getFieldName() {
        return null;
    }

    @Override
    public void addToLayout(ViewGroup layout) {
        layout.addView(this);
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void saveState(Bundle outState) {

    }

    public List<String> getNewAttaches() {
        ArrayList<String> result = new ArrayList<>();

        for (String image : mAdapter.getImagesPath())
            if (!mAttaches.containsKey(image))
                result.add(image);

        return result;
    }

    public List<Integer> getDeletedAttaches() {
        ArrayList<Integer> result = new ArrayList<>();

        if (mAttaches != null) {
            for (String attach : mAttaches.keySet()) {
                if (!mAdapter.getImagesPath().contains(attach))
                    result.add(mAttaches.get(attach));
            }
        }

        return result;
    }
}

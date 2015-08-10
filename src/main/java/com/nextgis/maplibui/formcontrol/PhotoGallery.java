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

package com.nextgis.maplibui.formcontrol;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.keenfin.easypicker.PhotoPicker;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.map.VectorLayer;
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
    private long mFeatureId = NOT_FOUND;
    private VectorLayer mLayer;
    private Map<String, Integer> mAttaches;
    private PhotoAdapter mAdapter;

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
                     Cursor featureCursor,
                     SharedPreferences preferences) throws JSONException {
        mAdapter = (PhotoAdapter) getAdapter();
        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);

        if (null != featureCursor && mLayer != null && mFeatureId != NOT_FOUND) { // feature exists
            MatrixCursor attachCursor;
            mAttaches = new HashMap<>();
            ArrayList<String> images = new ArrayList<>();

            IGISApplication app = (IGISApplication) ((Activity) getContext()).getApplication();

            Uri uri = Uri.parse("content://" + app.getAuthority() + "/" +
                    mLayer.getPath().getName() + "/" + mFeatureId + "/attach");

            attachCursor = (MatrixCursor) mLayer.query(uri,
                    new String[]{VectorLayer.ATTACH_DATA, VectorLayer.ATTACH_ID},
                    FIELD_ID + " = " + mFeatureId, null, null, null);

            if (attachCursor.moveToFirst()) {
                do {
                    mAttaches.put(attachCursor.getString(0), attachCursor.getInt(1));
                    images.add(attachCursor.getString(0));
                } while (attachCursor.moveToNext());
            }

            mAdapter.restoreImages(images);
            attachCursor.close();
        }

        int maxPhotos = attributes.getInt(JSON_MAX_PHOTO_KEY);
        setMaxPhotos(maxPhotos);
    }

    public void init(VectorLayer layer, long featureId) {
        mLayer = layer;
        mFeatureId = featureId;
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

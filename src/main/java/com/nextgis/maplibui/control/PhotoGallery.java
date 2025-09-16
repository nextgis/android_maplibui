/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2017, 2019-2021 NextGIS, info@nextgis.com
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

import com.keenfin.easypicker.AttachInfo;
import com.keenfin.easypicker.PhotoPicker;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FeatureAttachments;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.activity.ModifyAttributesActivity;
import com.nextgis.maplibui.api.IFormControl;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.FIELD_ATTACH_DESCRIPTION;
import static com.nextgis.maplib.util.Constants.FIELD_ATTACH_DISPLAYNAME;
import static com.nextgis.maplib.util.Constants.FIELD_ATTACH_ID;
import static com.nextgis.maplib.util.Constants.FIELD_ATTACH_MIMETYPE;
import static com.nextgis.maplib.util.Constants.FIELD_ATTACH_OPERATION;
import static com.nextgis.maplib.util.Constants.FIELD_FEATURE_ID;
import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.FIELD_OPERATION;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_MAX_PHOTO_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_PHOTO_COMMENT_KEY;

public class PhotoGallery extends PhotoPicker implements IFormControl {
    public static final String GALLERY_PREFIX = "<&PhotoGallery_";
    private static final String BUNDLE_DELETED_IMAGES = "deleted_images";
    private long mFeatureId = NOT_FOUND;
    private VectorLayer mLayer;
    private final List<AttachInfo> mAttaches = new ArrayList<>();
    private final Map<String, AttachInfo> onlineAttaches = new HashMap<>();
    private PhotoAdapter mAdapter;
    private String mComment;
    private final  List<Integer> mDeletedImages = new ArrayList<>();


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
                     SharedPreferences preferences,
                     Map<String, Map<String, String>> translations,
                     final ModifyAttributesActivity modifyAttributesActivity) throws JSONException {
        mAdapter = (PhotoAdapter) getAdapter();

        if (element != null) {
            JSONObject attributes = element.optJSONObject(JSON_ATTRIBUTES_KEY);

            if (attributes != null && attributes.has(JSON_MAX_PHOTO_KEY)) {
                int maxPhotos = attributes.getInt(JSON_MAX_PHOTO_KEY);
                setMaxPhotos(maxPhotos);
            }

            if (attributes != null && attributes.has(JSON_PHOTO_COMMENT_KEY)) {
                mComment = attributes.getString(JSON_PHOTO_COMMENT_KEY);
            }
        }

        if (mLayer != null && mFeatureId != NOT_FOUND && mAdapter != null && mAdapter.getItemCount() < 2) { // feature exists
            IGISApplication app = (IGISApplication) ((Activity) getContext()).getApplication();
            getOfflineAttaches(app, mLayer, mFeatureId, mAttaches, true, mComment);

            Map<String, AttachInfo> onlineAttachesCache =getOnlineAttaches(app, mLayer, mFeatureId);
            onlineAttaches.clear();
            onlineAttaches.putAll(onlineAttachesCache);
        }

        if (savedState != null) {
            if (savedState.getBoolean("<tabs&", false)) {
                final Bundle bundle = new Bundle();
                bundle.putAll(savedState);
                savedState.remove("<tabs&");
                post(new Runnable() {
                    @Override
                    public void run() {
                        onRestoreInstanceState(bundle);
                    }
                });
            }
        }
    }

    public static void getOfflineAttaches(IGISApplication app, VectorLayer layer, long featureId,
                                          List<AttachInfo> map,
                                          boolean excludeSign, String comment) {
        // get from cursor attaches in app folder (added attaches, not synced )
        Uri uri = Uri.parse("content://" + app.getAuthority() + "/" +
                layer.getPath().getName() + "/" + featureId + "/" + Constants.URI_ATTACH);
        MatrixCursor attachCursor = (MatrixCursor) layer.query(uri,
                new String[]{VectorLayer.ATTACH_DATA, VectorLayer.ATTACH_ID, VectorLayer.ATTACH_DESCRIPTION},
                FIELD_ID + " = " + featureId, null, null, null);

        if (attachCursor.moveToFirst()) {
            do {
                if (excludeSign && attachCursor.getInt(1) == Integer.MAX_VALUE)
                    continue;

                if (comment != null && !attachCursor.getString(2).equals(comment))
                    continue;
                map.add(new AttachInfo(false,attachCursor.getString(0), attachCursor.getString(1) ));
               // map.put(attachCursor.getString(0), attachCursor.getInt(1));
            } while (attachCursor.moveToNext());
        }

        attachCursor.close();

        // get attaches from webpart - ask from db
        //todo

    }

    // get attaches already On WebGIS
     public static Map<String, AttachInfo>  getOnlineAttaches(IGISApplication app,
                                                           VectorLayer layer,
                                                           long featureId) {
         AccountUtil.AccountData accountData = null;
         try {
             accountData = AccountUtil.getAccountData(layer.getContext(), ((NGWVectorLayer) layer).getAccountName());
         } catch (IllegalStateException e) {
             //throw new NGException(getContext().getString(com.nextgis.maplib.R.string.error_auth));
         }
         String protocol = "https://";
        if (accountData != null){
            String accountUrl = accountData.url;
            if (accountUrl.length() > 0 && accountUrl.startsWith("http://") && !accountUrl.startsWith("https://"))
                protocol = "http://";
        }


         Map<String, AttachInfo> result = new HashMap<>();

         if (!(layer instanceof NGWVectorLayer))
             return result;


         String[] projection =  new String[]{FIELD_FEATURE_ID, FIELD_ATTACH_ID, FIELD_ATTACH_DESCRIPTION, FIELD_ATTACH_DISPLAYNAME, FIELD_ATTACH_MIMETYPE};
         String selection = FIELD_FEATURE_ID + " = " + featureId;

         String tableName = ((NGWVectorLayer)layer).getAttachmentsTableName();
         FeatureAttachments.checkTable(tableName);
         Cursor attachmentsCursor = FeatureAttachments.query(tableName, projection, selection, null, null);
         // boolean onlineAttach, String url, String storePath, String filename, String description

        if (attachmentsCursor.moveToFirst()) {
            do {
                // https://alexey655.nextgis.com/api/resource/274/feature/1/attachment/249/image
                String attachId = attachmentsCursor.getString(1);
                String url = protocol + ((NGWVectorLayer) layer).getAccountName() + "/api/resource/" + ((NGWVectorLayer) layer).getRemoteId()
                        + "/feature/" + featureId + "/attachment/" + attachId + "/image";

                String storePath = "images/" + ((NGWVectorLayer) layer).getRemoteId()
                        + "/feature/" + featureId + "/attachment/" + attachId + "/";

                AttachInfo info = new AttachInfo(true, url,  storePath, attachmentsCursor.getString(3),
                        attachmentsCursor.getString(4), null, attachmentsCursor.getString(1));
                result.put( attachmentsCursor.getString(1),  info );

            } while (attachmentsCursor.moveToNext());

        }
         attachmentsCursor.close();

        return result;
    }

    public void init(VectorLayer layer, long featureId) {
        mLayer = layer;
        mFeatureId = featureId;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mLayer != null && mFeatureId != NOT_FOUND && mAdapter.getItemCount() < 2) {
            ArrayList<AttachInfo> images = new ArrayList<>();

            for (AttachInfo attach : mAttaches)
                if (attach!=null &&  !mDeletedImages.contains(attach.oldAttachString))
                    images.add(attach);
            restoreImages(images, onlineAttaches );
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = (Bundle) super.onSaveInstanceState();
        if (bundle != null)
            bundle.putIntegerArrayList(BUNDLE_DELETED_IMAGES, new ArrayList<>(getDeletedAttaches()));
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        ArrayList<String> list = null;
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            if (bundle.containsKey(BUNDLE_DELETED_IMAGES)) {
                ArrayList<Integer> deletedImages = bundle.getIntegerArrayList(BUNDLE_DELETED_IMAGES);

                if (deletedImages != null)
                    mDeletedImages.addAll(deletedImages);
            }

            if (mAdapter.getItemCount() >= 2) {
                list = bundle.getStringArrayList("attached_images");
                if (list == null)
                    list = new ArrayList<>();
                bundle.putStringArrayList("attached_images", new ArrayList<String>());
            }
        }

        super.onRestoreInstanceState(state);

        if (list != null) {
            if (state instanceof Bundle) {
                Bundle bundle = (Bundle) state;
                bundle.putStringArrayList("attached_images", list);
            }
        }
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
        return GALLERY_PREFIX + System.currentTimeMillis();
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
        Bundle bundle = (Bundle) super.onSaveInstanceState();
        if (bundle != null) {
            bundle.putIntegerArrayList(BUNDLE_DELETED_IMAGES, new ArrayList<>(getDeletedAttaches()));
            bundle.remove("instanceState");
            outState.putAll(bundle);
        }
    }

    public String getComment() {
        return mComment;
    }

    public List<AttachInfo> getNewAttaches() {
        ArrayList<AttachInfo> result = new ArrayList<>();

        for (AttachInfo image : mAdapter.getImagesPathOrUri()) {
            if (image == null || image.oldAttachString == null)
                continue;
            boolean exist = false;

            for (AttachInfo attachInfo : mAttaches) {
                if (attachInfo !=null && attachInfo.oldAttachString != null &&  image.oldAttachString.equals(attachInfo.oldAttachString)){
                    exist = true;
                    break;
                }
            }
            if (!exist)
                result.add(image);
        }
        return result;
    }

    public List<Integer> getDeletedAttaches() {
        ArrayList<Integer> result = new ArrayList<>();

        if (mAttaches != null) {
            for (AttachInfo attach : mAttaches) {
                if (attach != null && !mAdapter.getImagesPathOrUri().contains(attach))
                    result.add(Integer.valueOf(attach.attachId));
            }
        }

        return result;
    }
}

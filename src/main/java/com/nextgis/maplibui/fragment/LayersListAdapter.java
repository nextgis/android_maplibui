/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.MapEventListener;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.LocalTMSLayer;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.NGWLookupTable;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplib.map.Table;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.mapui.NGWRasterLayerUI;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;
import com.nextgis.maplibui.util.LayerUtil;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;


/**
 * An adapter to show layers as list
 */
public class LayersListAdapter
        extends BaseAdapter
        implements MapEventListener
{

    protected final MapDrawable mMap;
    protected final Context mContext;
    protected final Activity mActivity;
    protected DrawerLayout mDrawer;
    protected onEdit mEditListener;
    protected View.OnClickListener mOnPencilClickListener;

    public interface onEdit {
        void onLayerEdit(ILayer layer);
    }

    public LayersListAdapter(
            Activity activity,
            MapDrawable map)
    {
        mMap = map;
        mContext = mActivity = activity;

        if (null != mMap) {
            mMap.addListener(this);
        }
    }


    public void setOnLayerEditListener(onEdit listener) {
        mEditListener = listener;
    }


    public void setOnPencilClickListener(View.OnClickListener listener) {
        mOnPencilClickListener = listener;
    }


    @Override
    protected void finalize()
            throws Throwable
    {
        if (null != mMap) {
            mMap.removeListener(this);
        }
        super.finalize();
    }


    public void onResume() {
        notifyDataSetChanged();
    }


    @Override
    public int getCount()
    {
        if (null != mMap) {
            return mMap.getLayerCount();
        }
        return 0;
    }


    @Override
    public Object getItem(int i)
    {
        int nIndex = getCount() - 1 - i;
        if (null != mMap) {
            return mMap.getLayer(nIndex);
        }
        return null;
    }


    @Override
    public long getItemId(int i)
    {
        if (i < 0 || i >= mMap.getLayerCount()) {
            return NOT_FOUND;
        }
        Table layer = (Table) getItem(i);
        if (null != layer) {
            return layer.getId();
        }
        return NOT_FOUND;
    }


    @Override
    public View getView(
            int i,
            View view,
            ViewGroup viewGroup)
    {
        final Table layer = (Table) getItem(i);
        return getStandardLayerView(layer, view);
    }


    protected View getStandardLayerView(
            final ILayer layer,
            View view)
    {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = view;
        if (v == null || v.getId() == R.id.empty_row)
            v = inflater.inflate(R.layout.row_layer, null);

        if (layer instanceof NGWLookupTable)
            return inflater.inflate(R.layout.row_empty, null);

        final ILayerUI layerui;
        if (layer == null) {
            return v;
        } else if (layer instanceof ILayerUI) {
            layerui = (ILayerUI) layer;
        } else {
            layerui = null;
        }

        ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
        ivIcon.setImageDrawable(layerui != null ? layerui.getIcon(mContext) : null);

        TextView tvPaneName = (TextView) v.findViewById(R.id.tvLayerName);
        tvPaneName.setText(layer.getName());
        //final int id = layer.getId();

        final ImageButton btMore = (ImageButton) v.findViewById(R.id.btMore);
        ImageButton btShow = (ImageButton) v.findViewById(R.id.btShow);
        ImageView ivEdited = (ImageView) v.findViewById(R.id.ivEdited);

        boolean hide = layerui instanceof VectorLayer && ((VectorLayer) layerui).isLocked();
        btMore.setVisibility(hide ? View.GONE : View.VISIBLE);
        btShow.setVisibility(hide ? View.GONE : View.VISIBLE);
        ivEdited.setVisibility(hide ? View.VISIBLE : View.GONE);
        if (mOnPencilClickListener != null)
            ivEdited.setOnClickListener(mOnPencilClickListener);

        int[] attrs = new int[] { R.attr.ic_action_visibility_on, R.attr.ic_action_visibility_off};
        TypedArray ta = mContext.obtainStyledAttributes(attrs);
        Drawable visibilityOn = ta.getDrawable(0);
        Drawable visibilityOff = ta.getDrawable(1);

        if (layer instanceof Layer) {
            btShow.setImageDrawable(//setImageResource(
                    ((Layer) layer).isVisible()
                            ? visibilityOn
                            : visibilityOff);
            //btShow.refreshDrawableState();
            btShow.setOnClickListener(
                    new View.OnClickListener() {
                        public void onClick(View arg0) {
                            //Layer layer = mMap.getLayerById(id);
                            ((Layer) layer).setVisible(!((Layer) layer).isVisible());
                            layer.save();
                        }
                    });
        }
        ta.recycle();

        btMore.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View arg0)
                    {
                        PopupMenu popup = new PopupMenu(mContext, btMore);
                        popup.getMenuInflater().inflate(R.menu.layer_popup, popup.getMenu());

                        if (layerui == null) {
                            popup.getMenu().findItem(R.id.menu_settings).setEnabled(false);
                            popup.getMenu().findItem(R.id.menu_share).setEnabled(false);
                        }

                        if (layerui instanceof TrackLayer) {
                            popup.getMenu().findItem(R.id.menu_delete).setVisible(false);
                            popup.getMenu().findItem(R.id.menu_settings).setTitle(R.string.track_list);
                        } else if (layerui instanceof VectorLayer) {
                            popup.getMenu().findItem(R.id.menu_edit).setVisible(true);
                            popup.getMenu().findItem(R.id.menu_share).setVisible(true);
                            popup.getMenu().findItem(R.id.menu_zoom_extent).setVisible(true);
                        } else if (layerui instanceof LocalTMSLayer) {
                            popup.getMenu().findItem(R.id.menu_zoom_extent).setVisible(true);
                        } else if (layerui instanceof RemoteTMSLayer) {
                            popup.getMenu().findItem(R.id.menu_download_tiles).setVisible(true);
                        }

                        popup.setOnMenuItemClickListener(
                                new PopupMenu.OnMenuItemClickListener()
                                {
                                    public boolean onMenuItemClick(MenuItem item)
                                    {
                                        int i = item.getItemId();
                                        if (i == R.id.menu_settings) {
                                            //Layer layer = mMap.getLayerById(id);
                                            assert layerui != null;
                                            layerui.changeProperties(mContext);
                                        } else if (i == R.id.menu_share) {
                                            assert (layerui) != null;

                                            if (layerui instanceof VectorLayer) {
                                                VectorLayer vectorLayer = (VectorLayer) layerui;
                                                LayerUtil.shareLayerAsGeoJSON(vectorLayer);
                                            }
                                        } else if (i == R.id.menu_edit) {
                                            if (mEditListener != null)
                                                mEditListener.onLayerEdit(layer);
                                        } else if (i == R.id.menu_delete) {
                                            final int position = mMap.removeLayer(layer);

                                            View focus = mActivity.getCurrentFocus();
                                            if (focus == null)
                                                return true;

                                            Snackbar snackbar = Snackbar.make(focus, mActivity.getString(R.string.delete_layer_done), Snackbar.LENGTH_LONG)
                                                    .setAction(R.string.undo, new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            mMap.insertLayer(position, layer);
                                                        }
                                                    })
                                                    .setCallback(new Snackbar.Callback() {
                                                        @Override
                                                        public void onDismissed(Snackbar snackbar, int event) {
                                                            super.onDismissed(snackbar, event);
                                                            if (event == DISMISS_EVENT_MANUAL)
                                                                return;
                                                            if (event != DISMISS_EVENT_ACTION) {
                                                                layer.delete();
                                                                mMap.save();
                                                            }
                                                        }

                                                        @Override
                                                        public void onShown(Snackbar snackbar) {
                                                            super.onShown(snackbar);
                                                        }
                                                    });

                                            View view = snackbar.getView();
                                            TextView textView = (TextView) view.findViewById(R.id.snackbar_text);
                                            textView.setTextColor(ContextCompat.getColor(mActivity, R.color.color_white));
                                            snackbar.show();
                                        } else if (i == R.id.menu_zoom_extent) {
                                            mMap.zoomToExtent(layer.getExtents());

                                            if (mDrawer != null)
                                                mDrawer.closeDrawers();
                                        } else if(i == R.id.menu_download_tiles){
                                            GeoEnvelope env = mMap.getCurrentBounds();

                                            if (layer instanceof RemoteTMSLayerUI) {
                                                RemoteTMSLayerUI remoteTMSLayer = (RemoteTMSLayerUI) layer;
                                                remoteTMSLayer.downloadTiles(mContext, env);
                                            } else if(layer instanceof NGWRasterLayerUI) {
                                                NGWRasterLayerUI remoteTMSLayer = (NGWRasterLayerUI) layer;
                                                remoteTMSLayer.downloadTiles(mContext, env);
                                            }
                                        }

                                        return true;
                                    }
                                });

                        popup.show();
                    }
                });

        return v;
    }


    @Override
    public void onLayerAdded(int id)
    {
        notifyDataChanged();
    }


    @Override
    public void onLayerDeleted(int id)
    {
        notifyDataChanged();
    }


    @Override
    public void onLayerChanged(int id)
    {
        notifyDataChanged();
    }


    @Override
    public void onExtentChanged(
            float zoom,
            GeoPoint center)
    {

    }


    @Override
    public void onLayersReordered()
    {
        notifyDataChanged();
    }


    @Override
    public void onLayerDrawFinished(
            int id,
            float percent)
    {

    }


    @Override
    public void onLayerDrawStarted()
    {

    }


    protected void notifyDataChanged() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }


    public void setDrawer(DrawerLayout drawer) {
        mDrawer = drawer;
    }


    public void swapElements(
            int originalPosition,
            int newPosition)
    {
        //Log.d(TAG,
        //      "Original position: " + originalPosition + " Destination position: " + newPosition);
        if (null == mMap) {
            return;
        }
        int newPositionFixed = getCount() - 1 - newPosition;
        mMap.moveLayer(newPositionFixed, (com.nextgis.maplib.api.ILayer) getItem(originalPosition));
        notifyDataSetChanged();
    }


    public void endDrag()
    {
        if (null == mMap) {
            return;
        }
        mMap.save();

        mMap.thaw();
        mMap.runDraw(null);
    }


    public void beginDrag()
    {
        if (null == mMap) {
            return;
        }
        mMap.freeze();
    }
}

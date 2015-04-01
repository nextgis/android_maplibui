/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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

package com.nextgis.maplibui;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.nextgis.maplib.api.MapEventListener;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.MapEventSource;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.mapui.TrackLayerUI;
import com.nextgis.maplibui.mapui.VectorLayerUI;

import static com.nextgis.maplib.util.Constants.LAYERTYPE_REMOTE_TMS;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;


/**
 * An adapter to show layers as list
 */
public class LayersListAdapter
        extends BaseAdapter
        implements MapEventListener
{

    protected final MapEventSource mMap;
    protected final Context        mContext;


    public LayersListAdapter(
            Context context,
            MapEventSource map)
    {
        mMap = map;
        mContext = context;

        if(null != mMap)
            mMap.addListener(this);
    }


    @Override
    protected void finalize()
            throws Throwable
    {
        if(null != mMap)
            mMap.removeListener(this);
        super.finalize();
    }


    @Override
    public int getCount()
    {
        if(null != mMap)
            return mMap.getLayerCount();
        return 0;
    }


    @Override
    public Object getItem(int i)
    {
        int nIndex = getCount() - 1 - i;
        if(null != mMap)
            return mMap.getLayer(nIndex);
        return null;
    }


    @Override
    public long getItemId(int i)
    {
        if(i < 0 || i >= mMap.getLayerCount())
            return NOT_FOUND;
        Layer layer = (Layer) getItem(i);
        if(null != layer)
            return layer.getId();
        return NOT_FOUND;
    }


    @Override
    public View getView(
            int i,
            View view,
            ViewGroup viewGroup)
    {
        final Layer layer = (Layer) getItem(i);
        switch (layer.getType()) {
            case LAYERTYPE_REMOTE_TMS:
            default:
                return getStandardLayerView(layer, view);
        }
    }


    protected View getStandardLayerView(
            final Layer layer,
            View view)
    {
        View v = view;
        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            v = inflater.inflate(R.layout.layout_layer_row, null);
        }

        final ILayerUI layerui;
        if (layer instanceof ILayerUI) {
            layerui = (ILayerUI) layer;
        } else {
            layerui = null;
        }


        if (layerui != null) {
            ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
            ivIcon.setImageDrawable(layerui.getIcon());
        }

        TextView tvPaneName = (TextView) v.findViewById(R.id.tvLayerName);
        tvPaneName.setText(layer.getName());

        //final int id = layer.getId();

        ImageButton btShow = (ImageButton) v.findViewById(R.id.btShow);
        //Log.d(TAG, "Layer #" + id + " is visible " + layer.isVisible());
        btShow.setImageResource(layer.isVisible()
                                     ? R.drawable.ic_action_visibility_on
                                     : R.drawable.ic_action_visibility_off);
        //btShow.refreshDrawableState();
        btShow.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View arg0)
            {
                //Layer layer = mMap.getLayerById(id);
                layer.setVisible(!layer.isVisible());
                layer.save();
            }
        });

        final ImageButton btMore = (ImageButton) v.findViewById(R.id.btMore);
        btMore.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View arg0)
            {
                PopupMenu popup = new PopupMenu(mContext, btMore);
                popup.getMenuInflater().inflate(R.menu.layer_popup, popup.getMenu());

                if (layerui == null) {
                    popup.getMenu().findItem(R.id.menu_settings).setEnabled(false);
                    popup.getMenu().findItem(R.id.menu_share).setEnabled(false);
                }

                if(layerui instanceof TrackLayerUI){
                    popup.getMenu().findItem(R.id.menu_delete).setVisible(false);
                }
                else if (layerui instanceof VectorLayerUI) {
                    popup.getMenu().findItem(R.id.menu_share).setVisible(true);
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    public boolean onMenuItemClick(MenuItem item)
                    {
                        int i = item.getItemId();
                        if (i == R.id.menu_settings) {
                            //Layer layer = mMap.getLayerById(id);
                            assert layerui != null;
                            layerui.changeProperties();
                        } else if (i == R.id.menu_share) {
                            assert (layerui) != null;
                            ((VectorLayerUI) layerui).shareGeoJSON();
                        } else if (i == R.id.menu_delete) {
                            layer.delete();
                            mMap.save();
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
        notifyDataSetChanged();
    }


    @Override
    public void onLayerDeleted(int id)
    {
        notifyDataSetChanged();
    }


    @Override
    public void onLayerChanged(int id)
    {
        notifyDataSetChanged();
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
        notifyDataSetChanged();
    }


    @Override
    public void onLayerDrawFinished(
            int id,
            float percent)
    {

    }

    public void swapElements(
            int originalPosition,
            int newPosition)
    {
        //Log.d(TAG,
        //      "Original position: " + originalPosition + " Destination position: " + newPosition);
        if(null == mMap)
            return;
        int newPositionFixed = getCount() - 1 - newPosition;
        mMap.moveLayer(newPositionFixed, (com.nextgis.maplib.api.ILayer) getItem(originalPosition));
        notifyDataSetChanged();
    }


    public void endDrag()
    {
        if(null == mMap)
            return;
        mMap.thaw();
        mMap.runDraw(null);
    }


    public void beginDrag()
    {
        if(null == mMap)
            return;
        mMap.freeze();
    }
}

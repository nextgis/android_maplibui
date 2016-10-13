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

package com.nextgis.maplibui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ILayerSelector;
import com.nextgis.maplibui.api.ILayerUI;


/**
 * Class to show list of layers
 */
public class ChooseLayerListAdapter
        extends BaseAdapter
        implements AdapterView.OnItemClickListener
{
    protected ILayerSelector mSelector;


    public ChooseLayerListAdapter(ILayerSelector selector)
    {
        mSelector = selector;
    }


    @Override
    public int getCount()
    {
        return mSelector.getLayers().size();
    }


    @Override
    public Object getItem(int i)
    {
        return mSelector.getLayers().get(i);
    }


    @Override
    public long getItemId(int i)
    {
        return mSelector.getLayers().get(i).getId();
    }


    @Override
    public View getView(
            int i,
            View view,
            ViewGroup viewGroup)
    {
        final ILayer layer = (ILayer) getItem(i);
        View v = view;
        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(mSelector.getContext());
            v = inflater.inflate(R.layout.row_select_layer, null);
        }

        ILayerUI layerUI = (ILayerUI) layer;
        if (layerUI != null) {
            ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
            ivIcon.setImageDrawable(layerUI.getIcon(mSelector.getContext()));
        }

        TextView tvText = (TextView) v.findViewById(R.id.tvName);
        tvText.setText(layer.getName());

//        TextView tvDesc = (TextView) v.findViewById(R.id.tvDesc);
//        LayerGroup group = (LayerGroup) layer.getParent();
//        tvDesc.setText(group.getLayerFactory().getLayerTypeString(mSelector.getContext(), layer.getType()));

        return v;
    }


    @Override
    public void onItemClick(
            AdapterView<?> adapterView,
            View view,
            int i,
            long l)
    {
        ILayer layer = (ILayer) getItem(i);
        mSelector.onLayerSelect(layer);
    }
}

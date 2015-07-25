/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.nextgis.maplibui.R;

import java.util.List;


/**
 * Colors list to choose
 */
public class ChooseColorListAdapter
        extends BaseAdapter
{
    protected List<Pair<Integer, String>> mColors;
    protected Context                     mContext;


    public ChooseColorListAdapter(
            Context context,
            List<Pair<Integer, String>> colors)
    {
        mColors = colors;
        mContext = context;
    }


    @Override
    public int getCount()
    {
        return mColors.size();
    }


    @Override
    public Object getItem(int position)
    {
        return mColors.get(position);
    }


    @Override
    public long getItemId(int position)
    {
        return position;
    }


    @Override
    public View getView(
            int position,
            View convertView,
            ViewGroup parent)
    {
        final Pair<Integer, String> pair = (Pair<Integer, String>) getItem(position);
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            v = inflater.inflate(R.layout.row_color, null);
        }

        ImageView iv = (ImageView) v.findViewById(R.id.color_image);
        GradientDrawable sd = (GradientDrawable) iv.getDrawable();
        int color = pair.first;
        sd.setColor(color);

        // set color name
        TextView tv = (TextView) v.findViewById(R.id.color_name);
        tv.setText(pair.second);

        return v;
    }
}

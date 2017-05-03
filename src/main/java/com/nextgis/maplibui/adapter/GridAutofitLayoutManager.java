/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.adapter;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;


// From http://stackoverflow.com/a/30256880
public class GridAutofitLayoutManager
        extends GridLayoutManager
{
    protected float mColumnWidthPx;
    protected boolean mColumnWidthChanged = true;

    protected int mWidth;
    protected boolean mWidthChanged = true;

    protected OnChangeSpanCountListener mSpanCountListener;

    public GridAutofitLayoutManager(
            Context context,
            float columnWidthPx)
    {
        // Initially set spanCount to 1, will be changed automatically later.
        super(context, 1);
        setColumnWidthPx(checkedColumnWidth(context, columnWidthPx));
    }

    public GridAutofitLayoutManager(
            Context context,
            float columnWidthPx,
            int orientation,
            boolean reverseLayout)
    {
        // Initially set spanCount to 1, will be changed automatically later.
        super(context, 1, orientation, reverseLayout);
        setColumnWidthPx(checkedColumnWidth(context, columnWidthPx));
    }

    protected float checkedColumnWidth(
            Context context,
            float columnWidthPx)
    {
        if (columnWidthPx <= 0) {
            // Set default columnWidthPx value (48dp here).
            // It is better to move this constant to static constant on top,
            // but we need context to convert it to dp, so can't really do so.
            columnWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    context.getResources().getDisplayMetrics());
        }
        return columnWidthPx;
    }

    public void setColumnWidthPx(float newColumnWidthPx)
    {
        if (newColumnWidthPx > 0 && newColumnWidthPx != mColumnWidthPx) {
            mColumnWidthPx = newColumnWidthPx;
            mColumnWidthChanged = true;
        }
    }

    // http://stackoverflow.com/a/42241730
    @Override
    public void onLayoutChildren(
            RecyclerView.Recycler recycler,
            RecyclerView.State state)
    {
        int width = getWidth();
        int height = getHeight();

        if (width != mWidth) {
            mWidthChanged = true;
            mWidth = width;
        }

        if ((mWidthChanged || mColumnWidthChanged) && mColumnWidthPx > 0 && width > 0
                && height > 0) {
            int totalSpace;
            if (getOrientation() == VERTICAL) {
                totalSpace = width - getPaddingRight() - getPaddingLeft();
            } else {
                totalSpace = height - getPaddingTop() - getPaddingBottom();
            }
            int spanCount = Math.max(1, (int) Math.floor(totalSpace / mColumnWidthPx));
            setSpanCount(spanCount);
            mColumnWidthChanged = false;
            mWidthChanged = false;

            if (mSpanCount != spanCount) {
                mSpanCount = spanCount;
                mIsSpanCountChanged = true;
            }
        }
        super.onLayoutChildren(recycler, state);
    }

    protected int mSpanCount;
    protected boolean mIsSpanCountChanged = false;

    @Override
    public void onLayoutCompleted(RecyclerView.State state)
    {
        super.onLayoutCompleted(state);

        if (mIsSpanCountChanged) {
            mIsSpanCountChanged = false;
            if (mSpanCountListener != null) {
                mSpanCountListener.onChangeSpanCount(mSpanCount);
            }
        }
    }

    public void setSpanCountListener(OnChangeSpanCountListener spanCountListener)
    {
        mSpanCountListener = spanCountListener;
    }

    public interface OnChangeSpanCountListener
    {
        void onChangeSpanCount(int spanCount);
    }
}

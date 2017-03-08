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

import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import com.nextgis.maplibui.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public abstract class ListSelectorAdapter
        extends RecyclerView.Adapter<ListSelectorAdapter.ViewHolder>
{
    protected SparseBooleanArray mSelectedItems;
    protected boolean mSelectState      = false;
    protected boolean mHideCheckBox     = false;
    protected boolean mSingleSelectable = false;
    protected int mCurrentSingleSelected;

    protected Queue<OnSelectionChangedListener> mOnSelectionChangedListeners;

    protected ListSelectorAdapter.ViewHolder.OnItemClickListener     mOnItemClickListener;
    protected ListSelectorAdapter.ViewHolder.OnItemLongClickListener mOnItemLongClickListener;


    protected abstract int getItemViewResId();

    protected abstract ListSelectorAdapter.ViewHolder getViewHolder(View itemView);


    public void setOnItemClickListener(ListSelectorAdapter.ViewHolder.OnItemClickListener listener)
    {
        mOnItemClickListener = listener;
    }


    public void setOnItemLongClickListener(ListSelectorAdapter.ViewHolder.OnItemLongClickListener listener)
    {
        mOnItemLongClickListener = listener;
    }


    public ListSelectorAdapter()
    {
        mOnSelectionChangedListeners = new ConcurrentLinkedQueue<>();
        mSelectedItems = new SparseBooleanArray();
    }


    @Override
    final public ViewHolder onCreateViewHolder(
            ViewGroup parent,
            int viewType)
    {
        View itemView =
                LayoutInflater.from(parent.getContext()).inflate(getItemViewResId(), parent, false);
        return getViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(
            final ViewHolder holder,
            final int position)
    {
        holder.mPosition = position;

        if (null != holder.mCheckBox) {
            holder.mCheckBox.setChecked(isSelected(position));

            if (mHideCheckBox) {
                holder.mCheckBox.setVisibility(View.GONE);
            } else {
                holder.mCheckBox.setVisibility(View.VISIBLE);
                holder.mCheckBox.setTag(position);
                holder.mCheckBox.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        CompoundButton checkBox = (CompoundButton) view;
                        int clickedPos = (Integer) checkBox.getTag();
                        setSelection(clickedPos, checkBox.isChecked());
                    }
                });
            }
        }

        addOnSelectionChangedListener(holder);
    }


    @Override
    public void onViewRecycled(ViewHolder holder)
    {
        removeOnSelectionChangedListener(holder);
        super.onViewRecycled(holder);
    }


    public static abstract class ViewHolder
            extends RecyclerView.ViewHolder
            implements ListSelectorAdapter.OnSelectionChangedListener,
                       View.OnClickListener,
                       View.OnLongClickListener
    {
        public int                     mPosition;
        public CompoundButton          mCheckBox;
        public OnItemClickListener     mClickListener;
        public OnItemLongClickListener mLongClickListener;


        public ViewHolder(
                View itemView,
                boolean singleSelectable,
                OnItemClickListener clickListener,
                OnItemLongClickListener longClickListener)
        {
            super(itemView);

            if (singleSelectable) {
                mCheckBox = (CompoundButton) itemView.findViewById(R.id.item_radio_button);
            } else {
                mCheckBox = (CompoundButton) itemView.findViewById(R.id.item_checkbox);
            }

            if (null != clickListener) {
                mClickListener = clickListener;
                mLongClickListener = longClickListener;
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }
        }


        @Override
        public void onSelectionChanged(
                int position,
                boolean selection)
        {
            if (null != mCheckBox && position == getAdapterPosition()) {
                mCheckBox.setChecked(selection);
            }
        }


        @Override
        public void onClick(View view)
        {
            if (null != mClickListener) {
                mClickListener.onItemClick(getAdapterPosition());
            }
        }


        @Override
        public boolean onLongClick(View v)
        {
            if (null != mLongClickListener) {
                mLongClickListener.onItemLongClick(getAdapterPosition());
                return true;
            }
            return false;
        }


        public interface OnItemClickListener
        {
            void onItemClick(int position);
        }


        public interface OnItemLongClickListener
        {
            void onItemLongClick(int position);
        }
    }


    public void setSingleSelectable(boolean singleSelectable)
    {
        mSingleSelectable = singleSelectable;
    }


    public boolean isSingleSelectable()
    {
        return mSingleSelectable;
    }


    /**
     * Count the selected items
     *
     * @return Selected items count
     */
    public int getSelectedItemCount()
    {
        return mSelectedItems.size();
    }


    public boolean hasSelectedItems()
    {
        return getSelectedItemCount() > 0;
    }


    /**
     * Indicates if the item at position position is selected
     *
     * @param position
     *         Position of the item to check
     *
     * @return true if the item is selected, false otherwise
     */
    public boolean isSelected(int position)
    {
        return mSelectedItems.get(position, false);
    }


    /**
     * Clear the selection status for all items
     */
    public void clearSelectionForAll()
    {
        mSelectState = false;
        setSelectionForAll(false);
    }


    /**
     * Toggle the selection status of the item at a given position
     *
     * @param position
     *         Position of the item to toggle the selection status for
     */
    public void toggleSelection(int position)
    {
        setSelection(position, mSelectedItems.get(position, false));
    }


    public void toggleSelectionForAll()
    {
        mSelectState = !mSelectState;
        setSelectionForAll(mSelectState);
    }


    /**
     * Set the selection status of the item at a given position to the given state
     *
     * @param position
     *         Position of the item to toggle the selection status for
     * @param selection
     *         State for the item at position
     */
    public void setSelection(
            int position,
            boolean selection)
    {
        if (selection) {
            mSelectedItems.put(position, true);
        } else {
            mSelectedItems.delete(position);
        }

        for (OnSelectionChangedListener listener : mOnSelectionChangedListeners) {
            listener.onSelectionChanged(position, selection);
        }

        if (mSingleSelectable) {
            if (position != mCurrentSingleSelected) {
                mSelectedItems.delete(mCurrentSingleSelected);

                for (OnSelectionChangedListener listener : mOnSelectionChangedListeners) {
                    listener.onSelectionChanged(mCurrentSingleSelected, false);
                }

                mCurrentSingleSelected = position;
            }
        }
    }


    public void setSelectionForAll(boolean selection)
    {
        if (selection && mSingleSelectable) {
            throw new RuntimeException("ListSelectorAdapter is single selectable");
        }

        for (int i = 0, size = getItemCount(); i < size; ++i) {
            if (selection != isSelected(i)) {
                setSelection(i, selection);
            }
        }
    }


    /**
     * Indicates the list of selected items
     *
     * @return List of selected items ids
     */
    public List<Integer> getSelectedItemsIds()
    {
        List<Integer> items = new ArrayList<>(mSelectedItems.size());
        for (int i = 0, size = mSelectedItems.size(); i < size; ++i) {
            items.add(mSelectedItems.keyAt(i));
        }
        return items;
    }


    public Integer getCurrentSingleSelectedItemId()
    {
        if (!mSingleSelectable) {
            throw new RuntimeException("ListSelectorAdapter is not single selectable");
        }

        List<Integer> items = getSelectedItemsIds();

        if (items.size() == 0) {
            return null;
        } else {
            return items.get(0);
        }
    }


    protected void deleteSelected(int id)
            throws IOException
    {
        mSelectedItems.delete(id);
    }


    public void deleteAllSelected()
            throws IOException
    {
        int size = getItemCount();
        for (int i = size - 1; i >= 0; --i) {
            if (isSelected(i)) {
                deleteSelected(i);
                notifyItemRemoved(i);
            }
        }
        notifyItemRangeChanged(0, getItemCount());
    }


    public void addOnSelectionChangedListener(OnSelectionChangedListener listener)
    {
        if (mOnSelectionChangedListeners != null && !mOnSelectionChangedListeners.contains(
                listener)) {

            mOnSelectionChangedListeners.add(listener);
        }
    }


    public void removeOnSelectionChangedListener(OnSelectionChangedListener listener)
    {
        if (mOnSelectionChangedListeners != null) {
            mOnSelectionChangedListeners.remove(listener);
        }
    }


    public interface OnSelectionChangedListener
    {
        void onSelectionChanged(
                int position,
                boolean selection);
    }
}

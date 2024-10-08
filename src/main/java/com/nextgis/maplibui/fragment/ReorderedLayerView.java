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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.ControlHelper;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;


public class ReorderedLayerView
        extends ListView
        implements AdapterView.OnItemLongClickListener, AbsListView.OnScrollListener
{
    protected final static int SMOOTH_SCROLL_AMOUNT_AT_EDGE = 15;
    protected final static int LINE_THICKNESS               = 5;

    protected int mLastEventY = NOT_FOUND;

    protected int mDownY = NOT_FOUND;
    protected int mDownX = NOT_FOUND;

    protected int mTotalOffset = 0;

    protected boolean mCellIsMobile             = false;
    protected boolean mIsMobileScrolling        = false;
    protected int     mSmoothScrollAmountAtEdge = 0;

    protected long mAboveItemId  = NOT_FOUND;
    protected long mMobileItemId = NOT_FOUND;
    protected long mBelowItemId  = NOT_FOUND;

    protected BitmapDrawable mHoverCell;
    protected Rect           mHoverCellCurrentBounds;
    protected Rect           mHoverCellOriginalBounds;

    protected int mActivePointerId = NOT_FOUND;

    protected boolean mIsWaitingForScrollFinish = false;
    protected int     mScrollState              = OnScrollListener.SCROLL_STATE_IDLE;

    protected int mPreviousFirstVisibleItem = NOT_FOUND;
    protected int mPreviousVisibleItemCount = NOT_FOUND;
    protected int mCurrentFirstVisibleItem;
    protected int mCurrentVisibleItemCount;
    protected int mCurrentScrollState;

    protected DrawerLayout mDrawer;


    public ReorderedLayerView(Context context)
    {
        super(context);
        init(context);
    }


    public ReorderedLayerView(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }


    public ReorderedLayerView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReorderedLayerView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }


    protected void init(Context context)
    {
        setLongClickable(true);
        setOnItemLongClickListener(this);
        setOnScrollListener(this);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mSmoothScrollAmountAtEdge = (int) (SMOOTH_SCROLL_AMOUNT_AT_EDGE / metrics.density);
    }


    @Override
    public boolean onItemLongClick(
            AdapterView<?> adapterView,
            View view,
            int i,
            long l)
    {
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);

        mTotalOffset = 0;

        int position = pointToPosition(mDownX, mDownY);
        int itemNum = position - getFirstVisiblePosition();

        View selectedView = getChildAt(itemNum);
        mMobileItemId = getAdapter().getItemId(position);
        mHoverCell = getAndAddHoverView(selectedView);
        selectedView.setVisibility(INVISIBLE);

        mCellIsMobile = true;
        LayersListAdapter adapter = (LayersListAdapter) getAdapter();
        adapter.beginDrag();

        updateNeighborViewsForID(mMobileItemId);

        return true;
    }


    public void setDrawer(DrawerLayout drawer) {
        mDrawer = drawer;
    }


    protected void setDrawerLockMode(int lockMode) {
        if (mDrawer!= null)
            mDrawer.setDrawerLockMode(lockMode);
    }

    /**
     * Creates the hover cell with the appropriate bitmap and of appropriate size. The hover cell's
     * BitmapDrawable is drawn on top of the bitmap every single time an invalidate call is made.
     */
    protected BitmapDrawable getAndAddHoverView(View v)
    {

        int w = v.getWidth();
        int h = v.getHeight();
        int top = v.getTop();
        int left = v.getLeft();

        Bitmap b = getBitmapWithBorder(v);

        BitmapDrawable drawable = new BitmapDrawable(getResources(), b);

        mHoverCellOriginalBounds = new Rect(left, top, left + w, top + h);
        mHoverCellCurrentBounds = new Rect(mHoverCellOriginalBounds);

        drawable.setBounds(mHoverCellCurrentBounds);

        return drawable;
    }


    /**
     * Draws a black border over the screenshot of the view passed in.
     */
    protected Bitmap getBitmapWithBorder(View v)
    {
        Bitmap bitmap = getBitmapFromView(v);
        Canvas canvas = new Canvas(bitmap);

        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        canvas.drawBitmap(bitmap, 0, 0, null);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(LINE_THICKNESS);

        int accentColor = ControlHelper.getColor(getContext(), android.R.attr.colorAccent);

        paint.setColor(accentColor);
        canvas.drawRect(rect, paint);

        return bitmap;
    }


    /**
     * Returns a bitmap showing a screenshot of the view passed in.
     */
    protected Bitmap getBitmapFromView(View v)
    {
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(com.google.android.material.R.color.background_floating_material_light));
        Rect rect = new Rect(0, 0, v.getWidth(), v.getHeight());
        canvas.drawRect(rect, paint);

        v.draw(canvas);
        return bitmap;
    }


    /**
     * Stores a reference to the views above and below the item currently corresponding to the hover
     * cell. It is important to note that if this item is either at the top or bottom of the list,
     * mAboveItemId or mBelowItemId may be invalid.
     */
    protected void updateNeighborViewsForID(long itemID)
    {
        int position = getPositionForID(itemID);
        ListAdapter adapter = getAdapter();
        mAboveItemId = adapter.getItemId(position - 1);
        mBelowItemId = adapter.getItemId(position + 1);
    }


    /**
     * Retrieves the view in the list corresponding to itemID
     */
    public View getViewForID(long itemID)
    {
        int firstVisiblePosition = getFirstVisiblePosition();
        ListAdapter adapter = getAdapter();
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int position = firstVisiblePosition + i;
            long id = adapter.getItemId(position);
            if (id == itemID) {
                return v;
            }
        }
        return null;
    }


    /**
     * Retrieves the position in the list corresponding to itemID
     */
    public int getPositionForID(long itemID)
    {
        View v = getViewForID(itemID);
        if (v == null) {
            return NOT_FOUND;
        } else {
            return getPositionForView(v);
        }
    }


    /**
     * dispatchDraw gets invoked when all the child views are about to be drawn. By overriding this
     * method, the hover cell (BitmapDrawable) can be drawn over the listview's items whenever the
     * listview is redrawn.
     */
    @Override
    protected void dispatchDraw(
            @NonNull
            Canvas canvas)
    {
        super.dispatchDraw(canvas);
        if (mHoverCell != null) {
            mHoverCell.draw(canvas);
        }
    }


    @Override
    public boolean onTouchEvent(
            @NonNull
            MotionEvent event)
    {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) event.getX();
                mDownY = (int) event.getY();
                mActivePointerId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == NOT_FOUND) {
                    break;
                }

                int pointerIndex = event.findPointerIndex(mActivePointerId);

                mLastEventY = (int) event.getY(pointerIndex);
                int deltaY = mLastEventY - mDownY;

                if (mCellIsMobile) {
                    int top = mHoverCellOriginalBounds.top + deltaY + mTotalOffset;
                    mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left, top);
                    mHoverCell.setBounds(mHoverCellCurrentBounds);
                    invalidate();

                    handleCellSwitch();

                    mIsMobileScrolling = false;
                    handleMobileCellScroll();

                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
                touchEventsEnded();
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                ((LayersListAdapter) getAdapter()).notifyDataChanged();
                break;
            case MotionEvent.ACTION_CANCEL:
                touchEventsCancelled();
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                /* If a multitouch event took place and the original touch dictating
                 * the movement of the hover cell has ended, then the dragging event
                 * ends and the hover cell is animated to its corresponding position
                 * in the listview. */
                pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                               MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    touchEventsEnded();
                }
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                break;
            default:
                break;
        }

        return super.onTouchEvent(event);
    }


    /**
     * This method determines whether the hover cell has been shifted far enough to invoke a cell
     * swap. If so, then the respective cell swap candidate is determined and the data set is
     * changed. Upon posting a notification of the data set change, a layout is invoked to place the
     * cells in the right place. Using a ViewTreeObserver and a corresponding OnPreDrawListener, we
     * can offset the cell being swapped to where it previously was and then animate it to its new
     * position.
     */
    protected void handleCellSwitch()
    {
        final int deltaY = mLastEventY - mDownY;
        int deltaYTotal = mHoverCellOriginalBounds.top + mTotalOffset + deltaY;

        View belowView = getViewForID(mBelowItemId);
        View mobileView = getViewForID(mMobileItemId);
        View aboveView = getViewForID(mAboveItemId);

        boolean isBelow = (belowView != null) && (deltaYTotal > belowView.getTop());
        boolean isAbove = (aboveView != null) && (deltaYTotal < aboveView.getTop());

        if (isBelow || isAbove) {

            final long switchItemID = isBelow ? mBelowItemId : mAboveItemId;
            View switchView = isBelow ? belowView : aboveView;
            final int originalItem = getPositionForView(mobileView);

            if (switchView == null) {
                updateNeighborViewsForID(mMobileItemId);
                return;
            }

            LayersListAdapter adapter = (LayersListAdapter) getAdapter();
            if (null != adapter) {
                adapter.swapElements(originalItem, getPositionForView(switchView));
            }

            mDownY = mLastEventY;

            mobileView.setVisibility(View.VISIBLE);
            switchView.setVisibility(View.INVISIBLE);

            updateNeighborViewsForID(mMobileItemId);

        }
    }


    /**
     * Resets all the appropriate fields to a default state while also animating the hover cell back
     * to its correct location.
     */
    protected void touchEventsEnded()
    {
        final View mobileView = getViewForID(mMobileItemId);
        if (mCellIsMobile || mIsWaitingForScrollFinish) {

            LayersListAdapter adapter = (LayersListAdapter) getAdapter();
            adapter.endDrag();

            mCellIsMobile = false;
            mIsWaitingForScrollFinish = false;
            mIsMobileScrolling = false;
            mActivePointerId = NOT_FOUND;

            // If the autoscroller has not completed scrolling, we need to wait for it to
            // finish in order to determine the final location of where the hover cell
            // should be animated to.
            if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mIsWaitingForScrollFinish = true;
                return;
            }

            mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left, mobileView.getTop());

            mAboveItemId = NOT_FOUND;
            mMobileItemId = NOT_FOUND;
            mBelowItemId = NOT_FOUND;
            mobileView.setVisibility(VISIBLE);
            mHoverCell = null;

            invalidate();

        } else {
            touchEventsCancelled();
        }
    }


    /**
     * Resets all the appropriate fields to a default state.
     */
    protected void touchEventsCancelled()
    {
        View mobileView = getViewForID(mMobileItemId);
        if (mCellIsMobile) {

            LayersListAdapter adapter = (LayersListAdapter) getAdapter();
            adapter.endDrag();

            mAboveItemId = NOT_FOUND;
            mMobileItemId = NOT_FOUND;
            mBelowItemId = NOT_FOUND;
            mobileView.setVisibility(VISIBLE);
            mHoverCell = null;
            invalidate();
        }
        mCellIsMobile = false;

        mIsMobileScrolling = false;
        mActivePointerId = NOT_FOUND;
    }


    /**
     * Determines whether this listview is in a scrolling state invoked by the fact that the hover
     * cell is out of the bounds of the listview;
     */
    protected void handleMobileCellScroll()
    {
        mIsMobileScrolling = handleMobileCellScroll(mHoverCellCurrentBounds);
    }


    /**
     * This method is in charge of determining if the hover cell is above or below the bounds of the
     * listview. If so, the listview does an appropriate upward or downward smooth scroll so as to
     * reveal new items.
     */
    protected boolean handleMobileCellScroll(Rect r)
    {
        int offset = computeVerticalScrollOffset();
        int height = getHeight();
        int extent = computeVerticalScrollExtent();
        int range = computeVerticalScrollRange();
        int hoverViewTop = r.top;
        int hoverHeight = r.height();

        if (hoverViewTop <= 0 && offset > 0) {
            smoothScrollBy(-mSmoothScrollAmountAtEdge, 0);
            return true;
        }

        if (hoverViewTop + hoverHeight >= height && (offset + extent) < range) {
            smoothScrollBy(mSmoothScrollAmountAtEdge, 0);
            return true;
        }

        return false;
    }


    @Override
    public void onScrollStateChanged(
            AbsListView view,
            int scrollState)
    {
        mCurrentScrollState = scrollState;
        mScrollState = scrollState;
        isScrollCompleted();
    }


    @Override
    public void onScroll(
            AbsListView view,
            int firstVisibleItem,
            int visibleItemCount,
            int totalItemCount)
    {
        mCurrentFirstVisibleItem = firstVisibleItem;
        mCurrentVisibleItemCount = visibleItemCount;

        mPreviousFirstVisibleItem = (mPreviousFirstVisibleItem == NOT_FOUND)
                                    ? mCurrentFirstVisibleItem
                                    : mPreviousFirstVisibleItem;
        mPreviousVisibleItemCount = (mPreviousVisibleItemCount == NOT_FOUND)
                                    ? mCurrentVisibleItemCount
                                    : mPreviousVisibleItemCount;

        checkAndHandleFirstVisibleCellChange();
        checkAndHandleLastVisibleCellChange();

        mPreviousFirstVisibleItem = mCurrentFirstVisibleItem;
        mPreviousVisibleItemCount = mCurrentVisibleItemCount;
    }


    /**
     * This method is in charge of invoking 1 of 2 actions. Firstly, if the listview is in a state
     * of scrolling invoked by the hover cell being outside the bounds of the listview, then this
     * scrolling event is continued. Secondly, if the hover cell has already been released, this
     * invokes the animation for the hover cell to return to its correct position after the listview
     * has entered an idle scroll state.
     */
    protected void isScrollCompleted()
    {
        if (mCurrentVisibleItemCount > 0 && mCurrentScrollState == SCROLL_STATE_IDLE) {
            if (mCellIsMobile && mIsMobileScrolling) {
                handleMobileCellScroll();
            } else if (mIsWaitingForScrollFinish) {
                touchEventsEnded();
            }
        }
    }


    /**
     * Determines if the listview scrolled up enough to reveal a new cell at the top of the list. If
     * so, then the appropriate parameters are updated.
     */
    protected void checkAndHandleFirstVisibleCellChange()
    {
        if (mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) {
            if (mCellIsMobile && mMobileItemId != NOT_FOUND) {
                updateNeighborViewsForID(mMobileItemId);
                handleCellSwitch();
            }
        }
    }


    /**
     * Determines if the listview scrolled down enough to reveal a new cell at the bottom of the
     * list. If so, then the appropriate parameters are updated.
     */
    protected void checkAndHandleLastVisibleCellChange()
    {
        int currentLastVisibleItem = mCurrentFirstVisibleItem + mCurrentVisibleItemCount;
        int previousLastVisibleItem = mPreviousFirstVisibleItem + mPreviousVisibleItemCount;
        if (currentLastVisibleItem != previousLastVisibleItem) {
            if (mCellIsMobile && mMobileItemId != NOT_FOUND) {
                updateNeighborViewsForID(mMobileItemId);
                handleCellSwitch();
            }
        }
    }
}

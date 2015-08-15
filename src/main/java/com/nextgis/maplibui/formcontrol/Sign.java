/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.formcontrol;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;

/**
 * A special control for create sign picture.
 */
public class Sign extends View {

    protected Drawable mCleanImage;
    protected int mClearBuff;
    protected int mClearImageSize;

    protected final int CLEAR_BUFF_DP = 15;
    protected final int CLEAR_IMAGE_SIZE_DP = 32;

    public Sign(Context context) {
        super(context);
        init();
    }

    public Sign(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Sign(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Sign(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    protected void init(){
        //1. get clean
        int[] attrs = new int[] { R.attr.ic_clear };
        TypedArray ta = getContext().obtainStyledAttributes(attrs);
        mCleanImage = ta.getDrawable(0);
        ta.recycle();

        mClearBuff = (int) (getContext().getResources().getDisplayMetrics().density * CLEAR_BUFF_DP);
        mClearImageSize = (int) (getContext().getResources().getDisplayMetrics().density * CLEAR_IMAGE_SIZE_DP);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        int posX = canvas.getWidth() - mClearImageSize - mClearBuff;

        mCleanImage.setBounds(posX,  mClearBuff, posX + mClearImageSize, mClearImageSize + mClearBuff);
        mCleanImage.draw(canvas);

        /*
        if (mMap != null) {
            mMap.draw(canvas, 0, 0, false);
        } else {
            super.onDraw(canvas);
        }*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;

            case MotionEvent.ACTION_MOVE:
                break;

            case MotionEvent.ACTION_UP:
                int posX = getWidth() - mClearImageSize - mClearBuff;
                Log.d(Constants.TAG, "x: " + event.getX() + " y: " + event.getY() + " posX: " + posX + " posY: " + mClearBuff);
                if(event.getX() > posX && event.getY() < mClearImageSize + mClearBuff){
                    onClearSign();
                }
                break;

            default:
                break;
        }

        return true;
    }

    private void onClearSign() {
        int x = 0;
    }
}

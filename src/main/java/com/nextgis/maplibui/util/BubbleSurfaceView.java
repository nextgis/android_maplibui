/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * Based on https://github.com/nextgis/nextgismobile/blob/master/src/com/nextgis/mobile/forms/BubbleSurfaceView.java
 * *****************************************************************************
 * Copyright (c) 2015-2016 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;

import com.nextgis.maplibui.R;

public class BubbleSurfaceView extends View {

    private Bitmap mBubble, mBubbleCircle, mBubbleCircleRed, mBubbleCircleGreen;
    private float mAzimuth, mRoll, mPitch;
    private float mX, mY;
    //private float mScaleRollAdds, mScalePitchAdds;
    private float mHalfWidth;
    private float mHalfHeight;
    private float mHalfBubbleW;
    private float mHalfBubbleH;
    private float mScaleRoll;
    private float mScalePitch;

    public BubbleSurfaceView(Context context) {
        super(context);
        init();
    }

    public BubbleSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public BubbleSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public void onDraw(Canvas canvas) {

        /*while (Math.sqrt(mRoll * mRoll + mPitch * mPitch) > 26) {

            if (mRoll < 0)
                mRoll += 0.01;
            else
                mRoll -= 0.01;

            if (mPitch < 0)
                mPitch += 0.01;
            else
                mPitch -= 0.01;
        }*/
        // top left corner of the mBubble
        mX = -mRoll * mScaleRoll + mHalfWidth - mHalfBubbleW;;
        mY = -mPitch * mScalePitch + mHalfHeight - mHalfBubbleH;

        // clearing canvas
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC_ATOP);

        // distance between center of the circle and center of the mBubble
        float dist = (float) Math.sqrt(Math.pow(mX + mHalfBubbleW - mHalfWidth, 2)
                + Math.pow(mY + mHalfBubbleH - mHalfHeight, 2));

        if (dist + mHalfBubbleW > getHeight() / 3) {
            // drawing mBubble circle
            canvas.drawBitmap(mBubbleCircleRed, 0, 0, null);
        } else {
            if (dist + mHalfBubbleW > getHeight() / 4) {
                // drawing mBubble circle
                canvas.drawBitmap(mBubbleCircleGreen, 0, 0, null);
            } else {
                // drawing mBubble circle
                canvas.drawBitmap(mBubbleCircle, 0, 0, null);
            }
        }

        // drawing mBubble last
        canvas.drawBitmap(mBubble, mX, mY, null);
    }

    private void init() {
        mBubble = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
        mBubbleCircle = BitmapFactory.decodeResource(getResources(), R.drawable.bubble_circle);
        mBubbleCircleRed = BitmapFactory.decodeResource(getResources(), R.drawable.bubble_circle_red);
        mBubbleCircleGreen = BitmapFactory.decodeResource(getResources(), R.drawable.bubble_circle_green);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mScaleRoll = getWidth() * 0.7F / 90F;
        mScalePitch = getHeight() * 0.7F / 90F;

        mHalfWidth = getWidth() / 2;
        mHalfHeight = getHeight() / 2;
        mHalfBubbleW = mBubble.getWidth() / 2;
        mHalfBubbleH = mBubble.getHeight() / 2;

        // center of the circle
        mX = mY = mHalfWidth;

//        mScaleRollAdds = scaleRoll + mHalfWidth - mHalfBubbleW;
//        mScalePitchAdds = scalePitch + mHalfHeight - mHalfBubbleH;

    }

    public void setSensorData(float a, float r, float p) {
        this.mAzimuth = a;
        this.mRoll = r;
        this.mPitch = p;

        invalidate();
    }
}
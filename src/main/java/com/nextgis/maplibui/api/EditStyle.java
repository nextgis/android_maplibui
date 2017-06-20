/*
 *  Project:  RusGIS
 *  Author:   Stanislav Petriakov, stanislav.petryakov@nextgis.com
 *  Year:     2017
 */

package com.nextgis.maplibui.api;

import android.content.Context;

public class EditStyle {
    private int mAlpha;
    private int mColor, mSelectedColor;
    private float mWidth, mSelectedWidth;
    protected float mScaledDensity;

    public EditStyle(Context context, int alpha, int color, float width, int selectedColor, float selectedWidth) {
        mScaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        setAlpha(alpha);
        setColor(color);
        setWidth(width);
        setSelectedColor(selectedColor);
        setSelectedWidth(selectedWidth);
    }

    public int getAlpha() {
        return mAlpha;
    }

    public void setAlpha(int alpha) {
        mAlpha = alpha;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor = color;
    }

    public int getSelectedColor() {
        return mSelectedColor;
    }

    public void setSelectedColor(int selectedColor) {
        mSelectedColor = selectedColor;
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float width) {
        mWidth = width * mScaledDensity;
    }

    public float getSelectedWidth() {
        return mSelectedWidth;
    }

    public void setSelectedWidth(float selectedWidth) {
        mSelectedWidth = selectedWidth * mScaledDensity;
    }
}

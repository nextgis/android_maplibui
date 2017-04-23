/*
 *  Project:  RusGIS
 *  Author:   Stanislav Petriakov, stanislav.petryakov@nextgis.com
 *  Year:     2017
 */

package com.nextgis.maplibui.api;

public class EditStyle {
    private int mAlpha;
    private int mColor, mSelectedColor;
    private float mWidth, mSelectedWidth;

    public EditStyle(int alpha, int color, float width, int selectedColor, float selectedWidth) {
        mAlpha = alpha;
        mColor = color;
        mWidth = width;
        mSelectedColor = selectedColor;
        mSelectedWidth = selectedWidth;
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

    public void setColor(int mColor) {
        mColor = mColor;
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
        mWidth = width;
    }

    public float getSelectedWidth() {
        return mSelectedWidth;
    }

    public void setSelectedWidth(float selectedWidth) {
        mSelectedWidth = selectedWidth;
    }
}

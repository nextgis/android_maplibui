/*
 *  Project:  RusGIS
 *  Author:   Stanislav Petriakov, stanislav.petryakov@nextgis.com
 *  Year:     2017
 */

package com.nextgis.maplibui.api;

import android.content.Context;

public class VertexStyle extends EditStyle {
    private int mOutColor;
    private float mOutWidth, mOutRadius;
    private float mRadius, mSelectedRadius;

    public VertexStyle(Context context, int alpha, int color, float width, float radius, int selectedColor, float selectedWidth, float selectedRadius,
                       int outColor, float outWidth, float outRadius) {
        super(context, alpha, color, width, selectedColor, selectedWidth);
        setRadius(radius);
        setSelectedRadius(selectedRadius);
        setOutColor(outColor);
        setOutWidth(outWidth);
        setOutRadius(outRadius);
    }

    public int getOutColor() {
        return mOutColor;
    }

    public void setOutColor(int outColor) {
        mOutColor = outColor;
    }

    public float getOutWidth() {
        return mOutWidth;
    }

    public void setOutWidth(float outWidth) {
        mOutWidth = outWidth * mScaledDensity;
    }

    public float getOutRadius() {
        return mOutRadius;
    }

    public void setOutRadius(float outRadius) {
        mOutRadius = outRadius * mScaledDensity;
    }

    public float getRadius() {
        return mRadius;
    }

    public void setRadius(float radius) {
        mRadius = radius * mScaledDensity;
    }

    public float getSelectedRadius() {
        return mSelectedRadius;
    }

    public void setSelectedRadius(float selectedRadius) {
        mSelectedRadius = selectedRadius * mScaledDensity;
    }
}

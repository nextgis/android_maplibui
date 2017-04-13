/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2017 NextGIS, info@nextgis.com
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
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * A special control for create sign picture.
 */
public class Sign extends View implements IFormControl {
    public static final String SIGN_FILE = Integer.MAX_VALUE + "";

    protected Drawable mCleanImage;
    protected int mClearBuff;
    protected int mClearImageSize;
    protected Path    mPath;
    protected final LinkedList<Path> mPaths = new LinkedList<>();
    protected Paint mPaint;
    protected float mX, mY;
    protected String mPreviousSignPath;
    protected File mPreviousSign;
    protected boolean mNotInitialized;
    protected Bitmap mPreviousSignBitmap;

    protected final int CLEAR_BUFF_DP = 15;
    protected final int CLEAR_IMAGE_SIZE_DP = 32;
    protected static final float TOUCH_TOLERANCE = 4;

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

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3);
        mPaint.setDither(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        boolean bDark = ControlHelper.isDarkTheme(getContext());
        if(bDark)
            mPaint.setColor(Color.WHITE);
        else
            mPaint.setColor(Color.BLACK);

        mPath = new Path();
        mPaths.add(mPath);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mPreviousSign = new File(mPreviousSignPath, SIGN_FILE);
        if (mNotInitialized = mPreviousSign.exists()) {
            try {
                BitmapFactory.Options options = ControlHelper.getOptions(new FileInputStream(mPreviousSign), w, h);
                mPreviousSignBitmap = ControlHelper.getBitmap(new FileInputStream(mPreviousSign), options);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if (mNotInitialized && mPreviousSignBitmap != null)
            canvas.drawBitmap(mPreviousSignBitmap, 0, 0, null);

        int posX = canvas.getWidth() - mClearImageSize - mClearBuff;
        mCleanImage.setBounds(posX, mClearBuff, posX + mClearImageSize, mClearImageSize + mClearBuff);
        mCleanImage.draw(canvas);

        if (!mNotInitialized)
            for (Path path : mPaths)
                canvas.drawPath(path, mPaint);
    }

    protected void drawSign(Canvas canvas, int bkColor, Paint paint){
        canvas.drawColor(bkColor);

        for (Path path : mPaths)
            canvas.drawPath(path, paint);
    }

    protected void touchStart(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    protected void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }

    protected void touchUp() {
        mPath.lineTo(mX, mY);

        postInvalidate();

        // kill this so we don't double draw
        mPath = new Path();
        mPaths.add(mPath);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mNotInitialized)
                    touchStart(x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                if (!mNotInitialized) {
                    touchMove(x, y);
                    postInvalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                int posX = getWidth() - mClearImageSize - mClearBuff;
                //Log.d(Constants.TAG, "x: " + event.getX() + " y: " + event.getY() + " posX: " + posX + " posY: " + mClearBuff);
                if(event.getX() > posX && event.getY() < mClearImageSize + mClearBuff){
                    onClearSign();
                } else if (!mNotInitialized) {
                    touchUp();
                    invalidate();
                }
                break;

            default:
                break;
        }

        return true;
    }

    private void onClearSign() {
        if (mNotInitialized)
            mNotInitialized = false;

        mPaths.clear();
        mPath = new Path();
        mPaths.add(mPath);

        postInvalidate();
    }

    public void save(int width, int height, boolean transparentBackground, File sigFile) throws IOException {
        if (mNotInitialized)
            return;

        float scale = Math.min((float) width / getWidth(), (float) height / getHeight());
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.setMatrix(matrix);

        int color = transparentBackground ? Color.TRANSPARENT : 0xFFFFFF - mPaint.getColor();
        drawSign(canvas, color, mPaint);
        if(sigFile.exists() || sigFile.createNewFile()) {
            FileOutputStream out = new FileOutputStream(sigFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
        }
    }

    public void setPath(String path) {
        mPreviousSignPath = path;
    }

    @Override
    public void init(JSONObject element, List<Field> fields, Bundle savedState, Cursor featureCursor, SharedPreferences lastValue) throws JSONException {
        init();
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {

    }

    @Override
    public boolean isShowLast() {
        return false;
    }

    @Override
    public String getFieldName() {
        return null;
    }

    @Override
    public void addToLayout(ViewGroup layout) {
        layout.addView(this);
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void saveState(Bundle outState) {

    }
}

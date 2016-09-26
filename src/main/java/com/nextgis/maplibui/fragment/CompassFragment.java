/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * Based on https://github.com/nextgis/nextgismobile/blob/master/src/com/nextgis/mobile/forms/CompassFragment.java
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

package com.nextgis.maplibui.fragment;

import android.Manifest;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.nextgis.maplib.util.PermissionUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.BubbleSurfaceView;
import com.nextgis.maplibui.util.CompassImage;
import com.nextgis.maplibui.util.ControlHelper;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;

public class CompassFragment extends Fragment implements View.OnTouchListener {
    public static final String ACTION_COMPASS_UPDATES = "com.nextgis.mobile.ACTION_COMPASS_UPDATES";
    public static final char DEGREE_CHAR = (char) 0x00B0;

    protected Location mCurrentLocation;
    protected float mDeclination;
    protected float mAzimuth;
    protected float mDownX, mDownY;

    protected FrameLayout mParent;
    protected ImageView mBasePlate;
    protected BubbleSurfaceView mBubbleView;
    protected CompassImage mCompass, mCompassNeedle, mCompassNeedleMagnetic;
    protected TextView mTvAzimuth;

    protected SensorManager mSensorManager;
    protected Vibrator mVibrator;

    protected boolean mIsVibrationOn, mIsNeedleOnly, mTrueNorth = true, mShowMagnetic;
    private boolean mClickable = false;

    public void setStyle(boolean isNeedleOnly) {
        mIsNeedleOnly = isNeedleOnly;
    }

    protected void setInterface() {
        if (!mIsNeedleOnly) {
            mBasePlate.setVisibility(View.VISIBLE);

            if(null != mBubbleView)
                mBubbleView.setVisibility(View.VISIBLE);
            // magnetic north compass
            mCompass.setOnTouchListener(this);
            mCompass.setVisibility(View.VISIBLE);
            mTvAzimuth.setVisibility(View.VISIBLE);
            mTvAzimuth.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mCompass != null) {
                        mCompass.setAngle(0);
                        mCompass.postInvalidate();
                    }

                    return true;
                }
            });
        } else
            mParent.setBackgroundColor(Color.TRANSPARENT);

        mParent.setClickable(mClickable);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_compass, container, false);
        mParent = (FrameLayout) view.findViewById(R.id.compass_fragment);
        mBasePlate = (ImageView) view.findViewById(R.id.base_plate);
        mBubbleView = (BubbleSurfaceView) view.findViewById(R.id.bubble_view);
        mCompass = (CompassImage) view.findViewById(R.id.compass);
        mTvAzimuth = (TextView) view.findViewById(R.id.azimuth);
        mCompassNeedle = (CompassImage) view.findViewById(R.id.needle);
        mCompassNeedleMagnetic = (CompassImage) view.findViewById(R.id.needle_magnetic);

        mBasePlate.post(new Runnable() {
            @Override
            public void run() {
                loadImage(mBasePlate, "compass_baseplate.png");
            }
        });
        mCompass.post(new Runnable() {
            @Override
            public void run() {
                loadImage(mCompass, "compass_bezel.png");
            }
        });
        mCompassNeedle.post(new Runnable() {
            @Override
            public void run() {
                loadImage(mCompassNeedle, "compass_needle.png");
            }
        });
        mCompassNeedleMagnetic.post(new Runnable() {
            @Override
            public void run() {
                loadImage(mCompassNeedleMagnetic, "compass_needle.png");
            }
        });

        return view;
    }

    private void loadImage(ImageView view, String image) {
        int width = 0, height = 0;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            width = view.getRootView().getMeasuredWidth();
        else
            height = view.getRootView().getMeasuredHeight();

        InputStream is = null;
        try {
            is = getContext().getAssets().open(image);
            BitmapFactory.Options options = ControlHelper.getOptions(is, width, height);
            is = getContext().getAssets().open(image);
            Bitmap bitmap = ControlHelper.getBitmap(is, options);
            view.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (null == getParentFragment()) {
            setRetainInstance(true);
        }

        // reference to vibrator service
        mDeclination = 0;
        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        if(!PermissionUtil.hasPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                || !PermissionUtil.hasPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION))
            return;

        if (mCurrentLocation == null) {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (mCurrentLocation == null) {
                mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        if (mCurrentLocation != null)
            mDeclination = getDeclination(mCurrentLocation, System.currentTimeMillis());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        try {
            mSensorManager.unregisterListener(sensorListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        mSensorManager.registerListener(sensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);

        setInterface();
        super.onResume();
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float upX = event.getX();
                float upY = event.getY();

                double downR = Math.atan2(v.getHeight() / 2 - mDownY, mDownX - v.getWidth() / 2);
                int angle1 = (int) Math.toDegrees(downR);

                double upR = Math.atan2(v.getHeight() / 2 - upY, upX - v.getWidth() / 2);
                int angle2 = (int) Math.toDegrees(upR);

                this.rotateCompass(angle1 - angle2);

                if (mIsVibrationOn) {
                    mVibrator.vibrate(5);
                }

                // update starting point for next move event
                mDownX = upX;
                mDownY = upY;

                return true;
        }
        return false;
    }

    protected void rotateCompass(float angle) {
        // magnetic north compass
        if (mCompass != null) {
            mCompass.setAngle(mCompass.getAngle() + angle);
            mCompass.postInvalidate();
        }
    }

    public void updateCompass(float azimuth) {
        float rotation;

        // are we taking declination into account?
        if (!mTrueNorth || mCurrentLocation == null) {
            mDeclination = 0;
        }

        // magnetic north to true north, compensate for device's physical rotation
        rotation = getAzimuth(azimuth + mDeclination + getDeviceRotation());
        mAzimuth = rotation;

        if (mTvAzimuth != null) {
            String azimuthText = formatNumber(rotation, 0, 0) + DEGREE_CHAR + " " + getDirectionCode(rotation, getResources());
            mTvAzimuth.setText(azimuthText);
        }

        // true north compass
        if (mCompassNeedle.getVisibility() == View.VISIBLE) {
            mCompassNeedle.setAngle(360 - rotation);
            mCompassNeedle.postInvalidate();
        }

        // magnetic north compass
        if (mShowMagnetic) {
            mCompassNeedleMagnetic.setVisibility(View.VISIBLE);
            mCompassNeedleMagnetic.setAngle(360 - rotation + mDeclination);
            mCompassNeedleMagnetic.postInvalidate();
        } else {
            mCompassNeedleMagnetic.setVisibility(View.INVISIBLE);
        }
    }

    public static String getDirectionCode(float azimuth, Resources res) {
        int nIndex = Math.round(azimuth / 45);

        String directionCodes[] = {
                res.getString(R.string.N),
                res.getString(R.string.N) + res.getString(R.string.E),
                res.getString(R.string.E),
                res.getString(R.string.S) + res.getString(R.string.E),
                res.getString(R.string.S),
                res.getString(R.string.S) + res.getString(R.string.W),
                res.getString(R.string.W),
                res.getString(R.string.N) + res.getString(R.string.W),
                res.getString(R.string.N) };

        if (nIndex > 8 || nIndex < 0)
            return directionCodes[0];
        else
            return directionCodes[nIndex];
    }

    public static String formatNumber(Object value, int max, int min) {
        NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(max);
        f.setMinimumFractionDigits(min);
        f.setGroupingUsed(false);

        try {
            return f.format(value);
        } catch (IllegalArgumentException e) {
            return e.getLocalizedMessage();
        }
    }

    protected float getAzimuth(float az) {
        return az > 360 ? az - 360 : az;
    }

    protected SensorEventListener sensorListener = new SensorEventListener() {

        private float mPrevAzimuth;

        public void onSensorChanged(SensorEvent event) {
            float roll, pitch;
            float azimuth = event.values[0];

            if(Math.abs(mPrevAzimuth - azimuth) < 0.4f)
                return;

            mPrevAzimuth = azimuth;

            updateCompass(azimuth);
            int rotation = getDeviceRotation();

            if (rotation == 90) {
                roll = event.values[1];
                pitch = -event.values[2];
            } else if (rotation == 270) {
                roll = -event.values[1];
                pitch = event.values[2];
            } else {
                roll = event.values[2];
                pitch = event.values[1];
            }

            if(null != mBubbleView)
                mBubbleView.setSensorData(azimuth, roll, pitch);
        }

        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }
    };

    public static float getDeclination(Location location, long timestamp) {
        if (location == null)
            return 0;

        GeomagneticField field = new GeomagneticField((float) location.getLatitude(), (float) location.getLongitude(),
                (float) location.getAltitude(), timestamp);

        return field.getDeclination();
    }

    public int getDeviceRotation() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        final int rotation = display.getRotation();

        if (rotation == Surface.ROTATION_90) {
            return 90;
        } else if (rotation == Surface.ROTATION_180) {
            return 180;
        } else if (rotation == Surface.ROTATION_270) {
            return 270;
        }

        return 0;
    }

    public void setClickable(boolean clickable) {
        mClickable = clickable;
    }
}
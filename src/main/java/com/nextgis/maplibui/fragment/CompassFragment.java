/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * Based on https://github.com/nextgis/nextgismobile/blob/master/src/com/nextgis/mobile/forms/CompassFragment.java
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
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
import android.widget.TextView;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.BubbleSurfaceView;
import com.nextgis.maplibui.util.CompassImage;

import java.text.NumberFormat;

public class CompassFragment extends Fragment implements View.OnTouchListener {
    public static final String ACTION_COMPASS_UPDATES = "com.nextgis.mobile.ACTION_COMPASS_UPDATES";
    public static final char DEGREE_CHAR = (char) 0x00B0;

    protected Location mCurrentLocation;
    protected float mDeclination;
    protected float mAzimuth;
    protected float mDownX, mDownY;

    protected FrameLayout mParent;
    protected View mBasePlate;
    protected BubbleSurfaceView mBubbleView;
    protected CompassImage mCompass, mCompassNeedle, mCompassNeedleMagnetic;
    protected TextView mTvAzimuth;

    protected SensorManager mSensorManager;
    protected Vibrator mVibrator;

    protected boolean mIsVibrationOn, mIsNeedleOnly, mTrueNorth = true, mShowMagnetic;

    public void setStyle(boolean isNeedleOnly) {
        mIsNeedleOnly = isNeedleOnly;
    }

    protected void setInterface() {
        if (!mIsNeedleOnly) {
            mBasePlate.setVisibility(View.VISIBLE);
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
                        mCompass.invalidate();
                    }

                    return true;
                }
            });
        } else
            mParent.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_compass, container, false);
        mParent = (FrameLayout) view.findViewById(R.id.compass_fragment);
        mBasePlate = view.findViewById(R.id.base_plate);
        mBubbleView = (BubbleSurfaceView) view.findViewById(R.id.bubble_view);
        mCompass = (CompassImage) view.findViewById(R.id.compass);
        mTvAzimuth = (TextView) view.findViewById(R.id.azimuth);
        mCompassNeedle = (CompassImage) view.findViewById(R.id.needle);
        mCompassNeedleMagnetic = (CompassImage) view.findViewById(R.id.needle_magnetic);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (null == getParentFragment()) {
            setRetainInstance(true);
        }

        // reference to vibrator service
        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        if (mCurrentLocation == null) {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (mCurrentLocation == null) {
                mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        mDeclination = 0;
        if (mCurrentLocation != null)
            mDeclination = getDeclination(mCurrentLocation, System.currentTimeMillis());

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(sensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(sensorListener);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        if(mBubbleView != null)
            mBubbleView.pause();

        getActivity().unregisterReceiver(compassBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        setInterface();
        if(mBubbleView != null)
            mBubbleView.resume();

        // registering receiver for compass updates
        getActivity().registerReceiver(compassBroadcastReceiver, new IntentFilter(ACTION_COMPASS_UPDATES));
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
            mCompass.invalidate();
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
            mCompassNeedle.invalidate();
        }

        // magnetic north compass
        if (mShowMagnetic) {
            mCompassNeedleMagnetic.setVisibility(View.VISIBLE);
            mCompassNeedleMagnetic.setAngle(360 - rotation + mDeclination);
            mCompassNeedleMagnetic.invalidate();
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
        public void onSensorChanged(SensorEvent event) {
            // let's broadcast compass data to any activity waiting for updates
            Intent intent = new Intent(ACTION_COMPASS_UPDATES);

            // packing azimuth value into bundle
            Bundle bundle = new Bundle();
            bundle.putFloat("azimuth", event.values[0]);
            bundle.putFloat("pitch", event.values[1]);
            bundle.putFloat("roll", event.values[2]);

            intent.putExtras(bundle);

            // broadcasting compass updates
            if(getActivity() != null)
                getActivity().sendBroadcast(intent);
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

    protected BroadcastReceiver compassBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float roll, pitch;
            Bundle bundle = intent.getExtras();
            updateCompass(bundle.getFloat("azimuth"));
            int rotation = getDeviceRotation();

            if (rotation == 90) {
                roll = bundle.getFloat("pitch");
                pitch = -bundle.getFloat("roll");
            } else if (rotation == 270) {
                roll = -bundle.getFloat("pitch");
                pitch = bundle.getFloat("roll");
            } else {
                roll = bundle.getFloat("roll");
                pitch = bundle.getFloat("pitch");
            }

            if(mBubbleView != null)
                mBubbleView.setSensorData(bundle.getFloat("azimuth"), roll, pitch);
        }
    };
}
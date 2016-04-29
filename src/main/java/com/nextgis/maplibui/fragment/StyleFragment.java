/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.nextgis.maplib.display.SimpleLineStyle;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.maplib.display.SimplePolygonStyle;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.dialog.StyledDialogFragment;

import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;

public class StyleFragment extends StyledDialogFragment implements View.OnClickListener {
    protected ImageView mColorFillImage, mColorStrokeImage;
    protected TextView mColorFillName, mColorStrokeName;
    protected int mFillColor, mStrokeColor;
    protected Style mStyle;

    public StyleFragment() {

    }

    public void setStyle(Style style) {
        mStyle = style;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mStyle == null)
            return null;

        View body = null;
        mFillColor = mStyle.getColor();
        if (mStyle instanceof SimpleMarkerStyle) {
            body = inflater.inflate(R.layout.style_marker, container, false);
            inflateMarker(body);
        } else if (mStyle instanceof SimpleLineStyle) {
            body = inflater.inflate(R.layout.style_line, container, false);
            inflateLine(body);
        } else if (mStyle instanceof SimplePolygonStyle) {
            body = inflater.inflate(R.layout.style_polygon, container, false);
            inflatePolygon(body);
        }

        setView(body, true);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void inflateMarker(View v) {
        Spinner type = (Spinner) v.findViewById(R.id.type);
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((SimpleMarkerStyle) mStyle).setType(position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        type.setSelection(((SimpleMarkerStyle) mStyle).getType() - 1);

        float size = ((SimpleMarkerStyle) mStyle).getSize();
        EditText sizeText = (EditText) v.findViewById(R.id.size);
        sizeText.setText(String.format(Locale.getDefault(), "%.0f", size));
        sizeText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    ((SimpleMarkerStyle) mStyle).setSize(Float.parseFloat(s.toString()));
                } catch (Exception ignored) { }
            }
        });

        mStrokeColor = ((SimpleMarkerStyle) mStyle).getOutlineColor();
        mColorFillName = (TextView) v.findViewById(R.id.color_fill_name);
        mColorFillImage = (ImageView) v.findViewById(R.id.color_fill_ring);
        mColorStrokeName = (TextView) v.findViewById(R.id.color_stroke_name);
        mColorStrokeImage = (ImageView) v.findViewById(R.id.color_stroke_ring);

        LinearLayout color_fill = (LinearLayout) v.findViewById(R.id.color_fill);
        LinearLayout color_stroke = (LinearLayout) v.findViewById(R.id.color_stroke);
        color_fill.setOnClickListener(this);
        color_stroke.setOnClickListener(this);
        setFillColor(mFillColor);
        setStrokeColor(mStrokeColor);

        float width = ((SimpleMarkerStyle) mStyle).getWidth();
        EditText widthText = (EditText) v.findViewById(R.id.width);
        widthText.setText(String.format(Locale.getDefault(), "%.0f", width));
        widthText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    ((SimpleMarkerStyle) mStyle).setWidth(Float.parseFloat(s.toString()));
                } catch (Exception ignored) { }
            }
        });
    }

    private void inflateLine(View v) {
        mStrokeColor = ((SimpleLineStyle) mStyle).getOutColor();

        mColorFillName = (TextView) v.findViewById(R.id.color_fill_name);
        mColorFillImage = (ImageView) v.findViewById(R.id.color_fill_ring);
        mColorStrokeName = (TextView) v.findViewById(R.id.color_stroke_name);
        mColorStrokeImage = (ImageView) v.findViewById(R.id.color_stroke_ring);

        LinearLayout color_fill = (LinearLayout) v.findViewById(R.id.color_fill);
        LinearLayout color_stroke = (LinearLayout) v.findViewById(R.id.color_stroke);
        color_fill.setOnClickListener(this);
        color_stroke.setOnClickListener(this);
        setFillColor(mFillColor);
        setStrokeColor(mStrokeColor);

        float width = ((SimpleLineStyle) mStyle).getWidth();
        EditText widthText = (EditText) v.findViewById(R.id.width);
        widthText.setText(String.format(Locale.getDefault(), "%.0f", width));
        widthText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    ((SimpleLineStyle) mStyle).setWidth(Float.parseFloat(s.toString()));
                } catch (Exception ignored) { }
            }
        });

        Spinner type = (Spinner) v.findViewById(R.id.type);
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((SimpleLineStyle) mStyle).setType(position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        type.setSelection(((SimpleLineStyle) mStyle).getType() - 1);
    }

    private void inflatePolygon(View v) {
        float width = ((SimplePolygonStyle) mStyle).getWidth();
        boolean fill = ((SimplePolygonStyle) mStyle).isFill();

        mColorFillName = (TextView) v.findViewById(R.id.color_fill_name);
        mColorFillImage = (ImageView) v.findViewById(R.id.color_fill_ring);

        CheckBox fillCheck = (CheckBox) v.findViewById(R.id.fill);
        fillCheck.setChecked(fill);
        fillCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((SimplePolygonStyle) mStyle).setFill(isChecked);
            }
        });

        EditText widthText = (EditText) v.findViewById(R.id.width);
        widthText.setText(String.format(Locale.getDefault(), "%.0f", width));
        widthText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    ((SimplePolygonStyle) mStyle).setWidth(Float.parseFloat(s.toString()));
                } catch (Exception ignored) { }
            }
        });

        LinearLayout color_fill = (LinearLayout) v.findViewById(R.id.color_fill);
        color_fill.setOnClickListener(this);
        setFillColor(mFillColor);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setFillColor(int color) {
        setColor(mColorFillImage, mColorFillName, color);
    }

    protected void setStrokeColor(int color) {
        setColor(mColorStrokeImage, mColorStrokeName, color);
    }

    private static void setColor(ImageView image, TextView text, int color) {
        // set color
        GradientDrawable sd = (GradientDrawable) image.getDrawable();
        sd.setColor(color);
        image.invalidate();

        // set color name
        text.setText(getColorName(color));
    }

    protected static String getColorName(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.color_fill) {//show colors dialog
            AmbilWarnaDialog dialog = new AmbilWarnaDialog(v.getContext(), mFillColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    mFillColor = color;
                    setFillColor(color);
                    mStyle.setColor(color);
                }

                @Override
                public void onCancel(AmbilWarnaDialog dialog) {

                }
            });

            dialog.show();
        } else if (i == R.id.color_stroke) {//show colors dialog
            AmbilWarnaDialog dialog = new AmbilWarnaDialog(v.getContext(), mStrokeColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    mStrokeColor = color;
                    setStrokeColor(color);

                    if (mStyle instanceof SimpleMarkerStyle)
                        ((SimpleMarkerStyle) mStyle).setOutlineColor(color);
                    else if (mStyle instanceof SimpleLineStyle)
                        ((SimpleLineStyle) mStyle).setOutColor(color);
                }

                @Override
                public void onCancel(AmbilWarnaDialog dialog) {

                }
            });

            dialog.show();
        }
    }
}
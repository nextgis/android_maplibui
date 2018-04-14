/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2018 NextGIS, info@nextgis.com
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGIDLoginActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.nextgis.maplib.util.GeoConstants.GTLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPoint;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPolygon;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;
import static com.nextgis.maplib.util.GeoConstants.GTPolygon;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_INPUT_SEARCH;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_SHOW_LAST_KEY;


public final class ControlHelper
{
    private final static String BUNDLE_SAVED_STATE = "nextgis_control_";

    public static String getPercentValue(
            Context context,
            int stringLabel,
            float value)
    {
        return context.getString(stringLabel) + ": " + ((int) value * 100 / 255) + "%";
    }

    public static void setEnabled(
            MenuItem item,
            boolean state)
    {
        if (null == item) {
            return;
        }
        item.setEnabled(state);
        item.getIcon().setAlpha(state ? 255 : 160);
    }

    public static boolean isEnabled(
            List<Field> fields,
            String fieldName)
    {
        for (Field field : fields) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSaveLastValue(JSONObject attributes)
            throws JSONException
    {
        return !attributes.isNull(JSON_SHOW_LAST_KEY) && attributes.getBoolean(JSON_SHOW_LAST_KEY);
    }

    public static boolean isAutoComplete(JSONObject attributes)
            throws JSONException
    {
        return attributes.has(JSON_INPUT_SEARCH) && attributes.getBoolean(JSON_INPUT_SEARCH);
    }

    public static boolean hasKey(
            Bundle savedState,
            String fieldName)
    {
        return savedState != null && savedState.containsKey(getSavedStateKey(fieldName));
    }

    public static String getSavedStateKey(String fieldName)
    {
        return BUNDLE_SAVED_STATE + fieldName;
    }

    public static Drawable getIconByVectorType(
            Context context,
            int geometryType,
            int color,
            int defaultIcon,
            boolean syncable)
    {
        int drawableId;

        switch (geometryType) {
            case GTPoint:
                drawableId = R.drawable.ic_type_point;
                break;
            case GTMultiPoint:
                drawableId = R.drawable.ic_type_multipoint;
                break;
            case GTLineString:
                drawableId = R.drawable.ic_type_line;
                break;
            case GTMultiLineString:
                drawableId = R.drawable.ic_type_multiline;
                break;
            case GTPolygon:
                drawableId = R.drawable.ic_type_polygon;
                break;
            case GTMultiPolygon:
                drawableId = R.drawable.ic_type_multipolygon;
                break;
            default:
                return ContextCompat.getDrawable(context, defaultIcon);
        }

        BitmapDrawable icon = (BitmapDrawable) ContextCompat.getDrawable(context, drawableId);
        if (icon != null) {
            Bitmap src = icon.getBitmap();
            Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
            Canvas canvas = new Canvas(bitmap);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            paint.setColorFilter(filter);
            canvas.drawBitmap(src, 0, 0, paint);

            if (syncable) {
                int syncIconId = isDarkTheme(context)
                                 ? R.drawable.ic_action_refresh_dark
                                 : R.drawable.ic_action_refresh_light;
                src = BitmapFactory.decodeResource(context.getResources(), syncIconId);
                src = Bitmap.createScaledBitmap(src, bitmap.getWidth() / 2, bitmap.getWidth() / 2,
                        true);
                canvas.drawBitmap(src, bitmap.getWidth() - bitmap.getWidth() / 2,
                        bitmap.getWidth() - bitmap.getWidth() / 2, new Paint());
            }

            icon = new BitmapDrawable(context.getResources(), bitmap);
        }

        return icon;
    }

    public static boolean isDarkTheme(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsConstantsUI.KEY_PREF_THEME, SettingsConstantsUI.KEY_PREF_LIGHT)
                .equals(SettingsConstantsUI.KEY_PREF_DARK);
    }

    public static Drawable tintDrawable(
            Drawable drawable,
            int color)
    {
        if (drawable == null) {
            return null;
        }

        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, color);
        return drawable;
    }

    public static int getDialogTheme(
            Context context,
            int theme)
    {
        int dialogTheme = R.style.Theme_NextGIS_AppCompat_Light_Dialog;
        int[] attrs = {android.R.attr.alertDialogStyle};
        TypedArray ta = context.obtainStyledAttributes(theme, attrs);
        dialogTheme = ta.getResourceId(0, dialogTheme);
        ta.recycle();

        return dialogTheme;
    }

    public static String getSyncTime(
            Context context,
            long timeStamp)
    {
        String date =
                new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(timeStamp));
        String time =
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timeStamp));
        return String.format(context.getString(R.string.last_sync_time), date, time);
    }


    public static Bitmap getBitmap(
            InputStream is,
            BitmapFactory.Options options)
    {
        Bitmap result = null;
        if (is == null) {
            return null;
        }

        try {
            result = BitmapFactory.decodeStream(is, null, options);
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();

            try {
                options.inSampleSize *= 4;
                result = BitmapFactory.decodeStream(is, null, options);
            } catch (OutOfMemoryError oom1) {
                oom1.printStackTrace();
            }
        }

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    @SuppressWarnings("deprecation")
    public static BitmapFactory.Options getOptions(
            InputStream is,
            int width,
            int height)
    {
        if (is == null) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);

        if (height == 0) {
            height = (int) ((1f * width * options.outHeight / options.outWidth));
        } else if (width == 0) {
            width = (int) ((1f * height * options.outWidth / options.outHeight));
        }

        options.inSampleSize = calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        options.inDither = false;
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[32 * 1024];
        return options;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options,
            int reqWidth,
            int reqHeight)
    {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static int getColor(
            Context context,
            int attr)
    {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public static int dpToPx(
            int dp,
            Resources resources)
    {
        DisplayMetrics dm = resources.getDisplayMetrics();
        return Math.round(dp * (dm.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    // http://stackoverflow.com/a/32973351/2088273
    public static AppCompatActivity getActivity(Context context)
    {
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) {
                return (AppCompatActivity) context;
            }

            context = ((ContextWrapper) context).getBaseContext();
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void lockScreenOrientation(Activity activity)
    {
        WindowManager windowManager =
                (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Configuration configuration = activity.getResources().getConfiguration();
        int rotation = windowManager.getDefaultDisplay().getRotation();

        // Search for the natural position of the device
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && (
                rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
                || configuration.orientation == Configuration.ORIENTATION_PORTRAIT && (
                rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)) {
            // Natural position is Landscape
            switch (rotation) {
                case Surface.ROTATION_0:
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case Surface.ROTATION_90:
                    activity.setRequestedOrientation(
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface.ROTATION_180:
                    activity.setRequestedOrientation(
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    break;
                case Surface.ROTATION_270:
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
            }
        } else {
            // Natural position is Portrait
            switch (rotation) {
                case Surface.ROTATION_0:
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
                case Surface.ROTATION_90:
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case Surface.ROTATION_180:
                    activity.setRequestedOrientation(
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface.ROTATION_270:
                    activity.setRequestedOrientation(
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    break;
            }
        }
    }

    public static void unlockScreenOrientation(Activity activity)
    {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public static void setClearAction(final EditText target)
    {
        target.setOnTouchListener(new View.OnTouchListener()
        {
            final int RIGHT = 2;

            @Override
            public boolean onTouch(
                    View view,
                    MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int leftEdgeOfRightDrawable =
                            view.getRight() - target.getCompoundDrawables()[RIGHT].getBounds()
                                    .width();
                    // when EditBox has padding, adjust leftEdge like
                    // leftEdgeOfRightDrawable -= getResources().getDimension(R.dimen.edittext_padding_left_right);
                    if (event.getRawX() >= leftEdgeOfRightDrawable) {
                        // clicked on clear icon
                        target.setText("");
                        target.clearFocus();
                        return false;
                    }
                }
                return false;
            }
        });
    }

    public static void setZoomText(
            Activity activity,
            TextView text,
            int string,
            int zoom)
    {
        String scale = LocationUtil.formatLength(activity, MapUtil.getScaleInCm(activity, zoom), 0);
        text.setText(String.format(activity.getString(string) + "\r\n" + scale, zoom));
    }

    public static void highlightText(TextView textView)
    {
        final CharSequence text = textView.getText();
        final SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(
                new URLSpan(""), 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannableString, TextView.BufferType.SPANNABLE);
    }

    /**
     * Specifies a tint for {@code inputDrawable}.
     *
     * @param inputDrawable
     *         The Drawable which to apply the tint.
     * @param tint
     *         Color to use for tinting returned drawable.
     *
     * @return Wrapped drawable with {@link android.support.v4.graphics.drawable.DrawableCompat#wrap(Drawable)}
     */
    public static Drawable getTintedDrawable(@NonNull Drawable inputDrawable, @ColorInt int tint) {
        Drawable wrapDrawable = DrawableCompat.wrap(inputDrawable.mutate());
        DrawableCompat.setTint(wrapDrawable, tint);
        return wrapDrawable;
    }

    public static void showProDialog(final Context context) {
        AlertDialog builder = new AlertDialog.Builder(context)
                .setTitle(R.string.pro_user_only)
                .setMessage(R.string.get_pro)
                .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(context, NGIDLoginActivity.class);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        TextView message = builder.findViewById(android.R.id.message);
        if (message != null) {
            message.setMovementMethod(LinkMovementMethod.getInstance());
            message.setLinksClickable(true);
        }
    }
}

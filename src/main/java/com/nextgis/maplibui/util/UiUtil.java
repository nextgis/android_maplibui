/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2018, 2021 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.util;

import static com.nextgis.maplib.util.Constants.TAG;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplibui.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class UiUtil {


    public static void showSimpleOKAlert(final Activity activity, String text){
        new AlertDialog.Builder(activity)
                .setMessage(text)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    public static void showSimpleToast(final Activity activity, String text){
        Toast.makeText(activity, text , Toast.LENGTH_LONG).show();
    }

    // Thanks to https://stackoverflow.com/a/41562794
    public static void setForceShowIcon(PopupMenu popupMenu) {
        try {
            Field[] mFields = popupMenu.getClass().getDeclaredFields();
            for (Field field : mFields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> popupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method mMethods = popupHelper.getMethod("setForceShowIcon", boolean.class);
                    mMethods.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void notFound(Activity activity) {
        Toast.makeText(activity, R.string.no_activity_found, Toast.LENGTH_SHORT).show();
    }

    public static void share(File file, String mimeType, Activity activity, Boolean showInfoDialog) {
        Intent shareIntent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = activity.getPackageName() + FileUtil.AUTHORITY;
            Uri uri = FileProvider.getUriForFile(activity, authority, file);
            shareIntent = ShareCompat.IntentBuilder.from(activity)
                    .setStream(uri)
                    .setType(mimeType)
                    .getIntent()
                    .setAction(Intent.ACTION_SEND)
                    .setDataAndType(uri, mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            shareIntent = Intent.createChooser(shareIntent, activity.getString(R.string.menu_share));
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            shareIntent.setType(mimeType);
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
//        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisArray); // multiple data
        try {
            activity.startActivity(shareIntent);
        } catch (ActivityNotFoundException e) {
            if (showInfoDialog) {
                String info = activity.getString(R.string.get_file_manually, file.getName(), file.getAbsolutePath());
                AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
                dialog.setTitle(R.string.error_file_create)
                        .setMessage(info)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            } else
                notFound(activity);
        }
    }

    public static void save(File inputFile, String mimeType, Activity activity, Boolean showInfoDialog,
                            Intent outputData) {
        // save to choosed place
        if (outputData == null)
            return;
        try {
            final ContentResolver resolver = activity.getContentResolver();
            Uri docUri = outputData.getData();

            if (docUri != null) {
                if (docUri != null) {
                    final OutputStream output = resolver.openOutputStream(docUri);
                    try {
                        InputStream inputStream = new FileInputStream(inputFile);
                        byte[] buffer = new byte[10240]; // or other buffer size
                        int read;
                        while ((read = inputStream.read(buffer)) != -1) {
                            output.write(buffer, 0, read);
                        }
                        output.flush();
                        Toast.makeText(activity, R.string.save_complete, Toast.LENGTH_LONG).show();

                    } catch (Exception ex) {
                        new AlertDialog.Builder(activity)
                                .setMessage(R.string.error_on_save)
                                .setPositiveButton(R.string.ok, null)
                                .create()
                                .show();
                        //Toast.makeText(activity, R.string.error_on_save, Toast.LENGTH_LONG).show();
                        Log.e(TAG, ex.getMessage());

                    } finally {
                        output.close();
                    }
                }
            }
        } catch (Exception exception){
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.error_on_save)
                    .setPositiveButton(R.string.ok, null)
                    .create()
                    .show();
            //Toast.makeText(activity, R.string.error_on_save, Toast.LENGTH_LONG).show();
            Log.e(TAG, exception.getMessage());
        }
    }

    static public void showNoEditPermAlert(Activity activity){
        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setMessage(com.nextgis.maplib.R.string.layer_not_editable)
                .setPositiveButton(com.nextgis.maplibui.R.string.ok, null)
                .create()
                .show();
    }

//    static public void showNoEditPermAlert(Context context){
    // future

//        TextView textView = new TextView(context);
//        textView.setPadding(dpToPx(context, 16), dpToPx(context, 16),
//                dpToPx(context, 16), dpToPx(context, 16));
//        textView.setTextSize(16);
//        textView.setTextColor(Color.BLACK);
//        textView.setMovementMethod(LinkMovementMethod.getInstance()); // Важно для кликов
//
//        String fullText = context.getString(com.nextgis.maplib.R.string.layer_not_editable)
//                + " " + context.getString(com.nextgis.maplib.R.string.layer_not_editable_url);
//        String clickablePart = context.getString(com.nextgis.maplib.R.string.layer_not_editable_url);
//        final String url = "https://my.nextgis.com/login?next=contactee7806f0-9685-4487-bab8-569846ee18ff";
//
//        SpannableString spannableString = new SpannableString(fullText);
//
//        int startIndex = fullText.indexOf(clickablePart);
//        int endIndex = startIndex + clickablePart.length();
//
//        ClickableSpan clickableSpan = new ClickableSpan() {
//            @Override
//            public void onClick(View widget) {
//                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
//                intent.setData(android.net.Uri.parse(url));
//                context.startActivity(intent);
//            }
//        };
//        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        textView.setText(spannableString);
//
//        new androidx.appcompat.app.AlertDialog.Builder(context)
//                .setView(textView)
//                .setPositiveButton(com.nextgis.maplibui.R.string.ok, null)
//                .create()
//                .show();
//    }

    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

}

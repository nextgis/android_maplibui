/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nextgis.maplibui.R;


public class StyledDialogFragment
        extends DialogFragment
{
    protected Integer mIconId;

    protected Integer mTitleId;
    protected Integer mMessageId;
    protected Integer mPositiveTextId;
    protected Integer mNegativeTextId;

    protected CharSequence mTitleText;
    protected CharSequence mMessageText;
    protected CharSequence mPositiveText;
    protected CharSequence mNegativeText;

    protected Integer mThemeResId;
    protected boolean mIsThemeDark = false;

    protected LinearLayout mTitleView;
    protected ImageView    mTitleIconView;
    protected TextView     mTitleTextView;
    protected View         mTitleDivider;
    protected LinearLayout mDialogLayout;
    protected TextView     mMessage;
    protected View         mView;
    protected LinearLayout mButtons;
    protected Button       mButtonPositive;
    protected Button       mButtonNegative;

    protected OnPositiveClickedListener mOnPositiveClickedListener;
    protected OnNegativeClickedListener mOnNegativeClickedListener;
    protected OnCancelListener          mOnCancelListener;
    protected OnDismissListener         mOnDismissListener;

    protected Integer mTitleDividerVisibility;

    protected boolean mKeepInstance = false;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (null == getParentFragment()) {
            setRetainInstance(mKeepInstance);
        }
    }


    @Override
    public void onDestroyView()
    {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setOnDismissListener(null);
        }

        mDialogLayout.removeAllViews();

        super.onDestroyView();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Idea from here
        // http://thanhcs.blogspot.ru/2014/10/android-custom-dialog-fragment.html

        // StyledDialogFragment themes. These are fixed. To change the colors see colors.xml
        // Or use setThemeResId()
        ContextThemeWrapper context;

        if (null != mThemeResId) {
            context = new ContextThemeWrapper(getActivity(), mThemeResId);
        } else if (mIsThemeDark) {
            context = new ContextThemeWrapper(getActivity(), R.style.SdfTheme_Dark);
        } else {
            context = new ContextThemeWrapper(getActivity(), R.style.SdfTheme_Light);
        }

        Dialog dialog = new Dialog(context);

        Window window = dialog.getWindow();
        window.requestFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        return dialog;
    }


    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.sdf_layout, null);

        mTitleView = (LinearLayout) view.findViewById(R.id.title);
        mTitleIconView = (ImageView) view.findViewById(R.id.title_icon);
        mTitleTextView = (TextView) view.findViewById(R.id.title_text);
        mTitleDivider = view.findViewById(R.id.title_divider);

        mDialogLayout = (LinearLayout) view.findViewById(R.id.dialog_body);

        mButtons = (LinearLayout) view.findViewById(R.id.buttons);
        mButtonPositive = (Button) view.findViewById(R.id.button_positive);
        mButtonNegative = (Button) view.findViewById(R.id.button_negative);


        if (null != mMessageId) {
            setMessageView();
            mMessage.setText(mMessageId);
        }
        if (null != mMessageText) {
            setMessageView();
            mMessage.setText(mMessageText);
        }

        if (null != mView) {
            mDialogLayout.setVisibility(View.VISIBLE);
            mDialogLayout.addView(mView);
        }


        if (getShowsDialog()) {
            mTitleView.setVisibility(View.VISIBLE);

            if (null != mIconId) {
                mTitleIconView.setVisibility(View.VISIBLE);
                mTitleIconView.setImageResource(mIconId);
            }

            if (null != mTitleId) {
                mTitleTextView.setText(mTitleId);
            }
            if (null != mTitleText) {
                mTitleTextView.setText(mTitleText);
            }
            if (null != mTitleDividerVisibility) {
                mTitleDivider.setVisibility(mTitleDividerVisibility);
            }


            if (null != mPositiveTextId) {
                mButtons.setVisibility(View.VISIBLE);
                mButtonPositive.setVisibility(View.VISIBLE);
                mButtonPositive.setText(mPositiveTextId);
            }
            if (null != mPositiveText) {
                mButtons.setVisibility(View.VISIBLE);
                mButtonPositive.setVisibility(View.VISIBLE);
                mButtonPositive.setText(mPositiveText);
            }

            if (null != mNegativeTextId) {
                mButtons.setVisibility(View.VISIBLE);
                mButtonNegative.setVisibility(View.VISIBLE);
                mButtonNegative.setText(mNegativeTextId);
            }
            if (null != mNegativeText) {
                mButtons.setVisibility(View.VISIBLE);
                mButtonNegative.setVisibility(View.VISIBLE);
                mButtonNegative.setText(mNegativeText);
            }


            if (null != mOnPositiveClickedListener) {
                mButtons.setVisibility(View.VISIBLE);
                mButtonPositive.setVisibility(View.VISIBLE);
                mButtonPositive.setOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                if (null != mOnPositiveClickedListener) {
                                    mOnPositiveClickedListener.onPositiveClicked();
                                }
                                dismiss();
                            }
                        });
            }

            if (null != mOnNegativeClickedListener) {
                mButtons.setVisibility(View.VISIBLE);
                mButtonNegative.setVisibility(View.VISIBLE);
                mButtonNegative.setOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                if (null != mOnNegativeClickedListener) {
                                    mOnNegativeClickedListener.onNegativeClicked();
                                }
                                dismiss();
                            }
                        });
            }
        }

        return view;
    }


    @Override
    public void onCancel(DialogInterface dialog)
    {
        if (null != mOnCancelListener) {
            mOnCancelListener.onCancel();
        }
        super.onCancel(dialog);
    }


    @Override
    public void onDismiss(DialogInterface dialog)
    {
        if (null != mOnDismissListener) {
            mOnDismissListener.onDismiss();
        }
        super.onDismiss(dialog);
    }


    public StyledDialogFragment setKeepInstance(boolean keepInstance)
    {
        mKeepInstance = keepInstance;
        return this;
    }


    public void setThemeResId(Integer themeResId)
    {
        mThemeResId = themeResId;
    }


    public void setThemeDark(boolean isDarkTheme)
    {
        mIsThemeDark = isDarkTheme;
    }


    public boolean isThemeDark()
    {
        return mIsThemeDark;
    }


    protected void setMessageView()
    {
        LinearLayout layout =
                (LinearLayout) View.inflate(getActivity(), R.layout.sdf_message, null);
        mMessage = (TextView) layout.findViewById(R.id.dialog_message);
        mDialogLayout.setVisibility(View.VISIBLE);
        mDialogLayout.addView(layout);
    }


    public void setView(View view)
    {
        mView = view;
    }


    public StyledDialogFragment setIcon(int iconId)
    {
        mIconId = iconId;
        return this;
    }


    public StyledDialogFragment setTitle(int titleId)
    {
        mTitleId = titleId;
        return this;
    }


    public StyledDialogFragment setTitle(CharSequence titleText)
    {
        mTitleText = titleText;
        return this;
    }


    public void setTitleDividerVisibility(int visibility)
    {
        mTitleDividerVisibility = visibility;
    }


    public StyledDialogFragment setMessage(int messageId)
    {
        mMessageId = messageId;
        return this;
    }


    public StyledDialogFragment setMessage(CharSequence messageText)
    {
        mMessageText = messageText;
        return this;
    }


    public StyledDialogFragment setPositiveText(int positiveTextId)
    {
        mPositiveTextId = positiveTextId;
        return this;
    }


    public StyledDialogFragment setPositiveText(CharSequence positiveText)
    {
        mPositiveText = positiveText;
        return this;
    }


    public StyledDialogFragment setNegativeText(int negativeTextId)
    {
        mNegativeTextId = negativeTextId;
        return this;
    }


    public StyledDialogFragment setNegativeText(CharSequence negativeText)
    {
        mNegativeText = negativeText;
        return this;
    }


    public StyledDialogFragment setOnPositiveClickedListener(OnPositiveClickedListener onPositiveClickedListener)
    {
        mOnPositiveClickedListener = onPositiveClickedListener;
        return this;
    }


    public StyledDialogFragment setOnNegativeClickedListener(OnNegativeClickedListener onNegativeClickedListener)
    {
        mOnNegativeClickedListener = onNegativeClickedListener;
        return this;
    }


    public StyledDialogFragment setOnCancelListener(OnCancelListener onCancelListener)
    {
        mOnCancelListener = onCancelListener;
        return this;
    }


    public StyledDialogFragment setOnDismissListener(OnDismissListener onDismissListener)
    {
        mOnDismissListener = onDismissListener;
        return this;
    }


    public interface OnPositiveClickedListener
    {
        void onPositiveClicked();
    }


    public interface OnNegativeClickedListener
    {
        void onNegativeClicked();
    }


    public interface OnCancelListener
    {
        void onCancel();
    }


    public interface OnDismissListener
    {
        void onDismiss();
    }
}

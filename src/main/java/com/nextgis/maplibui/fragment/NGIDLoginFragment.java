/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2017-2018, 2020 NextGIS, info@nextgis.com
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

import static com.nextgis.maplib.util.Constants.MESSAGE_EXTRA;
import static com.nextgis.maplib.util.Constants.MESSAGE_TITLE_EXTRA;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.text.InputType;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGIDLoginActivity;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.NGIDUtils;

public class NGIDLoginFragment extends Fragment implements View.OnClickListener {
    protected EditText mLogin, mPassword;
    protected Button mSignInButton;
    protected View progressArea;
    protected TextView mServer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null == getParentFragment())
            setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_ngid_login, container, false);
        mLogin = view.findViewById(R.id.login);
        mPassword = view.findViewById(R.id.password);
        mSignInButton = view.findViewById(R.id.signin);
        mSignInButton.setOnClickListener(this);
        progressArea =  view.findViewById(R.id.progressArea);
        progressArea.setOnClickListener(this);
        mServer = view.findViewById(R.id.server);
        setUpServerInfo();
        TextView signUp = view.findViewById(R.id.signup);
        signUp.setText(signUp.getText().toString().toUpperCase());
        signUp.setOnClickListener(this);
        //view.findViewById(R.id.onpremise).setOnClickListener(this);
        view.findViewById(R.id.onpremiseButton).setOnClickListener(this);

        return view;
    }

    private void setUpServerInfo() {
        Activity activity = getActivity();
        if (activity == null)
            return;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String url = preferences != null ? preferences.getString("ngid_url", NGIDUtils.NGID_MY) : NGIDUtils.NGID_MY;
        url = NetworkUtil.trimSlash(url);
        if (mServer != null)
            mServer.setText(getString(R.string.ngid_server, url));
    }

    @Override
    public void onClick(View v) {
        final Activity activity = getActivity();
        if (activity == null)
            return;

        if (v.getId() == R.id.progressArea)
            return;

        if (v.getId() == R.id.signin) {
            boolean loginPasswordFilled = checkEditText(mLogin) && checkEditText(mPassword);
            if (!loginPasswordFilled) {
                Toast.makeText(activity, R.string.field_not_filled, Toast.LENGTH_SHORT).show();
                return;
            }

            progressArea.setVisibility(View.VISIBLE);

            IGISApplication application = (IGISApplication) activity.getApplication();
            application.sendEvent(ConstantsUI.GA_NGID, ConstantsUI.GA_CONNECT, ConstantsUI.GA_USER);
            mSignInButton.setEnabled(false);
            String login = mLogin.getText().toString().trim();
            String password = mPassword.getText().toString();
            NGIDUtils.getToken(activity, login, password, new NGIDUtils.OnFinish() {
                @Override
                public void onFinish(HttpResponse response) {
                    mSignInButton.setEnabled(true);
                    progressArea.setVisibility(View.GONE);

                    if (response.isOk()) {
                        activity.getIntent().putExtra(NGIDLoginActivity.EXTRA_SUCCESS, true);
                        activity.finish();
                    } else {
                        Toast.makeText(
                                activity,
                                NetworkUtil.getError(activity, response.getResponseCode()),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (v.getId() == R.id.signup) {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(NGIDUtils.NGID_MY));
            startActivity(browser);
        } else if (v.getId() == R.id.onpremiseButton) {
            createDialog();
        }
    }

    private void createDialog() {
        if (getContext() == null)
            return;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String url = preferences != null ? preferences.getString("ngid_url", NGIDUtils.NGID_MY) : NGIDUtils.NGID_MY;
        //final EditText editText = new EditText(getContext());


        View view = View.inflate(getContext(), R.layout.custom_ngid_server, null);
        RadioButton defaultServerRadio = view.findViewById(R.id.defaultServerRadio);
        RadioButton customServerRadio = view.findViewById(R.id.customServerRadio);
        EditText customURLEditText = view.findViewById(R.id.customURLEditText);
        TextView helpTextView = view.findViewById(R.id.help);

        customURLEditText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        customURLEditText.setHint(NGIDUtils.NGID_MY);

        defaultServerRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                customServerRadio.setChecked(false);
                customURLEditText.setVisibility(View.GONE);
            }
        });

        customServerRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                defaultServerRadio.setChecked(false);
                customURLEditText.setVisibility(View.VISIBLE);
            }
        });

        if (url.equals(NGIDUtils.NGID_MY)) {
            defaultServerRadio.setChecked(true);
            customURLEditText.setText("");
        } else {
            customServerRadio.setChecked(true);
            customURLEditText.setText(url);
        }


//        String message = getContext().getString(R.string.help_custom_auth_server);
//        final SpannableString s = new SpannableString(message); // msg should have url to enable clicking
//        Linkify.addLinks(s, Linkify.ALL);
//        helpTextView.setText(s);
        helpTextView.setMovementMethod(LinkMovementMethod.getInstance());


        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.ngid_server_caption)
                //.setView(editText)
                .setView(view)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    if (defaultServerRadio.isChecked()){
                        PreferenceManager.getDefaultSharedPreferences(getContext())
                                .edit()
                                .putString("ngid_url", NGIDUtils.NGID_MY)
                                .apply();
                        setUpServerInfo();
                        return;
                    }
                    // else // custom field selected

                    String url1 = customURLEditText.getText().toString();
                    if (url1.isEmpty()) {
                        url1 = NGIDUtils.NGID_MY;
                    }
                    if (!NetworkUtil.isValidUri(url1)) {
                        Toast.makeText(getContext(), R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("ngid_url", url1).apply();
                    setUpServerInfo();
                })
                .setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    private boolean checkEditText(EditText edit) {
        return edit.getText().length() > 0;
    }

}

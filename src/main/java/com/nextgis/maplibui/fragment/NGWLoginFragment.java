/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

import android.accounts.Account;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGWSettingsActivity;
import com.nextgis.maplibui.service.HTTPLoader;

import java.net.URI;
import java.net.URISyntaxException;

public class NGWLoginFragment
        extends Fragment
        implements LoaderManager.LoaderCallbacks<String>, View.OnClickListener
{
    protected static final String ENDING = ".nextgis.com";
    protected static final String DEFAULT_ACCOUNT = "administrator";

    protected EditText mURL;
    protected EditText mLogin;
    protected EditText mPassword;
    protected Button   mSignInButton;
    protected TextView mLoginTitle;
    protected SwitchCompat mManual;
    protected CheckBox mGuest;

    protected String mUrlText   = "";
    protected String mLoginText = "";

    protected boolean mForNewAccount      = true;
    protected boolean mChangeAccountUrl   = mForNewAccount;
    protected boolean mChangeAccountLogin = mForNewAccount;

    protected OnAddAccountListener mOnAddAccountListener;
    protected Loader<String> mLoader;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (null == getParentFragment()) {
            setRetainInstance(true);
        }
    }


    public void setForNewAccount(boolean forNewAccount)
    {
        mForNewAccount = forNewAccount;
    }


    public void setChangeAccountUrl(boolean changeAccountUrl)
    {
        mChangeAccountUrl = changeAccountUrl;
    }


    public void setChangeAccountLogin(boolean changeAccountLogin)
    {
        mChangeAccountLogin = changeAccountLogin;
    }


    public void setUrlText(String urlText)
    {
        mUrlText = urlText;
    }


    public void setLoginText(String loginText)
    {
        mLoginText = loginText;
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable
            ViewGroup container,
            @Nullable
            Bundle savedInstanceState)
    {
        final View view = inflater.inflate(R.layout.fragment_ngw_login, container, false);
        mURL = (EditText) view.findViewById(R.id.url);
        mLogin = (EditText) view.findViewById(R.id.login);
        mPassword = (EditText) view.findViewById(R.id.password);
        mSignInButton = (Button) view.findViewById(R.id.signin);

        TextWatcher watcher = new LocalTextWatcher();
        mURL.addTextChangedListener(watcher);
        mLoginTitle = (TextView) view.findViewById(R.id.login_title);

        mGuest = (CheckBox) view.findViewById(R.id.guest);
        mGuest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int visibility = !isChecked ? View.VISIBLE : View.GONE;
                mLogin.setEnabled(!isChecked && mManual.isChecked());
                mLogin.setVisibility(visibility);
                mPassword.setVisibility(visibility);
                mPassword.setEnabled(!isChecked);
            }
        });

        mManual = (SwitchCompat) view.findViewById(R.id.manual);
        mManual.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mURL.setCompoundDrawables(null, null, null, null);
                    mURL.setHint(R.string.ngw_url);
                    mLogin.setText(null);
                    mLogin.setEnabled(!mGuest.isChecked());
                    mLoginTitle.setText(R.string.ngw_login_title);
                } else {
                    @SuppressWarnings("deprecation")
                    Drawable addition = getResources().getDrawable(R.drawable.nextgis_addition);
                    mURL.setCompoundDrawablesWithIntrinsicBounds(null, null, addition, null);
                    mURL.setHint(R.string.instance_name);
                    mLogin.setText(DEFAULT_ACCOUNT);
                    mLogin.setEnabled(false);
                    mLoginTitle.setText(R.string.ngw_from_my_nextgis);
                }
            }
        });

        mLogin.setText(DEFAULT_ACCOUNT);
        if (!mForNewAccount) {
            mManual.setEnabled(false);
            mURL.setEnabled(mChangeAccountUrl);
            view.findViewById(R.id.ll_manual).setVisibility(View.GONE);

            if (mUrlText.endsWith(ENDING)) {
                mURL.setText(mUrlText.replace(ENDING, ""));
                mLogin.setEnabled(false);
            } else {
                mManual.performClick();
                mURL.setText(mUrlText);
                mLogin.setText(mLoginText);
                mLogin.setEnabled(mChangeAccountLogin);
            }

            boolean guest = Constants.NGW_ACCOUNT_GUEST.equals(mLoginText);
            if (guest) {
                mGuest.setChecked(true);
                mLogin.setEnabled(false);
                mPassword.setEnabled(false);
            }
        }

        return view;
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mSignInButton.setOnClickListener(this);
    }


    @Override
    public void onPause()
    {
        mSignInButton.setOnClickListener(null);
        super.onPause();
    }


    @Override
    public void onClick(View v) {
        if (v == mSignInButton) {
            boolean urlFilled = checkEditText(mURL);
            boolean loginPasswordFilled = checkEditText(mLogin) && checkEditText(mPassword);
            if (!urlFilled || (!mGuest.isChecked() && !loginPasswordFilled)) {
                Toast.makeText(getActivity(), R.string.field_not_filled, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.WEB_URL.matcher(mUrlText).matches()) {
                Toast.makeText(getActivity(), R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
                return;
            }

            int id = mGuest.isChecked() ? R.id.non_auth_token_loader : R.id.auth_token_loader;
            if (null != mLoader && mLoader.isStarted()) {
                mLoader = getLoaderManager().restartLoader(id, null, this);
            } else {
                mLoader = getLoaderManager().initLoader(id, null, this);
            }

            mSignInButton.setEnabled(false);
        }
    }


    protected boolean checkEditText(EditText edit)
    {
        return edit.getText().length() > 0;
    }


    @Override
    public Loader<String> onCreateLoader(
            int id,
            Bundle args)
    {
        String login = null;
        String password = null;

        if (id == R.id.auth_token_loader) {
            login = mLogin.getText().toString();
            password = mPassword.getText().toString();
        }

        return new HTTPLoader(getActivity().getApplicationContext(), mUrlText, login, password);
    }


    @Override
    public void onLoadFinished(
            Loader<String> loader,
            String token)
    {
        mSignInButton.setEnabled(true);

        String accountName = "";
        try {
            String url = mUrlText;
            if (!url.startsWith("http")) {
                url = "http://" + url;
            }
            URI uri = new URI(url);
            if (uri.getHost() != null && uri.getHost().length() > 0) {
                accountName += uri.getHost();
            }
            if (uri.getPort() != 80 && uri.getPort() > 0) {
                accountName += ":" + uri.getPort();
            }
            if (uri.getPath() != null && uri.getPath().length() > 0) {
                accountName += uri.getPath();
            }
        } catch (URISyntaxException e) {
            accountName = mUrlText;
        }

        if (loader.getId() == R.id.auth_token_loader) {
            if (token != null && token.length() > 0)
                onTokenReceived(accountName, token);
            else
                Toast.makeText(getActivity(), R.string.error_login, Toast.LENGTH_SHORT).show();
        } else if (loader.getId() == R.id.non_auth_token_loader)
            onTokenReceived(accountName, Constants.NGW_ACCOUNT_GUEST);
    }


    public void onTokenReceived(
            String accountName,
            String token)
    {
        IGISApplication app = (IGISApplication) getActivity().getApplication();
        boolean accountAdded = false;

        if (mForNewAccount) {
            String login = mLogin.getText().toString();
            String password = mPassword.getText().toString();

            if (token.equals(Constants.NGW_ACCOUNT_GUEST)) {
                login = Constants.NGW_ACCOUNT_GUEST;
                password = Constants.NGW_ACCOUNT_GUEST;
            }

            accountAdded = app.addAccount(accountName, mUrlText, login, password, token);
        } else {
            if (mChangeAccountUrl)    // do we need this?
                app.setUserData(accountName, "url", mUrlText);

            if (!token.equals(Constants.NGW_ACCOUNT_GUEST)) {
                if (mChangeAccountLogin)
                    app.setUserData(accountName, "login", mLogin.getText().toString());

                app.setPassword(accountName, mPassword.getText().toString());
                NGWSettingsActivity.updateAccountLayersCacheData(app, app.getAccount(accountName));
            }

            getActivity().finish();
        }

        if (null != mOnAddAccountListener) {
            Account account = null;
            if (accountAdded)
                account = app.getAccount(accountName);

            mOnAddAccountListener.onAddAccount(account, token, accountAdded);
        }
    }


    @Override
    public void onLoaderReset(Loader<String> loader)
    {

    }


    public class LocalTextWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            mUrlText = mURL.getText().toString().trim();

            if (null != mManual && !mManual.isChecked())
                mUrlText += ENDING;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }
    }


    public void setOnAddAccountListener(OnAddAccountListener onAddAccountListener)
    {
        mOnAddAccountListener = onAddAccountListener;
    }


    public interface OnAddAccountListener
    {
        void onAddAccount(
                Account account,
                String token,
                boolean accountAdded);
    }
}

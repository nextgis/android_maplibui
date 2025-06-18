/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2019, 2021 NextGIS, info@nextgis.com
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

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import android.accounts.Account;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.ngw.TokenContainer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.service.HTTPLoader;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

public class NGWLoginFragment
        extends Fragment
        implements LoaderManager.LoaderCallbacks<TokenContainer>, View.OnClickListener
{
    private static final String PASSWORD_HINT = "••••••••••";
    protected static final String ENDING = ".nextgis.com";
    protected static final String DEFAULT_ACCOUNT = "administrator";

    protected EditText mURL, mLogin, mPassword;
    protected Button   mSignInButton, mGuestButton;
    protected TextView mLoginTitle, mManual, mTip;
    protected TextView ngwFromMyNextgis;

    protected View progressArea;

    protected String mUrlText   = "";
    protected String mLoginText = "";
    protected String mPasswordText = "";
    protected boolean manualNGW = false;

    protected boolean mForNewAccount      = true;
    protected boolean mChangeAccountUrl   = mForNewAccount;
    protected boolean mChangeAccountLogin = mForNewAccount;

    protected OnAddAccountListener mOnAddAccountListener;
    protected Loader<TokenContainer> mLoader;
    protected AtomicReference<String> mUrlWithProtocol = new AtomicReference<>();

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
            @NonNull LayoutInflater inflater,
            @Nullable
            ViewGroup container,
            @Nullable
            Bundle savedInstanceState)
    {
        final View view = inflater.inflate(R.layout.fragment_ngw_login, container, false);
        mURL = view.findViewById(R.id.url);
        mLogin = view.findViewById(R.id.login);
        mPassword = view.findViewById(R.id.password);
        mSignInButton = view.findViewById(R.id.signin);

        TextWatcher watcher = new LocalTextWatcher();
        mURL.addTextChangedListener(watcher);
        mLoginTitle = view.findViewById(R.id.login_title);
        progressArea = view.findViewById(R.id.progressArea);
        mTip = view.findViewById(R.id.tip);

        mLoginTitle.setMovementMethod(LinkMovementMethod.getInstance());

        ngwFromMyNextgis = view.findViewById(R.id.login_title);

        mGuestButton = view.findViewById(R.id.guest);
        mGuestButton.setOnClickListener(this);

        mManual = view.findViewById(R.id.manual);
        mManual.setOnClickListener(this);
        ControlHelper.highlightText(mManual);

        //mLogin.setText(DEFAULT_ACCOUNT);
        if (!mForNewAccount) {
            view.findViewById(R.id.ll_manual).setVisibility(View.GONE);
            mURL.setEnabled(mChangeAccountUrl);
            mLogin.setText(mLoginText);
            mLogin.setEnabled(mChangeAccountLogin);
            mLoginTitle.setVisibility(View.GONE);
            mPassword.setHint(PASSWORD_HINT);
            mPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    mPassword.setHint(hasFocus ? null : PASSWORD_HINT);
                }
            });

            if (mUrlText.endsWith(ENDING)) {
                mURL.setText(mUrlText.replace(ENDING, ""));
            } else {
                mManual.performClick();
                mURL.setText(mUrlText);
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

        if (v.getId() == R.id.progressArea)
            return;
        if (v.getId() == R.id.signin || v.getId() == R.id.guest) {
            boolean guest = v.getId() == R.id.guest;
            boolean urlFilled = checkEditText(mURL);
            boolean loginPasswordFilled = checkEditText(mLogin) && checkEditText(mPassword);
            if (!urlFilled || (!guest && !loginPasswordFilled)) {
                Toast.makeText(getActivity(), R.string.field_not_filled, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!NetworkUtil.isValidUri(mUrlText)) {
                new AlertDialog.Builder(getContext())
                        .setMessage(R.string.error_invalid_url)
                        .setPositiveButton(R.string.ok, null)
                        .create()
                        .show();
                //Toast.makeText(getActivity(), R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
                return;
            }

            int id = guest ? R.id.non_auth_token_loader : R.id.auth_token_loader;
            if (null != mLoader && mLoader.isStarted()) {
                mLoader = getLoaderManager().restartLoader(id, null, this);
            } else {
                mLoader = getLoaderManager().initLoader(id, null, this);
            }
            progressArea.setVisibility(View.VISIBLE);

            IGISApplication application = (IGISApplication) getActivity().getApplication();
            application.sendEvent(ConstantsUI.GA_NGW, ConstantsUI.GA_CONNECT, guest ? ConstantsUI.GA_GUEST : ConstantsUI.GA_USER);

            mSignInButton.setEnabled(false);
            mGuestButton.setEnabled(false);
        } else if (v.getId() == R.id.manual) {
            manualNGW = !manualNGW;

            if (manualNGW) {
                mTip.setVisibility(View.GONE);
                mManual.setText(R.string.nextgis_com);
                mURL.setCompoundDrawables(null, null, null, null);
                mURL.setHint(R.string.ngw_url);
                mUrlText = mUrlText.replace(ENDING, "");
                mLoginTitle.setVisibility(View.GONE);
            } else {
                mTip.setVisibility(View.VISIBLE);
                mManual.setText(R.string.click_here);
                mLoginTitle.setVisibility(View.VISIBLE);
                @SuppressWarnings("deprecation")
                Drawable addition = getResources().getDrawable(R.drawable.nextgis_addition);
                mURL.setCompoundDrawablesWithIntrinsicBounds(null, null, addition, null);
                mURL.setHint(R.string.instance_name);
                if (!mUrlText.contains(ENDING))
                    mUrlText += ENDING;
            }

            ControlHelper.highlightText(mManual);
        }
    }


    protected boolean checkEditText(EditText edit)
    {
        return edit.getText().length() > 0;
    }


    @NonNull
    @Override
    public Loader<TokenContainer> onCreateLoader(
            int id,
            Bundle args)
    {
        if (id == R.id.auth_token_loader) {
            mLoginText = mLogin.getText().toString().trim();
            mPasswordText = mPassword.getText().toString();
        }

        mUrlWithProtocol = new AtomicReference<>(mUrlText);
        return new HTTPLoader(getActivity().getApplicationContext(), mUrlWithProtocol, mLoginText, mPasswordText);
    }


    @Override
    public void onLoadFinished(
            @NonNull Loader<TokenContainer> loader,
            TokenContainer token)
    {
        progressArea.setVisibility(View.GONE);
        mSignInButton.setEnabled(true);
        if (null != mGuestButton) { // needed for overrides
            mGuestButton.setEnabled(true);
        }

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

        mUrlText = mUrlWithProtocol.get();
        boolean error404 = token != null && token.responseCode == HTTP_NOT_FOUND;
        if (loader.getId() == R.id.auth_token_loader) {
            if (token != null && token.token!= null && token.token.length() > 0)
                onTokenReceived(accountName, token.token);
            else
                Toast.makeText(getActivity(),
                        error404 ? R.string.error_webgis_not_found : R.string.error_login
                        , Toast.LENGTH_SHORT).show();
        } else {
            if (loader.getId() == R.id.non_auth_token_loader) {
            if (token.responseCode == HTTP_NOT_FOUND)
                Toast.makeText(getActivity(),
                        error404 ? R.string.error_webgis_not_found : R.string.error_login
                        , Toast.LENGTH_SHORT).show();
            else
                onTokenReceived(accountName, Constants.NGW_ACCOUNT_GUEST);
            }
        }
    }


    public void onTokenReceived(
            String accountName,
            String token)
    {
        IGISApplication app = (IGISApplication) getActivity().getApplication();
        String login = mLoginText;
        String password = mPasswordText;
        if (token.equals(Constants.NGW_ACCOUNT_GUEST)) {
            login = Constants.NGW_ACCOUNT_GUEST;
            password = null;
        }

        if (mForNewAccount) {
            boolean accountAdded = app.addAccount(accountName, mUrlText.toLowerCase(), login, password, token);

            if (null != mOnAddAccountListener) {
                Account account = null;
                if (accountAdded)
                    account = app.getAccount(accountName);

                mOnAddAccountListener.onAddAccount(account, token, accountAdded);
            }
        } else {
            if (mChangeAccountUrl)
                app.setUserData(accountName, "url", mUrlText.toLowerCase());

            if (mChangeAccountLogin)
                app.setUserData(accountName, "login", login);

            app.setPassword(accountName, password);
            NGWSettingsFragment.updateAccountLayersCacheData(app, app.getAccount(accountName));

            getActivity().finish();
        }
    }


    @Override
    public void onLoaderReset(@NonNull Loader<TokenContainer> loader)
    {

    }


    public class LocalTextWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            mUrlText = mURL.getText().toString().trim();

            if (!manualNGW && !mUrlText.endsWith(ENDING))
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

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplibui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.nextgis.maplibui.service.HTTPLoader;

import java.net.URI;
import java.net.URISyntaxException;

import static com.nextgis.maplib.util.Constants.*;


public class NGWLoginFragment
        extends Fragment
        implements LoaderManager.LoaderCallbacks<String>, View.OnClickListener
{
    protected EditText mURL;
    protected EditText mLogin;
    protected EditText mPassword;
    protected Button   mSignInButton;

    protected OnAddAccountListener mOnAddAccountListener;


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
        mLogin.addTextChangedListener(watcher);
        mPassword.addTextChangedListener(watcher);

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
    public void onClick(View v)
    {
        if (v == mSignInButton) {
            getLoaderManager().restartLoader(R.id.auth_token_loader, null, this);
        }
    }


    protected void updateButtonState()
    {
        if (checkEditText(mURL) && checkEditText(mLogin) && checkEditText(mPassword)) {
            mSignInButton.setEnabled(true);
        }
    }


    private boolean checkEditText(EditText edit)
    {
        return edit.getText().length() > 0;
    }


    @Override
    public Loader<String> onCreateLoader(
            int id,
            Bundle args)
    {
        if (id == R.id.auth_token_loader) {
            return new HTTPLoader(
                    getActivity().getApplicationContext(), mURL.getText().toString().trim(),
                    mLogin.getText().toString(), mPassword.getText().toString());
        }
        return null;
    }


    @Override
    public void onLoadFinished(
            Loader<String> loader,
            String token)
    {
        if (loader.getId() == R.id.auth_token_loader) {
            if (token != null && token.length() > 0) {
                String accountName = "";
                try {
                    String url = mURL.getText().toString().trim();
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
                    accountName = mURL.getText().toString();
                }

                onTokenReceived(accountName, token);
            } else {
                Toast.makeText(getActivity(), R.string.error_login, Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void onTokenReceived(
            String accountName,
            String token)
    {
        final AccountManager am = AccountManager.get(getActivity());
        final Account account = new Account(accountName, NGW_ACCOUNT_TYPE);

        Bundle userData = new Bundle();
        userData.putString("url", mURL.getText().toString().trim());
        userData.putString("login", mLogin.getText().toString());

        boolean accountAdded =
                am.addAccountExplicitly(account, mPassword.getText().toString(), userData);

        if (accountAdded) {
            am.setAuthToken(account, account.type, token);
        }

        if (null != mOnAddAccountListener) {
            mOnAddAccountListener.onAddAccount(account, token, accountAdded);
        }
    }


    @Override
    public void onLoaderReset(Loader<String> loader)
    {

    }


    protected class LocalTextWatcher
            implements TextWatcher
    {
        public void afterTextChanged(Editable s)
        {
            updateButtonState();
        }


        public void beforeTextChanged(
                CharSequence s,
                int start,
                int count,
                int after)
        {
        }


        public void onTextChanged(
                CharSequence s,
                int start,
                int before,
                int count)
        {
        }
    }


    public void setOnAddAccountListener(OnAddAccountListener onAddAccountListener)
    {
        mOnAddAccountListener = onAddAccountListener;
    }


    public interface OnAddAccountListener
    {
        public void onAddAccount(
                Account account,
                String token,
                boolean accountAdded);
    }
}

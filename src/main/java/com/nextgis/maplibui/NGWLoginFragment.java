/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplibui;

import android.accounts.Account;
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
import com.nextgis.maplibui.account.NGWAccountAuthenticator;
import com.nextgis.maplibui.services.HTTPLoader;

import java.net.URI;
import java.net.URISyntaxException;


public class NGWLoginFragment extends Fragment implements LoaderManager.LoaderCallbacks<String>, View.OnClickListener
{
    protected EditText mURL;
    protected EditText mLogin;
    protected EditText mPassword;
    protected Button   mSignInButton;


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable
            ViewGroup container,
            @Nullable
            Bundle savedInstanceState)
    {
        final View view = inflater.inflate(R.layout.ngw_login_fragment, container, false);
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
    public void onResume() {
        super.onResume();
        mSignInButton.setOnClickListener(this);
    }
    @Override
    public void onPause() {
        mSignInButton.setOnClickListener(null);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        if (v == mSignInButton) {
            getLoaderManager().restartLoader(R.id.auth_token_loader, null, this);
        }
    }

    protected void updateButtonState() {
        if(checkEditText(mURL) && checkEditText(mLogin) && checkEditText(mPassword)){
            mSignInButton.setEnabled(true);
        }
    }

    private boolean checkEditText(EditText edit) {
        return edit.getText().length() > 0;
    }


    @Override
    public Loader<String> onCreateLoader(
            int id,
            Bundle args)
    {
        if (id == R.id.auth_token_loader) {
            return new HTTPLoader(
                    getActivity().getApplicationContext(),
                    mURL.getText().toString(),
                    mLogin.getText().toString(),
                    mPassword.getText().toString()
            );
        }
        return null;
    }


    @Override
    public void onLoadFinished(
            Loader<String> loader,
            String token)
    {
        if (loader.getId() == R.id.auth_token_loader) {
            if(token != null && token.length() > 0) {
                String name = "";
                try {
                    String url = mURL.getText().toString();
                    if(!url.startsWith("http"))
                        url = "http://" + url;
                    URI uri = new URI(url);
                    if (uri.getHost() != null && uri.getHost().length() > 0)
                        name += uri.getHost();
                    if (uri.getPort() != 80 && uri.getPort() > 0)
                        name += ":" + uri.getPort();
                    if (uri.getPath() != null && uri.getPath().length() > 0)
                        name += uri.getPath();
                } catch (URISyntaxException e) {
                    name = mURL.getText().toString();
                }

                ((NGWLoginActivity) getActivity()).onTokenReceived(new Account(name, NGWAccountAuthenticator.EXTRA_TOKEN_TYPE),
                                                                   mLogin.getText().toString(),
                                                                   mPassword.getText().toString(),
                                                                   mURL.getText().toString(), token);
            }
            else{
                Toast.makeText(getActivity(), R.string.error_login, Toast.LENGTH_SHORT)
                     .show();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader)
    {

    }


    protected class LocalTextWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            updateButtonState();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}

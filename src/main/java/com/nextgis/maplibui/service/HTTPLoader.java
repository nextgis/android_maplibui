/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015, 2017 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.service;

import android.content.Context;

import android.util.Log;

import com.hypertrack.hyperlog.HyperLog;
import com.nextgis.maplib.datasource.ngw.TokenContainer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.NGWUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static com.nextgis.maplib.util.Constants.TAG;

import androidx.loader.content.AsyncTaskLoader;


public class HTTPLoader
        extends AsyncTaskLoader<TokenContainer>
{
    protected final String mUrl;
    protected final String mLogin;
    protected final String mPassword;
    protected       TokenContainer mAuthToken;
    protected AtomicReference<String> mReference;


    public HTTPLoader(
            Context context,
            String url,
            String login,
            String password)
    {
        super(context);
        mUrl = url;
        mLogin = login;
        mPassword = password;
    }

    public HTTPLoader(
            Context context,
            AtomicReference<String> url,
            String login,
            String password)
    {
        super(context);
        mUrl = url.get();
        mReference = url;
        mLogin = login;
        mPassword = password;
    }


    public static TokenContainer signIn(
            Context context,
            String url,
            String login,
            String password)
    {
        try {
            return new HTTPLoader(context, url, login, password).signIn();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }


    @Override
    protected void onStartLoading()
    {
        if (mAuthToken == null || mAuthToken.token == null || mAuthToken.token.length() == 0) {
            forceLoad();
        } else {
            deliverResult(mAuthToken);
        }
    }


    @Override
    public void deliverResult(TokenContainer data)
    {
        mAuthToken = data;
        super.deliverResult(data);
    }


    @Override
    public TokenContainer loadInBackground()
    {
        try {
            return signIn();
        } catch (IOException e) {
            HyperLog.v(Constants.TAG, "HTTPLoader: loadInBackground exception " + e.getMessage());
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }


    protected TokenContainer signIn()
            throws IOException
    {
        String url = mUrl.trim();
        if (mReference == null)
            mReference = new AtomicReference<>(url);
        try {
            return NGWUtil.getConnectionCookie(mReference, mLogin, mPassword, true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            e.printStackTrace();
            return null;
        }
    }
}

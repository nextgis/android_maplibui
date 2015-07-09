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

package com.nextgis.maplibui.service;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;


public class HTTPLoader
        extends AsyncTaskLoader<String>
{
    protected final String mUrl;
    protected final String mLogin;
    protected final String mPassword;
    protected       String mAuthToken;


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


    public static String signIn(
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
        if (mAuthToken == null || mAuthToken.length() == 0) {
            forceLoad();
        } else {
            deliverResult(mAuthToken);
        }
    }


    @Override
    public void deliverResult(String data)
    {
        mAuthToken = data;
        super.deliverResult(data);
    }


    @Override
    public String loadInBackground()
    {
        try {
            return signIn();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }


    protected String signIn()
            throws IOException
    {
        //1. fix url
        String url = mUrl.trim();
        if (url.startsWith("http")) {
            url = url + "/login";
        } else {
            url = "http://" + url + "/login";
        }

        try {
            HttpPost httppost = new HttpPost(url);
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("login", mLogin));
            nameValuePairs.add(new BasicNameValuePair("password", mPassword));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, APP_USER_AGENT);
            httpclient.getParams()
                    .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, TIMEOUT_CONNECTION);
            httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, TIMEOUT_SOKET);
            httpclient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);

            HttpResponse response = httpclient.execute(httppost);
            //2 get cookie
            Header head = response.getFirstHeader("Set-Cookie");
            if (head == null) {
                return null;
            }
            return head.getValue();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }
}

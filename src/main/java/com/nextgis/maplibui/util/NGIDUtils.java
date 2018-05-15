/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2017-2018 NextGIS, info@nextgis.com
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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import static com.nextgis.maplib.util.Constants.SUPPORT;

public final class NGIDUtils {
    public static final String NGID_MY = "https://my.nextgis.com/";
    private static final String NGID_API = "https://my.nextgis.com/api/v1/";
    private static final String OAUTH_NEW = NGID_MY + "oauth2/token/?grant_type=password&username=%s&password=%s&client_id=%s";
    private static final String OAUTH_REFRESH = NGID_MY + "oauth2/token/?grant_type=refresh_token&client_id=%s&refresh_token=%s";
    public static final String USER_INFO = NGID_API + "user_info/";
    public static final String USER_SUPPORT = NGID_API + "support_info/";
    private static final String GET = "GET";
    private static final String POST = "POST";

    public static final String PREF_ACCESS_TOKEN = "access_token";
    public static final String PREF_REFRESH_TOKEN = "refresh_token";
    public static final String PREF_USERNAME = "username";
    public static final String PREF_EMAIL = "email";
    public static final String PREF_FIRST_NAME = "first_name";
    public static final String PREF_LAST_NAME = "last_name";

    private static SharedPreferences mPreferences;

    public interface OnFinish {
        void onFinish(HttpResponse response);
    }

    public static void get(Context context, String url, OnFinish callback) {
        if (mPreferences == null)
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String token = mPreferences.getString(PREF_ACCESS_TOKEN, "");
        new Load(callback).execute(url, "GET", token);
    }

    public static void getToken(Context context, String login, String password, OnFinish callback) {
        if (mPreferences == null)
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        new Load(callback).execute(USER_INFO, "GET", null, login, password);
    }

    private static class Load extends AsyncTask<String, Void, HttpResponse> {
        private OnFinish mCallback;

        Load(OnFinish callback) {
            mCallback = callback;
        }

        @Override
        protected HttpResponse doInBackground(String... args) {
            String url = args[0];
            String method = args[1];
            String token = args[2];

            if (TextUtils.isEmpty(token)) {
                try {
                    token = getToken(args);
                    userCheck(token);
                } catch (IOError e) {
                    return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);
                }
            }

            HttpResponse response = getResponse(url, method, token);
            if (!response.isOk()) {
                response = userCheck(token);
                if (!response.isOk())
                    return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);
                else
                    response = getResponse(url, method, token);
            }

            return response;
        }

        @Override
        protected void onPostExecute(HttpResponse result) {
            super.onPostExecute(result);

            if (mCallback != null)
                mCallback.onFinish(result);
        }
    }

    private static HttpResponse userCheck(String token) {
        HttpResponse response = getResponse(USER_INFO, "GET", token);
        String userCheck = response.getResponseBody();
        if (userCheck == null) {
            String refreshToken = mPreferences.getString(PREF_REFRESH_TOKEN, "");
            String accessRefresh = String.format(OAUTH_REFRESH, BuildConfig.CLIENT_ID, refreshToken);

            response = getResponse(accessRefresh, POST, null);
            if (!response.isOk())
                return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);

            token = parseResponseBody(response.getResponseBody());
            if (token == null)
                return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);

            response = getResponse(USER_INFO, GET, token);
            if (!response.isOk())
                return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);

            userCheck = response.getResponseBody();
        }

        saveUserInfo(userCheck);
        return response;
    }

    private static String getToken(String... args) throws IOError {
        String login = args.length > 3 ? args[3] : null;
        String password = args.length > 4 ? args[4] : null;

        if (login == null || password == null)
            throw new IOError(new Throwable("Login or Password is NULL"));

        try {
            login = URLEncoder.encode(login, "UTF-8").replaceAll("\\+", "%20");
            password = URLEncoder.encode(password, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException | NullPointerException ignored) {}

        String accessNew = String.format(OAUTH_NEW, login, password, BuildConfig.CLIENT_ID);

        HttpResponse response = getResponse(accessNew, POST, null);
        if (!response.isOk())
            throw new IOError(new Throwable("Response is not OK"));

        String token = parseResponseBody(response.getResponseBody());
        if (token == null)
            throw new IOError(new Throwable("Token is NULL"));

        return token;
    }

    private static void saveUserInfo(String userInfo) {
        try {
            JSONObject json = new JSONObject(userInfo);
            mPreferences.edit().putString(PREF_USERNAME, json.getString(PREF_USERNAME))
                        .putString(PREF_EMAIL, json.getString(PREF_EMAIL))
                        .putString(PREF_FIRST_NAME, json.getString(PREF_FIRST_NAME))
                        .putString(PREF_LAST_NAME, json.getString(PREF_LAST_NAME)).apply();
        } catch (JSONException | NullPointerException ignored) {}
    }

    private static String parseResponseBody(String responseBody) {
        String token = null, refreshToken;
        try {
            JSONObject json = new JSONObject(responseBody);
            token = json.getString("token_type") + " " + json.getString(PREF_ACCESS_TOKEN);
            refreshToken = json.getString(PREF_REFRESH_TOKEN);
            mPreferences.edit().putString(PREF_ACCESS_TOKEN, token).putString(PREF_REFRESH_TOKEN, refreshToken).apply();
        } catch (JSONException | NullPointerException ignored) {}

        return token;
    }

    private static HttpResponse getResponse(String target, String method, String token) {
        try {
            URL url = new URL(target);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            if (!TextUtils.isEmpty(token))
                conn.setRequestProperty("Authorization", token);

            conn.setRequestMethod(method);
            conn.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line, data = "";
            while ((line = in.readLine()) != null)
                data += line;
            in.close();

            HttpResponse response = new HttpResponse(200);
            response.setResponseBody(data);
            response.setOk(true);
            return response;
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }

        return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);
    }

    public static boolean isLoggedIn(SharedPreferences preferences) {
        return !TextUtils.isEmpty(preferences.getString(NGIDUtils.PREF_ACCESS_TOKEN, ""));
    }

    public static void signOut(SharedPreferences preferences, Context context) {
        preferences.edit()
                   .remove(NGIDUtils.PREF_USERNAME)
                   .remove(NGIDUtils.PREF_EMAIL)
                   .remove(NGIDUtils.PREF_FIRST_NAME)
                   .remove(NGIDUtils.PREF_LAST_NAME)
                   .remove(NGIDUtils.PREF_ACCESS_TOKEN)
                   .remove(NGIDUtils.PREF_REFRESH_TOKEN)
                   .apply();

        File support = context.getExternalFilesDir(null);
        if (support == null)
            support = new File(context.getFilesDir(), SUPPORT);
        else
            support = new File(support, SUPPORT);
        FileUtil.deleteRecursive(support);
    }
}

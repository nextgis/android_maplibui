/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2017-2018, 2020-2021 NextGIS, info@nextgis.com
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

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.ngw.TokenContainer;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.BuildConfig;
import com.nextgis.maplibui.service.TrackerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import static com.nextgis.maplib.util.Constants.SUPPORT;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.NetworkUtil.configureSSLdefault;
import static com.nextgis.maplib.util.NetworkUtil.getUserAgent;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;

public final class NGIDUtils {
    public static final String NGID_MY = "https://my.nextgis.com";
    private static final String OAUTH_URL = "/oauth2/token/";
    private static final String OAUTH_NEW = "grant_type=password&username=%s&password=%s&client_id=%s";
    private static final String OAUTH_REFRESH = "grant_type=refresh_token&client_id=%s&refresh_token=%s";
    private static final String USER_INFO = "/api/v1/user_info/";
    private static final String USER_SUPPORT = "/api/v1/support_info/";
    private static final String HUB_INFO_URL = "/api/v1/settings/";
    private static final String GET = "GET";
    private static final String POST = "POST";

    public static final String PREF_ACCESS_TOKEN = "access_token";
    public static final String PREF_REFRESH_TOKEN = "refresh_token";
    public static final String PREF_USERNAME = "username";
    public static final String PREF_EMAIL = "email";
    public static final String PREF_FIRST_NAME = "first_name";
    public static final String PREF_LAST_NAME = "last_name";
    public static final String PREF_NGW_ACCOUNT = "ngwaccount";

    //    public static final String COLLECTOR_HUB_URL = "http://dev.nextgis.com/collector_hub/api/collector/projects"
    public static final String COLLECTOR_HUB_URL = "https://collector-hub.nextgis.com";
    public static final String COLLECTOR_PROJECTS_URL = "/api/collector/projects";

    private static SharedPreferences mPreferences;

    public interface OnFinish {
        void onFinish(HttpResponse response);
    }

    public static void get(Context context, OnFinish callback) {
        if (mPreferences == null)
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String base = mPreferences.getString("ngid_url", NGIDUtils.NGID_MY);
        String token = mPreferences.getString(PREF_ACCESS_TOKEN, "");
        base = NetworkUtil.trimSlash(base);
        new Load(context, callback, base).execute(USER_SUPPORT, "GET", token);
    }

    public static void getToken(Context context, String login, String password, OnFinish callback) {
        if (mPreferences == null)
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String base = mPreferences.getString("ngid_url", NGIDUtils.NGID_MY);
        base = NetworkUtil.trimSlash(base);
        new Load(context, callback, base).execute(USER_INFO, "GET", null, login, password);
    }

    private static class Load extends AsyncTask<String, Void, HttpResponse> {
        private OnFinish mCallback;
        private String mBaseUrl;
        private WeakReference<Context> contextRef;


        Load(Context context, OnFinish callback, String base) {
            mCallback = callback;
            mBaseUrl = base;
            this.contextRef = new WeakReference<>(context.getApplicationContext()); // или activity, если надо
        }

        @Override
        protected HttpResponse doInBackground(String... args) {
            String url = mBaseUrl + args[0];
            String method = args[1];
            String token = args[2];

            if (TextUtils.isEmpty(token)) {
                try {
                    String login = args.length > 3 ? args[3] : null;
                    String password = args.length > 4 ? args[4] : null;

                    if (login == null || password == null)
                        return new HttpResponse(NetworkUtil.ERROR_AUTH);

                    final String originalLogin = login;
                    final String originalPass = password;
                    try {
                        login = URLEncoder.encode(login, "UTF-8").replaceAll("\\+", "%20");
                        password = URLEncoder.encode(password, "UTF-8").replaceAll("\\+", "%20");
                    } catch (UnsupportedEncodingException | NullPointerException ignored) {
                        return new HttpResponse(NetworkUtil.ERROR_AUTH);
                    }

                    String newUrl = mBaseUrl + OAUTH_URL;
                    String body = String.format(OAUTH_NEW, login, password, BuildConfig.CLIENT_ID);

                    HttpResponse response = getResponse(newUrl, POST, null, body);
                    if (!response.isOk())
                        return new HttpResponse(response.getResponseCode());

                    token = parseResponseBody(response.getResponseBody());
                    if (token == null)
                        return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);

                    userCheck(mBaseUrl, token);

                    try {
                        if (contextRef.get() != null)
                            createNGWAccount(contextRef.get(), originalLogin, originalPass);
                    } catch (Exception ex) {
                        Log.e(TAG, ex.getMessage());
                    }

                    getHubUrls(mBaseUrl, token);
                } catch (IOError e) {
                    return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);
                }
            }

            HttpResponse response = getResponse(url, method, token, null);
            if (!response.isOk()) {
                response = userCheck(mBaseUrl, token);
                if (!response.isOk())
                    return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);
                else
                    response = getResponse(url, method, token, null);
            }

            return response;
        }

        @Override
        protected void onPostExecute(HttpResponse result) {
            super.onPostExecute(result);

            NGWUtil.NGID = getUserInfo(PREF_USERNAME);
            if (mCallback != null)
                mCallback.onFinish(result);
        }
    }

    private static void getHubUrls(String base, String token) {
        String infoUrl = base + HUB_INFO_URL;
        HttpResponse response = getResponse(infoUrl, "GET", token, null);
        if (mPreferences != null) {
            try {
                String body = response.getResponseBody();
                JSONObject json = new JSONObject(body);
                String tracker = json.optString("tracker_hub", TrackerService.HOST);
                tracker = NetworkUtil.trimSlash(tracker);
                String collector = json.optString("collector_hub", COLLECTOR_HUB_URL);
                collector = NetworkUtil.trimSlash(collector);
                mPreferences.edit().putString("tracker_hub_url", tracker).putString("collector_hub_url", collector).apply();
            } catch (JSONException | NullPointerException ignored) {}
        }
    }

    private static HttpResponse userCheck(String base, String token) {
        String infoUrl = base + USER_INFO;
        HttpResponse response = getResponse(infoUrl, "GET", token, null);
        String userCheck = response.getResponseBody();
        if (userCheck == null) {
            String refreshUrl = base + OAUTH_URL;
            String refreshToken = mPreferences.getString(PREF_REFRESH_TOKEN, "");
            String body = String.format(OAUTH_REFRESH, BuildConfig.CLIENT_ID, refreshToken);

            response = getResponse(refreshUrl, POST, null, body);
            if (!response.isOk())
                return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);

            token = parseResponseBody(response.getResponseBody());
            if (token == null)
                return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);

            response = getResponse(infoUrl, GET, token, null);
            if (!response.isOk())
                return new HttpResponse(NetworkUtil.ERROR_CONNECT_FAILED);

            userCheck = response.getResponseBody();
        }

        saveUserInfo(userCheck);
        return response;
    }

    private static void createNGWAccount(Context context, String login,  String password){

        Set<String> webGis = mPreferences.getStringSet(PREF_NGW_ACCOUNT, new HashSet<>());
        if (webGis.size() > 0){
            AtomicReference<String> mReference = new AtomicReference<>(webGis.iterator().next());
            String ngwurl = mReference.get();
            Pattern pattern = Pattern.compile("^[a-z]+://([^./]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(ngwurl);

            if (matcher.find())
                login = matcher.group(1);

            String host = "";
            int start = ngwurl.indexOf("://");
            if (start != -1) {
                start += 3; // пропустить "://"
                int end = ngwurl.indexOf('/', start);
                host = (end != -1) ? ngwurl.substring(start, end) : ngwurl.substring(start);
            }

            try {
                TokenContainer tokenContainer = NGWUtil.getConnectionCookie(mReference, login, password, true);
                if (tokenContainer.responseCode == HTTP_OK || tokenContainer.responseCode == HTTP_ACCEPTED){

                    IGISApplication app = (IGISApplication) context.getApplicationContext();
                    boolean accountAdded = app.addAccount(host, ngwurl.toLowerCase(), login, password, tokenContainer.token);

//                    if (null != mOnAddAccountListener) {
//                        Account account = null;
//                        if (accountAdded)
//                            account = app.getAccount(accountName);
//
//                        //mOnAddAccountListener.onAddAccount(account, token, accountAdded);
//                    }
                }
            } catch (Exception ex){
            }
        }
    }

    private static void saveUserInfo(String userInfo) {
        try {
            JSONObject json = new JSONObject(userInfo);


            Set<String> webGiss =   new HashSet<>();
            if (json.has("available_webgises"))
                webGiss.addAll(jsonArrayToSet(json.getJSONArray("available_webgises")));

            mPreferences.edit().putString(PREF_USERNAME, json.getString(PREF_USERNAME))
                    .putString(PREF_EMAIL, json.getString(PREF_EMAIL))
                    .putString(PREF_FIRST_NAME, json.getString(PREF_FIRST_NAME))
                    .putString(PREF_LAST_NAME, json.getString(PREF_LAST_NAME))
                    .putStringSet(PREF_NGW_ACCOUNT, webGiss)
                    .apply();
        } catch (JSONException | NullPointerException ignored) {}
    }

    public static Set<String> jsonArrayToSet(JSONArray jsonArray) {
        Set<String> resultSet = new HashSet<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                resultSet.add(jsonArray.getString(i));

            } catch (Exception ex){

            }
        }
        return resultSet;
    }

    public static String getUserInfo(String key) {
        try {
            return mPreferences.getString(key, null);
        } catch (Exception ignored) {
            return null;
        }
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

    private static HttpResponse getResponse(String target, String method, String token, String payload) {
        try {
            URL url = new URL(target);
            HttpURLConnection conn;
            if (target.startsWith("https")) {
                configureSSLdefault();
                conn = (HttpsURLConnection) url.openConnection();
            }
            else
                conn = (HttpURLConnection) url.openConnection();

            conn.setRequestProperty("User-Agent", getUserAgent(Constants.MAPLIB_USER_AGENT_PART));
            conn.setRequestProperty("connection", "keep-alive");

            if (!TextUtils.isEmpty(token))
                conn.setRequestProperty("Authorization", token);

            conn.setRequestMethod(method);

            if (method.equals("POST")) {
                // Allow Outputs
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(payload);

                writer.flush();
                writer.close();
                os.close();
            }

            conn.connect();

            if (conn.getResponseCode() != 200) {
                int code = NetworkUtil.ERROR_CONNECT_FAILED;
                String msg = NetworkUtil.responseToString(conn.getErrorStream());
                if (msg == null)
                    code = NetworkUtil.ERROR_CONNECT_FAILED;
                else if (msg.contains("Invalid credentials given"))
                    code = NetworkUtil.ERROR_AUTH;
                return new HttpResponse(code);
            }

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
        NetworkUtil.setUserNGUID(null);
        NetworkUtil.setIsPro(false);

    }
}

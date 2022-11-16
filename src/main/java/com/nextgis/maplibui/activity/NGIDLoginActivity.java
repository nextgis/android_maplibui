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

package com.nextgis.maplibui.activity;

import android.content.Intent;
import android.os.Bundle;

import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.NGIDUtils;

import java.io.File;
import java.io.IOException;

import static com.nextgis.maplib.util.Constants.JSON_END_DATE_KEY;
import static com.nextgis.maplib.util.Constants.JSON_SIGNATURE_KEY;
import static com.nextgis.maplib.util.Constants.JSON_START_DATE_KEY;
import static com.nextgis.maplib.util.Constants.JSON_SUPPORTED_KEY;
import static com.nextgis.maplib.util.Constants.JSON_USER_ID_KEY;
import static com.nextgis.maplib.util.Constants.SUPPORT;

import org.json.JSONObject;

public class NGIDLoginActivity extends NGActivity {
    public static final String EXTRA_NEXT = "start_next";
    public static final String EXTRA_SUCCESS = "success";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngid_login);
        setToolbar(R.id.main_toolbar);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (getIntent().getBooleanExtra(EXTRA_SUCCESS, false)) {
            try {
                NGIDUtils.get(this, new NGIDUtils.OnFinish() {
                    @Override
                    public void onFinish(HttpResponse response) {
                        if (response.isOk()) {
                            File support = getExternalFilesDir(null);
                            if (support == null)
                                support = new File(getFilesDir(), SUPPORT);
                            else
                                support = new File(support, SUPPORT);

                            try {
                                String jsonString = FileUtil.readFromFile(support);
                                JSONObject json = new JSONObject(jsonString);
                                if (json.optBoolean(JSON_SUPPORTED_KEY)) {
                                    final String id = json.getString(JSON_USER_ID_KEY);
                                    NetworkUtil.setUserNGUID(id);
                                }
                            }catch (Exception exception){

                            }

                            try {
                                FileUtil.writeToFile(support, response.getResponseBody());
                            } catch (IOException ignored) {}

                            NetworkUtil.setIsPro(AccountUtil.isProUser(getBaseContext()));

                        }
                    }
                });
                Intent intent = new Intent(this, (Class<?>) getIntent().getSerializableExtra(EXTRA_NEXT));
                startActivity(intent);
            } catch (Exception ignored) {}
        }
    }
}

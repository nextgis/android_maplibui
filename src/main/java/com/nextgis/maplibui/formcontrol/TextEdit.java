/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2021 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.formcontrol;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.SyncStateContract;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.activity.ModifyAttributesActivity;
import com.nextgis.maplibui.api.IFormControl;
import com.nextgis.maplibui.util.ControlHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ATTRIBUTES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_FIELD_NAME_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_MAX_STRING_COUNT_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_ONLY_FIGURES_KEY;
import static com.nextgis.maplibui.util.ConstantsUI.JSON_TEXT_KEY;
import static com.nextgis.maplibui.util.NGIDUtils.PREF_FIRST_NAME;
import static com.nextgis.maplibui.util.NGIDUtils.PREF_LAST_NAME;
import static com.nextgis.maplibui.util.NGIDUtils.PREF_USERNAME;

@SuppressLint("ViewConstructor")
public class TextEdit extends AppCompatEditText
        implements IFormControl
{
    private static final String USE_LOGIN = "ngw_login";
    private static final String NGID_LOGIN = "ngid_login";

    protected boolean mIsShowLast;
    protected String mFieldName;

    public TextEdit(Context context) {
        super(context);
    }

    public TextEdit(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextEdit(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static String formUserName(String firstName, String lastName, String userName) {
        String name = firstName + " " + lastName;
        return TextUtils.isEmpty(name.trim()) ? userName : name;
    }

    @Override
    public void init(JSONObject element,
                     List<Field> fields,
                     Bundle savedState,
                     Cursor featureCursor,
                     SharedPreferences preferences,
                     Map<String, Map<String, String>> translations,
                     final ModifyAttributesActivity modifyAttributesActivity) throws JSONException{
        ControlHelper.setClearAction(this);

        JSONObject attributes = element.getJSONObject(JSON_ATTRIBUTES_KEY);
        mFieldName = ControlHelper.getFieldName(attributes.getString(JSON_FIELD_NAME_KEY));
        mIsShowLast = ControlHelper.isSaveLastValue(attributes);
        boolean enabled = ControlHelper.isEnabled(fields, mFieldName);

        int column = -1;
        String value = null;
        if (ControlHelper.hasKey(savedState, mFieldName))
            value = savedState.getString(ControlHelper.getSavedStateKey(mFieldName));
        else if (null != featureCursor) { // feature exists
            column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0)
                value = featureCursor.getString(column);
        } else {    // new feature
            if (!attributes.isNull(JSON_TEXT_KEY))
                value = attributes.getString(JSON_TEXT_KEY);

            if (mIsShowLast)
                value = preferences.getString(mFieldName, value);
        }

        boolean useLogin = attributes.optBoolean(USE_LOGIN);
        String accountName = element.optString(SyncStateContract.Columns.ACCOUNT_NAME);
        if (useLogin && !TextUtils.isEmpty(accountName)) {
            enabled = false;
            Activity activity = ControlHelper.getActivity(getContext());
            if (activity != null) {
                GISApplication app = (GISApplication) activity.getApplication();
                Account account = app.getAccount(accountName);
                value = app.getAccountLogin(account);
            }
        }

        boolean ngidLogin = attributes.optBoolean(NGID_LOGIN);
        if (ngidLogin) {
            enabled = false;
            if (column == -1) {
                String fName = element.optString(PREF_FIRST_NAME);
                String lName = element.optString(PREF_LAST_NAME);
                String uName = element.optString(PREF_USERNAME);
                value = formUserName(fName, lName, uName);
            }
        }

        setEnabled(enabled);
        setText(value);

        //let's create control
        int maxLines = attributes.getInt(JSON_MAX_STRING_COUNT_KEY);
        if (maxLines < 2) {
            setSingleLine(true);
        } else {
            setMaxLines(maxLines);
        }

        int fieldType = NOT_FOUND;
        for (Field field : fields) {
            if (field.getName().equals(mFieldName)) {
                fieldType = field.getType();
                break;
            }
        }

        boolean onlyFigures = attributes.getBoolean(JSON_ONLY_FIGURES_KEY);
        if (onlyFigures) {
            //check field type
            switch (fieldType) {
                default:
                case GeoConstants.FTInteger:
                    setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);
                    break;

                case GeoConstants.FTLong:
                    setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);
                    break;

                case GeoConstants.FTReal:
                    setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    break;
            }
        }
    }

    @Override
    public void saveLastValue(SharedPreferences preferences) {
        preferences.edit().putString(mFieldName, getText().toString()).apply();
    }

    @Override
    public boolean isShowLast() {
        return mIsShowLast;
    }

    @Override
    public String getFieldName() {
        return mFieldName;
    }

    @Override
    public void addToLayout(ViewGroup layout) {
        layout.addView(this);
    }

    @Override
    public Object getValue() {
        return getText().toString();
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putString(ControlHelper.getSavedStateKey(mFieldName), getText().toString());
    }

}

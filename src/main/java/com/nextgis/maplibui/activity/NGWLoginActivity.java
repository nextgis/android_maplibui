/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.fragment.NGWLoginFragment;

import java.util.ArrayList;
import java.util.List;


public class NGWLoginActivity
        extends NGActivity
        implements NGWLoginFragment.OnAddAccountListener
{
    public static final String FOR_NEW_ACCOUNT      = "for_new_account";
    public static final String ACCOUNT_URL_TEXT     = "account_url_text";
    public static final String ACCOUNT_LOGIN_TEXT   = "account_login_text";
    public static final String CHANGE_ACCOUNT_URL   = "change_account_url";
    public static final String CHANGE_ACCOUNT_LOGIN = "change_account_login";

    protected final static int PERMISSIONS_REQUEST_ACCOUNT = 2;

    protected boolean mForNewAccount      = true;
    protected boolean mChangeAccountUrl   = mForNewAccount;
    protected boolean mChangeAccountLogin = mForNewAccount;

    protected String mUrlText   = "";
    protected String mLoginText = "";

    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle                       mResultBundle                 = null;


    @Override
    protected void onCreate(Bundle icicle)
    {
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(FOR_NEW_ACCOUNT)) {
                mForNewAccount = extras.getBoolean(FOR_NEW_ACCOUNT);
            }

            if (!mForNewAccount) {
                if (extras.containsKey(ACCOUNT_URL_TEXT)) {
                    mUrlText = extras.getString(ACCOUNT_URL_TEXT);
                }
                if (extras.containsKey(ACCOUNT_LOGIN_TEXT)) {
                    mLoginText = extras.getString(ACCOUNT_LOGIN_TEXT);
                }
                if (extras.containsKey(CHANGE_ACCOUNT_URL)) {
                    mChangeAccountUrl = extras.getBoolean(CHANGE_ACCOUNT_URL);
                }
                if (extras.containsKey(CHANGE_ACCOUNT_LOGIN)) {
                    mChangeAccountLogin = extras.getBoolean(CHANGE_ACCOUNT_LOGIN);
                }
                setTitle(R.string.action_edit);
            }
        }

        super.onCreate(icicle);

        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        createView();
    }


    protected void createView() {
        setContentView(R.layout.activity_ngw_login);
        setToolbar(R.id.main_toolbar);

        FragmentManager fm = getSupportFragmentManager();
        NGWLoginFragment ngwLoginFragment = (NGWLoginFragment) fm.findFragmentByTag("NGWLogin");

        if (ngwLoginFragment == null) {
            ngwLoginFragment = getNewLoginFragment();
            ngwLoginFragment.setForNewAccount(mForNewAccount);
            ngwLoginFragment.setUrlText(mUrlText);
            ngwLoginFragment.setLoginText(mLoginText);
            ngwLoginFragment.setChangeAccountUrl(mChangeAccountUrl);
            ngwLoginFragment.setChangeAccountLogin(mChangeAccountLogin);


            ngwLoginFragment.setOnAddAccountListener(this);

            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.login_frame, ngwLoginFragment, "NGWLogin");
            ft.commit();
        }


        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED){
            List<String> permslist = new ArrayList<>();
            permslist.add(Manifest.permission.GET_ACCOUNTS);
            requestPermissions(this, R.string.permissions, R.string.account_permissions, PERMISSIONS_REQUEST_ACCOUNT,
                    permslist.toArray(new String[permslist.size()])); // list.toArray(new Foo[list.size()])
        }
    }

    public void requestPermissions(final Activity activity1, int title, int message, final int requestCode,
                                   final String... permissions) {
        final Activity activity = activity1;
        if (true) {
            androidx.appcompat.app.AlertDialog builder = new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(com.nextgis.maplibui.R.string.allow, (dialog, which) -> {
                        ActivityCompat.requestPermissions(activity, permissions, requestCode);})
                    .setNegativeButton(com.nextgis.maplibui.R.string.deny, (dialog, which) -> {
                        Toast.makeText(activity1, getString(R.string.no_contancts_access), Toast.LENGTH_LONG).show();
                    })
                    .create();
            builder.setCanceledOnTouchOutside(false);
            builder.show();
        }
    }


    protected NGWLoginFragment getNewLoginFragment()
    {
        return new NGWLoginFragment();
    }


    public void finish()
    {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(
                        AccountManager.ERROR_CODE_CANCELED, getString(R.string.canceled));
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }


    @Override
    public void onAddAccount(
            Account account,
            String token,
            boolean accountAdded)
    {
        if (null != account) {
            mResultBundle = new Bundle();

            if (accountAdded) {
                mResultBundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                mResultBundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                mResultBundle.putString(AccountManager.KEY_AUTHTOKEN, token);
            } else {
                mResultBundle.putString(
                        AccountManager.KEY_ERROR_MESSAGE, getString(R.string.ngw_account_already_exists));
            }
        }

        setResult(RESULT_OK);
        finish();
    }
}

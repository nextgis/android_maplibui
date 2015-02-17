/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;


public class NGWLoginActivity
        extends ActionBarActivity
        implements NGWLoginFragment.OnAddAccountListener
{
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle                       mResultBundle                 = null;


    @Override
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        createView();
    }


    protected void createView()
    {
        setContentView(R.layout.activity_ngw_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.getBackground().setAlpha(255);
        setSupportActionBar(toolbar);

        FragmentManager fm = getSupportFragmentManager();
        NGWLoginFragment ngwLoginFragment = (NGWLoginFragment) fm.findFragmentByTag("NGWLogin");

        if (ngwLoginFragment == null) {
            ngwLoginFragment = new NGWLoginFragment();
        }

        ngwLoginFragment.setOnAddAccountListener(this);

        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.login_frame, ngwLoginFragment, "NGWLogin");
        ft.commit();
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
        mResultBundle = new Bundle();

        if (accountAdded) {
            mResultBundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            mResultBundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            mResultBundle.putString(AccountManager.KEY_AUTHTOKEN, token);
        } else {
            mResultBundle.putString(
                    AccountManager.KEY_ERROR_MESSAGE, getString(R.string.account_already_exists));
        }

        setResult(RESULT_OK);
        finish();
    }
}

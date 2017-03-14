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

package com.nextgis.maplibui.account;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplibui.activity.NGWLoginActivity;


public class NGWAccountAuthenticator
        extends AbstractAccountAuthenticator
{
    protected Context mContext;


    public NGWAccountAuthenticator(Context context)
    {
        super(context);
        mContext = context;
    }


    @Override
    public Bundle editProperties(
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            String s)
    {
        return null;
    }


    @Override
    public Bundle addAccount(
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            String accountType,
            String authTokenType,
            String[] requiredFeatures,
            Bundle options)
            throws NetworkErrorException
    {
        IGISApplication app = (IGISApplication) mContext.getApplicationContext();
        final Intent intent = new Intent(mContext, NGWLoginActivity.class);
        intent.putExtra(app.getAccountsType(), accountType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        final Bundle bundle = new Bundle();
        if (options != null) {
            bundle.putAll(options);
        }
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }


    @Override
    public Bundle confirmCredentials(
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            Account account,
            Bundle bundle)
            throws NetworkErrorException
    {
        return null;
    }


    @Override
    public Bundle getAuthToken(
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            Account account,
            String s,
            Bundle bundle)
            throws NetworkErrorException
    {
        return null;
    }


    @Override
    public String getAuthTokenLabel(String s)
    {
        return null;
    }


    @Override
    public Bundle updateCredentials(
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            Account account,
            String s,
            Bundle bundle)
            throws NetworkErrorException
    {
        return null;
    }


    @Override
    public Bundle hasFeatures(
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            Account account,
            String[] strings)
            throws NetworkErrorException
    {
        return null;
    }
}

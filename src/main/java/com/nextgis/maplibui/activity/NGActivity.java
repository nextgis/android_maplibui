/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017, 2019 NextGIS, info@nextgis.com
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

import static com.nextgis.maplibui.util.ConstantsUI.CODE_SAVE_FILE;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.LayerUtil;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.lang.ref.WeakReference;
import java.util.function.Function;


/**
 * A base activity for NextGIS derived activities
 */
public class NGActivity
        extends AppCompatActivity
{
    protected SharedPreferences mPreferences;
    protected boolean           mIsDarkTheme;
    protected String            mCurrentTheme;
    protected WeakReference<ILayer> layerToSave = new WeakReference<>(null);
    int nChoise =0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mIsDarkTheme = ControlHelper.isDarkTheme(this);
        setCurrentThemePref();
        setTheme(getThemeId());
        super.onCreate(savedInstanceState);
    }

    public String getAppName() {
        String appName = "NextGIS Mobile";
        if (AccountUtil.isProUser(this))
            appName += " Pro";
        return appName;
    }

    public int getThemeId()
    {
        return mIsDarkTheme ? R.style.Theme_NextGIS_AppCompat_Dark : R.style.Theme_NextGIS_AppCompat_Light;
    }


    // for overriding in a subclass
    protected void setCurrentThemePref()
    {
        mCurrentTheme = mIsDarkTheme ?
                SettingsConstantsUI.KEY_PREF_DARK :
                SettingsConstantsUI.KEY_PREF_LIGHT;
    }


    // for overriding in a subclass
    protected void refreshCurrentTheme()
    {
        String newTheme = mPreferences.getString(SettingsConstantsUI.KEY_PREF_THEME, SettingsConstantsUI.KEY_PREF_LIGHT);

        if (newTheme != null && !newTheme.equals(mCurrentTheme)) {
            refreshActivityView();
        }
    }


    /**
     * This hook is called whenever an item in your options menu is selected. The default
     * implementation simply returns false to have the normal processing happen (calling the item's
     * Runnable or sending a message to its Handler as appropriate).  You can use this method for
     * any items for which you would like to do processing without those other facilities. <p/>
     * <p>Derived classes should call through to the base class for it to perform the default menu
     * handling.</p>
     *
     * @param item
     *         The menu item that was selected.
     *
     * @return boolean Return false to allow normal menu processing to proceed, true to consume it
     * here.
     *
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isHomeEnabled()) {
                    finish();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    protected boolean isHomeEnabled()
    {
        return true;
    }


    protected void setToolbar(int toolbarId)
    {
        Toolbar toolbar = findViewById(toolbarId);
        if (null == toolbar) {
            return;
        }
        toolbar.getBackground().setAlpha(getToolbarAlpha());
        setSupportActionBar(toolbar);

        if (null != getSupportActionBar()) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    protected int getToolbarAlpha()
    {
        return 255; // not transparent
    }


    @Override
    protected void onResume()
    {
        refreshCurrentTheme();
        super.onResume();
    }


    public void refreshActivityView()
    {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }


    public void updateMenuView()
    {
        supportInvalidateOptionsMenu();
    }

    protected boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

   public void updateChoise(int choise){
        nChoise =choise;
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        switch (requestCode) {
            case CODE_SAVE_FILE: // save file to
                // start to choose - export with aliases or not
                VectorLayer layer = (VectorLayer)getLayerForSave();
                if (layer != null) {
                    storeLayerForSave(null);
                    LayerUtil.shareLayerAsGeoJSON(NGActivity.this, layer, true,
                            true, data, nChoise == 1);
                }
                break;
        }
    }

    public void storeLayerForSave(ILayer layer){
        layerToSave = new WeakReference<>(layer);
    }

    public ILayer getLayerForSave(){
        return layerToSave.get();
    }

}

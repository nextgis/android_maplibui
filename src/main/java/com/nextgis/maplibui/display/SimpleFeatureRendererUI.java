/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.display;

import android.support.v4.app.Fragment;

import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplibui.fragment.StyleFragment;

public class SimpleFeatureRendererUI extends RendererUI {
    private static Style mStyle;

    public SimpleFeatureRendererUI(SimpleFeatureRenderer renderer) {
        mRenderer = renderer;
        mStyle = renderer.getStyle();
    }

    @Override
    public Fragment getSettingsScreen(VectorLayer vectorLayer) {
        mSettings = super.getSettingsScreen(vectorLayer);
        if (mSettings == null) {
            mSettings = new StyleFragment();
            ((StyleFragment) mSettings).setStyle(mStyle);
            ((StyleFragment) mSettings).setLayer(vectorLayer);
        }

        return mSettings;
    }
}

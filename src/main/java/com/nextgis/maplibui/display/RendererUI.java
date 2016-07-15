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

import com.nextgis.maplib.api.IRenderer;
import com.nextgis.maplib.map.VectorLayer;

public abstract class RendererUI {
    protected IRenderer mRenderer;
    protected Fragment mSettings;

    public IRenderer getRenderer() {
        return mRenderer;
    }

    public Fragment getSettingsScreen(VectorLayer vectorLayer) {
        if (mSettings != null)
            return mSettings;
        else
            return null;
    }
}

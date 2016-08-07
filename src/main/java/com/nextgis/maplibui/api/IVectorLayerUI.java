/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.api;

import android.content.Context;
import com.nextgis.maplib.datasource.GeoGeometry;


/**
 * Vector layer interface
 */
public interface IVectorLayerUI extends ILayerUI
{
    int MODIFY_REQUEST = 1;
    /**
     * This method executed, when the form need to be shown
     * @param context activity context
     * @param featureId a feature identificator to fill form controls
     * @param geometry a new geometry for feature
     */
    void showEditForm(
            Context context,
            long featureId,
            GeoGeometry geometry);

    void showAttributes();
}

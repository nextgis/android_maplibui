/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

import android.os.Bundle;
import android.view.ViewGroup;

/**
 * Interface for control in edit form.
 * @author Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 */
public interface IControl
{
    /**
     * Return layer field name linked with control
     * @return field name
     */
    String getFieldName();

    /**
     * Executed to add control to the specified layout
     * @param layout to add control
     */
    void addToLayout(ViewGroup layout);

    /**
     * Return the value from feature or entered by user
     * @return value from user input or feature
     */
    Object getValue();

    /**
     * Save control state in onPause and etc.
     * @param outState bundle to save state to
     */
    void saveState(Bundle outState);
}

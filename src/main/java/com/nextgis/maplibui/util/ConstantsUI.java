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
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package com.nextgis.maplibui.util;


public interface ConstantsUI
{
    /**
     * Draw state
     */
    int DRAW_SATE_none              = 0;
    int DRAW_SATE_drawing           = 1;
    int DRAW_SATE_drawing_noclearbk = 2;
    int DRAW_SATE_panning           = 3;
    int DRAW_SATE_zooming           = 4;
    int DRAW_SATE_panning_fling     = 5;

    String KEY_MESSAGE    = "msg";
    String KEY_LAYER_ID   = "layer_id";
    String KEY_FEATURE_ID = "feature_id";
    String KEY_GEOMETRY   = "geometry";
    String KEY_FORM_PATH  = "form_path";

    String MESSAGE_INTENT = "com.nextgis.malibui.MESSAGE";

    String FILE_FORM = "form.json";

    int TOLERANCE_DP = 20;
}

/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplibui.util;


public interface ConstantsUI
{
    /**
     * Draw state
     */
    public static final int DRAW_SATE_none = 0;
    public static final int DRAW_SATE_drawing = 1;
    public static final int DRAW_SATE_drawing_noclearbk = 2;
    public static final int DRAW_SATE_panning = 3;
    public static final int DRAW_SATE_zooming = 4;
    public static final int DRAW_SATE_edit_drawing = 5;
    public static final int DRAW_SATE_panning_fling = 6;

    public static final String KEY_MESSAGE = "msg";
    public static final String KEY_LAYER_ID = "layer_id";
    public static final String KEY_FEATURE_ID = "feature_id";
    public static final String KEY_GEOMETRY = "geometry";
    public static final String KEY_FORM_PATH = "form_path";

    public static final String MESSAGE_INTENT = "com.nextgis.malibui.MESSAGE";

    public final static String FILE_FORM = "form.json";


}

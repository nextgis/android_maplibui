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

package com.nextgis.maplibui.util;

public interface SettingsConstantsUI
{
    String KEY_PREF_KEEPSCREENON         = "keep_screen_on";
    String KEY_PREF_COORD_FORMAT         = "coordinates_format";
    String KEY_PREF_SYNC_PERIODICALLY    = "sync_periodically";
    String KEY_PREF_SYNC_PERIOD          = "sync_period";
    String KEY_PREF_SYNC_PERIOD_SEC_LONG = "sync_period_sec_long";
    String KEY_PREF_SHOW_STATUS_PANEL    = "show_status_panel";
    String KEY_PREF_SHOW_CURRENT_LOC     = "show_current_location";
    String KEY_PREF_DARKTHEME            = "is_theme_dark";
    String KEY_PREF_APP_FIRST_RUN        = "app_first_run";
    String KEY_PREF_MAP_NAME             = "map_name";

    String OSM_URL = "http://{a,b,c}.tile.openstreetmap.org/{z}/{x}/{y}.png";
}

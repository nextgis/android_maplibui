/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

public interface ConstantsUI
{
    /**
     * Draw state
     */
    int DRAW_STATE_none = 0;
    int DRAW_STATE_drawing = 1;
    int DRAW_STATE_drawing_noclearbk = 2;
    int DRAW_STATE_panning = 3;
    int DRAW_STATE_zooming = 4;
    int DRAW_STATE_panning_fling = 5;

    String KEY_MESSAGE          = "msg";
    String KEY_LAYER_ID         = "layer_id";
    String KEY_FEATURE_ID       = "feature_id";
    String KEY_GEOMETRY         = "geometry";
    String KEY_GEOMETRY_CHANGED = "geometry_changed";
    String KEY_FORM_PATH        = "form_path";
    String KEY_META_PATH        = "meta_path";
    String KEY_VIEW_ONLY        = "view_only";

    String MESSAGE_INTENT = "com.nextgis.malibui.MESSAGE";
    String TARGET_CLASS   = "target_class";
    String TARGET_EXTRAS  = "target_extras";

    String FILE_FORM = "form.json";

    int TOLERANCE_DP       = 20;
    int MIN_ZOOM_LEVEL     = 18;
    int NOTIFICATION_DELAY = 500;

    String JSON_NGW_CONNECTION_KEY = "ngw_connection";
    String JSON_NGW_ID_KEY         = "ngw_id";

    String JSON_ATTRIBUTES_KEY        = "attributes";
    String JSON_TABS_KEY              = "tabs";
    String JSON_ALBUM_ELEMENTS_KEY    = "album_elements";
    String JSON_PORTRAIT_ELEMENTS_KEY = "portrait_elements";
    String JSON_FIELD_NAME_KEY        = "field";
    String JSON_DEFAULT_KEY           = "default";
    String JSON_SHOW_LAST_KEY         = "last";
    String JSON_TEXT_KEY              = "text";
    String JSON_MAX_STRING_COUNT_KEY  = "max_string_count";
    String JSON_ONLY_FIGURES_KEY      = "only_figures";
    String JSON_VALUES_KEY            = "values";
    String JSON_VALUE_NAME_KEY        = "name";
    String JSON_VALUE_ALIAS_KEY       = "alias";
    String JSON_VALUE_ALIAS2_KEY      = "alias2";
    String JSON_DATE_TYPE_KEY         = "date_type";
    String JSON_FIELD_LEVEL1_KEY      = "field_level1";
    String JSON_FIELD_LEVEL2_KEY      = "field_level2";
    String JSON_INIT_VALUE_KEY        = "init_value";
    String JSON_MAX_PHOTO_KEY         = "gallery_size";
    String JSON_INPUT_SEARCH          = "input_search";
    String JSON_ALLOW_NEW_VALUES      = "allow_adding_values";
    String JSON_HIDDEN_KEY            = "hidden";
    String JSON_LISTS_KEY             = "lists";
    String JSON_KEY_LIST_KEY          = "key_list";
    String JSON_KEY_LIST_SAVED_KEY    = "key_list_saved";
    String JSON_LABEL_KEY             = "label1";
    String JSON_LABEL2_KEY            = "label2";

    String JSON_TEXT_LABEL_VALUE      = "text_label";
    String JSON_TEXT_EDIT_VALUE       = "text_edit";
    String JSON_DATE_TIME_VALUE       = "date_time";
    String JSON_RADIO_GROUP_VALUE     = "radio_group";
    String JSON_COMBOBOX_VALUE        = "combobox";
    String JSON_SPLIT_COMBOBOX_VALUE  = "split_combobox";
    String JSON_DOUBLE_COMBOBOX_VALUE = "double_combobox";
    String JSON_SPACE_VALUE           = "space";
    String JSON_CHECKBOX_VALUE        = "checkbox";
    String JSON_PHOTO_VALUE           = "photo";
    String JSON_SIGN_VALUE            = "signature";
    String JSON_COUNTER_VALUE         = "counter";
    String JSON_COORDINATES_VALUE     = "coordinates";

    String PERMISSION_AUTHENTICATE_ACCOUNTS = "android.permission.AUTHENTICATE_ACCOUNTS";
    String PERMISSION_MANAGE_ACCOUNTS       = "android.permission.MANAGE_ACCOUNTS";

    String GA_DIALOG_QMS = "Geoservice QMS";
    String GA_DIALOG_TMS = "Geoservice New";
    String GA_LAYER      = "Layer";
    String GA_EDIT       = "Edit";
    String GA_FAB        = "FAB";
    String GA_MENU       = "Menu";
    String GA_CREATE     = "Create";
    String GA_LOCAL      = "Local";
    String GA_IMPORT     = "Import";
    String GA_GEOSERVICE = "Geoservice";
    String GA_NGW        = "NGW";
    String GA_NGID       = "NGID";
    String GA_CONNECT    = "Connect";
    String GA_GUEST      = "Guest";
    String GA_USER       = "User";

    /**
     * Fragments tags
     */
    String FRAGMENT_NGW_HEADER_SETTINGS  = "ngw_header_settings";
    String FRAGMENT_NGW_SETTINGS         = "ngw_settings";
    String FRAGMENT_NGID_HEADER_SETTINGS = "ngid_header_settings";
    String FRAGMENT_NGID_SETTINGS        = "ngid_settings";
    String FRAGMENT_NGID_LOGIN           = "ngid_login";
    String FRAGMENT_SELECT_RESOURCE      = "select_resource";

    String PREF_SCREEN_TITLE = "pref_screen_title";
}

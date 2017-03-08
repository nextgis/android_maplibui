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

package com.nextgis.maplibui.adapter;

import android.support.annotation.NonNull;
import com.nextgis.maplib.util.FileUtil;

import java.io.File;

import static com.nextgis.maplib.util.Constants.*;


public class LocalResourceListItem
        implements Comparable<LocalResourceListItem>
{
    protected File    mFile;
    protected int     mType;
    protected boolean mSelectable;
    protected boolean mVisibleCheckBox;


    public LocalResourceListItem(
            File file,
            boolean selectable,
            boolean forWritableFolder)
    {
        init(file, getFileType(file), selectable, forWritableFolder);
    }


    public LocalResourceListItem(
            File file,
            int type,
            boolean selectable,
            boolean forWritableFolder)
    {
        init(file, type, selectable, forWritableFolder);
    }


    protected void init(
            File file,
            int type,
            boolean selectable,
            boolean forWritableFolder)
    {
        mFile = file;
        mType = type;

        if (selectable) {
            if (!forWritableFolder) {
                mSelectable = true;
                mVisibleCheckBox = true;
            } else if (FILETYPE_FOLDER == mType) {
                mSelectable = FileUtil.isDirectoryWritable(file);
                mVisibleCheckBox = true;
            }
        } else {
            mSelectable = false;
            mVisibleCheckBox = false;
        }
    }


    public File getFile()
    {
        return mFile;
    }


    public int getType()
    {
        return mType;
    }


    public boolean isSelectable()
    {
        return mSelectable;
    }


    public boolean isVisibleCheckBox()
    {
        return mVisibleCheckBox;
    }


    public static int getFileType(File file)
    {
        if (file.isDirectory()) {
            return FILETYPE_FOLDER;
        }
        if (file.getName().endsWith(".zip")) {
            return FILETYPE_ZIP;
        }
        if (file.getName().endsWith(".ngfb")) {
            return FILETYPE_FB;
        }
        if (file.getName().endsWith(".geojson")) {
            return FILETYPE_GEOJSON;
        }
        return FILETYPE_UNKNOWN;
    }


    @Override
    public int compareTo(
            @NonNull
                    LocalResourceListItem other)
    {
        if (FILETYPE_PARENT == mType && FILETYPE_PARENT != other.mType) {
            return -1;
        }

        if (FILETYPE_PARENT != mType && FILETYPE_PARENT == other.mType) {
            return 1;
        }

        if (FILETYPE_FOLDER == mType && FILETYPE_FOLDER != other.mType) {
            return -1;
        }

        if (FILETYPE_FOLDER != mType && FILETYPE_FOLDER == other.mType) {
            return 1;
        }

        return mFile.compareTo(other.mFile);
    }
}

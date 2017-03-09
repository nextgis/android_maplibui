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

import android.content.Context;
import android.os.Build;
import android.support.v4.content.AsyncTaskLoader;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;


// see example in the
// http://developer.android.com/reference/android/content/AsyncTaskLoader.html
public class LocalResourceListLoader
        extends AsyncTaskLoader<List<LocalResourceListItem>>

{
    protected File    mPath;
    protected int     mTypeMask;
    protected boolean mCanSelectMulti;
    protected boolean mCanWrite;

    protected List<LocalResourceListItem> mResources;


    public LocalResourceListLoader(
            Context context,
            File path)
    {
        super(context);
        mPath = path;
    }


    public void setCanSelectMulti(boolean canSelectMulti)
    {
        mCanSelectMulti = canSelectMulti;
    }


    public void setCanWrite(boolean canWrite)
    {
        mCanWrite = canWrite;
    }


    public void setTypeMask(int typeMask)
    {
        mTypeMask = typeMask;
    }


    public void setCurrentPath(File path)
    {
        mPath = path;
        // Tell the loader about the change.
        onContentChanged();
    }


    public File getCurrentPath()
    {
        return mPath;
    }


    /**
     * This is where the bulk of our work is done.  This function is called in a background thread
     * and should generate a new set of data to be published by the loader.
     */
    @Override
    public List<LocalResourceListItem> loadInBackground()
    {
        List<LocalResourceListItem> resources = new LinkedList<>();

        if (null == mPath) {
            return resources;
        }

        File parentFile = mPath.getParentFile();
        if (null != parentFile) {
            // TODO: get list of sdcards
            LocalResourceListItem item =
                    new LocalResourceListItem(parentFile, FILETYPE_PARENT, false, false);
            resources.add(item);
        }

        File[] files = mPath.listFiles();
        if (null != files) {
            for (File file : files) {
                int type = LocalResourceListItem.getFileType(file);
                boolean selectable = 0 != (mTypeMask & type);
                // always show folders
                if (selectable || type == FILETYPE_FOLDER) {
                    LocalResourceListItem item =
                            new LocalResourceListItem(file, type, selectable, mCanWrite);
                    resources.add(item);
                }
            }
        }

        Collections.sort(resources);

        return resources;
    }


    /**
     * Called when there is new data to deliver to the client.  The super class will take care of
     * delivering it; the implementation here just adds a little more logic.
     */
    @Override
    public void deliverResult(List<LocalResourceListItem> resources)
    {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We don't need the result.
            if (resources != null) {
                onReleaseResources(resources);
            }
        }

        List<LocalResourceListItem> oldResources = null;
        if (resources != mResources) {
            oldResources = mResources;
            mResources = resources;
        }

        if (isStarted()) {
            // If the Loader is currently started, we can immediately deliver its results.
            super.deliverResult(resources);
        }

        // At this point we can release the resources associated with 'oldResources' if needed;
        // now that the new result is delivered we know that it is no longer in use.
        if (oldResources != null) {
            onReleaseResources(oldResources);
        }
    }


    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading()
    {
        if (mResources != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mResources);
        }

        // Start watching for changes in the resources.
        //if (null != mOutChangeEvent) {
        //    mOutChangeEvent.setOutChangeListener(this);
        //}

        if (takeContentChanged() || mResources == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }


    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading()
    {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }


    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<LocalResourceListItem> resources)
    {
        super.onCanceled(resources);

        // At this point we can release the resources associated with 'resources' if needed.
        onReleaseResources(resources);
    }


    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset()
    {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'mResources'  if needed.
        if (mResources != null) {
            onReleaseResources(mResources);
            mResources = null;
        }

        // Stop monitoring for changes.
        //if (null != mOutChangeEvent) {
        //    mOutChangeEvent.setOutChangeListener(null);
        //}
    }


    /**
     * Helper function to take care of releasing resources associated with an actively loaded data
     * set.
     */
    protected void onReleaseResources(List<LocalResourceListItem> resources)
    {
        if (null != resources) {
            resources.clear();
        }
    }


    //@Override
    //public void onOutChangeEvent()
    //{
    //   // Tell the loader about the change.
    //    onContentChanged();
    //}
}

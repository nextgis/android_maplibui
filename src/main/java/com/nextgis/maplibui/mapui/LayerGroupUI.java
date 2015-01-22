package com.nextgis.maplibui.mapui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.nextgis.maplib.map.LayerFactory;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ILayerUI;

import java.io.File;


/**
 * UI for Layer group
 */
public class LayerGroupUI extends LayerGroup implements ILayerUI
{
    public LayerGroupUI(
            Context context,
            File path,
            LayerFactory layerFactory)
    {
        super(context, path, layerFactory);
    }


    @Override
    public Drawable getIcon()
    {
        return mContext.getResources().getDrawable(R.drawable.ic_ngw_folder);
    }


    @Override
    public void changeProperties()
    {

    }
}

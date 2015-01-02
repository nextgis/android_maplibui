package com.nextgis.maplibui.mapui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ILayerUI;

import java.io.File;


public class RemoteTMSLayerUI extends RemoteTMSLayer implements ILayerUI
{
    public RemoteTMSLayerUI(
            Context context,
            File path)
    {
        super(context, path);
    }


    @Override
    public Drawable getIcon()
    {
        return mContext.getResources().getDrawable(R.drawable.ic_remote_tms);
    }


    @Override
    public void changeProperties()
    {

    }
}

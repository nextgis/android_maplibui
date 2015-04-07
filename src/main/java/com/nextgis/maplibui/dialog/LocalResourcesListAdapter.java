/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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

package com.nextgis.maplibui.dialog;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ISelectResourceDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;


/**
 * Adapter to represent folders and files
 */
public class LocalResourcesListAdapter extends BaseAdapter
        implements AdapterView.OnItemClickListener
{
    protected ISelectResourceDialog mDialog;
    protected List<String>          mCheckState;
    protected int                   mTypeMask;
    protected PathView              mPathView;
    protected boolean               mCanSelectMulti;
    protected boolean               mCanWrite;
    protected RadioButton mUncheckBtn;

    protected File       mPath;
    protected List<File> mFiles;


    public LocalResourcesListAdapter(ISelectResourceDialog dialog)
    {
        mDialog = dialog;
    }


    public void setTypeMask(int typeMask)
    {
        mTypeMask = typeMask;
    }


    public void setPathLayout(LinearLayout linearLayout)
    {
        mPathView = new PathView(linearLayout);
        mPathView.onUpdate(mPath);
    }


    public List<String> getCheckState()
    {
        return mCheckState;
    }


    public void setCheckState(List<String> checkState)
    {
        mCheckState = checkState;
    }


    public void setCurrentPath(File path)
    {
        mPath = path;
        if (null != path) {
            mUncheckBtn = null;
            fillFiles();
            notifyDataSetChanged();
            if (null != mPathView)
                mPathView.onUpdate(path);
        }
    }


    public File getCurrentPath()
    {
        return mPath;
    }


    protected void fillFiles()
    {
        mFiles = new ArrayList<>();
        if (null != mPath.getParentFile())
            mFiles.add(null);
        File[] files = mPath.listFiles();
        if (null != files) {
            for (File file : files) {
                int type = getFileType(file);
                if (0 != (mTypeMask & type))
                    mFiles.add(file);
                else if (type == FILETYPE_FOLDER) // always show folders
                    mFiles.add(file);
            }
        }
    }


    protected int getFileType(File file)
    {
        if (file.isDirectory())
            return FILETYPE_FOLDER;
        if (file.getName().endsWith("zip"))
            return FILETYPE_ZIP;
        if (file.getName().endsWith("ngfb"))
            return FILETYPE_FB;
        if (file.getName().endsWith("geojson"))
            return FILETYPE_GEOJSON;
        return 1 << 255;
    }


    public void setCanSelectMulti(boolean canSelectMulti)
    {
        mCanSelectMulti = canSelectMulti;
    }

    public void setCanWrite(boolean canWrite)
    {
        mCanWrite = canWrite;
    }

    @Override
    public int getCount()
    {
        return mFiles.size();
    }


    @Override
    public Object getItem(int i)
    {
        if(mFiles.isEmpty())
            return null;
        return mFiles.get(i);
    }


    @Override
    public long getItemId(int i)
    {
        return i;
    }


    @Override
    public View getView(
            int i,
            View view,
            ViewGroup viewGroup)
    {
        View v = view;
        final File file = (File) getItem(i);
        int viewId;
        if(null == file){
            if (null == v || v.getId() != R.id.resourcegroup_row) {
                LayoutInflater inflater = LayoutInflater.from(mDialog.getContext());
                v = inflater.inflate(R.layout.layout_resourcegroup_row, null);
                v.setId(R.id.resourcegroup_row);

                ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                ivIcon.setImageDrawable(mDialog.getContext().getResources().getDrawable(R.drawable.ic_folder));
            }

            TextView tvText = (TextView) v.findViewById(R.id.tvName);
            tvText.setText(mDialog.getContext().getString(R.string.up_dots));

            TextView tvDesc = (TextView) v.findViewById(R.id.tvDesc);
            tvDesc.setText(mDialog.getContext().getString(R.string.up));
        }
        else{
            ImageView ivIcon;
            TextView tvDesc;
            CheckBox checkBox;
            RadioButton radioButton;
            LayoutInflater inflater = LayoutInflater.from(mDialog.getContext());

            switch (getFileType(file)){
                case FILETYPE_FOLDER:
                    if(0 == (mTypeMask & FILETYPE_FOLDER) || (mCanWrite && !file.canWrite())) {
                        if(v == null || v.getId() != R.id.resourcegroup_row) {
                            v = inflater.inflate(R.layout.layout_resourcegroup_row, null);
                            v.setId(R.id.resourcegroup_row);
                        }
                    }
                    else{
                        if(mCanSelectMulti) //chow checkbox
                        {
                            viewId = R.id.resource_check_row;
                            if(v == null || v.getId() != viewId) {
                                v = inflater.inflate(R.layout.layout_resource_check_row, null);
                                v.setId(viewId);


                            }
                            //add check listener
                            checkBox = (CheckBox)v.findViewById(R.id.check);
                            setCheckBox(checkBox, file);
                        }
                        else
                        {
                            viewId = R.id.resource_radio_row;
                            if(v == null || v.getId() != viewId) {
                                v = inflater.inflate(R.layout.layout_resource_radio_row, null);
                                v.setId(viewId);
                            }

                            //add check listener
                            radioButton = (RadioButton)v.findViewById(R.id.radio);
                            setRadioButton(radioButton, file);
                        }
                    }
                    ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(
                            mDialog.getContext().getResources().getDrawable(R.drawable.ic_folder));

                    tvDesc = (TextView) v.findViewById(R.id.tvDesc);
                    tvDesc.setText(mDialog.getContext().getString(R.string.folder));
                    break;
                case FILETYPE_ZIP:
                    if(mCanSelectMulti) //chow checkbox
                    {
                        viewId = R.id.resource_check_row;
                        if(v == null || v.getId() !=viewId) {
                            v = inflater.inflate(R.layout.layout_resource_check_row, null);
                            v.setId(viewId);
                        }
                        //add check listener
                        checkBox = (CheckBox)v.findViewById(R.id.check);
                        setCheckBox(checkBox, file);
                    }
                    else
                    {
                        viewId = R.id.resource_radio_row;
                        if(v == null || v.getId() != viewId) {
                            v = inflater.inflate(R.layout.layout_resource_radio_row, null);
                            v.setId(viewId);
                        }
                        //add check listener
                        radioButton = (RadioButton)v.findViewById(R.id.radio);
                        setRadioButton(radioButton, file);
                    }

                    ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(
                            mDialog.getContext().getResources().getDrawable(R.drawable.ic_zip));

                    tvDesc = (TextView) v.findViewById(R.id.tvDesc);
                    tvDesc.setText(mDialog.getContext().getString(R.string.zip));

                    break;
                case FILETYPE_FB:
                    if(mCanSelectMulti) //chow checkbox
                    {
                        viewId = R.id.resource_check_row;
                        if(v == null || v.getId() != viewId) {
                            v = inflater.inflate(R.layout.layout_resource_check_row, null);
                            v.setId(viewId);
                        }
                        //add check listener
                        checkBox = (CheckBox)v.findViewById(R.id.check);
                        setCheckBox(checkBox, file);
                    }
                    else
                    {
                        viewId = R.id.resource_radio_row;
                        if(v == null || v.getId() != viewId) {
                            v = inflater.inflate(R.layout.layout_resource_radio_row, null);
                            v.setId(viewId);
                        }
                        //add check listener
                        radioButton = (RadioButton)v.findViewById(R.id.radio);
                        setRadioButton(radioButton, file);
                    }

                    ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(
                            mDialog.getContext().getResources().getDrawable(R.drawable.ic_formbuilder));

                    tvDesc = (TextView) v.findViewById(R.id.tvDesc);
                    tvDesc.setText(mDialog.getContext().getString(R.string.formbuilder));

                    break;
                case FILETYPE_GEOJSON:
                    if(mCanSelectMulti) //chow checkbox
                    {
                        viewId = R.id.resource_check_row;
                        if(v == null || v.getId() != viewId) {
                            v = inflater.inflate(R.layout.layout_resource_check_row, null);
                            v.setId(viewId);
                        }
                        //add check listener
                        checkBox = (CheckBox)v.findViewById(R.id.check);
                        setCheckBox(checkBox, file);
                    }
                    else
                    {
                        viewId = R.id.resource_radio_row;
                        if(v == null || v.getId() != viewId) {
                            v = inflater.inflate(R.layout.layout_resource_radio_row, null);
                            v.setId(viewId);
                        }
                        //add check listener
                        radioButton = (RadioButton)v.findViewById(R.id.radio);
                        setRadioButton(radioButton, file);
                    }

                    ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(
                            mDialog.getContext().getResources().getDrawable(R.drawable.ic_geojson));

                    tvDesc = (TextView) v.findViewById(R.id.tvDesc);
                    tvDesc.setText(mDialog.getContext().getString(R.string.geojson));

                    break;
            }

            TextView tvText = (TextView) v.findViewById(R.id.tvName);
            tvText.setText(file.getName());
        }
        return v;
    }


    protected void setRadioButton(
            final RadioButton radioButton,
            final File file)
    {
        radioButton.setOnCheckedChangeListener(null);

        if(mCheckState.contains(file.getAbsolutePath())) {

            mUncheckBtn = radioButton;
            if(!radioButton.isChecked())
                radioButton.setChecked(true);
        }
        else{
            if(radioButton.isChecked())
                radioButton.setChecked(false);
        }

        radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(
                    CompoundButton compoundButton,
                    boolean b)
            {
                if(b) {
                    if(mCheckState.isEmpty())
                        mCheckState.add(file.getAbsolutePath());
                    else
                        mCheckState.set(0, file.getAbsolutePath());

                    if(null != mUncheckBtn && mUncheckBtn != radioButton)
                        mUncheckBtn.setChecked(false);
                    mUncheckBtn = radioButton;
                }

                mDialog.updateButtons();
            }
        });
    }


    protected void setCheckBox(
            final CheckBox checkBox,
            final File file)
    {
        checkBox.setOnCheckedChangeListener(null);

        if(mCheckState.contains(file.getAbsolutePath())) {
            if(!checkBox.isChecked())
                checkBox.setChecked(true);
        }
        else {
            if(checkBox.isChecked())
                checkBox.setChecked(false);
        }

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(
                    CompoundButton compoundButton,
                    boolean b)
            {
                if(b && !mCheckState.contains(file.getAbsolutePath()))
                    mCheckState.add(file.getAbsolutePath());
                else
                    mCheckState.remove(file.getAbsolutePath());
                mDialog.updateButtons();
            }
        });
    }


    @Override
    public void onItemClick(
            AdapterView<?> adapterView,
            View view,
            int i,
            long l)
    {
        File file = (File) getItem(i);
        if(null == file){
            //go
            setCurrentPath(mPath.getParentFile());
        }
        else{
            //go deep
            setCurrentPath(file);
        }
    }
    /**
     * A path view class. the path is a resources names divide by arrows in head of dialog.
     * If user click on name, the dialog follow the specified path.
     */
    protected class PathView{
        protected LinearLayout mLinearLayout;


        public PathView(LinearLayout linearLayout)
        {
            mLinearLayout = linearLayout;
        }

        public void onUpdate(File path){
            if(null == mLinearLayout || null == path)
                return;
            mLinearLayout.removeAllViewsInLayout();

            File parent = path;
            while (null != parent){
                final File parentPath = parent;
                TextView name = new TextView(mDialog.getContext());
                String sName = parent.getName();
                name.setText(sName);
                name.setTypeface(name.getTypeface(), Typeface.BOLD);
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                name.setSingleLine(true);
                name.setMaxLines(1);
                name.setBackgroundResource(android.R.drawable.list_selector_background);
                name.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        setCurrentPath(parentPath);
                    }
                });

                mLinearLayout.addView(name, 0);

                parent = parent.getParentFile();

                if(null != parent){
                    ImageView image = new ImageView(mDialog.getContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(30,30);
                    image.setLayoutParams(params);
                    image.setImageDrawable(mDialog.getContext().getResources().getDrawable(R.drawable.ic_next));
                    mLinearLayout.addView(image, 0);
                }
            }
        }
    }
}

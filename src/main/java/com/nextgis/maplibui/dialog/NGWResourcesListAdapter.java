/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.dialog;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
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
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Connections;
import com.nextgis.maplib.datasource.ngw.INGWResource;
import com.nextgis.maplib.datasource.ngw.LayerWithStyles;
import com.nextgis.maplib.datasource.ngw.Resource;
import com.nextgis.maplib.datasource.ngw.ResourceGroup;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.util.CheckState;

import java.util.List;

import static com.nextgis.maplib.util.Constants.NGW_ACCOUNT_GUEST;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;


public class NGWResourcesListAdapter
        extends BaseAdapter
        implements AdapterView.OnItemClickListener
{
    protected Connections             mConnections;
    protected INGWResource            mCurrentResource;
    protected SelectNGWResourceDialog mSelectNGWResourceDialog;
    protected boolean                 mLoading;
    protected PathView                mPathView;
    protected int                     mTypeMask;
    protected List<CheckState>        mCheckState;


    public NGWResourcesListAdapter(SelectNGWResourceDialog dialog)
    {
        mSelectNGWResourceDialog = dialog;
        mLoading = false;
    }


    public void setTypeMask(int typeMask)
    {
        mTypeMask = typeMask;
    }


    public void setConnections(Connections connections)
    {
        mConnections = connections;
    }


    public Connections getConnections()
    {
        return mConnections;
    }


    public int getCurrentResourceId()
    {
        return mCurrentResource.getId();
    }


    public void setCurrentResourceId(int id)
    {
        mCurrentResource = mConnections.getResourceById(id);
        if (null != mCurrentResource) {
            notifyDataSetChanged();
            if (null != mPathView) {
                mPathView.onUpdate(mCurrentResource);
            }
        }
    }


    public void setPathLayout(LinearLayout linearLayout)
    {
        mPathView = new PathView(linearLayout);
        mPathView.onUpdate(mCurrentResource);
    }


    public List<CheckState> getCheckState()
    {
        return mCheckState;
    }


    public void setCheckState(List<CheckState> checkState)
    {
        mCheckState = checkState;
    }


    @Override
    public int getCount()
    {
        if (null == mCurrentResource) {
            return 0;
        }
        if (mLoading) {
            return 2;
        }
        return mCurrentResource.getChildrenCount() + 1; //add up button or add connections button
    }


    @Override
    public Object getItem(int i)
    {
        if (null == mCurrentResource || mLoading) {
            return null;
        }
        if (mCurrentResource.getType() == Connection.NGWResourceTypeConnections) {
            if (i > mCurrentResource.getChildrenCount()) {
                return null;
            }
            return mCurrentResource.getChild(i);
        } else if (mCurrentResource.getType() == Connection.NGWResourceTypeConnection) {
            if (i == 0) {
                return null;
            }
            return mCurrentResource.getChild(i - 1);
        } else if (mCurrentResource.getType() == Connection.NGWResourceTypeResourceGroup) {
            if (i == 0) {
                return null;
            }
            return mCurrentResource.getChild(i - 1);
        }
        return null;
    }


    @Override
    public long getItemId(int i)
    {
        INGWResource resource = (INGWResource) getItem(i);
        if (null == resource) {
            return NOT_FOUND;
        }
        return resource.getId();
    }


    @Override
    public View getView(
            int i,
            View view,
            ViewGroup viewGroup)
    {
        if (mLoading && i > 0) {
            //show loading view
            return getLoadingView(view);
        }

        switch (mCurrentResource.getType()) {
            case Connection.NGWResourceTypeConnections:
                final Connection connection = (Connection) getItem(i);
                return getConnectionView(connection, view);
            case Connection.NGWResourceTypeConnection:
            case Connection.NGWResourceTypeResourceGroup:
                Resource resource = (Resource) getItem(i);
                return getResourceView(resource, view);
            default:
                return null;
        }
    }


    protected View getLoadingView(View view)
    {
        View v = view;
        if (null == v || v.getId() != R.id.loading_row) {
            Context context = mSelectNGWResourceDialog.getActivity();
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(R.layout.row_loading, null);
            v.setId(R.id.loading_row);
        }
        return v;
    }


    protected View getConnectionView(
            Connection connection,
            View view)
    {
        View v = view;
        Context context = mSelectNGWResourceDialog.getActivity();
        if (null == connection) { //create add account button
            if (null == v || v.getId() != R.id.resourcegroup_row) {
                LayoutInflater inflater = LayoutInflater.from(context);
                v = inflater.inflate(R.layout.row_resourcegroup, null);
                v.setId(R.id.resourcegroup_row);
            }
            ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
            ivIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_add_account));

            TextView tvText = (TextView) v.findViewById(R.id.tvName);
            tvText.setText(context.getString(R.string.add_account));

            TextView tvDesc = (TextView) v.findViewById(R.id.tvDesc);
            tvDesc.setVisibility(View.GONE);
        } else {
            if (null == v || v.getId() != R.id.resourcegroup_row) {
                LayoutInflater inflater = LayoutInflater.from(context);
                v = inflater.inflate(R.layout.row_resourcegroup, null);
                v.setId(R.id.resourcegroup_row);
            }
            ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
            ivIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_ngw));

            TextView tvText = (TextView) v.findViewById(R.id.tvName);
            tvText.setText(connection.getName());

            TextView tvDesc = (TextView) v.findViewById(R.id.tvDesc);
            tvDesc.setVisibility(View.GONE);
        }

        return v;
    }


    protected View getResourceView(
            Resource resource,
            View view)
    {
        View v = view;
        Context context = mSelectNGWResourceDialog.getActivity();
        if (null == resource) { //create up button
            if (null == v || v.getId() != R.id.resourcegroup_row) {
                LayoutInflater inflater = LayoutInflater.from(context);
                v = inflater.inflate(R.layout.row_resourcegroup, null);
                v.setId(R.id.resourcegroup_row);

                ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                ivIcon.setImageDrawable(
                        context.getResources().getDrawable(R.drawable.ic_ngw_folder));
            }

            TextView tvText = (TextView) v.findViewById(R.id.tvName);
            tvText.setText(context.getString(R.string.up_dots));

            TextView tvDesc = (TextView) v.findViewById(R.id.tvDesc);
            tvDesc.setText(context.getString(R.string.up));
        } else {
            ImageView ivIcon;
            TextView tvDesc;

            int resourceType = resource.getType();
            if (0 == (mTypeMask & resourceType) &&
                resourceType != Connection.NGWResourceTypeResourceGroup) {
                if (null == v || v.getId() != R.id.empty_row) {
                    LayoutInflater inflater = LayoutInflater.from(context);
                    v = inflater.inflate(R.layout.row_empty, null);
                    v.setId(R.id.empty_row);
                }
                return v;
            }

            final int id = resource.getId();
            CheckBox checkBox1, checkBox2;

            switch (resourceType) {
                case Connection.NGWResourceTypeResourceGroup:
                    if (null == v || v.getId() != R.id.resourcegroup_row) {
                        LayoutInflater inflater = LayoutInflater.from(context);
                        v = inflater.inflate(R.layout.row_resourcegroup, null);
                        v.setId(R.id.resourcegroup_row);

                        ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                        ivIcon.setImageDrawable(
                                context.getResources().getDrawable(R.drawable.ic_ngw_folder));
                    }

                    tvDesc = (TextView) v.findViewById(R.id.tvDesc);
                    tvDesc.setText(context.getString(R.string.resource_group));
                    break;
                case Connection.NGWResourceTypeRasterLayer:
                    if (null == v || v.getId() != R.id.ngw_layer_check_row) {
                        LayoutInflater inflater = LayoutInflater.from(context);
                        v = inflater.inflate(R.layout.row_ngwlayer_check, null);
                        v.setId(R.id.ngw_layer_check_row);
                    }
                    ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(
                            context.getResources().getDrawable(R.drawable.ic_ngw_raster));

                    tvDesc = (TextView) v.findViewById(R.id.tvDesc);
                    tvDesc.setText(context.getString(R.string.raster_layer));

                    //add check listener
                    checkBox1 = (CheckBox) v.findViewById(R.id.checkBox1);
                    setCheckBox(checkBox1, id, 1);
                    break;
                case Connection.NGWResourceTypeWMSClient:
                    if (null == v || v.getId() != R.id.ngw_layer_check_row) {
                        LayoutInflater inflater = LayoutInflater.from(context);
                        v = inflater.inflate(R.layout.row_ngwlayer_check, null);
                        v.setId(R.id.ngw_layer_check_row);
                    }
                    ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(
                            context.getResources().getDrawable(R.drawable.ic_ngw_raster));

                    tvDesc = (TextView) v.findViewById(R.id.tvDesc);
                    tvDesc.setText(context.getString(R.string.raster_layer));

                    //add check listener
                    checkBox1 = (CheckBox) v.findViewById(R.id.checkBox1);
                    setCheckBox(checkBox1, id, 1);
                    break;
                case Connection.NGWResourceTypeVectorLayer:
                    LayerWithStyles layer = (LayerWithStyles) resource;
                    if (layer.getStyleCount() > 0) {
                        if (null == v || v.getId() != R.id.ngw_layer_doublecheck_row) {
                            LayoutInflater inflater = LayoutInflater.from(context);
                            v = inflater.inflate(R.layout.row_ngwlayer_doublecheck, null);
                            v.setId(R.id.ngw_layer_doublecheck_row);
                        }

                        //add check listener
                        checkBox1 = (CheckBox) v.findViewById(R.id.checkBox1);
                        setCheckBox(checkBox1, id, 1);

                        checkBox2 = (CheckBox) v.findViewById(R.id.checkBox2);
                        setCheckBox(checkBox2, id, 2);
                    } else {
                        if (null == v || v.getId() != R.id.ngw_layer_check_row) {
                            LayoutInflater inflater = LayoutInflater.from(context);
                            v = inflater.inflate(R.layout.row_ngwlayer_check, null);
                            v.setId(R.id.ngw_layer_check_row);
                        }

                        TextView tvType = (TextView) v.findViewById(R.id.type1);
                        tvType.setText(context.getString(R.string.vector));

                        //add check listener
                        checkBox1 = (CheckBox) v.findViewById(R.id.checkBox1);
                        setCheckBox(checkBox1, id, 2);
                    }

                    ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(
                            context.getResources().getDrawable(R.drawable.ic_ngw_vector));

                    tvDesc = (TextView) v.findViewById(R.id.tvDesc);
                    tvDesc.setText(context.getString(R.string.vector_layer));
                    break;
                case Connection.NGWResourceTypePostgisLayer:
                    LayerWithStyles pglayer = (LayerWithStyles) resource;
                    if (pglayer.getStyleCount() > 0) {
                        if (null == v || v.getId() != R.id.ngw_layer_doublecheck_row) {
                            LayoutInflater inflater = LayoutInflater.from(context);
                            v = inflater.inflate(R.layout.row_ngwlayer_doublecheck, null);
                            v.setId(R.id.ngw_layer_doublecheck_row);
                        }


                        //add check listener
                        checkBox1 = (CheckBox) v.findViewById(R.id.checkBox1);
                        setCheckBox(checkBox1, id, 1);

                        checkBox2 = (CheckBox) v.findViewById(R.id.checkBox2);
                        setCheckBox(checkBox2, id, 2);
                    } else {
                        if (null == v || v.getId() != R.id.ngw_layer_check_row) {
                            LayoutInflater inflater = LayoutInflater.from(context);
                            v = inflater.inflate(R.layout.row_ngwlayer_check, null);
                            v.setId(R.id.ngw_layer_check_row);
                        }

                        TextView tvType = (TextView) v.findViewById(R.id.type1);
                        tvType.setText(context.getString(R.string.vector));

                        //add check listener
                        checkBox1 = (CheckBox) v.findViewById(R.id.checkBox1);
                        setCheckBox(checkBox1, id, 2);
                    }

                    ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(
                            context.getResources().getDrawable(R.drawable.ic_pg_vector));

                    tvDesc = (TextView) v.findViewById(R.id.tvDesc);
                    tvDesc.setText(context.getString(R.string.pg_layer));
                    break;
                case Connection.NGWResourceTypeWebMap:
                    if (null == v || v.getId() != R.id.ngw_layer_check_row) {
                        LayoutInflater inflater = LayoutInflater.from(context);
                        v = inflater.inflate(R.layout.row_ngwlayer_check, null);
                        v.setId(R.id.ngw_layer_check_row);
                    }
                    ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(
                            context.getResources().getDrawable(R.drawable.ic_ngw_raster));

                    tvDesc = (TextView) v.findViewById(R.id.tvDesc);
                    tvDesc.setText(context.getString(R.string.web_map));

                    //add check listener
                    checkBox1 = (CheckBox) v.findViewById(R.id.checkBox1);
                    setCheckBox(checkBox1, id, 1);
                    break;
                default:
                    return null;
            }

            TextView tvText = (TextView) v.findViewById(R.id.tvName);
            tvText.setText(resource.getName());
        }

        return v;
    }


    public void setCheckBox(
            final CheckBox checkBox,
            final int id,
            final int checkNo)
    {
        checkBox.setOnCheckedChangeListener(null);
        for (CheckState state : mCheckState) {
            if (checkNo == 1) {
                if (state.getId() == id && state.isCheckState1()) {
                    checkBox.setChecked(true);
                }
            } else if (checkNo == 2) {
                if (state.getId() == id && state.isCheckState2()) {
                    checkBox.setChecked(true);
                }
            }
        }

        checkBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton compoundButton,
                            boolean b)
                    {
                        CheckState checkedState = null;
                        for (CheckState state : mCheckState) {
                            if (state.getId() == id) {
                                checkedState = state;
                                break;
                            }
                        }

                        switch (checkNo) {
                            case 1:
                                if (checkedState != null)
                                    checkedState.setCheckState1(b);
                                else
                                    mCheckState.add(new CheckState(id, true, false));
                                break;
                            case 2:
                                if (checkedState != null)
                                    checkedState.setCheckState2(b);
                                else
                                    mCheckState.add(new CheckState(id, false, true));
                                break;
                        }

                        if (checkedState != null && !checkedState.isCheckState1() && !checkedState.isCheckState2())
                            mCheckState.remove(checkedState);

                        mSelectNGWResourceDialog.updateSelectButton();
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
        if (mCurrentResource.getType() == Connection.NGWResourceTypeConnections) {
            if (i >= mCurrentResource.getChildrenCount()) {
                //start add account activity
                mSelectNGWResourceDialog.onAddAccount(mSelectNGWResourceDialog.getActivity());
            } else {
                Connection connection = (Connection) mCurrentResource.getChild(i);
                mCurrentResource = connection;
                if (connection.isConnected()) {
                    notifyDataSetChanged();
                } else {
                    NGWResourceAsyncTask task = new NGWResourceAsyncTask(
                            mSelectNGWResourceDialog.getActivity(), connection);
                    task.execute();
                }
            }
        } else if (mCurrentResource.getType() == Connection.NGWResourceTypeConnection ||
                   mCurrentResource.getType() == Connection.NGWResourceTypeResourceGroup) {
            if (i == 0) {
                //go up
                INGWResource resource = mCurrentResource.getParent();
                if (resource instanceof Resource) {
                    Resource resourceGroup = (Resource) resource;
                    if (resourceGroup.getRemoteId() == 0) {
                        resource = resource.getParent();
                    }
                }
                mCurrentResource = resource;
                notifyDataSetChanged();
            } else {
                if(mCurrentResource.getChildrenCount() > 0) {
                    //go deep
                    ResourceGroup resourceGroup = (ResourceGroup) mCurrentResource.getChild(i - 1);
                    if (null != resourceGroup) {
                        mCurrentResource = resourceGroup;
                        if (resourceGroup.isChildrenLoaded()) {
                            notifyDataSetChanged();
                        } else {
                            NGWResourceAsyncTask task = new NGWResourceAsyncTask(
                                    mSelectNGWResourceDialog.getActivity(), resourceGroup);
                            task.execute();
                        }
                    }
                }
            }
        }
        mPathView.onUpdate(mCurrentResource);
    }


    /**
     * A path view class. the path is a resources names divide by arrows in head of dialog. If user
     * click on name, the dialog follow the specified path.
     */
    protected class PathView
    {
        protected LinearLayout mLinearLayout;


        public PathView(LinearLayout linearLayout)
        {
            mLinearLayout = linearLayout;
        }


        public void onUpdate(INGWResource mCurrentResource)
        {
            if (null == mLinearLayout || null == mCurrentResource) {
                return;
            }
            mLinearLayout.removeAllViewsInLayout();
            INGWResource parent = mCurrentResource;
            while (null != parent) {
                //skip root resource
                if (parent instanceof Resource) {
                    Resource resource = (Resource) parent;
                    if (resource.getRemoteId() == 0) {
                        parent = parent.getParent();
                        continue;
                    }
                }

                final int id = parent.getId();
                TextView name = new TextView(mSelectNGWResourceDialog.getActivity());
                String sName = parent.getName();
                name.setText(sName);
                name.setTypeface(name.getTypeface(), Typeface.BOLD);
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                name.setSingleLine(true);
                name.setMaxLines(1);
                name.setBackgroundResource(android.R.drawable.list_selector_background);
                name.setOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                setCurrentResourceId(id);
                            }
                        });

                mLinearLayout.addView(name, 0);

                parent = parent.getParent();

                if (null != parent) {
                    ImageView image = new ImageView(mSelectNGWResourceDialog.getActivity());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(30, 30);
                    image.setLayoutParams(params);
                    image.setImageDrawable(
                            mSelectNGWResourceDialog.getActivity()
                                    .getResources()
                                    .getDrawable(R.drawable.ic_next_light));
                    mLinearLayout.addView(image, 0);
                }
            }
        }
    }


    /**
     * A async task to execute resources functions (connect, loadChildren, etc.) asynchronously.
     */
    protected class NGWResourceAsyncTask
            extends AsyncTask<Void, Void, String>
    {
        protected INGWResource mINGWResource;
        protected Context      mContext;


        public NGWResourceAsyncTask(
                Context context,
                INGWResource INGWResource)
        {
            mINGWResource = INGWResource;
            mContext = context;
        }


        @Override
        protected void onPreExecute()
        {
            mLoading = true;
            notifyDataSetChanged();
        }


        @Override
        protected String doInBackground(Void... voids)
        {
            if (mINGWResource instanceof Connection) {
                Connection connection = (Connection) mINGWResource;
                if (connection.connect(NGW_ACCOUNT_GUEST.equals(connection.getLogin()))) {
                    connection.loadChildren();
                } else {
                    return mContext.getString(R.string.error_connect_failed);
                }
            } else if (mINGWResource instanceof ResourceGroup) {
                ResourceGroup resourceGroup = (ResourceGroup) mINGWResource;
                resourceGroup.loadChildren();
            }
            return "";
        }


        @Override
        protected void onPostExecute(String error)
        {
            if (null != error && error.length() > 0) {
                Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show();
            }
            mLoading = false;
            notifyDataSetChanged();
        }
    }
}
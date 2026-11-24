/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *************************************************************************
 * Copyright (c) 2015-2019 NextGIS, info@nextgis.com
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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.AsyncTask;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

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


import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Connections;
import com.nextgis.maplib.datasource.ngw.INGWResource;
import com.nextgis.maplib.datasource.ngw.LayerWithStyles;
import com.nextgis.maplib.datasource.ngw.Resource;
import com.nextgis.maplib.datasource.ngw.ResourceGroup;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.SelectNGWResourceActivity;
import com.nextgis.maplibui.util.CheckState;
import com.nextgis.maplibui.util.ControlHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.NGW_ACCOUNT_GUEST;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;


public class NGWResourcesListAdapter
        extends BaseAdapter
        implements AdapterView.OnItemClickListener
{
    protected Connections             mConnections;
    protected INGWResource            mCurrentResource;
    protected WeakReference<Activity>                mActivity;
    protected boolean                 mLoading;
    protected boolean                 mShowAccounts = true;
    protected boolean                 mShowCheckboxes = true;
    protected PathView                mPathView;
    protected int                     mTypeMask;
    protected List<CheckState>        mCheckState;
    protected OnConnectionListener mConnectionListener;
    boolean skipSubLoad = false;

    public Map<Integer, List<Option>> choosedOptionsRaster = new HashMap<>();
    public Map<Integer, List<Option>> choosedOptionsForms = new HashMap<>();

    // additional options - for raster layers and forms (case - more than one r/f for one layer)
    public static class Option {
        public final Long resId;
        public final String resName;
        public Option(Long resId, String resName) {
            this.resId = resId; this.resName = resName; }
    }

    public interface OnOptionsListener {
        List<Option> getOptions(int resourceId, int checkNo);                 // откуда брать пары
        void onOptionsSelected(int resourceId, int checkNo, List<Option> selected); // сохранить выбор
        void onOptionsCleared(int resourceId, int checkNo);                   // галку сняли, выбор можно стереть
    }

    protected OnOptionsListener mOptionsListener = null;

    public void setOptionsListener(OnOptionsListener l) {
        mOptionsListener = l;
    }

    public interface OnConnectionListener {
        void onConnectionSelected(Connection connection);
        void onAddConnection();
    }

    public NGWResourcesListAdapter(final Activity activity)
    {
        mActivity = new WeakReference<>(activity);
        mLoading = false;


        setOptionsListener(new OnOptionsListener() {
            @Override
            public List<Option> getOptions(int resourceId, int checkNo) {

                List<Option> options = new ArrayList<>();

                INGWResource ingwResource = mCurrentResource.getResourceById(resourceId);
                if (ingwResource != null){
                    if (ingwResource instanceof  LayerWithStyles){
                        LayerWithStyles layer = (LayerWithStyles)ingwResource;
                        if (checkNo == 1){
                            // raster
                            Map<Long ,String> mapRasters = layer.getStyles();
                            if (mapRasters != null ) {
                                for (Map.Entry<Long, String> entry : mapRasters.entrySet()) {
                                    options.add(new Option(
                                            entry.getKey(),
                                            entry.getValue()
                                    ));
                                }
                            }
                        } else if (checkNo == 2){
                            // forms
                            Map<Long ,String> mapForms = layer.getForms();
                            if (mapForms != null ) {
                                for (Map.Entry<Long, String> entry : mapForms.entrySet()) {
                                    options.add(new Option(
                                            entry.getKey(),
                                            entry.getValue()
                                    ));
                                }
                            }
                        }
                    }
                }
                return options;
            }

            @Override
            public void onOptionsSelected(int resourceId, int checkNo, List<Option> selected) {
                if (checkNo == 1)
                    choosedOptionsRaster.put(resourceId, selected);
                else
                    choosedOptionsForms.put(resourceId, selected);
            }

            @Override
            public void onOptionsCleared(int resourceId, int checkNo) {
                if (checkNo == 1)
                    choosedOptionsRaster.remove(resourceId);
                else
                    choosedOptionsForms.remove(resourceId);
            }
        });

    }

    public void setShowAccounts(boolean show) {
        mShowAccounts = show;
    }

    public void setShowCheckboxes(boolean show) {
        mShowCheckboxes = show;
    }

    public boolean isAccountsDisabled() {
        return !mShowAccounts && mCurrentResource instanceof Connection;
    }

    public void setConnectionListener(OnConnectionListener connectionListener) {
        mConnectionListener = connectionListener;
    }

    public void setTypeMask(int typeMask)
    {
        mTypeMask = typeMask;
    }

    public void setConnections(Connections connections, boolean skipSubLoad)
    {
        mConnections = connections;
        this.skipSubLoad = skipSubLoad;
    }

    public Connections getConnections()
    {
        return mConnections;
    }public int getCurrentResourceId()
{
    return mCurrentResource.getId();
}

    public void refresh() {
        if (mCurrentResource instanceof Connection)
            ((Connection) mCurrentResource).getRootResource().setLoadChildren(false);
        else if (mCurrentResource instanceof ResourceGroup)
            ((ResourceGroup) mCurrentResource).setLoadChildren(false);

        final NGWResourceAsyncTask task = new NGWResourceAsyncTask(this, mActivity.get(), mCurrentResource, skipSubLoad, mActivity.get());
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public INGWResource getCurrentResource()
    {
        return mCurrentResource;
    }


    public void setCurrentResourceId(int id)
    {
        mCurrentResource = mConnections.getResourceById(id);
        if (null != mCurrentResource) {
            if (mCurrentResource instanceof Connection) {
                Connection connection = (Connection) mCurrentResource;
                if (mConnectionListener != null) {
                    mConnectionListener.onConnectionSelected(connection);
                } else if (connection.isConnected()) {
                    notifyDataSetChanged();
                } else {
                    NGWResourceAsyncTask task = new NGWResourceAsyncTask(this,  mActivity.get(), connection, skipSubLoad, mActivity.get());
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }

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

        // do not show up button if it is not needed
        if (isAccountsDisabled()) {
            if (mLoading)
                return 1;

            return mCurrentResource.getChildrenCount();
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
            int offset = mShowAccounts ? 1 : 0;
            if (i == 0 && mShowAccounts) {
                return null;
            }
            return mCurrentResource.getChild(i - offset);
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
        //show loading view
        if (mLoading) {
            if (isAccountsDisabled())
                return getLoadingView(view);
            else if (i > 0)
                return getLoadingView(view);
        }switch (mCurrentResource.getType()) {
        case Connection.NGWResourceTypeConnections:
            final Connection connection = (Connection) getItem(i);
            return getConnectionView(connection, view);
        case Connection.NGWResourceTypeConnection:
//            case Connection.NGWResourceTypeCollector:
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
            LayoutInflater inflater = LayoutInflater.from(mActivity.get());
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
        if (null == connection) { //create add account button
            if (null == v || v.getId() != R.id.resourcegroup_row) {
                LayoutInflater inflater = LayoutInflater.from(mActivity.get());
                v = inflater.inflate(R.layout.row_resourcegroup, null);
                v.setId(R.id.resourcegroup_row);
            }
            ImageView ivIcon = v.findViewById(R.id.ivIcon);
            ivIcon.setImageDrawable(ContextCompat.getDrawable(mActivity.get(), R.drawable.ic_add_account));

            TextView tvText = v.findViewById(R.id.tvName);
            tvText.setText(mActivity.get().getString(R.string.ngw_account_add_item));
        } else {
            if (null == v || v.getId() != R.id.resourcegroup_row) {
                LayoutInflater inflater = LayoutInflater.from(mActivity.get());
                v = inflater.inflate(R.layout.row_resourcegroup, null);
                v.setId(R.id.resourcegroup_row);
            }
            ImageView ivIcon = v.findViewById(R.id.ivIcon);
            ivIcon.setImageDrawable(ContextCompat.getDrawable(mActivity.get(), R.drawable.ic_ngw));

            TextView tvText = v.findViewById(R.id.tvName);
            tvText.setText(connection.getName());
        }

        TextView tvDesc = v.findViewById(R.id.tvDesc);
        tvDesc.setVisibility(View.GONE);

        return v;
    }


    protected View getResourceView(
            final Resource resource,
            final View view)
    {
//        Log.e("CRVV", "getResourceView " + resource.getName());
        View v = view;
        TextView tvDesc;
        if (null == resource) { //create up button

            if (null == v || v.getId() != R.id.resourcegroup_row) {

                LayoutInflater inflater = LayoutInflater.from(mActivity.get());
                v = inflater.inflate(R.layout.row_resourcegroup, null);
                v.setId(R.id.resourcegroup_row);

                ImageView ivIcon = v.findViewById(R.id.ivIcon);
                ivIcon.setImageDrawable(ContextCompat.getDrawable(mActivity.get(), R.drawable.ic_ngw_folder));

//                Log.e("CRVV", "Inflate view for  " + resource.getName());
            }

            TextView tvText = v.findViewById(R.id.tvName);
            tvText.setText(mActivity.get().getString(R.string.up_dots));

            tvDesc = v.findViewById(R.id.tvDesc);
            tvDesc.setText(mActivity.get().getString(R.string.up));
        } else {
            ImageView ivIcon;
            CheckBox checkBox1 = null, checkBox2 = null;
            final int id = resource.getId();
            int resourceType = resource.getType();

            if (0 == (mTypeMask & resourceType) &&
                    resourceType != Connection.NGWResourceTypeResourceGroup) {
                if (null == v || v.getId() != R.id.empty_row) {
//                    Log.e("CRVV", "Inflate view for  " + resource.getName());
                    LayoutInflater inflater = LayoutInflater.from(mActivity.get());
                    v = inflater.inflate(R.layout.row_empty, null);
                    v.setId(R.id.empty_row);
                }
                return v;
            }

            switch (resourceType) {
                case Connection.NGWResourceTypeResourceGroup:
                    if (null == v || v.getId() != R.id.resourcegroup_row) {
//                        Log.e("CRVV", "Inflate view for  " + resource.getName());

                        LayoutInflater inflater = LayoutInflater.from(mActivity.get());
                        v = inflater.inflate(R.layout.row_resourcegroup, null);
                        v.setId(R.id.resourcegroup_row);

                        ivIcon = v.findViewById(R.id.ivIcon);
                        ivIcon.setImageDrawable(ContextCompat.getDrawable(mActivity.get(), R.drawable.ic_ngw_folder));
                    }

                    tvDesc = v.findViewById(R.id.tvDesc);
                    tvDesc.setText(mActivity.get().getString(R.string.resource_group));
                    break;
                case Connection.NGWResourceTypeRasterLayer:
                case Connection.NGWResourceTypeWMSClient:
                    if (null == v || v.getId() != R.id.ngw_layer_check_row) {
//                        Log.e("CRVV", "Inflate view for  " + resource.getName());

                        LayoutInflater inflater = LayoutInflater.from(mActivity.get());
                        v = inflater.inflate(R.layout.row_ngwlayer_check, null);
                        v.setId(R.id.ngw_layer_check_row);
                    }
                    ivIcon = v.findViewById(R.id.ivIcon);
                    int icon = resourceType == Connection.NGWResourceTypeRasterLayer ? R.drawable.ic_raster : R.drawable.ic_ngw_wms_client;
                    ivIcon.setImageDrawable(ContextCompat.getDrawable(mActivity.get(), icon));

                    tvDesc = v.findViewById(R.id.tvDesc);
                    tvDesc.setText(mActivity.get().getString(R.string.raster_layer));

                    //add check listener
                    checkBox1 = v.findViewById(R.id.checkBox1);
                    setCheckBox(resourceType, checkBox1, id, 1);
                    break;
                case Connection.NGWResourceTypeVectorLayer:
                case Connection.NGWResourceTypePostgisLayer:
                    LayerWithStyles layer = (LayerWithStyles) resource;
                    if (layer.getStyleCount() > 0) {
                        if (null == v || v.getId() != R.id.ngw_layer_doublecheck_row) {
//                            Log.e("CRVV", "Inflate view for  " + resource.getName());

                            LayoutInflater inflater = LayoutInflater.from(mActivity.get());
                            v = inflater.inflate(R.layout.row_ngwlayer_doublecheck, null);
                            v.setId(R.id.ngw_layer_doublecheck_row);
                        }

                        //add check listener
                        checkBox1 = v.findViewById(R.id.checkBox1);
                        setCheckBox(resourceType, checkBox1, id, 1);

                        checkBox2 = v.findViewById(R.id.checkBox2);
                        setCheckBox(resourceType, checkBox2, id, 2);
                    } else {
                        if (null == v || v.getId() != R.id.ngw_layer_check_row) {
//                            Log.e("CRVV", "Inflate view for  " + resource.getName());

                            LayoutInflater inflater = LayoutInflater.from(mActivity.get());
                            v = inflater.inflate(R.layout.row_ngwlayer_check, null);
                            v.setId(R.id.ngw_layer_check_row);
                        }

                        TextView tvType = v.findViewById(R.id.type1);
                        tvType.setText(mActivity.get().getString(R.string.vector));//add check listener
                        checkBox1 = v.findViewById(R.id.checkBox1);
                        setCheckBox(resourceType, checkBox1, id, 2);
                    }

                    int vectorIcon = resourceType == Connection.NGWResourceTypeVectorLayer ? R.drawable.ic_vector : R.drawable.ic_pg_vector;
                    ivIcon = v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(ContextCompat.getDrawable(mActivity.get(), vectorIcon));

                    int desc = resourceType == Connection.NGWResourceTypeVectorLayer ? R.string.vector_layer : R.string.pg_layer;
                    tvDesc = v.findViewById(R.id.tvDesc);
                    tvDesc.setText(mActivity.get().getString(desc));
                    break;
                case Connection.NGWResourceTypeWebMap:
                    if (null == v || v.getId() != R.id.ngw_layer_check_row) {
//                        Log.e("CRVV", "Inflate view for  " + resource.getName());

                        LayoutInflater inflater = LayoutInflater.from(mActivity.get());
                        v = inflater.inflate(R.layout.row_ngwlayer_check, null);
                        v.setId(R.id.ngw_layer_check_row);
                    }
                    ivIcon = v.findViewById(R.id.ivIcon);
                    ivIcon.setImageDrawable(ContextCompat.getDrawable(mActivity.get(), R.drawable.ic_ngw_webmap));

                    tvDesc = v.findViewById(R.id.tvDesc);
                    tvDesc.setText(mActivity.get().getString(R.string.web_map));

                    //add check listener
                    checkBox1 = v.findViewById(R.id.checkBox1);
                    setCheckBox(resourceType, checkBox1, id, 1);
                    break;
                default:
                    return null;
            }

            tvDesc.setVisibility(View.GONE);

            if (!mShowCheckboxes) {
                if (checkBox1 != null)
                    v.findViewById(R.id.check_raster).setVisibility(View.GONE);
                if (checkBox2 != null)
                    v.findViewById(R.id.check_vector).setVisibility(View.GONE);
//                if (checkBox3 != null)
//                    v.findViewById(R.id.check_fb).setVisibility(View.GONE);
            }

            TextView tvText = v.findViewById(R.id.tvName);
            tvText.setText(resource.getName());
        }

        return v;
    }

    public void setCheckBox(
            final int resourceType,
            final CheckBox checkBox,
            final int id,
            final int checkNo)
    {
        checkBox.setOnCheckedChangeListener(null);

        CheckState current = findState(id);
        boolean checked = false;
        if (current != null)
            checked = checkNo == 1 ? current.isCheckState1() : current.isCheckState2();
        checkBox.setChecked(checked);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean b) {
                if (b) {
                    if (resourceType == Connection.NGWResourceTypePostgisLayer
                            && !AccountUtil.isUserExists(mActivity.get())) {
                        ControlHelper.showNoLoginDialog(mActivity.get());
                        revertToUnchecked(button);
                        return;
                    }


                    final List<Option> options = mOptionsListener.getOptions(id, checkNo);
                    if (options.size() > 1) { // if one option - just check
                        revertToUnchecked(button);
                        if (checkNo == 1)
                            showOptionsDialogForRasters(id, checkNo, checkNo == 1? R.string.raster_title : R.string.forms_title );
                        else
                            showOptionsDialogForForms(id, checkNo, checkNo == 1? R.string.raster_title : R.string.forms_title );
                    } else {
                        applyCheck(id, checkNo, true);
                    }

                } else {
                    applyCheck(id, checkNo, false);
                    if (mOptionsListener != null)
                        mOptionsListener.onOptionsCleared(id, checkNo);
                }
            }
        });
    }

    private void revertToUnchecked(CompoundButton button) {
        CompoundButton.OnCheckedChangeListener l = null;
        button.setOnCheckedChangeListener(null);
        button.setChecked(false);
        // listener be back at next getView/setCheckBox
        // anywhere - applyCheck + notifyDataSetChanged after dialog
    }

    private CheckState findState(int id) {
        for (CheckState state : mCheckState)
            if (state.getId() == id)
                return state;
        return null;
    }

    private void applyCheck(int id, int checkNo, boolean value) {
        CheckState state = findState(id);
        if (state == null) {
            if (!value)
                return;
            mCheckState.add(new CheckState(id, checkNo == 1, checkNo == 2));
            return;
        }
        if (checkNo == 1)
            state.setCheckState1(value);
        else if (checkNo == 2)
            state.setCheckState2(value);

        if (!state.isCheckState1() && !state.isCheckState2())
            mCheckState.remove(state);
    }

    private void showOptionsDialogForRasters(final int id, final int checkNo, int titleResId) {
        final Activity activity = mActivity.get();
        if (activity == null || mOptionsListener == null)
            return;

        final List<Option> options = mOptionsListener.getOptions(id, checkNo);
        if (options == null || options.isEmpty())
            return;

        String[] titles = new String[options.size()];

        final boolean[] selected = new boolean[options.size()];

        for (int i = 0; i < options.size(); i++) {
            titles[i] = options.get(i).resName;
            selected[i] = false;
        }
        selected[0] = true; // first selected by default

        new AlertDialog.Builder(activity)
                .setTitle(titleResId )
                .setMultiChoiceItems(titles, selected,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which, boolean isChecked) {
                                selected[which] = isChecked;
                            }
                        })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        List<Option> result = new ArrayList<>();
                        for (int i = 0; i < selected.length; i++)
                            if (selected[i])
                                result.add(options.get(i));

                        if (result.isEmpty())
                            return; // no select - no check

                        applyCheck(id, checkNo, true);
                        mOptionsListener.onOptionsSelected(id, checkNo, result);
                        notifyDataSetChanged(); // redraw checks from  mCheckState
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        notifyDataSetChanged(); // redraw checks from  mCheckState
                    }
                })
                .show();
    }

    private void showOptionsDialogForForms(final int id, final int checkNo, int titleResId) {
        final Activity activity = mActivity.get();
        if (activity == null || mOptionsListener == null)
            return;

        final List<Option> options = mOptionsListener.getOptions(id, checkNo);

        String[] titles = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            titles[i] = options.get(i).resName;
        }

        final int[] selected = { 0 }; // default 1 case

        new AlertDialog.Builder(activity)
                .setTitle(titleResId)
                .setSingleChoiceItems(titles, selected[0],
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                selected[0] = which;
                            }
                        })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        List<Option> result = new ArrayList<>();
                        result.add(options.get(selected[0]));

                        applyCheck(id, checkNo, true);
                        mOptionsListener.onOptionsSelected(id, checkNo, result);
                        notifyDataSetChanged(); //  redraw checks from  mCheckState
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        notifyDataSetChanged(); //
                    }
                })
                .show();
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
                if (mConnectionListener != null)
                    mConnectionListener.onAddConnection();
            } else {
                Connection connection = (Connection) mCurrentResource.getChild(i);
                mCurrentResource = connection;if (mConnectionListener != null) {
                    mConnectionListener.onConnectionSelected(connection);
                } else if (connection.isConnected()) {
                    notifyDataSetChanged();
                } else {
                    NGWResourceAsyncTask task = new NGWResourceAsyncTask(this,mActivity.get(), connection, skipSubLoad, mActivity.get());
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        } else if (mCurrentResource.getType() == Connection.NGWResourceTypeConnection) {
            if (mShowAccounts) {
                if (i == 0)
                    goUp();
                else
                    goDeep(i - 1);
            } else
                goDeep(i);
        } else if (mCurrentResource.getType() == Connection.NGWResourceTypeResourceGroup) {
            if (i == 0)
                goUp();
            else
                goDeep(i - 1);
        }
        mPathView.onUpdate(mCurrentResource);
    }

    public void goUp() {
        INGWResource resource = mCurrentResource.getParent();
        if (resource instanceof Resource) {
            Resource resourceGroup = (Resource) resource;
            if (resourceGroup.getRemoteId() == 0) {
                resource = resource.getParent();
            }
        }

        mCurrentResource = resource;
        notifyDataSetChanged();
    }

    private void goDeep(int position) {
        if (mCurrentResource.getChildrenCount() > 0) {
            INGWResource resource = mCurrentResource.getChild(position);

            if (resource instanceof ResourceGroup) {
                ResourceGroup resourceGroup = (ResourceGroup) resource;
                mCurrentResource = resourceGroup;
                if (resourceGroup.isChildrenLoaded()) {
                    notifyDataSetChanged();
                } else {
                    NGWResourceAsyncTask task = new NGWResourceAsyncTask(this, mActivity.get(), resourceGroup, skipSubLoad, mActivity.get());
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }
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
                if (!mShowAccounts && parent instanceof Connections)
                    break;

                //skip root resource
                if (parent instanceof Resource) {
                    Resource resource = (Resource) parent;
                    if (resource.getRemoteId() == 0) {
                        parent = parent.getParent();
                        continue;
                    }
                }

                final int id = parent.getId();
                TextView name = new TextView(mActivity.get());String sName = parent.getName();
                if (parent instanceof Connection && !mShowAccounts)
                    sName = "/ ";
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
                    ImageView image = new ImageView(mActivity.get());
                    int px = ControlHelper.dpToPx(16, mActivity.get().getResources());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(px, px);
                    image.setLayoutParams(params);
                    image.setImageDrawable(ContextCompat.getDrawable(mActivity.get(), R.drawable.ic_next_light));
                    mLinearLayout.addView(image, 0);
                }
            }
        }
    }


    /**
     * A async task to execute resources functions (connect, loadChildren, etc.) asynchronously.
     */
    static class NGWResourceAsyncTask
            extends AsyncTask<Void, Void, String>
    {
        private final WeakReference<NGWResourcesListAdapter> adapterRef;
        private final WeakReference<Context> mContext;
        private final WeakReference<Activity> mActivity;
        private final INGWResource resource;
        boolean skipSubLoad = false;


        NGWResourceAsyncTask(
                NGWResourcesListAdapter adapter,
                Context context,
                INGWResource resource,
                boolean skipSubLoad,
                Activity activity) {
            adapterRef = new WeakReference<>(adapter);
            mContext = new WeakReference<>(context.getApplicationContext());
            mActivity= new WeakReference<>(activity);
            this.resource = resource;
            this.skipSubLoad = skipSubLoad;
        }


        @Override
        protected void onPreExecute()
        {
            adapterRef.get().mLoading = true;
            adapterRef.get().notifyDataSetChanged();
            if (mActivity.get() instanceof SelectNGWResourceActivity)
                ((SelectNGWResourceActivity) mActivity.get()).disableButton();
        }


        @Override
        protected String doInBackground(Void... voids)
        {
            if (mContext.get() == null)
                return "";
            if (resource instanceof Connection) {
                final Connection connection = (Connection) resource;
                if (connection.connect(NGW_ACCOUNT_GUEST.equals(connection.getLogin()))) {
                    connection.loadChildren(skipSubLoad);
                } else {
                    return mContext.get().getString(com.nextgis.maplib.R.string.error_connect_failed);
                }
            } else if (resource instanceof ResourceGroup) {
                final ResourceGroup resourceGroup = (ResourceGroup) resource;
                resourceGroup.loadChildren(skipSubLoad);
            }
            return "";
        }


        @Override
        protected void onPostExecute(String error)
        {
            if (null != error && error.length() > 0 && mActivity.get() != null) {
                new AlertDialog.Builder(mActivity.get())
                        .setMessage(error)
                        .setPositiveButton(R.string.ok, null)
                        .create()
                        .show();
                //Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show();
            }
            if (adapterRef.get() != null) {
                adapterRef.get().mLoading = false;
                adapterRef.get().notifyDataSetChanged();
            }

            if (mActivity.get() != null && mActivity.get() instanceof  SelectNGWResourceActivity)
                ((SelectNGWResourceActivity) mActivity.get()).enableButton();

        }
    }//                case Connection.NGWResourceTypeCollector:
//                    CollectorResource collectorResource = (CollectorResource) resource;
//
//                    if (null == v || v.getId() != R.id.ngw_layer_check_row) {
//                        LayoutInflater inflater = LayoutInflater.from(mActivity);
//                        v = inflater.inflate(R.layout.row_ngwlayer_check, null);
//                        v.setId(R.id.ngw_layer_check_row);
//                    }
//
//                    TextView tvType = v.findViewById(R.id.type1);
//                    tvType.setText(mActivity.getString(R.string.collector));
//
//                    //add check listener
//                    checkBox1 = v.findViewById(R.id.checkBox1);
//                    setCheckBox(resourceType, checkBox1, id, 2);
//
//
//                    int vIcon = R.mipmap.ic_collector;
//                    ivIcon = v.findViewById(R.id.ivIcon);
//                    ivIcon.setImageDrawable(ContextCompat.getDrawable(mActivity, vIcon));
//
//                    int desc1 = R.string.collector;
//                    tvDesc = v.findViewById(R.id.tvDesc);
//                    tvDesc.setText(mActivity.getString(desc1));
//                    break;

}
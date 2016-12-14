/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.display;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.display.FieldStyleRule;
import com.nextgis.maplib.display.RuleFeatureRenderer;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.dialog.StyledDialogFragment;
import com.nextgis.maplibui.fragment.StyleFragment;

import java.util.ArrayList;
import java.util.List;

public class RuleFeatureRendererUI extends RendererUI {
    private static final String STYLE_DIALOG_FRAGMENT = "STYLE_DIALOG_FRAGMENT";
    private static VectorLayer mLayer;
    private static Style mStyle;

    public RuleFeatureRendererUI(RuleFeatureRenderer renderer, VectorLayer layer) {
        mLayer = layer;
        mRenderer = renderer;
        mStyle = renderer.getStyle();
    }

    @Override
    public Fragment getSettingsScreen(VectorLayer vectorLayer) {
        mSettings = super.getSettingsScreen(vectorLayer);

        if (mSettings == null) {
            RuleStyleFragment fragment = new RuleStyleFragment();
            fragment.mRenderer = (RuleFeatureRenderer) mRenderer;
            fragment.mStyleRule = (FieldStyleRule) fragment.mRenderer.getStyleRule();

            if (fragment.mStyleRule == null) {
                fragment.mStyleRule = new FieldStyleRule(mLayer);
                fragment.mRenderer.setStyleRule(fragment.mStyleRule);
            }

            mSettings = fragment;
        }

        return mSettings;
    }

    public static class RuleStyleFragment extends Fragment {
        private RuleFeatureRenderer mRenderer;
        private FieldStyleRule mStyleRule;
        private SimpleCursorAdapter mValueAdapter;
        private Cursor mData;
        private String mSelectedField, mSelectedValue;
        private RulesAdapter mRulesAdapter;
        private List<String> mRulesList;
        private ListView mRulesListView;
        private List<Field> mFields;

        public RuleStyleFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.style_rules, container, false);

            if (mStyleRule == null) {
                Toast.makeText(getContext(), R.string.error_layer_not_inited, Toast.LENGTH_SHORT).show();
                return v;
            }

            Button defaultStyle = (Button) v.findViewById(R.id.default_style);
            defaultStyle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showStyleDialog(null);
                }
            });

            mRulesList = new ArrayList<>();
            for (String rule : mStyleRule.getStyleRules().keySet())
                mRulesList.add(rule);

            mRulesAdapter = new RulesAdapter(getContext());
            mRulesListView = (ListView) v.findViewById(R.id.rules);
            mRulesListView.setAdapter(mRulesAdapter);
            mRulesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mSelectedValue = mRulesAdapter.getItem(position);
                    showStyleDialog(mSelectedValue);
                }
            });

            int id = -1;
            String key = mStyleRule.getKey();
            if (key == null)
                key = Constants.FIELD_ID;

            mFields = mLayer.getFields();
            mFields.add(0, new Field(GeoConstants.FTInteger, Constants.FIELD_ID, Constants.FIELD_ID));
            final List<String> fieldNames = new ArrayList<>();
            for (int i = 0; i < mFields.size(); i++) {
                fieldNames.add(mFields.get(i).getAlias());
                if (key.equals(mFields.get(i).getName()))
                    id = i;
            }

            ArrayAdapter fieldAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, fieldNames);
            final Spinner fieldSpinner = (Spinner) v.findViewById(R.id.field);
            fieldSpinner.setAdapter(fieldAdapter);
            fieldSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                    final String newField = mFields.get(position).getName();
                    if (mSelectedField == null) {
                        mSelectedField = newField;
                        fillFieldValues();
                        return;
                    }

                    if (newField.equals(mSelectedField))
                        return;

                    if (mStyleRule.size() == 0) {
                        mSelectedField = newField;
                        fillFieldValues();
                        return;
                    }

                    AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                    alert.setTitle(R.string.are_you_sure).setMessage(R.string.replace_field_rule)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mStyleRule.clearRules();
                                    mRulesList.clear();
                                    mRulesAdapter.notifyDataSetChanged();
                                    setListViewHeightBasedOnChildren();
                                    mSelectedField = newField;
                                    fillFieldValues();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    fieldSpinner.setSelection(getPositionForField(mSelectedField));
                                }
                            });
                    alert.create().show();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            if (id != -1)
                fieldSpinner.setSelection(id);

            Button newRule = (Button) v.findViewById(R.id.new_rule);
            newRule.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.value).setSingleChoiceItems(mData, -1, mSelectedField, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (mData.moveToPosition(which))
                                        mSelectedValue = mData.getString(1);
                                }
                            })
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (mSelectedValue != null)
                                        showStyleDialog(mSelectedValue);
                                    else
                                        Toast.makeText(getContext(), R.string.nothing_selected, Toast.LENGTH_SHORT).show();
                                }
                            }).setNegativeButton(android.R.string.cancel, null);

                    mSelectedValue = null;
                    builder.create().show();
                }
            });

            setListViewHeightBasedOnChildren();
            return v;
        }

        private int getPositionForField(String field) {
            for (int i = 0; i < mFields.size(); i++)
                if (mFields.get(i).getName().equals(field))
                    return i;

            return -1;
        }

        private void fillFieldValues() {
            String[] column = new String[]{Constants.FIELD_ID, mSelectedField};
            String[] from = new String[]{mSelectedField};
            int[] to = new int[]{android.R.id.text1};

            MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
            SQLiteDatabase db = map.getDatabase(true);
            mData = db.query(true, mLayer.getPath().getName(), column, null, null, column[1], null, null, null);
            mValueAdapter = new SimpleCursorAdapter(getContext(), android.R.layout.simple_spinner_item, mData, from, to, 0);
            mValueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mStyleRule.setKey(mSelectedField);
        }

        private void showStyleDialog(final String value) {
            try {
                final Style style = value == null ? mStyle : mRulesList.contains(mSelectedValue) ? mStyleRule.getStyle(mSelectedValue) : mStyle.clone();

                FragmentManager fm = getActivity().getSupportFragmentManager();
                final StyleFragment styleFragment = new StyleFragment();
                styleFragment.setLayer(mLayer);
                styleFragment.setStyle(style);
                styleFragment.setTitle(R.string.style);
                styleFragment.setPositiveText(android.R.string.ok);
                styleFragment.setOnPositiveClickedListener(new StyledDialogFragment.OnPositiveClickedListener() {
                    @Override
                    public void onPositiveClicked() {
                        if (value != null) {
                            if (!mRulesList.contains(mSelectedValue)) {
                                mRulesList.add(mSelectedValue);
                                mRulesAdapter.notifyDataSetChanged();
                                setListViewHeightBasedOnChildren();
                            }

                            mStyleRule.setStyle(mSelectedValue, style);
                        }

                        styleFragment.dismiss();
                    }
                });
                styleFragment.setNegativeText(android.R.string.cancel);
                styleFragment.setOnNegativeClickedListener(new StyledDialogFragment.OnNegativeClickedListener() {
                    @Override
                    public void onNegativeClicked() {
                        styleFragment.dismiss();
                    }
                });
                styleFragment.show(fm, STYLE_DIALOG_FRAGMENT);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        // http://stackoverflow.com/a/19311197/2088273
        /**** Method for Setting the Height of the ListView dynamically.
         **** Hack to fix the issue of not showing all the items of the ListView
         **** when placed inside a ScrollView  ****/
        private void setListViewHeightBasedOnChildren() {
            if (mRulesAdapter == null)
                return;

            int desiredWidth = View.MeasureSpec.makeMeasureSpec(mRulesListView.getWidth(), View.MeasureSpec.UNSPECIFIED);
            int totalHeight = 0;
            View view = null;
            for (int i = 0; i < mRulesAdapter.getCount(); i++) {
                view = mRulesAdapter.getView(i, view, mRulesListView);
                if (i == 0)
                    view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

                view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                totalHeight += view.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = mRulesListView.getLayoutParams();
            params.height = totalHeight + (mRulesListView.getDividerHeight() * (mRulesAdapter.getCount() - 1));
            mRulesListView.setLayoutParams(params);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        private class RulesAdapter extends ArrayAdapter<String> {

            RulesAdapter(Context context) {
                super(context, R.layout.rule_item, android.R.id.text1, mRulesList);
            }

            @NonNull
            @Override
            public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                final ImageButton remove = (ImageButton) view.findViewById(R.id.rule_remove);
                remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String value = getItem(position);
                        mRulesList.remove(value);
                        mRulesAdapter.notifyDataSetChanged();
                        setListViewHeightBasedOnChildren();
                        mStyleRule.removeStyle(value);
                    }
                });

                return view;
            }
        }
    }
}

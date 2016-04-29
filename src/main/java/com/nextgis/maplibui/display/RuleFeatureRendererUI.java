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

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.Spinner;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.display.FieldStyleRule;
import com.nextgis.maplib.display.RuleFeatureRenderer;
import com.nextgis.maplib.display.Style;
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
    public Fragment getSettingsScreen() {
        mSettings = super.getSettingsScreen();
        return mSettings == null ? mSettings = new RuleStyleFragment() : mSettings;
    }

    public static class RuleStyleFragment extends Fragment {
        protected static RuleFeatureRenderer mRenderer;
        protected static FieldStyleRule mStyleRule;
        SimpleCursorAdapter valueAdapter;
        Cursor data;
        String selectedField, selectedValue;
        ArrayAdapter rulesAdapter;
        List<String> rulesList;
        ListView rules;

        public RuleStyleFragment() {
            mRenderer = (RuleFeatureRenderer) RuleFeatureRendererUI.mRenderer;
            mStyleRule = (FieldStyleRule) mRenderer.getStyleRule();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.style_rules, container, false);

            Button defaultStyle = (Button) v.findViewById(R.id.default_style);
            defaultStyle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showStyleDialog(null);
                }
            });

            rulesList = new ArrayList<>();
            rulesAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, rulesList);
            rules = (ListView) v.findViewById(R.id.rules);
            rules.setAdapter(rulesAdapter);

            final List<Field> fields = mLayer.getFields();
            fields.add(0, new Field(GeoConstants.FTInteger, Constants.FIELD_ID, getString(R.string.id)));
            List<String> fieldNames = new ArrayList<>();
            for (Field field : fields)
                fieldNames.add(field.getAlias());

            ArrayAdapter fieldAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, fieldNames);
            Spinner fieldSpinner = (Spinner) v.findViewById(R.id.field);
            fieldSpinner.setAdapter(fieldAdapter);
            fieldSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedField = fields.get(position).getName();
                    String[] column = new String[]{Constants.FIELD_ID, selectedField};
                    String[] from = new String[]{selectedField};
                    int[] to = new int[]{android.R.id.text1};
                    data = mLayer.query(column, null, null, null, null);
                    valueAdapter = new SimpleCursorAdapter(getContext(), android.R.layout.simple_spinner_item, data, from, to, 0);
                    valueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            Button newRule = (Button) v.findViewById(R.id.new_rule);
            newRule.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.value).setSingleChoiceItems(data, -1, selectedField, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (data.moveToPosition(which))
                                        selectedValue = data.getString(1);
                                }
                            })
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    showStyleDialog(selectedValue);
                                }
                            }).setNegativeButton(android.R.string.cancel, null);

                    builder.create().show();
                }
            });

            return v;
        }

        private void showStyleDialog(final String value) {
            try {
                final Style style = value == null ? mStyle : mStyle.clone();

                FragmentManager fm = getActivity().getSupportFragmentManager();
                final StyleFragment styleFragment = new StyleFragment();
                styleFragment.setStyle(style);
                styleFragment.setTitle(R.string.style);
                styleFragment.setPositiveText(android.R.string.ok);
                styleFragment.setOnPositiveClickedListener(new StyledDialogFragment.OnPositiveClickedListener() {
                    @Override
                    public void onPositiveClicked() {
                        if (value != null) {
                            if (!rulesList.contains(selectedValue)) {
                                rulesList.add(selectedValue);
                                rulesAdapter.notifyDataSetChanged();
                                rules.invalidateViews();
                            }

                            if (mStyleRule == null) {
                                mStyleRule = new FieldStyleRule(mLayer, null);
                                mRenderer.setStyleRule(mStyleRule);
                            }

                            mStyleRule.setKey(selectedField);
                            mStyleRule.setStyle(selectedValue, style);
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

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }
}

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.util;

import com.inqbarna.tablefixheaders.adapters.BaseTableAdapter;
import com.nextgis.maplibui.R;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import androidx.annotation.NonNull;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MatrixTableAdapter<T> extends BaseTableAdapter {
	private View.OnClickListener mListener;

	private final static int WIDTH_DIP = 110;
	private final static int HEIGHT_DIP = 32;

	private final Context context;

	private T[][] table;

	private final int width;
	private final int height;

	public MatrixTableAdapter(Context context) {
		this(context, null);
	}

	public MatrixTableAdapter(Context context, T[][] table) {
		this.context = context;
		Resources r = context.getResources();

		width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, WIDTH_DIP, r.getDisplayMetrics()));
		height = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, HEIGHT_DIP, r.getDisplayMetrics()));

		setInformation(table);
	}

	public void setOnClickListener(@NonNull View.OnClickListener listener) {
		mListener = listener;
	}

	public void setInformation(T[][] table) {
		this.table = table;
	}

	@Override
	public int getRowCount() {
		return table.length - 1;
	}

	@Override
	public int getColumnCount() {
		return table[0].length - 1;
	}

	private static void setPadding(Context context, TextView view) {
		int padding = ControlHelper.dpToPx(2, context.getResources());
		view.setPadding(padding, padding, padding, padding);
		view.setGravity(Gravity.CENTER_VERTICAL);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
		}
	}

	@Override
	public View getView(int row, int column, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = new TextView(context);
			setPadding(context, (TextView) convertView);
		}

		convertView.setOnClickListener(mListener);
		convertView.setTag(R.id.text1, table[row + 1][0]);
		((TextView) convertView).setText(table[row + 1][column + 1].toString());
		return convertView;
	}

	public static int getHeight(Context context, String text, int deviceWidth) {
		TextView textView = new TextView(context);
		setPadding(context, textView);

		textView.setText(text);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

		int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(deviceWidth, View.MeasureSpec.AT_MOST);
		int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

		textView.measure(widthMeasureSpec, heightMeasureSpec);
		return textView.getMeasuredHeight();
	}

	@Override
	public int getHeight(int row) {
		int max = height;
		for (T value : table[row + 1]) {
			max = Math.max(getHeight(context, value.toString(), width), max);
		}
		return max;
	}

	@Override
	public int getWidth(int column) {
		return width;
	}

	@Override
	public int getItemViewType(int row, int column) {
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}
}

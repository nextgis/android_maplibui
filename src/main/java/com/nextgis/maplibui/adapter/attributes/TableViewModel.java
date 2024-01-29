/*
 * MIT License
 *
 * Copyright (c) 2021 Evren Coşkun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nextgis.maplibui.adapter.attributes;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;


import com.nextgis.maplibui.R;

import java.util.ArrayList;
import java.util.List;


public class TableViewModel {

    private final int COLUMN_SIZE;
    private final int ROW_SIZE ;

    private final  String [][] data;
    private final String [] data0row;
    private final  String [] data0col;


    // Drawables
    @DrawableRes
    private final int mBallDrawable;


    public TableViewModel(final int colSize, final int rowSize,
                          final String [][] data,
                          final String [] data0row,
                          final String [] data0col){
        COLUMN_SIZE = colSize;
        ROW_SIZE = rowSize;
        this.data = data;
        this.data0row = data0row;
        this.data0col = data0col;

        mBallDrawable = R.drawable.ball;
    }

    @NonNull
    private List<RowHeader> getSimpleRowHeaderList() {
        List<RowHeader> list = new ArrayList<>();
        for (int i = 0; i < ROW_SIZE; i++) {
            RowHeader header = new RowHeader(String.valueOf(i), data0col[i] );
            list.add(header);
        }

        return list;
    }

    /**
     * This is a dummy model list test some cases.
     */
    @NonNull
    private List<ColumnHeader> getRandomColumnHeaderList() {
        List<ColumnHeader> list = new ArrayList<>();

        for (int i = 0; i < COLUMN_SIZE; i++) {
            ColumnHeader header = new ColumnHeader(String.valueOf(i), data0row[i]);
            list.add(header);
        }
        return list;
    }

    /**
     * This is a dummy model list test some cases.
     */
    @NonNull
    final private List<List<Cell>> getCellListForSortingTest() {
        final List<List<Cell>> list = new ArrayList<>();
        for (int i = 0; i < ROW_SIZE; i++) {
            final List<Cell> cellList = new ArrayList<>();
            for (int j = 0; j < COLUMN_SIZE; j++) {
                final String text = data[i][j];
                final Cell cell;
                cell = new Cell(data0col[i], text);
                cellList.add(cell);
            }
            list.add(cellList);
        }
        return list;
    }

    @DrawableRes
    public int getDrawable() {
        return mBallDrawable;
    }

    @NonNull
    final public List<List<Cell>> getCellList() {
        return getCellListForSortingTest();
    }

    @NonNull
    public List<RowHeader> getRowHeaderList() {
        return getSimpleRowHeaderList();
    }

    @NonNull
    public List<ColumnHeader> getColumnHeaderList() {
        return getRandomColumnHeaderList();
    }
}
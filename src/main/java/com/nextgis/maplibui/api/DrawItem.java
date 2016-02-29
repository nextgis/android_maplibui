/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016 NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.api;

import android.graphics.PointF;

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;

import java.util.ArrayList;
import java.util.List;

public class DrawItem {
    public static final int TYPE_VERTEX = 1;
    public static final int TYPE_EDGE   = 2;

    protected List<float[]> mDrawItemsVertex;
    protected List<float[]> mDrawItemsEdge;
    protected int mSelectedRing = 0, mSelectedPoint = 0;

    public DrawItem() {
        mDrawItemsVertex = new ArrayList<>();
        mDrawItemsEdge = new ArrayList<>();
    }

    public DrawItem(int type, float[] points) {
        this();

        if (type == TYPE_VERTEX)
            addVertices(points);
        else if (type == TYPE_EDGE)
            addEdges(points);
    }

    public DrawItem zoom(PointF location, float scale) {
        DrawItem drawItem = new DrawItem();
        drawItem.setSelectedRing(mSelectedRing);
        drawItem.setSelectedPoint(mSelectedPoint);

        for (float[] items : mDrawItemsVertex) {
            float[] newItems = new float[items.length];
            for (int i = 0; i < items.length - 1; i += 2) {
                newItems[i] = items[i] - (1 - scale) * (items[i] + location.x);
                newItems[i + 1] = items[i + 1] - (1 - scale) * (items[i + 1] + location.y);
            }
            drawItem.addVertices(newItems);
        }

        for (float[] items : mDrawItemsEdge) {
            float[] newItems = new float[items.length];
            for (int i = 0; i < items.length - 1; i += 2) {
                newItems[i] = items[i] - (1 - scale) * (items[i] + location.x);
                newItems[i + 1] = items[i + 1] - (1 - scale) * (items[i + 1] + location.y);
            }
            drawItem.addEdges(newItems);
        }

        return drawItem;
    }

    public DrawItem pan(PointF offset) {
        DrawItem drawItem = new DrawItem();
        drawItem.setSelectedRing(mSelectedRing);
        drawItem.setSelectedPoint(mSelectedPoint);

        for (float[] items : mDrawItemsVertex) {
            float[] newItems = new float[items.length];
            for (int i = 0; i < items.length - 1; i += 2) {
                newItems[i] = items[i] - offset.x;
                newItems[i + 1] = items[i + 1] - offset.y;
            }
            drawItem.addVertices(newItems);
        }

        for (float[] items : mDrawItemsEdge) {
            float[] newItems = new float[items.length];
            for (int i = 0; i < items.length - 1; i += 2) {
                newItems[i] = items[i] - offset.x;
                newItems[i + 1] = items[i + 1] - offset.y;
            }
            drawItem.addEdges(newItems);
        }

        return drawItem;
    }

    public void addVertices(float[] points) {
        mDrawItemsVertex.add(points);
    }

    public void addEdges(float[] points) {
        mDrawItemsEdge.add(points);
    }

    public List<float[]> getEdges() {
        return mDrawItemsEdge;
    }

    public void addNewPoint(float x, float y) {
        float[] points = getSelectedRing();
        if (null == points) {
            return;
        }
        float[] newPoints = new float[points.length + 2];
        System.arraycopy(points, 0, newPoints, 0, points.length);
        newPoints[points.length] = x;
        newPoints[points.length + 1] = y;

        setRing(mSelectedRing, newPoints);
    }

    public void insertNewPoint(int insertPosition, float x, float y) {
        float[] points = getSelectedRing();
        if (null == points) {
            return;
        }
        float[] newPoints = new float[points.length + 2];
        int count = 0;
        for (int i = 0; i < newPoints.length - 1; i += 2) {
            if (i == insertPosition) {
                newPoints[i] = x;
                newPoints[i + 1] = y;
            } else {
                newPoints[i] = points[count++];
                newPoints[i + 1] = points[count++];
            }
        }

        setRing(mSelectedRing, newPoints);
    }

    public void setSelectedPointCoordinates(float x, float y) {
        float[] points = mDrawItemsVertex.get(mSelectedRing);
        if (null != points && mSelectedPoint >= 0 && mSelectedPoint < points.length - 1) {
            points[mSelectedPoint] = x;
            points[mSelectedPoint + 1] = y;
        }
    }

    public PointF getSelectedPoint() {
        float[] points = getSelectedRing();
        if (null != points &&  mSelectedPoint >= 0 && mSelectedPoint < points.length - 1)
            return new PointF(points[mSelectedPoint], points[mSelectedPoint + 1]);
        else
            return null;
    }

    public int getSelectedPointId() {
        return mSelectedPoint;
    }

    public void deleteSelectedPoint(VectorLayer layer) {
        float[] points = getSelectedRing();
        if (null == points || mSelectedPoint < 0)
            return;

        if (points.length <= getMinPointCount(layer.getGeometryType()) * 2) {
            mDrawItemsVertex.remove(mSelectedRing);
            mSelectedRing = mDrawItemsVertex.size() > 0 ? 0 : Constants.NOT_FOUND;
            mSelectedPoint = Constants.NOT_FOUND;
            return;
        }

        float[] newPoints = new float[points.length - 2];
        int counter = 0;
        for (int i = 0; i < points.length; i++) {
            if (i == mSelectedPoint || i == mSelectedPoint + 1)
                continue;

            newPoints[counter++] = points[i];
        }

        if (mSelectedPoint >= newPoints.length)
            mSelectedPoint = 0;

        setRing(mSelectedRing, newPoints);
    }

    public void setSelectedPoint(int selectedPoint) {
        mSelectedPoint = selectedPoint;
    }

    public void setRing(int ring, float[] points) {
        if (ring >= 0 && ring < mDrawItemsVertex.size())
            mDrawItemsVertex.set(ring, points);
    }

    public void setSelectedRing(int selectedRing) {
        if (mSelectedRing >= 0 && mSelectedRing < mDrawItemsVertex.size())
            mSelectedRing = selectedRing;
    }

    public float[] getSelectedRing() {
        return getRing(mSelectedRing);
    }

    public int getSelectedRingId() {
        return mSelectedRing;
    }

    public float[] getRing(int ring) {
        return ring < 0 || ring >= mDrawItemsVertex.size() ? null :
                mDrawItemsVertex.get(ring).clone();
    }

    public int getRingCount() {
        return mDrawItemsVertex.size();
    }

    public void deleteSelectedRing() {
        mDrawItemsVertex.remove(mSelectedRing);
        mSelectedRing = mSelectedPoint = mDrawItemsVertex.size() > 0 ? 0 : Constants.NOT_FOUND;
    }

    public boolean isTapNearSelectedPoint(GeoEnvelope screenEnv) {
        float[] points = getSelectedRing();
        if (null != points && mSelectedPoint >= 0 && points.length > mSelectedPoint + 1) {
            if (screenEnv.contains(new GeoPoint(
                    points[mSelectedPoint], points[mSelectedPoint + 1]))) {
                return true;
            }
        }
        return false;
    }

    public boolean intersectsVertices(GeoEnvelope screenEnv) {
        int point;
        for (int ring = 0; ring < mDrawItemsVertex.size(); ring++) {
            point = 0;
            float[] items = mDrawItemsVertex.get(ring);
            for (int i = 0; i < items.length - 1; i += 2) {
                if (screenEnv.contains(new GeoPoint(items[i], items[i + 1]))) {
                    mSelectedRing = ring;
                    mSelectedPoint = point;
                    return true;
                }
                point += 2;
            }
        }

        return false;
    }

    public boolean intersectsEdges(GeoEnvelope screenEnv) {
        for (int ring = 0; ring < mDrawItemsEdge.size(); ring++) {
            float[] items = mDrawItemsEdge.get(ring);
            for (int i = 0; i < items.length - 1; i += 2) {
                if (screenEnv.contains(new GeoPoint(items[i], items[i + 1]))) {
                    mSelectedPoint = i + 2;
                    mSelectedRing = ring;
                    insertNewPoint(mSelectedPoint, items[i], items[i + 1]);

                    return true;
                }
            }
        }

        return false;
    }

    public static int getMinPointCount(int type)
    {
        switch (type) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                return 1;
            case GeoConstants.GTLineString:
            case GeoConstants.GTMultiLineString:
                return 2;
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
                return 3;
            default:
                return 1;
        }
    }

}

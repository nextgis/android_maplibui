/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2017 NextGIS, info@nextgis.com
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
    public static final int TYPE_EDGE = 2;

    public final static int VERTEX_RADIUS = 5;
    public final static int EDGE_RADIUS = 3;
    public final static int LINE_WIDTH = 4;

    protected static Paint mPaint;

    protected static Bitmap mAnchor;
    public static float mAnchorTolerancePX;
    protected static float mAnchorCenterX, mAnchorCenterY;
    protected static float mAnchorRectOffsetX, mAnchorRectOffsetY;

    private static VertexStyle mEdgeStyle, mVertexStyle;
    private static EditStyle mLineStyle, mPolygonStyle;

    protected List<float[]> mDrawItemsVertex;
    protected List<float[]> mDrawItemsEdge;
    protected int mSelectedRing = 0, mSelectedPoint = 0;

    public DrawItem() {
        mDrawItemsVertex = new ArrayList<>();
        mDrawItemsEdge = new ArrayList<>();

        if (mPaint == null) {
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
        }
    }

    public DrawItem(int type, float[] points) {
        this();

        if (type == TYPE_VERTEX)
            addVertices(points);
        else if (type == TYPE_EDGE)
            addEdges(points);
    }

    public static void setEdgeStyle(VertexStyle edgeStyle) {
        mEdgeStyle = edgeStyle;
    }

    public static void setVertexStyle(VertexStyle vertexStyle) {
        mVertexStyle = vertexStyle;
    }

    public static void setLineStyle(EditStyle lineStyle) {
        mLineStyle = lineStyle;
    }

    public static void setPolygonStyle(EditStyle polygonStyle) {
        mPolygonStyle = polygonStyle;
    }

    public static void setAnchor(Context context, Bitmap anchor) {
        mAnchor = anchor;
        if (mAnchor != null) {
            mAnchorRectOffsetX = -mAnchor.getWidth() * 0.05f;
            mAnchorRectOffsetY = -mAnchor.getHeight() * 0.05f;
            mAnchorCenterX = mAnchor.getWidth() * 0.75f;
            mAnchorCenterY = mAnchor.getHeight() * 0.75f;
            mAnchorTolerancePX = mAnchor.getScaledWidth(context.getResources().getDisplayMetrics());
        }
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
        if (points != null)
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
        if (null != points && mSelectedPoint >= 0 && mSelectedPoint < points.length - 1)
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
        float[] ring = getSelectedRing();
        if (ring != null)
            if (selectedPoint >= 0 && selectedPoint < ring.length - 1) {
                mSelectedPoint = selectedPoint;
            } else
                mSelectedRing = ring.length - 2;
    }

    public void setRing(int ring, float[] points) {
        if (ring >= 0 && ring < mDrawItemsVertex.size())
            mDrawItemsVertex.set(ring, points);
    }

    public void setSelectedRing(int selectedRing) {
        if (selectedRing >= 0 && selectedRing < mDrawItemsVertex.size())
            mSelectedRing = selectedRing;
        else
            mSelectedRing = 0;
    }

    public float[] getSelectedRing() {
        return getRing(mSelectedRing);
    }

    public int getSelectedRingId() {
        return mSelectedRing;
    }

    public float[] getRing(int ring) {
        return ring < 0 || ring >= mDrawItemsVertex.size() ? null : mDrawItemsVertex.get(ring).clone();
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
            if (screenEnv.contains(new GeoPoint(points[mSelectedPoint], points[mSelectedPoint + 1]))) {
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

    public static int getMinPointCount(int type) {
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

    public void drawPoints(Canvas canvas, boolean isSelected) {
        if (mVertexStyle == null)
            return;

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAlpha(mVertexStyle.getAlpha());
        for (int i = 0; i < getRingCount(); i++) {
            float[] items = getRing(i);
            if (items == null)
                continue;

            mPaint.setColor(mVertexStyle.getOutColor());
            mPaint.setStrokeWidth(mVertexStyle.getOutWidth());
            drawPoints(canvas, items, mVertexStyle.getOutRadius());

            mPaint.setColor(mVertexStyle.getColor());
            mPaint.setStrokeWidth(mVertexStyle.getWidth());
            drawPoints(canvas, items, mVertexStyle.getRadius());
        }

        //draw selected point
        if (isSelected && getSelectedRingId() != Constants.NOT_FOUND && getSelectedPointId() != Constants.NOT_FOUND) {
            float[] items = getSelectedRing();
            if (null != items && items.length > 0) {
                mPaint.setColor(mVertexStyle.getSelectedColor());
                mPaint.setStrokeWidth(mVertexStyle.getSelectedWidth());
                float[] point = new float[]{items[getSelectedPointId()], items[getSelectedPointId() + 1]};
                drawPoints(canvas, point, mVertexStyle.getSelectedRadius());
                drawAnchor(canvas, items);
            }
        }
    }

    public void drawLines(Canvas canvas, boolean isSelected, boolean drawPoints, boolean drawEdges, boolean closed) {
        Path[] paths = new Path[getRingCount()];
        Path pathFill = new Path();

        // fill paths
        for (int j = 0; j < getRingCount(); j++) {
            float[] itemsVertex = getRing(j);
            if (itemsVertex == null)
                continue;

            if (itemsVertex.length >= 2) {
                Path path = new Path();
                path.moveTo(itemsVertex[0], itemsVertex[1]);
                for (int i = 2; i < itemsVertex.length - 1; i += 2)
                    path.lineTo(itemsVertex[i], itemsVertex[i + 1]);

                if (closed)
                    path.lineTo(itemsVertex[0], itemsVertex[1]);

                paths[j] = path;
                pathFill.addPath(path);
            }
        }

        // draw filled polygon
        if (closed && mPolygonStyle != null) {
            pathFill.setFillType(Path.FillType.EVEN_ODD);
            mPaint.setColor(isSelected ? mPolygonStyle.getSelectedColor() : mPolygonStyle.getColor());
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAlpha(mPolygonStyle.getAlpha());
            canvas.drawPath(pathFill, mPaint);
        }

        if (mLineStyle != null) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setAlpha(mLineStyle.getAlpha());
            for (int i = 0; i < paths.length; i++) {
                Path path = paths[i];
                if (path == null)
                    continue;

                if (isSelected && getSelectedRingId() == i) {
                    mPaint.setColor(mLineStyle.getSelectedColor());
                    mPaint.setStrokeWidth(mLineStyle.getSelectedWidth());
                } else {
                    mPaint.setColor(mLineStyle.getColor());
                    mPaint.setStrokeWidth(mLineStyle.getWidth());
                }

                canvas.drawPath(path, mPaint);
            }
        }

        if (drawEdges && mEdgeStyle != null) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setAlpha(mEdgeStyle.getAlpha());
            for (float[] items : getEdges()) {
                mPaint.setColor(mEdgeStyle.getOutColor());
                mPaint.setStrokeWidth(mEdgeStyle.getOutWidth());
                drawPoints(canvas, items, mEdgeStyle.getOutRadius());

                mPaint.setColor(mEdgeStyle.getColor());
                mPaint.setStrokeWidth(mEdgeStyle.getWidth());
                drawPoints(canvas, items, mEdgeStyle.getRadius());
            }
        }

        if (drawPoints)
            drawPoints(canvas, isSelected);
    }

    protected void drawAnchor(Canvas canvas, float[] items) {
        if (mAnchor != null) {
            float anchorX = items[getSelectedPointId()] + mAnchorRectOffsetX;
            float anchorY = items[getSelectedPointId() + 1] + mAnchorRectOffsetY;
            canvas.drawBitmap(mAnchor, anchorX, anchorY, null);
        }
    }

    protected void drawPoints(Canvas canvas, float[] items, float radius) {
        for (int i = 0; i < items.length - 1; i += 2)
            canvas.drawCircle(items[i], items[i + 1], radius, mPaint);
    }
}

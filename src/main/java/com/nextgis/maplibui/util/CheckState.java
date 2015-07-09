/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplibui.util;


import android.os.Parcel;
import android.os.Parcelable;


public class CheckState
        implements Parcelable
{

    protected int     mId;
    protected boolean mCheckState1;
    protected boolean mCheckState2;


    public CheckState(
            int id,
            boolean checkState1,
            boolean checkState2)
    {
        mId = id;
        mCheckState1 = checkState1;
        mCheckState2 = checkState2;
    }


    @Override
    public int describeContents()
    {
        return 0;
    }


    @Override
    public void writeToParcel(
            Parcel parcel,
            int i)
    {
        parcel.writeInt(mId);
        parcel.writeByte(mCheckState1 ? (byte) 1 : (byte) 0);
        parcel.writeByte(mCheckState2 ? (byte) 1 : (byte) 0);
    }


    public static final Parcelable.Creator<CheckState> CREATOR =
            new Parcelable.Creator<CheckState>()
            {
                public CheckState createFromParcel(Parcel in)
                {
                    return new CheckState(in);
                }


                public CheckState[] newArray(int size)
                {
                    return new CheckState[size];
                }
            };


    protected CheckState(Parcel in)
    {
        mId = in.readInt();
        mCheckState1 = in.readByte() == 1;
        mCheckState2 = in.readByte() == 1;
    }


    public int getId()
    {
        return mId;
    }


    public boolean isCheckState1()
    {
        return mCheckState1;
    }


    public boolean isCheckState2()
    {
        return mCheckState2;
    }


    public void setCheckState1(boolean checkState1)
    {
        mCheckState1 = checkState1;
    }


    public void setCheckState2(boolean checkState2)
    {
        mCheckState2 = checkState2;
    }
}
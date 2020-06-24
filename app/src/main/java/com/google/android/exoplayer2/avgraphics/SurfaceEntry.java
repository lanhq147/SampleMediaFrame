package com.google.android.exoplayer2.avgraphics;

import android.view.Surface;

public class SurfaceEntry {
    private Surface mSurface;
    private int width;
    private int height;

    public SurfaceEntry(Surface mSurface,int width,int height){
        this.mSurface = mSurface;
        this.width = width;
        this.height = height;
    }

    public Surface getSurface(){
        return mSurface;
    }

    public int getWidth(){
        return  width;
    }

    public int getHeight(){
        return height;
    }

    @Override
    public String toString() {
        return "SurfaceEntry{" +
                "mSurface=" + mSurface +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}

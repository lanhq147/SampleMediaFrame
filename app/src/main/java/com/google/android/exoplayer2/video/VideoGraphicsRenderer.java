package com.google.android.exoplayer2.video;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.Matrix;
import android.view.Surface;

import com.google.android.exoplayer2.avgraphics.SurfaceEntry;
import com.google.android.exoplayer2.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频图像的渲染处理
 * @author lanhq
 */
public class VideoGraphicsRenderer {

    private static final String TAG = "VideoGraphicsRenderer";
    private static final int DEFAULT_FRAME_RATE = 24;

    private static final int PIXEL_FORMAT_NONE = 0;
    private static final int PIXEL_FORMAT_NV21 = 1;
    private static final int PIXEL_FORMAT_YV12 = 2;
    private static final int PIXEL_FORMAT_NV12 = 3;
    private static final int PIXEL_FORMAT_YUV420P = 4;


    private Context mContext;
    private List<SurfaceEntry> surfaces;
    private AssetManager assetManager;
    private float[] mMatrix = new float[16];
    private int gWidth;
    private int gHeight;

    static {
        System.loadLibrary("ffmpeg");
        System.loadLibrary("ffcodec");
        System.loadLibrary("gles");
        System.loadLibrary("sles");
    }

    protected VideoGraphicsRenderer(Context context){
        surfaces = new ArrayList<>();
        this.mContext = context;
        assetManager = mContext.getAssets();
        Matrix.setIdentityM(mMatrix, 0);
    }

    protected void addSurfaceEntry(SurfaceEntry surfaceEntry){
        if (!surfaces.contains(surfaceEntry)){
            surfaces.add(surfaceEntry);
            initYuvRenderer(surfaceEntry);
        }
    }

    protected void removeSurfaceEntry(SurfaceEntry surfaceEntry){
        surfaces.remove(surfaceEntry);
    }

    protected void setGraphicsSize(int width,int height){
        this.gWidth = width;
        this.gHeight = height;
        Log.i(TAG,"setGraphicsSize gWidth=" + gWidth + ",gHeight=" + gHeight);
    }

    protected void initYuvRenderer(SurfaceEntry surfaceEntry){
        Log.i(TAG,"initYuvRenderer surfaceEntry=" + surfaceEntry);
        _addSurface(surfaceEntry.getSurface(), surfaceEntry.getWidth(), surfaceEntry.getHeight(), gWidth, gHeight, DEFAULT_FRAME_RATE, assetManager);
    }

    protected void renderGraphicsGl(byte[] graphics){
        Log.i(TAG,"renderGraphicsGl graphics.length=" + graphics.length);
        _drawGL(graphics, graphics.length, gWidth, gHeight, PIXEL_FORMAT_NV12, mMatrix);
    }

    protected void finish(){
        _stopGL();
    }

    private static native void _addSurface(Surface surface, int width, int height, int imgWidth, int imgHeight, int frameRate, AssetManager manager);

    private static native void _drawGL(byte[] pixel, int length, int imgWidth, int imgHeight, int pixelFormat, float[] matrix);

    private static native void _stopGL();
}

package com.google.android.exoplayer2.video;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.Matrix;
import android.view.Surface;

import com.google.android.exoplayer2.avgraphics.SurfaceEntry;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.video.av.AVInfo;
import com.steven.avgraphics.activity.VideoPlayActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频图像的渲染处理
 * @author lanhq
 */
public class VideoGraphicsRenderer {

    private static final String TAG = "VideoGraphicsRenderer";

    private Context mContext;
    private List<SurfaceEntry> surfaces;

    protected VideoGraphicsRenderer(Context context){
        surfaces = new ArrayList<>();
        this.mContext = context;
    }

    protected void addSurfaceEntry(SurfaceEntry surfaceEntry){
        if (!surfaces.contains(surfaceEntry)){
            surfaces.add(surfaceEntry);
        }
    }

    protected void removeSurfaceEntry(SurfaceEntry surfaceEntry){
        surfaces.remove(surfaceEntry);
    }

    protected void setGraphicsSize(int width,int height){

    }

    protected void renderGraphicsGl(byte[] graphics){
        VideoPlayActivity.videoPlayActivity.onImageDecoded(graphics);
    }
}

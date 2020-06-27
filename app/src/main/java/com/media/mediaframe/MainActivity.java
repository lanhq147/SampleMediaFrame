package com.media.mediaframe;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.SeekBar;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.avgraphics.SurfaceEntry;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private boolean showMultiWindowTimeBar;
    private boolean multiWindowTimeBar;
    private boolean isTracking = false;

    private Timeline.Period period;
    private Timeline.Window window;
    private Runnable updateProgressAction;

    private SeekBar mSeekBar;
    private SurfaceView mSurfaceView1;
    private SurfaceView mSurfaceView2;
    private SimpleExoPlayer simpleExoPlayer;
    private DefaultTrackSelector trackSelector;
    private com.google.android.exoplayer2.ControlDispatcher controlDispatcher;

    private long currentWindowOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        trackSelector = new DefaultTrackSelector(this);
        simpleExoPlayer = new SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build();
        controlDispatcher = new com.google.android.exoplayer2.DefaultControlDispatcher();
        mSurfaceView1 = findViewById(R.id.vplay_sv_window1);
        mSurfaceView2 = findViewById(R.id.vplay_sv_window2);
        mSeekBar = findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                simpleExoPlayer.seekTo(0, seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTracking = false;

            }
        });
        updateProgressAction = this::updateProgress;
        period = new Timeline.Period();
        window = new Timeline.Window();

        mSurfaceView1.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                simpleExoPlayer.addVideoSurface(new SurfaceEntry(surfaceHolder.getSurface(),mSurfaceView1.getWidth(),mSurfaceView1.getHeight()));
            }

            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}
        });

        mSurfaceView2.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                simpleExoPlayer.addVideoSurface(new SurfaceEntry(surfaceHolder.getSurface(),mSurfaceView2.getWidth(),mSurfaceView2.getHeight()));
            }

            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}
        });


        simpleExoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, int reason) {
                updateTimeline();
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Log.i(TAG,"onPlayerStateChanged isplaying=" + simpleExoPlayer.isPlaying());
            }
        });

        simpleExoPlayer.addVideoListener(new VideoListener() {
            @Override
            public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                Log.i(TAG,"onVideoSizeChanged width=" + width + ",height=" + height);
                layoutSurfaceView(width,height);
            }
        });

        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!simpleExoPlayer.isPlaying()) {
                    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(MainActivity.this, Util.getUserAgent(MainActivity.this, "yourApplicationName"));
                    MediaSource createdMediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/videoplayback.mp4")));
                    simpleExoPlayer.setPlayWhenReady(true);
                    simpleExoPlayer.prepare(createdMediaSource);
                }else{
                    controlDispatcher.dispatchSetPlayWhenReady(simpleExoPlayer,false);
                }
            }
        });

    }

    private void updateTimeline() {
        multiWindowTimeBar = showMultiWindowTimeBar && canShowMultiWindowTimeBar(simpleExoPlayer.getCurrentTimeline(), window);
        currentWindowOffset = 0;
        long durationUs = 0;
        int adGroupCount = 0;
        Timeline timeline = simpleExoPlayer.getCurrentTimeline();
        if (!timeline.isEmpty()) {
            int currentWindowIndex = simpleExoPlayer.getCurrentWindowIndex();
            int firstWindowIndex = multiWindowTimeBar ? 0 : currentWindowIndex;
            int lastWindowIndex = multiWindowTimeBar ? timeline.getWindowCount() - 1 : currentWindowIndex;
            for (int i = firstWindowIndex; i <= lastWindowIndex; i++) {
                if (i == currentWindowIndex) {
                    currentWindowOffset = C.usToMs(durationUs);
                }
                timeline.getWindow(i, window);
                if (window.durationUs == C.TIME_UNSET) {
                    Assertions.checkState(!multiWindowTimeBar);
                    break;
                }
                for (int j = window.firstPeriodIndex; j <= window.lastPeriodIndex; j++) {
                    timeline.getPeriod(j, period);
                    int periodAdGroupCount = period.getAdGroupCount();
                    for (int adGroupIndex = 0; adGroupIndex < periodAdGroupCount; adGroupIndex++) {
                        long adGroupTimeInPeriodUs = period.getAdGroupTimeUs(adGroupIndex);
                        if (adGroupTimeInPeriodUs == C.TIME_END_OF_SOURCE) {
                            if (period.durationUs == C.TIME_UNSET) {
                                // Don't show ad markers for postrolls in periods with unknown duration.
                                continue;
                            }
                            adGroupTimeInPeriodUs = period.durationUs;
                        }
                        long adGroupTimeInWindowUs = adGroupTimeInPeriodUs + period.getPositionInWindowUs();
                        if (adGroupTimeInWindowUs >= 0) {
                            adGroupCount++;
                        }
                    }
                }
                durationUs += window.durationUs;
            }
        }
        long durationMs = C.usToMs(durationUs);
        if (mSeekBar != null) {
            mSeekBar.setMax((int) durationMs);
        }
        updateProgress();
    }

    private void updateProgress() {
        long position = currentWindowOffset + simpleExoPlayer.getContentPosition();

        if (mSeekBar != null && !isTracking) {
            mSeekBar.setProgress((int) position);
        }

        if (simpleExoPlayer.isPlaying()) {
            mSeekBar.postDelayed(updateProgressAction, 1000);
        }
    }

    private static boolean canShowMultiWindowTimeBar(Timeline timeline, Timeline.Window window) {
        if (timeline.getWindowCount() > 100) {
            return false;
        }
        int windowCount = timeline.getWindowCount();
        for (int i = 0; i < windowCount; i++) {
            if (timeline.getWindow(i, window).durationUs == C.TIME_UNSET) {
                return false;
            }
        }
        return true;
    }

    private void layoutSurfaceView(int gwidth,int gheight) {
        double ratio = 1.0 * gheight / gwidth;
        int width = getScreenWidth();
        int heigth = getScreenWidth();
        if (ratio > 1) {
            width = (int) (heigth / ratio);
        } else {
            heigth = (int) (width * ratio);
        }
        mSurfaceView2.getLayoutParams().width = width;
        mSurfaceView2.getLayoutParams().height = heigth;
    }

    public int getScreenWidth() {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }

    public int getScreenHeight() {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        return metrics.heightPixels;
    }

}

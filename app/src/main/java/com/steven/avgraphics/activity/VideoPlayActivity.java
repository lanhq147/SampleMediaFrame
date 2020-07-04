package com.steven.avgraphics.activity;

import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.steven.avgraphics.BaseActivity;
import com.steven.avgraphics.R;
import com.steven.avgraphics.module.av.AVInfo;
import com.steven.avgraphics.module.av.FFCodec;
import com.steven.avgraphics.module.av.HWDecoder;
import com.steven.avgraphics.module.av.OnDecodeListener;
import com.steven.avgraphics.util.ToastHelper;
import com.steven.avgraphics.util.Utils;

import java.io.File;

public class VideoPlayActivity extends BaseActivity {

    private static final int DEFAULT_FRAME_RATE = 24;
    private static final int DEFAULT_PIXEL_FORMAT = AVInfo.PIXEL_FORMAT_NV12;
    private static final int DEFAULT_SAMPLE_RATE = 48000;
    private static final int DEFAULT_SAMPLE_FORMAT = AVInfo.SAMPLE_FORMAT_16BIT;

    private SurfaceView mSurfaceView;
    private SurfaceView mSurfaceView1;
    private Button mBtnStart;
    private Button mBtnStop;
    private TextView mTvAVInfo;
    private TextView mTvTime;

    private Surface mSurface;
    private Surface mSurface1;
    private HWDecoder mDecoder = new HWDecoder();
    private DecodeListener mDecodeListener;
    private AudioTrack mAudioTrack;
    private AVInfo mAVInfo;
    private CountDownTimer mCountDownTimer;
    private SimpleExoPlayer simpleExoPlayer;
    private DefaultTrackSelector trackSelector;
    private com.google.android.exoplayer2.ControlDispatcher controlDispatcher;
    private File mFile;
    private SeekBar mSeekbar;
    private boolean multiWindowTimeBar;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private int mSurfaceWidth1;
    private int mSurfaceHeight1;

    private int mImageWidth;
    private int mImageHeight;
    private int mFrameRate = DEFAULT_FRAME_RATE;
    private int mPixelFormat = DEFAULT_PIXEL_FORMAT;
    private int mSampleRate;
    private int mSampleFormat = DEFAULT_SAMPLE_FORMAT;
    private int mChannels;
    private float[] mMatrix = new float[16];
    private volatile boolean mIsPlaying = false;

    private Timeline.Period period;
    private Timeline.Window window;
    private Runnable updateProgressAction;

    private boolean isTracking;
    private boolean showMultiWindowTimeBar;

    private long currentWindowOffset = 0;

    public static VideoPlayActivity videoPlayActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);
        init();
        videoPlayActivity = this;
        trackSelector = new DefaultTrackSelector(this);
        simpleExoPlayer = new SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build();
        controlDispatcher = new com.google.android.exoplayer2.DefaultControlDispatcher();

        updateProgressAction = this::updateProgress;
        period = new Timeline.Period();
        window = new Timeline.Window();

        simpleExoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, int reason) {
                updateTimeline();
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                com.google.android.exoplayer2.util.Log.i(TAG,"onPlayerStateChanged isplaying=" + simpleExoPlayer.isPlaying());
                if (simpleExoPlayer.isPlaying()) {
                    mSeekbar.postDelayed(updateProgressAction, 1000);
                }else{
                    mSeekbar.removeCallbacks(updateProgressAction);
                }
            }
        });
    }

    private void init() {
        Matrix.setIdentityM(mMatrix, 0);
        mFile = new File(Environment.getExternalStorageDirectory()+"/videoplayback.mp4");
        mAVInfo = FFCodec.getAVInfo(Environment.getExternalStorageDirectory()+"/videoplayback.mp4");
        findView();
        showAVInfo();
        //layoutSurfaceView();
        setListener();
    }

    private void findView() {
        mSurfaceView = findViewById(R.id.vplay_sv_window);
        mSurfaceView1 = findViewById(R.id.vplay_sv_window1);
        mBtnStart = findViewById(R.id.vplay_btn_start);
        mBtnStop = findViewById(R.id.vplay_btn_stop);
        mTvAVInfo = findViewById(R.id.vplay_tv_avinfo);
        mTvTime = findViewById(R.id.vplay_tv_time);
        mSeekbar = findViewById(R.id.video_seekbar);

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (isTracking) {
                    simpleExoPlayer.seekTo(0, seekBar.getProgress());
                }
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
    }

    private void showAVInfo() {
        if (mAVInfo != null) {
            double fileSize = (double) mFile.length() / 1024 / 1024;
            int index = String.valueOf(fileSize).lastIndexOf(".");
            String fileSizeStr = String.valueOf(fileSize).substring(0, index + 3);
            String string = "[file path: " + mFile.getAbsolutePath() + "]\n[file size: "
                    + fileSizeStr + "M]\n" + mAVInfo.toString();
            Log.i(TAG, "video file info:\n" + string);
            mTvAVInfo.setText(string);
        }
    }

    private void layoutSurfaceView() {
        double ratio = mAVInfo == null ? 1 : 1.0 * mAVInfo.height / mAVInfo.width;
        int width = Utils.getScreenWidth();
        int heigth = Utils.getScreenWidth();
        if (ratio > 1) {
            width = (int) (heigth / ratio);
        } else {
            heigth = (int) (width * ratio);
        }
        mSurfaceView.getLayoutParams().width = width;
        mSurfaceView.getLayoutParams().height = heigth;

        mSurfaceView1.getLayoutParams().width = width;
        mSurfaceView1.getLayoutParams().height = heigth;
        Log.i(TAG,"layoutSurfaceView width=" + width + ",heigth=" + heigth);
    }

    private void setListener() {
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                mSurface = surfaceHolder.getSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mSurfaceWidth = width;
                mSurfaceHeight = height;
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}
        });

        mSurfaceView1.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                mSurface1 = surfaceHolder.getSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mSurfaceWidth1 = width;
                mSurfaceHeight1 = height;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });
        mBtnStart.setOnClickListener(v -> start());
        mBtnStop.setOnClickListener(v -> stop());
    }

    private void start() {
        if (!mFile.exists()) {
            Log.e(TAG, "start video player failed: file not found");
            ToastHelper.show(R.string.vplay_msg_no_file);
            return;
        }

        mIsPlaying = true;
        mBtnStop.setEnabled(true);
        mBtnStart.setEnabled(false);

        setupVideoParams();
        //startDecode();
        //setupAudioTrack();
//        _startSL(mSampleRate, mSampleFormat, mChannels);
        _startGL(mSurface,mSurfaceWidth, mSurfaceHeight, mImageWidth, mImageHeight, mFrameRate, getAssets());
        _startGL1(mSurface1, mSurfaceWidth1, mSurfaceHeight1, mImageWidth, mImageHeight, mFrameRate, getAssets());
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "yourApplicationName"));
        MediaSource createdMediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/videoplayback.mp4")));
        simpleExoPlayer.setPlayWhenReady(true);
        simpleExoPlayer.prepare(createdMediaSource);
        //startCounDownTimer();
    }

    private void setupVideoParams() {
        mImageWidth = mAVInfo != null && mAVInfo.width > 0 ? mAVInfo.width : mSurfaceWidth;
        mImageHeight = mAVInfo != null && mAVInfo.height > 0 ? mAVInfo.height : mSurfaceHeight;
        //mFrameRate = mAVInfo != null && mAVInfo.frameRate > 0 ? mAVInfo.frameRate : DEFAULT_FRAME_RATE;
        mChannels = mAVInfo != null && mAVInfo.channels == 2 ? 2 : 1;
        mSampleRate = mAVInfo != null && mAVInfo.sampleRate > 0 ? mAVInfo.sampleRate : DEFAULT_SAMPLE_RATE;
        // MediaCodec 解码出来的基本都是 NV12
        mPixelFormat = DEFAULT_PIXEL_FORMAT;
        mSampleFormat = mAVInfo != null && mAVInfo.sampleFormat > 0 ? mAVInfo.sampleFormat : DEFAULT_SAMPLE_FORMAT;
    }

    private void setupAudioTrack() {
        int channelConfig = mChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int bufferSize = AudioTrack.getMinBufferSize(mSampleRate, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
    }

    private void startDecode() {
        mDecodeListener = new DecodeListener();
        mDecoder.setDecodeWithPts(true);
        mDecoder.start(mFile.getAbsolutePath(), mDecodeListener);
    }

    private void startCounDownTimer() {
        mCountDownTimer = new CountDownTimer(mAVInfo.duration + 1000, 1000) {

            long mPass = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                String str = mPass + "s";
                mTvTime.setText(str);
                mPass++;
            }

            @Override
            public void onFinish() {

            }
        };
        mCountDownTimer.start();
    }

    private void stop() {
        //mIsPlaying = false;
        //mBtnStart.setEnabled(true);
        //mBtnStop.setEnabled(false);
        //mDecoder.stop();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        if (simpleExoPlayer.isPlaying()){
            controlDispatcher.dispatchSetPlayWhenReady(simpleExoPlayer,false);
        }else{
            controlDispatcher.dispatchSetPlayWhenReady(simpleExoPlayer,true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsPlaying = false;
        mDecodeListener = null;
        stop();
        releaseAudioTrack();
//        _stopSL();
        _stopGL();
    }

    private synchronized void releaseAudioTrack() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }


    private class SurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurface = holder.getSurface();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

    }

    private class DecodeListener implements OnDecodeListener {

        @Override
        public void onImageDecoded(byte[] data) {
            if (mIsPlaying) {
                Log.i(TAG,"onImageDecoded lenght=" + data.length);
                //_drawGL(data, data.length, mImageWidth, mImageHeight, mPixelFormat, mMatrix);
            }
        }

        @Override
        public void onSampleDecoded(byte[] data) {
            synchronized (VideoPlayActivity.this) {
                if (mIsPlaying) {
                    mAudioTrack.write(data, 0, data.length);
                    mAudioTrack.play();
//                    _writeSL(data, data.length);
                }
            }
        }

        @Override
        public void onDecodeEnded(boolean vsucceed, boolean asucceed) {
            Utils.runOnUiThread(() -> {
                stop();
                releaseAudioTrack();
//                _stopSL();
                _stopGL();
            });
        }
    }

    public void onImageDecoded(byte[] data){
        //Log.i(TAG,"onImageDecoded lenght=" + data.length);
        _drawGL(data, data.length, mImageWidth, mImageHeight, mPixelFormat, mMatrix);
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
        if (mSeekbar != null) {
            mSeekbar.setMax((int) durationMs);
        }
        updateProgress();
    }

    private void updateProgress() {
        long position = currentWindowOffset + simpleExoPlayer.getContentPosition();

        //Log.i(TAG,"updateProgress position=" + position);
        if (mSeekbar != null && !isTracking) {
            mSeekbar.setProgress((int) position);
        }

        if (simpleExoPlayer.isPlaying()) {
            mSeekbar.postDelayed(updateProgressAction, 1000);
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

    private static native void _startGL(Surface surface,int width, int height, int imgWidth, int imgHeight, int frameRate, AssetManager manager);
    private static native void _startGL1(Surface surface,int width, int height, int imgWidth, int imgHeight, int frameRate, AssetManager manager);

    private static native void _drawGL(byte[] pixel, int length, int imgWidth, int imgHeight, int pixelFormat, float[] matrix);

    private static native void _stopGL();
}

package com.google.android.exoplayer2.video.av;


public interface OnDecodeListener {

    void onImageDecoded(byte[] image);

    void onSampleDecoded(byte[] sample);

    void onDecodeEnded(boolean vsucceed, boolean asucceed);

}

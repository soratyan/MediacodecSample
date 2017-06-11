package com.mediacodecsample.player;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AudioTrackPlayer {
    private Thread mOutputThread;
    private MediaCodec mCodec = null;
    MediaExtractor mMediaExtractor = null;
    String Mmine = null;
    MediaFormat mMediaFormat = null;
    AudioTrack  mAudioTrack = null;
    private Context mContext;

    public float nowRate = 1.0f;

     //一時停止状態
    public boolean mPause = false;

    /**
     * シングルトンインスタンス
     */
    private static AudioTrackPlayer INSTANCE;
    public AudioTrackPlayer() {
        // 最初はインスタンスを生成しない
        INSTANCE = null;
    }

    public  synchronized  AudioTrackPlayer getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AudioTrackPlayer();
            this.mContext = context;
        }
        return INSTANCE;
    }

    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    /**
     * コンテンツを読み込む処理
     * @param extractor
     * @throws IOException
     */
    public synchronized void loadContent(MediaExtractor extractor) throws IOException {

        final int trackCount = extractor.getTrackCount();

        List<MediaFormat> trackMediaFormatList = new ArrayList<MediaFormat>();
        for (int i = 0; i < trackCount; i++) {
            final MediaFormat format = extractor.getTrackFormat(i);
            final String mime = format.getString(MediaFormat.KEY_MIME);
            trackMediaFormatList.add(format);
            final long duration = format.getLong(MediaFormat.KEY_DURATION);
            Log.e("sample","Duration : "+ duration);

//            if (mime.startsWith("video/")) {
//                final int height = format.getInteger(MediaFormat.KEY_HEIGHT);
//                final int width = format.getInteger(MediaFormat.KEY_WIDTH);
//
//
//            } else
            if (mime.startsWith("audio/")) {
                final int channelCount = format
                        .getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                final int sampleRate = format
                        .getInteger(MediaFormat.KEY_SAMPLE_RATE);

                Log.e("sample","Channel Count : " + channelCount);
                Log.e("sample","Sample Rate : " + sampleRate);

                extractor.selectTrack(i);
                Log.e("sample","extractor.selectTrack(i) : " + i);

                setMediaType(extractor,mime,format);

                break;
            }

        }
    }

    /**
     * 音声再生処理
     */
    public synchronized void play()  {
        //再生中であれば処理をしない
        if (mOutputThread != null &&  mOutputThread.isAlive()){
            return;
        }
        // デコーターを作成する
        try {
            mCodec = MediaCodec.createDecoderByType(Mmine);
        } catch (IOException e) {

        }

        mCodec.configure(mMediaFormat, null, null, 0);
        mCodec.start();

        //出力Thread
        mOutputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ByteBuffer[] inputBuffers = null;
                ByteBuffer[] outputBuffers = null;
                mAudioTrack = null;
                boolean isPlayed = false;
                byte[] buffer = null;
                boolean sawInputEOS = false;

                Thread.currentThread().setPriority(Thread.MAX_PRIORITY - 1);

                // デコーダーからInputBuffer, OutputBufferを取得する
                    inputBuffers = mCodec.getInputBuffers();
                    outputBuffers = mCodec.getOutputBuffers();

                createAudioTrack();

                while (!Thread.currentThread().isInterrupted()) {
                    while (mPause) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                        //inputBuffer値をdeque(取り出す)
                        final int inputBufIndex = mCodec.dequeueInputBuffer(1000);

                    //mediacodecから取得した値を分割してデコードしていく
                    if (inputBufIndex >= 0) {
                        // インプットバッファの配列から対象のバッファを取得
                        final ByteBuffer dstBuf = inputBuffers[inputBufIndex];

                        // バッファサイズ
                        int sampleSize = mMediaExtractor.readSampleData(dstBuf, 0);
                            long presentationTimeUs = 0;
                            if (sampleSize < 0) {
                                sawInputEOS = true;
                                sampleSize = 0;
                            } else {
                                presentationTimeUs = mMediaExtractor.getSampleTime();
                            }

                            // デコード処理してアウトプットバッファに追加
                            mCodec.queueInputBuffer(
                                    inputBufIndex,
                                    0, // offset
                                    sampleSize,
                                    presentationTimeUs,
                                    sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                            : 0);

                            // インプットバッファがEnd Of Streamかどうかを判定
                            if (!sawInputEOS) {
                                //次のサンプルに進む
                                mMediaExtractor.advance();
                            }

                        }

                    // 出力処理
                        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 1000);
                        if (outputBufferIndex >= 0) {
                            ByteBuffer buf = outputBuffers[outputBufferIndex];
                            if (buffer == null || buffer.length < bufferInfo.size) {
                                // チャンクを作る
                                buffer = new byte[bufferInfo.size];
                            }
                            buf.position(bufferInfo.offset);
                            buf.get(buffer, 0, bufferInfo.size);

                            if (bufferInfo.size > 0) {
                                int remaining = bufferInfo.size;
                                int written = 0, written_once;
                                // AudioTrackに書き込む
                                while (!Thread.currentThread().isInterrupted()) {//割り込みもしくは書き込み完了まで無限ループ
                                    written_once = mAudioTrack.write(buffer,
                                            written, remaining);
                                    written += written_once;
                                    remaining -= written_once;

                                    if (!isPlayed && (remaining == 0 || written_once == 0)) {
                                        isPlayed = true;
                                        mAudioTrack.play();
                                    }
                                    if (remaining == 0){
                                        break;
                                    }

                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        return;
                                    }

                                }

                            }
                            mCodec.releaseOutputBuffer(outputBufferIndex, false);
                            buf.clear();

                            //bufferストリームが終了された
                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {

                                //Threadが終了した時解放する
                                releaseAudioTrack();

                                break;
                            }

                            //inputBufferが変更された
                        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            outputBuffers = mCodec.getOutputBuffers();

                            //mediaFormatが変更された
                        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            createAudioTrack();

                        }
                }

                stop();

            }
        });
        mOutputThread.start();
    }

    /**
     * 再生の終了
     */
    public synchronized void stop() {
        Thread moribund = mOutputThread;
        if (moribund != null) {
            mOutputThread = null;
            if (moribund.isAlive() && !moribund.isInterrupted()) {
                moribund.interrupt();
                try {
                    moribund.join();
                    releaseAudioTrack();

                } catch (InterruptedException ignore) {
                }
            }
        }

    }

    /**
     * 解放処理
     */
    private void releaseAudioTrack(){
        if (mAudioTrack != null && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED){
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
        if (mCodec != null){
            mCodec.stop();
            mCodec.release();
        }
        if (mMediaExtractor != null){
            mMediaExtractor.release();
            mMediaExtractor = null;
        }
    }


    /**
     * 一時停止
     */
    public synchronized void pause() {
        Thread t = mOutputThread;
        if (t != null && t.isAlive() && !t.isInterrupted()) {
            mPause = true;
        }
    }

    /**
     * 一時停止からの再生
     */
    public synchronized void reStart() {
        mPause = false;
    }

    /**
     *現在の再生時間を取得
     * @return
     */
    public synchronized int getCurrentPosition() {
        if (mMediaExtractor != null){
            return (int) (mMediaExtractor.getSampleTime() / 1000);
        }else{
            return 0;
        }
    }

    /**
     * コンテンツの合計時間を取得
     * @return
     */
    public synchronized long getDuration() {
        return mMediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000;
    }

    /**
     * 再生位置の移動
     * @param i
     */
    public synchronized void seekTo(int i) {
        if (mMediaExtractor == null){
            return;
        }
        mMediaExtractor.seekTo(i * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    }

    /**
     * コンテンツの各種値を保持する
     * @param mediaExtractor
     * @param mime
     * @param mediaFormat
     */
    private synchronized void setMediaType(MediaExtractor mediaExtractor, String mime, MediaFormat mediaFormat){
        mMediaExtractor = mediaExtractor;
        Mmine = mime;
        mMediaFormat = mediaFormat;
    }

    /**
     * AudioTrackのフォーマットを最適化して生成
     */
    private synchronized   void createAudioTrack(){
        //フォーマット生成
        final int channelCount = mMediaFormat
                .getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        final int sampleRate = mMediaFormat
                .getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelConfig = 0;
        if (channelCount == 1) {
            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        }else if (channelCount == 2) {
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        }
        final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        Log.e("sample","channelCount--"+channelCount);

        //再生速度の倍率
        final int speedRateValue = 2;

        //コンテンツのバッファサイズ*再生速度分のバッファを計算
        final int bufferSize = AudioTrack.getMinBufferSize(
                sampleRate, channelConfig, audioFormat) * 2 * speedRateValue;

        //連続して再生を繰り返した場合にリソースが枯渇するため解放処理を追加
        if (mAudioTrack != null){
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate,
                channelConfig, audioFormat, bufferSize,
                AudioTrack.MODE_STREAM);
    }

    public int getPlayState() {
        if (mAudioTrack != null) {
            int state = mAudioTrack.getPlayState();
            if (state != AudioTrack.PLAYSTATE_STOPPED) {
                state = mPause ? AudioTrack.PLAYSTATE_PAUSED : AudioTrack.PLAYSTATE_PLAYING;
            }
            return state;
        }else{
        }
        return AudioTrack.PLAYSTATE_STOPPED;
    }

    /**
     * 再生速度変換処理(倍率指定)
     * @param rate
     * @return
     */
    public  void changeRate(float rate) {
        if (mAudioTrack != null) {
            int result = mAudioTrack.setPlaybackRate((int) (rate * mAudioTrack.getSampleRate()));
            switch (result){
                case AudioTrack.SUCCESS :
                    nowRate = rate;
                    break;

                case AudioTrack.ERROR_BAD_VALUE:
                    Log.e("sample","AudioTrack.ERROR_BAD_VALUE");
                    break;
                default:
                    break;
            }
        }
    }

}

package com.mediacodecsample.activity;

import android.content.res.AssetFileDescriptor;
import android.media.AudioTrack;
import android.media.MediaExtractor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mediacodecsample.R;
import com.mediacodecsample.player.AudioTrackPlayer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private AudioTrackPlayer mPlayer = new AudioTrackPlayer();
    private float mRate = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //再生と停止ボタン
        final TextView playView = (TextView) findViewById(R.id.playView);
        playView.setText("play");

        playView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //0秒時点の再生
                if (getCurrentPosition() == 0){
                    //mp3読み込み
                    mPlayer.getInstance(getApplicationContext());
                    try {
                        AssetFileDescriptor afd = getAssets().openFd("sample1.mp3");
                        MediaExtractor extractor = new MediaExtractor();
                        extractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mPlayer.loadContent(extractor);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    start();
                    playView.setText("play");
                }else {
                    //途中からの再生
                    if (isPlaying()){
                        pause();
                        playView.setText("pause");
                    }else{
                        restart();
                        playView.setText("play");
                    }
                }
            }
        });

        //再生と停止ボタン
        final TextView rateView = (TextView) findViewById(R.id.rateView);
        rateView.setText("rate:1.0");

        //再生速度設定バー
        final SeekBar seekBar = (SeekBar)findViewById(R.id.SeekBar);

        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        // ツマミをドラッグしたときに呼ばれる

                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる

                        int currentPosition = seekBar.getProgress();

                        //再生速度の設定(0.5倍~1.5倍)
                        if (currentPosition <= 5){
                            mPlayer.changeRate(0.5f);

                        }else if (currentPosition >= 6 && currentPosition <= 15){
                            mPlayer.changeRate(0.6f);

                        }else if (currentPosition >= 16 && currentPosition <= 25){
                            mPlayer.changeRate(0.7f);

                        }else if (currentPosition >= 26 && currentPosition <= 35){
                            mPlayer.changeRate(0.8f);

                        }else if (currentPosition >= 36 && currentPosition <= 45){
                            mPlayer.changeRate(0.9f);

                        }else if (currentPosition >= 46 && currentPosition <= 55){
                            mPlayer.changeRate(1.0f);

                        }else if (currentPosition >= 56 && currentPosition <= 65){
                            mPlayer.changeRate(1.1f);

                        }else if (currentPosition >= 66 && currentPosition <= 75){
                            mPlayer.changeRate(1.2f);

                        }else if (currentPosition >= 76 && currentPosition <= 85){
                            mPlayer.changeRate(1.3f);

                        }else if (currentPosition >= 86 && currentPosition <= 94){
                            mPlayer.changeRate(1.4f);

                        }else if (currentPosition >= 95){
                            mPlayer.changeRate(1.5f);
                        }else{
                            mPlayer.changeRate(1.0f);
                        }
                        seekBar.setProgress(currentPosition);
                        rateView.setText("rate:"+mPlayer.nowRate);

                    }
                }
        );

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer = null;
        }
    }

    /**
     * 再生/一時停止の切り替え
     */
    public void changePlay() {
        if (isPlaying()) {
            pause();
        } else {
            restart();
        }
    }

    /**
     * 再生を開始します。
     */
    public void start() {
        if (mPlayer == null ) {
            return;
        }
        mPlayer.play();

    }

    /**
     * 停止
     */
    private void stop() {
        if (mPlayer == null) {
            return;
        }
        mPlayer.stop();
    }

    /**
     * 一時停止
     */
    public void pause() {
        if (mPlayer == null ) {
            return;
        }
        mPlayer.pause();

    }

    /**
     * 一時停止状態からの再生
     */
    public void restart() {
        if (mPlayer == null ) {
            return;
        }
        mPlayer.reStart();
    }

    /**
     * 再生中かどうか返却
     *
     * @return boolean 再生中：true　停止中：false
     */
    public boolean isPlaying() {
        if (mPlayer == null){
            return false;
        }
        return mPlayer.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }

    /**
     * 再生位置のシーク
     *
     * @param msec int ミリ秒
     * @return
     */
    public boolean seek(int msec) {
        if (mPlayer == null) {
            return false;
        }
        if (msec < 0) {
            mPlayer.seekTo(0);
        } else if (msec > getDuration()) {
            mPlayer.seekTo(getDuration());
        } else {
            mPlayer.seekTo(msec);
        }
        return false;
    }

    /**
     * 再生位置を返却
     *
     * @return int 再生位置
     */
    public int getCurrentPosition() {
        if (mPlayer == null) {
            return 0;
        }
        try {
            return mPlayer.getCurrentPosition();
        }catch (Exception e){
            return 0;
        }
    }

    /**
     * 全体の再生時間を返却
     *
     * @return int 全体の再生時間
     */
    public int getDuration() {
        if (mPlayer == null) {
            return 0;
        }
        try {
            return (int)mPlayer.getDuration();
        }catch (Exception e){
            return 0;
        }
    }

}

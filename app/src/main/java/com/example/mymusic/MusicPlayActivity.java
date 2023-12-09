package com.example.mymusic;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.mymusic.data.GlobalConstants;
import com.example.mymusic.data.Song;
import com.example.mymusic.listener.MyPlayerListener;
import com.example.mymusic.service.MyMusicService;
import com.example.mymusic.utils.PlayModeHelper;
import com.example.mymusic.utils.TimeUtil;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MusicPlayActivity extends AppCompatActivity {

    private ImageView ivPlayOrPause, ivPre, ivNext;
    private TextView tvTitle, tvCurTime, tvDuration, tvPlayMode;
    private SeekBar mSeekBar;

    private ArrayList<Song> mSongArrayList;
    private int curSongIndex;
    private Song mCurSong;
    private MyMusicService.MyMusicBind mMusicBind;
    private boolean isSeekbarDragging;
    private Timer timer;
    private int currentPlayMode = PlayModeHelper.PLAY_MODE_ORDER;

    private MyPlayerListener mPlayerListener = new MyPlayerListener() {
        @Override
        public void onComplete(int songIndex, Song song) {

        }

        @Override
        public void onNext(int songIndex, Song song) {
            curSongIndex = songIndex;
            mCurSong = song;
            upDateTitle();
            // 修改图标为即将暂停状态
            ivPlayOrPause.setImageResource(android.R.drawable.ic_media_pause);
        }

        @Override
        public void onPre(int songIndex, Song song) {
            curSongIndex = songIndex;
            mCurSong = song;
            upDateTitle();
            // 修改图标为即将暂停状态
            ivPlayOrPause.setImageResource(android.R.drawable.ic_media_pause);
        }

        @Override
        public void onPause(int songIndex, Song song) {
            // 修改图标为即将播放状态
            ivPlayOrPause.setImageResource(android.R.drawable.ic_media_play);
        }

        @Override
        public void onPlay(int songIndex, Song song) {
            // 修改图标为即将暂停状态
            ivPlayOrPause.setImageResource(android.R.drawable.ic_media_pause);
        }
    };

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // 服务已经建立，传递信息
            mMusicBind = (MyMusicService.MyMusicBind) iBinder;
            mMusicBind.updateMusicList(mSongArrayList);
            mMusicBind.updateCurrentMusicIndex(curSongIndex);
            mMusicBind.setPlayMode(currentPlayMode);
            mMusicBind.addPlayerListener(mPlayerListener);
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMusicBind.removePlayerListener(mPlayerListener);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);
        initView();

        Intent intent = getIntent();
        curSongIndex = intent.getIntExtra(GlobalConstants.KEY_SONG_INDEX, 0);
//        mSongArrayList = (ArrayList<Song>) intent.getSerializableExtra(GlobalConstants.KEY_SONG_LIST);
        mSongArrayList = intent.getParcelableArrayListExtra(GlobalConstants.KEY_SONG_LIST);
        mCurSong = mSongArrayList.get(curSongIndex);
        Log.d("tag", "当前歌曲：" + curSongIndex);
        if (mSongArrayList != null) {
            Log.d("tag", "当前歌曲列表：" + mSongArrayList);
        }
        upDateTitle();
        startMusicService();

    }

    private void updateUI() {
        // 当前时间更新
        int curProgress = mMusicBind.getCurProgress();
        updateCurTimeText(curProgress);

        // 总时间更新
        int duration = mMusicBind.getDuration();
        tvDuration.setText(TimeUtil.millToTimeFormat(duration));

        // 更新进度条
        mSeekBar.setMax(duration);
        mSeekBar.setProgress(curProgress);
        updateSeekbar();
    }

    private void updateSeekbar() {
        if (timer != null) {
            return;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int curProgress = mMusicBind.getCurProgress();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isSeekbarDragging && mMusicBind.isPlaying()) {
                            mSeekBar.setProgress(curProgress);
                        }
                    }
                });
            }
        },0, 200);
    }

    private void upDateTitle() {
        tvTitle.setText(mCurSong.getSongName());
    }

    private void updateCurTimeText(int progress) {
        // 当前时间更新
        tvCurTime.setText(TimeUtil.millToTimeFormat(progress));
    }

    private void initView() {
        ivPlayOrPause = findViewById(R.id.iv_play_pause);
        ivNext = findViewById(R.id.iv_next);
        ivPre = findViewById(R.id.iv_previous);
        tvTitle = findViewById(R.id.tv_music_title);
        tvCurTime = findViewById(R.id.tv_cur_time);
        tvDuration = findViewById(R.id.tv_duration);
        mSeekBar = findViewById(R.id.seek_bar_music);
        tvPlayMode = findViewById(R.id.tv_play_mode);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                updateCurTimeText(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekbarDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekbarDragging = false;
                int progress = seekBar.getProgress();
                mMusicBind.seekTo(progress);
            }
        });

    }

    private void startMusicService() {
        Intent intent = new Intent(this, MyMusicService.class);
        // 通过 start的方式启动 service
        startService(intent);

        // 通过 bind 的形式启动 service
        bindService(intent, conn, BIND_AUTO_CREATE);
    }

    public void playOrPause(View view) {
        if (mMusicBind.isPlaying()) {
            // 暂停音乐
            mMusicBind.pause();
            // 修改图标为即将播放状态
            ivPlayOrPause.setImageResource(android.R.drawable.ic_media_play);
        } else {
            // 播放音乐
            mMusicBind.play();
            // 修改图标为即将暂停状态
            ivPlayOrPause.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    public void preMusic(View view) {
        mMusicBind.previous();
        mCurSong = mMusicBind.getCurSong();
        upDateTitle();
    }

    public void nextMusic(View view) {
        mMusicBind.next();
        mCurSong = mMusicBind.getCurSong();
        upDateTitle();
    }

    public void stopMusic(View view) {
        mMusicBind.stop();
        // 修改图标为即将播放状态
        ivPlayOrPause.setImageResource(android.R.drawable.ic_media_play);
        updateCurTimeText(0);
        mSeekBar.setProgress(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void switchPlayMode(View view) {
        int playMode = PlayModeHelper.changePlayMode(currentPlayMode);
        currentPlayMode = playMode;

        String strPlayMode = PlayModeHelper.strPlayMode(currentPlayMode);
        tvPlayMode.setText(strPlayMode);

        mMusicBind.setPlayMode(currentPlayMode);
    }

//    @Override
//    public void onBackPressed() {
//        moveTaskToBack(true);
//    }

    public void back(View view) {
        this.finish();
    }
}
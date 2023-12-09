package com.example.mymusic.service;

import static com.example.mymusic.data.GlobalConstants.KEY_SONG_INDEX;
import static com.example.mymusic.data.GlobalConstants.KEY_SONG_LIST;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.mymusic.MusicPlayActivity;

import com.example.mymusic.R;
import com.example.mymusic.data.GlobalConstants;
import com.example.mymusic.data.Song;
import com.example.mymusic.listener.MyPlayerListener;
import com.example.mymusic.utils.PlayModeHelper;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MyMusicService extends Service {

    private static final String CHANNEL_ID = "song_play_channel";
    // 定义一个前台serviceId
    public static final int FOREGROUND_ID = 1;
    private MediaPlayer mMediaPlayer;
    private ArrayList<Song> mSongArrayList;//歌曲列表
    private int curSongIndex;//当前歌曲索引
    private int curPlayMode;//当前播放模式
    private MyPlayerListener mMyPlayerListener;
    private List<MyPlayerListener> mMyPlayerListeners = new ArrayList<>();
    private RemoteViews remoteView;
    private boolean haveNotification;//是否通知标记
    private Notification notification;
    private NotificationManager notificationManager;


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case GlobalConstants.ACTION_CLOSE_MUSIC:
                    stopForeground(true);
                    stopSelf();
                    break;
                case GlobalConstants.ACTION_PRE_MUSIC:
                    previous();
                    break;
                case GlobalConstants.ACTION_NEXT_MUSIC:
                    next();
                    break;
                case GlobalConstants.ACTION_PLAY_PAUSE_MUSIC:
                    if (isPlaying()) {
                        pause();
                    } else{
                        play();
                    }

                    break;
                case GlobalConstants.ACTION_START_PLAY_ACTIVITY:
                    startSongPlayActivity();
                    break;
            }
        }
    };

    private void startSongPlayActivity() {
        Intent startSongPlayIntent = new Intent(this, MusicPlayActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(KEY_SONG_LIST, mSongArrayList);
        bundle.putInt(KEY_SONG_INDEX, curSongIndex);
        startSongPlayIntent.putExtras(bundle);
        startSongPlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startSongPlayIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(startSongPlayIntent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                next();
            }
        });
        mSongArrayList = new ArrayList<>();

       // 广播注册
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GlobalConstants.ACTION_CLOSE_MUSIC);
        intentFilter.addAction(GlobalConstants.ACTION_NEXT_MUSIC);
        intentFilter.addAction(GlobalConstants.ACTION_PRE_MUSIC);
        intentFilter.addAction(GlobalConstants.ACTION_PLAY_PAUSE_MUSIC);
        intentFilter.addAction(GlobalConstants.ACTION_START_PLAY_ACTIVITY);
        registerReceiver(mReceiver, intentFilter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification();

        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotification() {
        if (haveNotification) {
            return;
        }
        //创建通知的渠道，也就是CHANNEL_ID
        notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "音乐播放通知", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        // 自定义通知中的内容View
        remoteView = new RemoteViews(getPackageName(), R.layout.notification_music_layout);
        // 设置通知中的TextView文字
        Song song = getCurSong();
        if (song != null) {
            remoteView.setTextViewText(R.id.tv_notification_title, song.getSongName());
        }
        // 点击通知中内容View中的子View，发出广播
        // 下一曲
        Intent nextIntent = new Intent(GlobalConstants.ACTION_NEXT_MUSIC);
        PendingIntent nextPendIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0);
        remoteView.setOnClickPendingIntent(R.id.iv_next, nextPendIntent);

        // 上一曲
        Intent preIntent = new Intent(GlobalConstants.ACTION_PRE_MUSIC);
        PendingIntent prePendIntent = PendingIntent.getBroadcast(this, 0, preIntent, 0);
        remoteView.setOnClickPendingIntent(R.id.iv_previous, prePendIntent);

        // 暂停、播放
        Intent playPauseIntent = new Intent(GlobalConstants.ACTION_PLAY_PAUSE_MUSIC);
        PendingIntent playPausePendIntent = PendingIntent.getBroadcast(this, 0, playPauseIntent, 0);
        remoteView.setOnClickPendingIntent(R.id.iv_play_pause, playPausePendIntent);

        // 关闭音乐
        Intent closeIntent = new Intent(GlobalConstants.ACTION_CLOSE_MUSIC);
        PendingIntent closePendIntent = PendingIntent.getBroadcast(this, 0, closeIntent, 0);
        remoteView.setOnClickPendingIntent(R.id.iv_close, closePendIntent);

        Intent startSongPlayIntent = new Intent(GlobalConstants.ACTION_START_PLAY_ACTIVITY);
        PendingIntent startSongPlayPendIntent = PendingIntent.getBroadcast(this, 0, startSongPlayIntent,  0);
        remoteView.setOnClickPendingIntent(R.id.rl_notification_title_container, startSongPlayPendIntent);


        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentText("这是音乐内容")
                .setContentTitle("这是音乐标题")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setCustomContentView(remoteView)
//                .setContentIntent(startSongPlayPendIntent)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_media_pause))
                .build();

        startForeground(FOREGROUND_ID, notification);
        haveNotification = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyMusicBind(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        // 解除注册
        unregisterReceiver(mReceiver);

    }

    private void updateMusicList(ArrayList<Song> songArrayList) {
        this.mSongArrayList = songArrayList;
    }

    public void updateCurrentMusicIndex(int index) {
        if (index < 0 || index >= mSongArrayList.size()) {
            return;
        }
        this.curSongIndex = index;
        // 播放该条歌曲
        Song song = mSongArrayList.get(curSongIndex);
        String songName = song.getSongName();
        AssetManager assetManager = getAssets();

        try {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            AssetFileDescriptor fileDescriptor = assetManager.openFd(songName);
            mMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());
            mMediaPlayer.prepare();
            mMediaPlayer.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 更新通知里的歌曲名
        if (remoteView != null) {
            remoteView.setTextViewText(R.id.tv_notification_title, song.getSongName());
        }
        // 然后更新通知
        notificationManager.notify(FOREGROUND_ID, notification);

        // 设置通知View中的图标
        notification.contentView.setImageViewResource(R.id.iv_play_pause,
                android.R.drawable.ic_media_pause);
        // 然后更新通知
        notificationManager.notify(FOREGROUND_ID, notification);
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public void pause() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }

        // 设置通知View中的图标
        notification.contentView.setImageViewResource(R.id.iv_play_pause,
                android.R.drawable.ic_media_play);
        // 然后更新通知
        notificationManager.notify(FOREGROUND_ID, notification);

        for (MyPlayerListener listener :
                mMyPlayerListeners) {
            listener.onPause(curSongIndex, getCurSong());
        }
    }

    public void play() {
        if (mMediaPlayer.isPlaying()) {
            return;
        }
        mMediaPlayer.start();
        // 设置通知View中的图标
        notification.contentView.setImageViewResource(R.id.iv_play_pause,
                android.R.drawable.ic_media_pause);
        // 然后更新通知
        notificationManager.notify(FOREGROUND_ID, notification);

        for (MyPlayerListener listener :
                mMyPlayerListeners) {
            listener.onPlay(curSongIndex, getCurSong());
        }

    }


    public void previous() {
        if (curPlayMode == PlayModeHelper.PLAY_MODE_CIRCLE) {
            updateCurrentMusicIndex(curSongIndex);
        } else if (curPlayMode == PlayModeHelper.PLAY_MODE_RANDOM) {
            int nextRandomIndex = getNextRandomIndex();
            updateCurrentMusicIndex(nextRandomIndex);
        } else {
            int preIndex = curSongIndex - 1;
            if (preIndex < 0) {
                preIndex = mSongArrayList.size() - 1;
            }
            updateCurrentMusicIndex(preIndex);
        }

        for (MyPlayerListener listener :
                mMyPlayerListeners) {
            listener.onPre(curSongIndex, getCurSong());
        }

    }

    public void next() {
        if (curPlayMode == PlayModeHelper.PLAY_MODE_CIRCLE) {
            updateCurrentMusicIndex(curSongIndex);
        } else if (curPlayMode == PlayModeHelper.PLAY_MODE_RANDOM) {
            int nextRandomIndex = getNextRandomIndex();
            updateCurrentMusicIndex(nextRandomIndex);
        } else {
            int nextIndex = curSongIndex + 1;
            if (nextIndex > mSongArrayList.size() - 1) {
                nextIndex = 0;
            }
            updateCurrentMusicIndex(nextIndex);
        }


        for (MyPlayerListener listener :
                mMyPlayerListeners) {
            listener.onNext(curSongIndex, getCurSong());
        }
    }


    private int getNextRandomIndex() {
        mSongArrayList.size();
        Random random = new Random();
        int randomIndex = random.nextInt(mSongArrayList.size());
        return randomIndex;
    }

    public void stop() {
        mMediaPlayer.stop();
        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Song getCurSong() {
        if (curSongIndex < 0 || curSongIndex >= mSongArrayList.size()) {
            return null;
        }
        return mSongArrayList.get(curSongIndex);
    }

    public int getCurSongIndex() {
        return curSongIndex;
    }


    private int getCurProgress() {
        return mMediaPlayer.getCurrentPosition();
    }

    private int getDuration() {
        return mMediaPlayer.getDuration();
    }

    private void seekTo(int progress) {
        mMediaPlayer.seekTo(progress);
    }

    private void setPlayMode(int mode) {
        this.curPlayMode = mode;
    }

    public void addPlayerListener(MyPlayerListener playerListener) {
        this.mMyPlayerListeners.add(playerListener);
    }

    public void removePlayerListener(MyPlayerListener playerListener) {
        this.mMyPlayerListeners.remove(playerListener);
    }

    public class MyMusicBind extends Binder {

        private MyMusicService mMusicService;

        public MyMusicBind(MyMusicService musicService) {
            mMusicService = musicService;
        }

        public void startPlay() {

        }

        public void updateMusicList(ArrayList<Song> songArrayList) {
            mMusicService.updateMusicList(songArrayList);
        }

        public void updateCurrentMusicIndex(int index) {
            mMusicService.updateCurrentMusicIndex(index);
        }

        public boolean isPlaying() {
            return mMusicService.isPlaying();
        }

        public void pause() {
            mMusicService.pause();
        }

        public void play() {
            mMusicService.play();
        }

        public void previous() {
            mMusicService.previous();
        }

        public void next() {
            mMusicService.next();
        }

        public void stop() {
            mMusicService.stop();
        }

        public Song getCurSong() {
            return mMusicService.getCurSong();
        }

        public int getCurSongIndex() {
            return mMusicService.getCurSongIndex();
        }

        public int getCurProgress() {
            return mMusicService.getCurProgress();
        }

        public int getDuration() {
            return mMusicService.getDuration();
        }

        public void seekTo(int progress) {
            mMusicService.seekTo(progress);
        }

        public void setPlayMode(int mode) {
            mMusicService.setPlayMode(mode);
        }

        public void addPlayerListener(MyPlayerListener playerListener) {
            mMusicService.addPlayerListener(playerListener);
        }

        public void removePlayerListener(MyPlayerListener playerListener) {
            mMusicService.removePlayerListener(playerListener);
        }
    }


}

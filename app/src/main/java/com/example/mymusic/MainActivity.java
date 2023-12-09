package com.example.mymusic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mymusic.adapter.MySongListAdapter;
import com.example.mymusic.data.GlobalConstants;
import com.example.mymusic.data.Song;
import com.example.mymusic.listener.MyPlayerListener;
import com.example.mymusic.service.MyMusicService;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRCVSongList;
    private LinearLayout mBottomContainer;
    private ImageView ivMusicIcon;
    private TextView tvMusicTitle;
    private MySongListAdapter mSongListAdapter;
    private ArrayList<Song> mSongArrayList;
    private MyMusicService.MyMusicBind mMusicBind;
    private boolean isPlaying = false;
    private Song curSong;
    private int curSongIndex;
    private ObjectAnimator rotationY;

    private MyPlayerListener mMyPlayerListener = new MyPlayerListener() {
        @Override
        public void onComplete(int songIndex, Song song) {
            tryUpdateBottomUI();
        }

        @Override
        public void onNext(int songIndex, Song song) {
            tryUpdateBottomUI();
        }

        @Override
        public void onPre(int songIndex, Song song) {
            tryUpdateBottomUI();
        }

        @Override
        public void onPause(int songIndex, Song song) {
            tryUpdateBottomUI();
        }

        @Override
        public void onPlay(int songIndex, Song song) {
            tryUpdateBottomUI();
        }
    };

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mMusicBind = (MyMusicService.MyMusicBind)iBinder;
            tryUpdateBottomUI();
            mMusicBind.addPlayerListener(mMyPlayerListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMusicBind.removePlayerListener(mMyPlayerListener);
        }
    };

    private void tryUpdateBottomUI() {
        if (mMusicBind != null) {
            isPlaying = mMusicBind.isPlaying();
            curSong = mMusicBind.getCurSong();
            curSongIndex = mMusicBind.getCurSongIndex();
            updateBottomUI();
        }
    }

    private void updateBottomUI() {
        if (isPlaying) {
            // 旋转动画，旋转图标
            if (rotationY == null) {
                rotationY = ObjectAnimator.ofFloat(ivMusicIcon, "rotationY", 0, 360);
                rotationY.setRepeatCount(ValueAnimator.INFINITE);
                rotationY.setDuration(1000);
                rotationY.setInterpolator(new LinearInterpolator());
                rotationY.start();
            }
        } else {
            // 停止旋转动画
            if (rotationY != null) {
                rotationY.cancel();
                rotationY = null;
            }
        }
        String songName = "还没选择歌曲呢~";
        if (curSong != null) {
             songName = curSong.getSongName();
        }
        tvMusicTitle.setText(songName);
        tvMusicTitle.setSelected(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
        initSongList();
        bindMusicService();

    }

    @Override
    protected void onResume() {
        super.onResume();
        tryUpdateBottomUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rotationY != null) {
            rotationY.cancel();
            rotationY = null;
        }
    }

    private void bindMusicService() {
        Intent intent = new Intent(this, MyMusicService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);
    }

    private void initData() {
        mSongArrayList = new ArrayList<>();
        mSongArrayList.add(new Song("crimes.mp3"));
        mSongArrayList.add(new Song("彼此的结局.mp3"));
        mSongArrayList.add(new Song("会不会.mp3"));
        mSongArrayList.add(new Song("空虚沸腾.mp3"));
        mSongArrayList.add(new Song("蓝色的海.mp3"));
    }

    private void initSongList() {
        mSongListAdapter = new MySongListAdapter(mSongArrayList, this);
        mSongListAdapter.setItemClickListener(new MySongListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Toast.makeText(MainActivity.this, "点击了"+position, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(MainActivity.this, MusicPlayActivity.class);
                intent.putExtra(GlobalConstants.KEY_SONG_LIST, mSongArrayList);
                intent.putExtra(GlobalConstants.KEY_SONG_INDEX, position);
                intent.putParcelableArrayListExtra(GlobalConstants.KEY_SONG_LIST, mSongArrayList);
                startActivity(intent);

            }
        });
        mRCVSongList.setAdapter(mSongListAdapter);
        mRCVSongList.setLayoutManager(new LinearLayoutManager(this));

    }

    private void initView() {
        mRCVSongList = findViewById(R.id.rcv_song_list);
        tvMusicTitle = findViewById(R.id.tv_bottom_song_name);
        ivMusicIcon = findViewById(R.id.iv_bottom_icon);
        mBottomContainer = findViewById(R.id.ll_bottom_container);
        mBottomContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MusicPlayActivity.class);
                intent.putExtra(GlobalConstants.KEY_SONG_LIST, mSongArrayList);
                intent.putExtra(GlobalConstants.KEY_SONG_INDEX, curSongIndex);
                intent.putParcelableArrayListExtra(GlobalConstants.KEY_SONG_LIST, mSongArrayList);
                startActivity(intent);
            }
        });
    }
}
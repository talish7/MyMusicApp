package com.example.mymusic.listener;

import com.example.mymusic.data.Song;

public interface MyPlayerListener {


    void onComplete(int songIndex, Song song);

    void onNext(int songIndex, Song song);

    void onPre(int songIndex, Song song);

    void onPause(int songIndex, Song song);

    void onPlay(int songIndex, Song song);

}

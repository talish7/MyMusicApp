package com.example.mymusic.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Bean类，Serializable, Parcelable两种测试
 */
public class Song implements Serializable, Parcelable {
    private String songName;

    public Song() {
    }

    public Song(String songName) {
        this.songName = songName;
    }

    protected Song(Parcel in) {
        songName = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    @Override
    public String toString() {
        return "Song{" +
                "songName='" + songName + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(songName);
    }
}

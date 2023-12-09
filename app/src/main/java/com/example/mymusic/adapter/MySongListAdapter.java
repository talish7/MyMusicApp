package com.example.mymusic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.mymusic.R;
import com.example.mymusic.data.Song;


import java.util.ArrayList;

public class MySongListAdapter extends RecyclerView.Adapter<MySongListAdapter.MySongItemViewHolder> {
    /**
     * 1.生成自己ViewHolder
     * 2.数据源，生成构造方法
     * 3.重写适配器三个方法
     */
    private ArrayList<Song> mSongArrayList;
    private Context mContext;
    private OnItemClickListener mItemClickListener;

    public MySongListAdapter(ArrayList<Song> songArrayList, Context context) {
        mSongArrayList = songArrayList;
        mContext = context;
    }

    @NonNull
    @Override
    public MySongItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.song_item_layout, parent, false);
        return new MySongItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MySongItemViewHolder holder, int position) {
        Song song = mSongArrayList.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return mSongArrayList == null ? 0 : mSongArrayList.size();
    }


    class MySongItemViewHolder extends RecyclerView.ViewHolder {

        private TextView mTvSongName;
        private LinearLayout llContainer;

        public MySongItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mTvSongName = itemView.findViewById(R.id.tv_song_name);
            llContainer = itemView.findViewById(R.id.ll_song_item_container);
            llContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClick(getAdapterPosition());
                    }
                }
            });
        }

        public void bind(Song song) {
            mTvSongName.setText(song.getSongName());
        }
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        mItemClickListener = itemClickListener;
    }


    public interface OnItemClickListener {
        void onItemClick(int position);
    }


}

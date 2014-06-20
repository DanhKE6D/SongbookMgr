package com.dql.dbutil;

/**
 * Created by dql on 9/27/13.
 */
import java.util.ArrayList;

public class DataHandler {

    ArrayList<SongID> mSongList = new ArrayList<SongID>();

    public void setData(ArrayList<SongID> l) {
        mSongList = l;
    }

    public ArrayList<SongID> getData() {
        return mSongList;
    }
}

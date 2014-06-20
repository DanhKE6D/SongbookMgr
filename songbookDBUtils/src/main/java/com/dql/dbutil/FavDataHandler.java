package com.dql.dbutil;

/**
 * Created by dql on 9/27/13.
 */
import java.util.ArrayList;

public class FavDataHandler {

    ArrayList<FavSongID> mSongList = new ArrayList<FavSongID>();

    public void setData(ArrayList<FavSongID> l) {
        mSongList = l;
    }

    public ArrayList<FavSongID> getData() {
        return mSongList;
    }
}

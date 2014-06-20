package com.dql.songbookmgr;

import android.content.Context;
import android.database.SQLException;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.dql.dbutil.FavDataHandler;
import com.dql.dbutil.DatabaseHandler;
import com.dql.dbutil.FavSongID;
import com.dql.dbutil.FavoriteDBHandler;
import com.dql.dbutil.SongID;
import com.dql.dbutil.SongbookCfg;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by dql on 9/27/13.
 */
public class AsyncFavSongbookReader extends AsyncTaskLoader<FavDataHandler> {
    private static final String TAG = "AsyncFavSongbookReader";
    FavDataHandler mDataHandler;
    String dbName = null;
    Context myCtx = null;
    FavoriteDBHandler db = null;

    public AsyncFavSongbookReader(Context context, String dbFileName) {
        super(context);
        this.myCtx = context;
        this.dbName = dbFileName;
        this.mDataHandler = new FavDataHandler();
    }

    public void setNewFavDBFileName(String dbFileName) {
        this.dbName = dbFileName;
    }

    @Override
    public FavDataHandler loadInBackground() {

        db = new FavoriteDBHandler(myCtx, SongbookCfg.getInstance().getPackageName(), dbName );
        ArrayList<FavSongID> songs;
        if (db != null)
            songs = (ArrayList<FavSongID>) db.getAllSongs();
        else {
            Log.d(TAG, "db is NULL!!!");
            songs = new ArrayList<FavSongID>();
        }
        Log.d(TAG, "Get songList completed, Number of Songs = " + songs.size());
        mDataHandler.setData(songs);
        return mDataHandler;

    }

    @Override
    public void deliverResult(FavDataHandler data) {
        super.deliverResult(data);
        //mlisListView.setAdapter(new DataBinder(mAcFragmentActivity, data
        //        .getData()));
    }
}


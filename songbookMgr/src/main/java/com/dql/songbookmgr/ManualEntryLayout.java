package com.dql.songbookmgr;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.dql.quickaction.QuickAction;

/**
 * Created by dql on 12/29/14.
 */
public class ManualEntryLayout extends Fragment  {

    private static final String TAG = "ManualEntryLayout";
    String myTag;
    static QuickAction mQuickAction;
    static final int PLAYLIST_PLAY = 0;

   @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // set up tag so that the host activity can call me back
        myTag = getTag();
        Log.i(TAG, "ManualEntryLayout.onCreate: myTag = " + myTag);
        ((SongbookMgrActivity) getActivity()).setTabPlaylistFragment(myTag);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.rmt_cmd_screen, null);
        return root;
    }
}

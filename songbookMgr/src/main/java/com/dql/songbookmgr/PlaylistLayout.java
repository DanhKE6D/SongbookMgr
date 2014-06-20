package com.dql.songbookmgr;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.dql.dbutil.DataHandler;
import com.dql.dbutil.DatabaseHandler;
import com.dql.dbutil.SongID;
import com.dql.dbutil.SongbookCfg;
import com.dql.quickaction.ActionItem;
import com.dql.quickaction.QuickAction;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by dql on 10/15/13.
 */
public class PlaylistLayout extends Fragment implements
        LoaderManager.LoaderCallbacks<DataHandler> {

    private static final String TAG = "PlaylistFragment";
    private static final int LOADER_PLAYLIST = 110;
    String myTag;
    static QuickAction mQuickAction;
    static final int PLAYLIST_PLAY       = 0;
    private static CustomListAdapter listAdapter = null;
    private static ArrayList<SongID> songList = null;
    private OnSongSelectedListener songSelectedListener;

    ListView listView;
    int selectSong = -1;


    public interface OnSongSelectedListener {
        public void onSongSelected(String myFragmentTag, SongID song);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            songSelectedListener = (OnSongSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSongSelectedListener");
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // set up tag so that the host activity can call me back
        myTag = getTag();
        Log.i(TAG, "PlaylistLayout.onCreate: myTag = " + myTag);
        ((SongbookMgrActivity) getActivity()).setTabPlaylistFragment(myTag);
    }

    void onLongListItemClick(View v, int position,long id) {

        selectSong = position;
        Log.i(TAG, "onLongListItemClick: selectSong = " + selectSong);
        // Toast.makeText(v.getContext(),"onLongListItemClick", Toast.LENGTH_SHORT).show();
        mQuickAction.show(v);
        mQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fav_songbook, null);
        listView = (ListView) root.findViewById(R.id.ListView2);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                onLongListItemClick(v, pos, id);
                return true;
            }

        });
        ActionItem replayAction = new ActionItem();
        replayAction.setTitle("Add Playlist");
        replayAction.setIcon(getResources().getDrawable(R.drawable.ic_add_playlist));


        mQuickAction = new QuickAction(getActivity());
        mQuickAction.addActionItem(replayAction);

        mQuickAction
                .setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
                    public void onItemClick(int pos) {

                        switch (pos) {
                            case PLAYLIST_PLAY:
                                if (selectSong > 0) {
                                    songSelectedListener.onSongSelected(myTag, songList.get(selectSong));
                                    selectSong = -1;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //registerForContextMenu(getListView());
        //mMediaServer = ((VlcKaraokeSongbookMgrActivity) getActivity()).getMediaServer();
        //if (mMediaServer != null) {
            Log.i(TAG, "PlaylistLayout.onActivityCreated().initLoader():");
            getActivity().getSupportLoaderManager().initLoader(LOADER_PLAYLIST, Bundle.EMPTY, this);
        //}
    }

    @Override
    public void onResume() {
        Log.i(TAG, "PlaylistLayout.onResume() - registerReceiver():");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "PlaylistLayout.onPause() - unregisterReceiver():");
        //getActivity().unregisterReceiver(mStatusReceiver);
        //mStatusReceiver = null;
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.playlist_options, menu);

    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        //MenuItem item = menu.findItem(R.id.menu_clear_playlist);

        //if (item != null) {
        //    item.setVisible(mMediaServer.inHostMode());
        //}

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                reload();
                return true;
            case R.id.menu_clear_playlist:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public Loader<DataHandler> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "PlaylistFragment.onCreateLoader():");
        if (songList != null) {
            songList.clear();
            redefineListAdapter();
        }
        AsyncPlaylistReader asyncPlaybookReader = new AsyncPlaylistReader(PlaylistLayout.this.getActivity());
        asyncPlaybookReader.forceLoad();
        return asyncPlaybookReader;
    }

    @Override
    public void onLoadFinished(Loader<DataHandler> loader, DataHandler arg1) {
        Log.i(TAG, "PlaylistFragment.onLoadFinished():");
        songList = arg1.getData();
        redefineListAdapter();
    }

    @Override
    public void onLoaderReset(Loader<DataHandler> loader) {
        listAdapter = null;
    }

    public void reload() {
        //if (mMediaServer != null) {
            Log.i(TAG, "PlaylistFragment.restartLoader():");
        if (songList != null) {
            songList.clear();
            redefineListAdapter();
        }
            getLoaderManager().restartLoader(LOADER_PLAYLIST, Bundle.EMPTY, this);
        //}
    }

    public static class AsyncPlaylistReader extends AsyncTaskLoader<DataHandler> {

        DataHandler mDataHandler;
        Context myCtx;

        public AsyncPlaylistReader(Context context) {
            super(context);
            myCtx = context;
            mDataHandler = new DataHandler();
        }

        @Override
        public DataHandler loadInBackground() {
            ArrayList<SongID> songs = new ArrayList<SongID>();
            final String cmd = "GET_PLAYLIST_HISTORY";
            String response = null;
            DataOutputStream outToServer = null;
            BufferedReader inFromServer = null;
            Socket clientSocket = null;

            try {
                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(SongbookCfg.getInstance().getServerName(),
                        SongbookCfg.SBCM_PORT_NO), SongbookCfg.TCP_DEF_CONN_TIMEOUT);
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                outToServer.writeBytes(cmd + '\n');
                Log.i(TAG, "Sent to server: " + cmd);
                response = inFromServer.readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //Log.i(TAG, "Exception occurred in reading playlist history");
            }

            finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                 e.printStackTrace();
                }
            }

            if ((response != null) && (!response.isEmpty())) {
                //Log.i(TAG, "Response from server: " + response);
                // response contains a list of songIDs, need to look them up in DB to get the SongID object
                // for each one of them
                ArrayList<String> mrlList = null;
                XmlParser xmlParser = new XmlParser();
                mrlList = xmlParser.parsePlaylistXmlStream(response);
                if ((mrlList != null) && !mrlList.isEmpty()) {
                    DatabaseHandler db = DatabaseHandler.getInstance(myCtx, SongbookCfg.getInstance().getPackageName());
                    for (String l : mrlList) {
                        SongID s = db.getSong(Integer.valueOf(l));
                        if (s != null)
                            songs.add(s);
                    }
                }
            }
            mDataHandler.setData(songs);
            return mDataHandler;
        }

        @Override
        public void deliverResult(DataHandler data) {
            super.deliverResult(data);

        }
    }

    void redefineListAdapter() {
        // listAdapter.notifyDataSetChanged();
        listAdapter = new CustomListAdapter(/* myCtx */ getActivity().getApplicationContext(), R.layout.custom_song_list , songList);
        listView.setAdapter(listAdapter);
        //Log.d(TAG, "Set adapter completed");
        listView.setTextFilterEnabled(true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
            }
        });

        listView.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                onLongListItemClick(v,pos,id);
                return true;
            }

        });
        listAdapter.notifyDataSetChanged();

    }
}

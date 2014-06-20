package com.dql.songbookmgr;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dql.dbutil.FavDataHandler;
import com.dql.dbutil.FavSongID;
import com.dql.dbutil.FavoriteDBHandler;
import com.dql.dbutil.SongID;
import com.dql.dbutil.SongbookCfg;
import com.dql.quickaction.ActionItem;
import com.dql.quickaction.QuickAction;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FavSongbookLayout extends Fragment  implements
        LoaderManager.LoaderCallbacks<FavDataHandler> {
    private static final String TAG = "FavSongbookLayout";
    protected static final int LOAD_FAV_SONGBOOK = 11;          // any number is fine
    ListView listView;
    private boolean songSortAscendingOrder = true;
    private OnSongSelectedListener songSelectedListener;
    private OnFavSongbookCmdListener favSongbookCmdListener;
    private ArrayList<FavSongID> songList = null;
    // want to queue the songs the in order they were selected
    private ArrayList<Integer> selectedSongList = new ArrayList<Integer>();
    private FavSongListAdapter listAdapter = null;
    //private PerformLoadSongTask m_task = null;
    AsyncFavSongbookReader asyncFavSongbookReader = null;
    QuickAction mQuickAction;
    private Context myCtx = null;
    static int selectedSongNumber = -1; 		// this is the list position selected by the user
    boolean multiSelectMode = false;
    String myTag;                               // tag for my fragment -- use it when communicating with activity
    public enum SelectedFavSongCmd {
        ToggleSound, SkipBackward, AddToPlaylist, PlaybackPause, SkipForward
    };

    public enum FavSongbookCmd {
        DeleteMyPage
    };

    String myFavDBFileName;
    int myLoaderID = 0;

    //private static final int TOGGLE_SOUND    = 0;
    //private static final int SKIP_BACKWARD   = 1;
    private static final int ADD_TO_PLAYLIST = 0;
    private static final int PLAYBACK_PAUSE  = 1;
    private static final int SKIP_FORWARD    = 2;
    private static final int UNDIM_SONG      = 3;
    private static final int SWITCH_TRACK    = 4;
    private static final int DELETE_SONG     = 5;

    public void addToFavSongbook(SongID song) {
        // add this song to favorite songbook
        // first find out if it is in out book already, only add if it is not in our book
        if (songList != null) {
            for (FavSongID s :  songList) {
                if ((s.getSong().getSinger().equalsIgnoreCase(song.getSinger())) &&
                        (s.getSong().getName().equalsIgnoreCase(song.getName()))) {
                    Log.i(TAG,song.getName() + " already in favorite songbook");
                    return;
                }
            }
        }
        final FavoriteDBHandler db = new FavoriteDBHandler(myCtx, SongbookCfg.getInstance().getPackageName(), myFavDBFileName );
        FavSongID newsong = new FavSongID();
        newsong.setSong(song.getID(), song.getName(), song.getSinger());

        db.addSong(newsong);
        songList.add(newsong);
        listAdapter.notifyDataSetChanged();
        // Toast.makeText(getActivity(),
        //		song.getName() + " added to Favorite", Toast.LENGTH_SHORT)
        //	.show();
    }

    public void updateSongbook(String dbFileName) {
        Log.i(TAG, "Updating Favorite songbook");
        myFavDBFileName = dbFileName;
        if (asyncFavSongbookReader == null) {
            Log.i(TAG, "Starting new load");

            asyncFavSongbookReader = new AsyncFavSongbookReader(FavSongbookLayout.this.getActivity(), myFavDBFileName);
            asyncFavSongbookReader.forceLoad();

        }
        else {
            Log.i(TAG, "Restarting load");
            // set new dbFileName
            asyncFavSongbookReader.setNewFavDBFileName(dbFileName);
        }
        getActivity().getSupportLoaderManager().restartLoader(myLoaderID, null, this);
    }

    @Override
    public Loader<FavDataHandler> onCreateLoader(int arg0, Bundle arg1) {
        Log.i(TAG, "onCreateLoader: LoaderID = " + myLoaderID + " myFavDBFileName = " + myFavDBFileName);
        asyncFavSongbookReader = new AsyncFavSongbookReader(FavSongbookLayout.this.getActivity(), myFavDBFileName);
        asyncFavSongbookReader.forceLoad();
        return asyncFavSongbookReader;
    }

    @Override
    public void onLoadFinished(Loader<FavDataHandler> arg0, FavDataHandler arg1) {
        Log.i(TAG, "onLoadFinished: LoaderID = " + myLoaderID);
        songList = arg1.getData();

        listAdapter = new FavSongListAdapter(getActivity(), R.layout.custom_song_list , songList);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                toggleSongSelection(position);
            }
        });

        listView.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                onLongListItemClick(v,pos,id);
                return true;
            }

        });
    }

    @Override
    public void onLoaderReset(Loader<FavDataHandler> arg0) {
        listAdapter = null;
    }

    public interface OnSongSelectedListener {
        public void onFavSongSelected(String myFragmentTag,FavSongID song, SelectedFavSongCmd cmd);
    }

    public interface OnFavSongbookCmdListener {
        public void onFavSongbookCmd(FavSongbookCmd cmd, String tag);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "FavSongbookLayout.onCreate");
        // set up tag so that the host activity can call me back
        myTag = getTag();
        Log.i(TAG, "FavSongbookLayout.onCreate: myTag = " + myTag);
        ((SongbookMgrActivity) getActivity()).setTabFavSongbookFragment(myTag);
        myFavDBFileName = FavoriteDBHandler.DEFAULT_DATABASE_NAME;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fav_songbook, null);
        setHasOptionsMenu(true);

        myFavDBFileName = ((SongbookMgrActivity) getActivity()).getFavBookDBName(myTag);
        myLoaderID = ((SongbookMgrActivity) getActivity()).getLoaderID(myTag);
        Log.i(TAG, "FavSongbookLayout.onCreateView: my DB Filename = " + myFavDBFileName + " myLoaderID = " + myLoaderID);
        myCtx = getActivity().getApplicationContext();

        listView = (ListView) root.findViewById(R.id.ListView2);

        ActionItem addToPlaylistAction = new ActionItem();
        addToPlaylistAction.setTitle("Add Playlist");
        addToPlaylistAction.setIcon(getResources().getDrawable(R.drawable.ic_add_playlist));
        /*
        ActionItem soundToggleAction = new ActionItem();
        soundToggleAction.setTitle("Sound");
        soundToggleAction.setIcon(getResources().getDrawable(R.drawable.ic_sound));

        ActionItem skipBackwardAction = new ActionItem();
        skipBackwardAction.setTitle("Rewind");
        skipBackwardAction.setIcon(getResources().getDrawable(R.drawable.ic_media_skip_backward));
        */
        ActionItem pauseAction = new ActionItem();
        pauseAction.setTitle("Pause");
        pauseAction.setIcon(getResources().getDrawable(R.drawable.ic_playback_pause));

        ActionItem skipForwardAction = new ActionItem();
        skipForwardAction.setTitle("Forward");
        skipForwardAction.setIcon(getResources().getDrawable(R.drawable.ic_media_skip_forward));

        ActionItem dimAction = new ActionItem();
        dimAction.setTitle("Un-Dim");
        dimAction.setIcon(getResources().getDrawable(R.drawable.ic_bright_star));

        ActionItem switchPlayStartModeAction = new ActionItem();
        switchPlayStartModeAction.setTitle("Switch Track");
        switchPlayStartModeAction.setIcon(getResources().getDrawable(R.drawable.ic_arrow_switch_right));

        ActionItem deleteAction = new ActionItem();
        deleteAction.setTitle("Delete");
        deleteAction.setIcon(getResources().getDrawable(R.drawable.ic_edit_delete));

        mQuickAction = new QuickAction(getActivity());
        //mQuickAction.addActionItem(soundToggleAction);
        //mQuickAction.addActionItem(skipBackwardAction);
        mQuickAction.addActionItem(addToPlaylistAction);
        mQuickAction.addActionItem(pauseAction);
        mQuickAction.addActionItem(skipForwardAction);
        mQuickAction.addActionItem(dimAction);
        mQuickAction.addActionItem(switchPlayStartModeAction);
        mQuickAction.addActionItem(deleteAction);

        // setup the action item click listener
        mQuickAction
                .setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
                    public void onItemClick(int pos) {

                        final FavSongID s = songList.get(selectedSongNumber);
                        switch (pos) {
                            case ADD_TO_PLAYLIST:
                                // Note selectedSongNumber is set in onLongListItemClick. It is only used
                                // when the user does not select anything and select ADD_TO_PLAYLIST
                                if (selectedSongList.isEmpty()) {
                                    selectedSongList.add(selectedSongNumber);
                                    s.setStatusBit(FavSongID.SONG_SELECTED);
                                }
                                sendSelectedSongsToPlaylist();
                                break;
                            /*
                            case TOGGLE_SOUND:
                            case SKIP_BACKWARD:
                            */
                            case SKIP_FORWARD:
                                songSelectedListener.onFavSongSelected(myTag, s, SelectedFavSongCmd.SkipForward);
                                break;
                            case PLAYBACK_PAUSE:
                                songSelectedListener.onFavSongSelected(myTag, s, SelectedFavSongCmd.PlaybackPause);
                                break;

                            case UNDIM_SONG:
                                unDimSelectedSong();
                                break;
                            case SWITCH_TRACK:
                                switchTrack();
                                break;
                            case DELETE_SONG:
                                deleteSelectedSong(getActivity());
                                break;

                        }
                    }
                });

        getActivity().getSupportLoaderManager().initLoader(myLoaderID, null, this);
        return root;
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
        try {
            favSongbookCmdListener = (OnFavSongbookCmdListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFavSongbookCmdListener");
        }
    }


    void onLongListItemClick(View v, int position,long id) {
        selectedSongNumber = position;
        // send it to the Favorite list
        // Toast.makeText(v.getContext(),"onLongListItemClick", Toast.LENGTH_SHORT).show();
        mQuickAction.show(v);
        mQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);
    }

    void unDimSelectedSong() {
        if (songList != null) {
            final FavoriteDBHandler db = new FavoriteDBHandler(myCtx, SongbookCfg.getInstance().getPackageName(), myFavDBFileName );
            for (FavSongID s :  songList) {
                if (s.getBit(FavSongID.SONG_SELECTED)) {
                    s.clearStatusBit(FavSongID.SONG_GRAYED_OUT);		// change the count so that the display goes back to normal
                    s.clearStatusBit(FavSongID.SONG_SELECTED);
                    db.updateSong(s);
                }
            }
            listAdapter.notifyDataSetChanged();
        }
        selectedSongList.clear();
    }

    void switchTrack() {
        if (songList != null) {
            final FavoriteDBHandler db = new FavoriteDBHandler(myCtx, SongbookCfg.getInstance().getPackageName(), myFavDBFileName );
            for (FavSongID s :  songList) {
                if (s.getBit(FavSongID.SONG_SELECTED)) {
                    boolean curTrack = s.getBit(FavSongID.SONG_PLAYED_IN_REVERSE_TRACK);
                    if (curTrack)
                        s.clearStatusBit(FavSongID.SONG_PLAYED_IN_REVERSE_TRACK);
                    else
                        s.setStatusBit(FavSongID.SONG_PLAYED_IN_REVERSE_TRACK);
                    s.clearStatusBit(FavSongID.SONG_SELECTED);
                    db.updateSong(s);
                }
            }
            listAdapter.notifyDataSetChanged();
        }
        selectedSongList.clear();
    }

    void deleteSelectedSong(Context ctx) {

        /////////////////////////////
        int noOfSongsDeleted = 0;
        if (songList != null) {
            for (FavSongID song :  songList) {
                if (song.getBit(FavSongID.SONG_SELECTED)) {
                    noOfSongsDeleted++;
                }
            }
            if (noOfSongsDeleted > 0) {
                final FavoriteDBHandler db = new FavoriteDBHandler(myCtx, SongbookCfg.getInstance().getPackageName(), myFavDBFileName );
                /////////////////////////////

                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setTitle("Song Delete Confirmation");
                builder.setIcon(R.drawable.ic_alert);
                builder.setMessage("OK to delete " + noOfSongsDeleted + " selected song(s)?");
                builder.setPositiveButton("Delete",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // delete this song and update the listview also
                                for (FavSongID s : songList) {
                                    if (s.getBit(FavSongID.SONG_SELECTED)) {
                                        s.clearStatusBit(FavSongID.SONG_SELECTED);
                                        db.deleteSong(s);
                                    }
                                }
                                updateSongbook(myFavDBFileName);
                                listAdapter.notifyDataSetChanged();
                            }
                        });

                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                // create alert dialog
                AlertDialog alertDialog = builder.create();
                // show it
                alertDialog.show();
            }
        }
        selectedSongList.clear();
    }

    void toggleSongSelection(int pos) {
        // toggle the selection bit of the current song
        // need to search through the selectedSongList to check to see if the song
        // is already selected. if the song is already selected, don't add another one
        // Note: don't want to add to the list the song that has been deselected also

        final FavSongID s = songList.get(pos);
        if (s.getBit(FavSongID.SONG_SELECTED)) {
            Log.i(TAG, "SongID: " + s.getSong().getID() + " Pos: " + pos + " was previously selected - now un-select");
            s.clearStatusBit(FavSongID.SONG_SELECTED);
            // goto our list and delete the node
            for (int i = 0; i < selectedSongList.size(); i++) {
                if (selectedSongList.get(i) == pos) {
                    // Log.i(TAG, "Removing " + pos + " from select list");
                    selectedSongList.remove(i);     // delete this node
                }
            }
        }
        else {
            s.setStatusBit(FavSongID.SONG_SELECTED);
            Log.i(TAG, "Adding SongID: " + s.getSong().getID() + " to select list");
            selectedSongList.add(pos);
            if (multiSelectMode) {
                if (selectedSongList.size() == 2) {
                    // must be exactly 2 to do the multi select
                    expandSelectionList();
                }
            }
        }

        listAdapter.notifyDataSetChanged();
    }

    void expandSelectionList() {
        // there should be exactly 2 elements in the list
        int t;
        FavSongID s;
        int n1 = selectedSongList.get(0);
        int n2 = selectedSongList.get(1);
        if (n1 > n2) {
            t = n1;
            n1 = n2;
            n2 = t;
        }
        if ((n2 - n1) > 1) {
            selectedSongList.clear();
            while (n1 <= n2) {
                selectedSongList.add(n1);
                songList.get(n1).setStatusBit(FavSongID.SONG_SELECTED);
                n1++;
            }
            listAdapter.notifyDataSetChanged();
        }
        multiSelectMode = false;
    }

    void sendSelectedSongsToPlaylist() {
        //FavoriteDBHandler.getInstance(myCtx, SongbookCfg.getInstance().getPackageName()).updateSong(s);
        if (!selectedSongList.isEmpty()) {
            Log.i(TAG, "sendSelectedSongsToPlaylist: myFavDBFileName = " + myFavDBFileName);
            final FavoriteDBHandler db = new FavoriteDBHandler(myCtx, SongbookCfg.getInstance().getPackageName(), myFavDBFileName );
            for (int i = 0; i < selectedSongList.size(); i++) {
                FavSongID s = songList.get(selectedSongList.get(i));
                if (s.getBit(FavSongID.SONG_SELECTED)) {
                    songSelectedListener.onFavSongSelected(myTag, s, SelectedFavSongCmd.AddToPlaylist); // send it to the playlist
                    s.clearStatusBit(FavSongID.SONG_SELECTED);
                    s.setStatusBit(FavSongID.SONG_GRAYED_OUT);
                    db.updateSong(s);
                }
            }
            listAdapter.notifyDataSetChanged();
        }
        selectedSongList.clear();
    }


    class FavSongListAdapter extends ArrayAdapter<FavSongID> {

        private Context mContext;
        private int id;
        private List <FavSongID>items ;
        static final int MAX_SINGER_NAME_LENGTH = 30;

        public FavSongListAdapter(Context context, int textViewResourceId , List<FavSongID> list ) {
            super(context, textViewResourceId, list);
            mContext = context;
            id = textViewResourceId;
            items = list ;
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {

            View mView = v ;
            float fontSize = SongbookCfg.getInstance().getDisplayTextSize();
            float fontSizeSinger = SongbookCfg.getInstance().getSingerDisplayTextSize();
            boolean longFmtSongID = SongbookCfg.getInstance().longFormatSongID();
            int backgroundColorforIDField, backgroundColor;

            if (mView == null) {
                LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                //LayoutInflater vi = LayoutInflater.from(mContext);
                mView = vi.inflate(id, null);
                ViewHolder holder = new ViewHolder();
                holder.layout = (LinearLayout) mView.findViewById(R.id.custom_song_id);
                holder.songID = (TextView) mView.findViewById(R.id.songID);
                holder.songName = (TextView) mView.findViewById(R.id.songName);
                holder.singer = (TextView) mView.findViewById(R.id.singer);
                mView.setTag(holder);

            }

            if (items.get(position) != null ) {

                ViewHolder holder = (ViewHolder) mView.getTag();
                holder.songID.setTextSize(fontSizeSinger);
                int c = Color.BLACK;

                if (items.get(position).getBit(FavSongID.SONG_GRAYED_OUT)) {
                    c = Color.GRAY;
                }
                holder.layout.setBackgroundColor(Color.parseColor(App.getContext().getResources().getString(R.color.app_background)));
                holder.songID.setTextColor(c);
                if (items.get(position).getBit(FavSongID.SONG_PLAYED_IN_REVERSE_TRACK))
                    backgroundColorforIDField = Color.parseColor(App.getContext().getResources().getString(R.color.songid_revtrack_background));


                else {
                    if (items.get(position).getBit(FavSongID.SONG_SELECTED))
                        backgroundColorforIDField = Color.parseColor(App.getContext().getResources().getString(R.color.fav_selected_bg));
                    else
                        backgroundColorforIDField = Color.parseColor(App.getContext().getResources().getString(R.color.app_background));
                }


                holder.songID.setBackgroundColor(backgroundColorforIDField);
                //holder.songID.setTypeface(null,Typeface.BOLD);
                if (longFmtSongID)
                    holder.songID.setText(String.format("%05d", items.get(position).getSong().getID()));
                else
                    holder.songID.setText(String.format("%04d", items.get(position).getSong().getID()));
                if (items.get(position).getBit(FavSongID.SONG_SELECTED)) {
                    backgroundColor = Color.parseColor(App.getContext().getResources().getString(R.color.fav_selected_bg));
                    holder.layout.setBackgroundColor(backgroundColor);
                }
                else {
                    backgroundColor = Color.parseColor(App.getContext().getResources().getString(R.color.app_background));
                    holder.layout.setBackgroundColor(backgroundColor);
                }
                //holder.songName.setWidth(holder.widthSongField);
                holder.songName.setBackgroundColor(backgroundColor);
                holder.songName.setTextSize(fontSize);
                holder.songName.setTextColor(c);
                holder.songName.setTypeface(null,Typeface.BOLD);
                holder.songName.setText(items.get(position).getSong().getName());

                holder.singer.setBackgroundColor(backgroundColor);
                holder.singer.setTextSize(fontSizeSinger);
                holder.singer.setTextColor(c);
                if (items.get(position).getSong().getSinger().length() > MAX_SINGER_NAME_LENGTH) {
                    holder.singer.setText( items.get(position).getSong().getSinger().substring(0, MAX_SINGER_NAME_LENGTH));
                }
                else
                    holder.singer.setText(items.get(position).getSong().getSinger());
            }

            return mView;
        }

        class ViewHolder {
            TextView songID;
            TextView songName;
            TextView singer;
            LinearLayout layout;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fav_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch(item.getItemId()) {
            /*
            case R.id.rmt_sound:
            case R.id.rmt_ff:
            case R.id.rmt_bwd:

                ArrayList<SongID> sl = new ArrayList<SongID>();
                sl.add(new SongID());   // don't care about song content
                SelectedFavSongCmd cmd;
                switch (item.getItemId()) {
                    case R.id.rmt_ff:
                        cmd = SelectedFavSongCmd.SkipForward;
                        break;
                    case R.id.rmt_bwd:
                        cmd = SelectedFavSongCmd.SkipBackward;
                        break;
                    default:
                        cmd = SelectedFavSongCmd.ToggleSound;
                        break;
                }
                // don't about fav song content
                songSelectedListener.onFavSongSelected(myTag, new FavSongID(), cmd);
                break;
            */
            case R.id.song_sort:
                Log.i(TAG, "song_sort:");
                if (songSortAscendingOrder)
                    Collections.sort(songList);
                else
                    Collections.sort(songList, Collections.reverseOrder());
                songSortAscendingOrder = !songSortAscendingOrder;
                // change icon display
                if (songSortAscendingOrder) {
                    item.setIcon(R.drawable.ic_sort_ascending);
                }
                else {
                    item.setIcon(R.drawable.ic_sort_descending);
                }
                listAdapter.notifyDataSetChanged();

                break;
            case R.id.fav_multi_select:
                multiSelectMode = !multiSelectMode;
                if (multiSelectMode) {
                    if (selectedSongList.size() == 2) {
                        // got 2 entries -- expand them out
                        expandSelectionList();
                    }
                    else
                        Toast.makeText(getActivity(),"Multi Selection ON", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.fav_restore:
                // first close the DB so that there is no conflict during restoring
                //db.close();
                // pass myTag around so that when startActivityForResult finishes, activity
                // know where to send new db file name to.
                intent = new Intent(getActivity(), FavSongbookDBRestoreActivity.class);
                intent.putExtra(FavSongbookDBRestoreActivity.CLIENT_TAG, myTag);
                getActivity().startActivityForResult(intent, SongbookMgrActivity.RESTORE_FAVSONGBOOK_DB);
                break;
            case R.id.fav_backup:
                intent = new Intent(getActivity(), FavSongbookDBBackupActivity.class);
                intent.putExtra(FavSongbookDBBackupActivity.DB_FILENAME, myFavDBFileName);
                getActivity().startActivityForResult(intent, SongbookMgrActivity.BACKUP_FAVSONGBOOK_DB);
                break;
            case R.id.remove_favbook:
            {
                // remove this page

                /////////////////////////////

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Fav Songbook Delete");
                builder.setIcon(R.drawable.ic_alert);
                builder.setMessage("OK to delete this Favorite Songbook?");
                builder.setPositiveButton("Delete",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                favSongbookCmdListener.onFavSongbookCmd(FavSongbookCmd.DeleteMyPage, myTag);
                            }
                        });

                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                // create alert dialog
                AlertDialog alertDialog = builder.create();
                // show it
                alertDialog.show();
            }

            break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

}

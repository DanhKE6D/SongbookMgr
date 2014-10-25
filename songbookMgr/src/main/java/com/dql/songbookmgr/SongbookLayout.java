package com.dql.songbookmgr;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.dql.dbutil.DatabaseHandler;
import com.dql.dbutil.DataHandler;
import com.dql.dbutil.FavoriteDBHandler;
import com.dql.dbutil.SongID;
import com.dql.dbutil.SongbookCfg;
import com.dql.quickaction.ActionItem;
import com.dql.quickaction.QuickAction;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewSwitcher;

public class SongbookLayout extends Fragment implements
        LoaderManager.LoaderCallbacks<DataHandler> {
	
	private static final String TAG = "SongbookLayout";
    private OnSongSelectedListener songSelectedListener;
    private OnSongbookCmdListener songbookCmdListener;
    private static ArrayList<SongID> songList = null, originalSongList = null;
    private static ArrayList<Integer> selectedSongList = new ArrayList<Integer>();
    private static CustomListAdapter listAdapter = null;
    private static ExpandableListAdapter expListAdapter = null;
    boolean expDisplayMode = true;
	ListView listView;
    ExpandableListView expListView;
	private boolean songSortAscendingOrder = true;
	private boolean singerSortAscendingOrder = true;
	private  static Context myCtx = null;
	static QuickAction mQuickAction;
	static SongID selectedSong = null;
    String myTag;
    boolean multiSelectMode = false;
    private ProgressDialog progressDialog;
    int myLoaderID = 0;
    List<String> listDataHeader;
    HashMap<String, List<SongID>>  listDataChild;
    private ViewSwitcher switcher;

	private enum ListSearchMode {
	    None, Songs, Singers
	};

	public enum SelectedSongCmd {
	    AddToFav, AddToPlaylist, PlaybackPause, SkipForward, StartOnAltTrack
	};

    public enum SongbookCmd {
        NewFavoriteBook
    };

    public enum SongSelectedListener_CmdStatus {
        CmdStatus_OK, CmdStatus_Failed, CmdStatus_BookNotExist
    };

    private static final int ADD_TO_FAVORITE = 0;
	//private static final int TOGGLE_SOUND    = 1;
	//private static final int SKIP_BACKWARD   = 2;
    private static final int ADD_TO_PLAYLIST = 1;
	private static final int PLAYBACK_PAUSE  = 2;
	private static final int SKIP_FORWARD    = 3;
	private static final int START_ON_ALTRACK = 4;

    public interface OnSongSelectedListener {
        public SongSelectedListener_CmdStatus onSongSelected(ArrayList<SongID> songList, SelectedSongCmd cmd);
    }
    public interface OnSongbookCmdListener {
        public void onSongbookCmd(SongbookCmd cmd);
    }

    public void updateSongbook() {
  	    Log.i(TAG, "Updating songbook");
        getActivity().getSupportLoaderManager().restartLoader(myLoaderID, null, this);
    }

    public void restoreOrignalBook() {
        // restore the original songbook -- done when home in click in main activity
        Log.i(TAG, "Home is clicked");
        if (!expDisplayMode) {
            expDisplayMode = true;      // force expandable view
            switcher.showPrevious();
        }
        redefineListAdapter();
        //BuildSongList(ListSearchMode.None, new String());
        //listAdapter.notifyDataSetChanged();
    }

    /////////////////////////////////////////////////////////////
    //
    // Set up loader manager callback for song list load from DB
    //
    /////////////////////////////////////////////////////////////

    @Override
    public Loader<DataHandler> onCreateLoader(int arg0, Bundle arg1) {
        progressDialog = new ProgressDialog(SongbookLayout.this.getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Loading from Database...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        AsyncSongbookReader asyncSongbookReader = new AsyncSongbookReader(SongbookLayout.this.getActivity());
        asyncSongbookReader.forceLoad();
        return asyncSongbookReader;
    }

    @Override
    public void onLoadFinished(Loader<DataHandler> arg0, DataHandler arg1) {
        songList = arg1.getData();
        // make the original songlist sorted in ascending order
        Collections.sort(songList);
        songSortAscendingOrder = !songSortAscendingOrder;
        originalSongList = new ArrayList<SongID>(songList);
        //for (SongID s : songList) {
        //    originalSongList.add(s);
        //}
        setupExpandableSonglist();
        redefineListAdapter();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void onLoaderReset(Loader<DataHandler> arg0) {
        listAdapter = null;
    }

    public static class AsyncSongbookReader extends AsyncTaskLoader<DataHandler> {
        DataHandler mDataHandler;

        public AsyncSongbookReader(Context context) {
            super(context);
            mDataHandler = new DataHandler();
        }
        @Override
        public DataHandler loadInBackground() {
            DatabaseHandler db = DatabaseHandler.getInstance(myCtx, SongbookCfg.getInstance().getPackageName());
            // if the database does not exist, copy it over from asset directory.
            // dql
            try {

                db.createDataBase();
            } catch (IOException ioe) {
                throw new Error("Unable to create database");
            }
            try {

                db.openDataBase();
            } catch(SQLException sqle) {
                throw sqle;
            }
            ArrayList<SongID> songs;
            songs = (ArrayList<SongID>) db.getAllSongs();
            Log.d(TAG, "Get songList completed");
            mDataHandler.setData(songs);
            return mDataHandler;
        }

        @Override
        public void deliverResult(DataHandler data) {
            super.deliverResult(data);
            //mlisListView.setAdapter(new DataBinder(mAcFragmentActivity, data
            //        .getData()));
        }
    }

    /////////////////////////////////////////////////////////////
    //
    // Set up expandable song listview. The original song list should be already
    // accented sort order when the list is returned from load finish()
    // Breaking up the sorted list into 28 bins -- 26 representing alphabet 'A' - 'Z'. The
    // first bin is anything < 'A' and the last bin is anything > 'Z'
    //
    /////////////////////////////////////////////////////////////
    void setupExpandableSonglist() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<SongID>>();
        Resources res = getResources();
        for(int i = 0; i < res.getStringArray(R.array.song_group_titles).length; i++){
            listDataHeader.add(res.getStringArray(R.array.song_group_titles)[i]);
        }
        // everything to 'A'
        int index = 0;
        ArrayList<SongID> songData = new ArrayList<SongID>();
        while ((index < originalSongList.size()) && originalSongList.get(index).getName().compareToIgnoreCase(listDataHeader.get(1)) < 0) {
            songData.add(originalSongList.get(index++));
        }
        listDataChild.put(listDataHeader.get(0), songData);
        Log.i(TAG, listDataHeader.get(0) + ": songData.size = " + songData.size());
        // between 'A' and 'Y'
        int n1 = 1, n2 = 2;
        while (n2 < listDataHeader.size() - 1) {
            ArrayList<SongID> s = new ArrayList<SongID>();
            while ((index < originalSongList.size()) && originalSongList.get(index).getName().compareToIgnoreCase(listDataHeader.get(n2)) < 0) {
                s.add(originalSongList.get(index++));
            }
            listDataChild.put(listDataHeader.get(n1), s);
            Log.i(TAG, listDataHeader.get(n1) + ": songData.size = " + s.size());
            n1++; n2++;
        }
        // Z needs to be handled differently
        n1 = listDataHeader.size()- 2;
        //Log.i(TAG, "n1 = " + n1 + " Header[] = " + listDataHeader.get(n1) + " Index = " + index);
        ArrayList<SongID> sz = new ArrayList<SongID>();
        while ((index < originalSongList.size()) && originalSongList.get(index).getName().startsWith(listDataHeader.get(n1))) {
            sz.add(originalSongList.get(index++));
        }
        listDataChild.put(listDataHeader.get(n1), sz);
        Log.i(TAG, listDataHeader.get(n1) + ": songData.size = " + sz.size());
        // anything bigger than 'Z' to end of list
        n1++;    // last one
        ArrayList<SongID> sl = new ArrayList<SongID>();
        while (index < originalSongList.size()) {
            sl.add(originalSongList.get(index++));
        }
        listDataChild.put(listDataHeader.get(n1), sl);
        Log.i(TAG, listDataHeader.get(n1) + ": songData.size = " + sl.size());

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "SongbookLayout.onCreate");
        // set up tag so that the host activity can call me back
        myTag = getTag();
        Log.i(TAG, "FavSongbookLayout.onCreate: myTag = " + myTag);
        ((SongbookMgrActivity) getActivity()).setTabSongbookFragment(myTag);

    }
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
	
		// set up tag so that the host activity can call me back
        ViewGroup root;
		setHasOptionsMenu(true);
        expDisplayMode = true;      // force expandable view to start because the first view in
                                    // out layout
        root = (ViewGroup) inflater.inflate(R.layout.main_songbook, null);
        switcher = (ViewSwitcher) root.findViewById(R.id.profileSwitcher);
        expListView = (ExpandableListView) root.findViewById(R.id.lvExp);
        listView = (ListView) root.findViewById(R.id.ListView1);
        //if (expDisplayMode) {
        //    root = (ViewGroup) inflater.inflate(R.layout.main_songbook_explv, null);
        //    expListView = (ExpandableListView) root.findViewById(R.id.lvExp);
        //}
        //else {
		//    root = (ViewGroup) inflater.inflate(R.layout.main_songbook, null);
        //    listView = (ListView) root.findViewById(R.id.ListView1);
        //}

        myLoaderID = ((SongbookMgrActivity) getActivity()).getLoaderID(myTag);

   	    // If originalSongList is not empty, we must have ran it once. copy the list over instead of
   	    // rereading from db.
  	    //Log.i(TAG, "before getting view"); 

        myCtx = getActivity().getApplicationContext();
  	    //Log.i(TAG, "after getting view");
		ActionItem addToPlaylistAction = new ActionItem();
		addToPlaylistAction.setTitle("Add Playlist");
		addToPlaylistAction.setIcon(getResources().getDrawable(R.drawable.ic_add_playlist));
		
		// Add action item
		ActionItem addAction = new ActionItem();
		addAction.setTitle("Add Fav");
		addAction.setIcon(getResources().getDrawable(R.drawable.ic_favorites_add));

		//ActionItem soundToggleAction = new ActionItem();
		//soundToggleAction.setTitle("Sound");
		//soundToggleAction.setIcon(getResources().getDrawable(R.drawable.ic_sound));
	
		//ActionItem skipBackwardAction = new ActionItem();
		//skipBackwardAction.setTitle("Rewind");
		//skipBackwardAction.setIcon(getResources().getDrawable(R.drawable.ic_media_skip_backward));

		ActionItem pauseAction = new ActionItem();
		pauseAction.setTitle("Pause");
		pauseAction.setIcon(getResources().getDrawable(R.drawable.ic_playback_pause));
		
		ActionItem skipForwardAction = new ActionItem();
		skipForwardAction.setTitle("Forward");
		skipForwardAction.setIcon(getResources().getDrawable(R.drawable.ic_media_skip_forward));
		
		//ActionItem terminalAction = new ActionItem();
		//terminalAction.setTitle("Terminal");
		//terminalAction.setIcon(getResources().getDrawable(R.drawable.ic_terminal));
		
		ActionItem switchPlayStartModeAction = new ActionItem();
		switchPlayStartModeAction.setTitle("Play Mode");
		switchPlayStartModeAction.setIcon(getResources().getDrawable(R.drawable.ic_arrow_switch_right));		
		
		mQuickAction = new QuickAction(getActivity());
		mQuickAction.addActionItem(addAction);
		//mQuickAction.addActionItem(soundToggleAction);
		//mQuickAction.addActionItem(skipBackwardAction);
		mQuickAction.addActionItem(addToPlaylistAction);
		mQuickAction.addActionItem(pauseAction);
		mQuickAction.addActionItem(skipForwardAction);
		mQuickAction.addActionItem(switchPlayStartModeAction);
		// setup the action item click listener
		mQuickAction
				.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
					public void onItemClick(int pos) {
                        /*
                        // 2013-09-12: need more work here. When the favorite songbook is not available
                        // must be able to create one and insert song to it at the same time.

                        if (pos  == SelectedSongCmd.AddToFav.ordinal()) {
                            // add to favorite -- check to see if any favorite book is available
                            Log.i(TAG, "Adding song favorite songbook");
                            if (!((SongbookMgrActivity) getActivity()).favoriteSongbookAvailable()) {
                                songbookCmdListener.onSongbookCmd(SongbookCmd.NewFavoriteBook);
                                Toast.makeText(myCtx,"Creating new Favorite Songbook", Toast.LENGTH_SHORT).show();
                            }
                        }
                        */
						//songSelectedListener.onSongSelected(selectedSong, SelectedSongCmd.values()[pos]);
						switch (pos) {
							case ADD_TO_PLAYLIST:
                                sendSelectedSongsToPlaylist();
								break;
							case ADD_TO_FAVORITE:
                                sendSelectedSongsToFavorite();
								break;
                            default:
                                ArrayList<SongID> sl = new ArrayList<SongID>();
                                sl.add(selectedSong);
                                songSelectedListener.onSongSelected(sl, SelectedSongCmd.values()[pos]);
								break;

						}


					}
				});		
   	    if (originalSongList != null) {
            setupExpandableSonglist();
   	    	redefineListAdapter();
   	    }
   	    else {
            getActivity().getSupportLoaderManager().initLoader(myLoaderID, null, this);
   	    }
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
            songbookCmdListener = (OnSongbookCmdListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSongbookCmdListener");
        }
    }

    void sendSelectedSongsToPlaylist() {
        ArrayList<SongID> sl = new ArrayList<SongID>();
        if (!selectedSongList.isEmpty()) {
            Log.i(TAG, "sendSelectedSongsToPlaylist:");
            for (int i = 0; i < selectedSongList.size(); i++) {
                sl.add(songList.get(selectedSongList.get(i)));

            }
        }
        else {
            // Add at least one song
            sl.add(selectedSong);
        }
        songSelectedListener.onSongSelected(sl, SelectedSongCmd.AddToPlaylist); // send it to the playlist
        // This is kinda complicated now. In expanded view mode, we disable multi-select.
        // multi-select is only enable in linear list view
        if (!expDisplayMode)
            clearSelectIndicators(listAdapter);
    }

    void sendSelectedSongsToFavorite() {
        ArrayList<SongID> sl = new ArrayList<SongID>();
        if (!selectedSongList.isEmpty()) {
            Log.i(TAG, "sendSelectedSongsToPlaylist:");
            for (int i = 0; i < selectedSongList.size(); i++) {
                sl.add(songList.get(selectedSongList.get(i)));
            }

        }
        else {
            // Add at least one song
            sl.add(selectedSong);
        }
        if (songSelectedListener.onSongSelected(sl, SelectedSongCmd.AddToFav) ==
                SongSelectedListener_CmdStatus.CmdStatus_BookNotExist) {
            // do it one more time because the previous call created the fav book already
            //Toast.makeText(myCtx,"Creating new Favorite Songbook", Toast.LENGTH_LONG).show();
            //songSelectedListener.onSongSelected(sl, SelectedSongCmd.AddToFav);
        }
        if (!expDisplayMode)
            clearSelectIndicators(listAdapter);
    }

	void redefineListAdapter() {
        if (expDisplayMode) {
            expListAdapter = new ExpandableListAdapter(getActivity().getApplicationContext(), listDataHeader, listDataChild);

            // setting list adapter
            expListView.setAdapter(expListAdapter);
            // Listview Group click listener
            expListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

                @Override
                public boolean onGroupClick(ExpandableListView parent, View v,
                                            int groupPosition, long id) {
                    /*
                    Toast.makeText(getApplicationContext(),
                    "Group Clicked " + listDataHeader.get(groupPosition),
                    Toast.LENGTH_SHORT).show();
                    */
                    return false;
                }
            });

            // Listview Group expanded listener
            expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

                @Override
                public void onGroupExpand(int groupPosition) {
                    /*
                    Toast.makeText(getActivity().getApplicationContext(),
                            listDataHeader.get(groupPosition) + " Expanded",
                            Toast.LENGTH_SHORT).show();
                    */
                }
            });

            // Listview Group collasped listener
            expListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {

                @Override
                public void onGroupCollapse(int groupPosition) {
                    /*
                    Toast.makeText(getActivity().getApplicationContext(),
                            listDataHeader.get(groupPosition) + " Collapsed",
                            Toast.LENGTH_SHORT).show();
                    */

                }
            });

            // Listview on child click listener
            expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

                @Override
                public boolean onChildClick(ExpandableListView parent, View v,
                                            int groupPosition, int childPosition, long id) {
                    // TODO Auto-generated method stub
                    /*
                    Toast.makeText(
                            getActivity().getApplicationContext(),
                            listDataHeader.get(groupPosition)
                                    + " : "
                                    + listDataChild.get(
                                    listDataHeader.get(groupPosition)).get(
                                    childPosition), Toast.LENGTH_SHORT)
                            .show();
                    */
                    return false;
                }
            });
            expListView.setOnItemLongClickListener(new ExpandableListView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                        int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                        int childPosition = ExpandableListView.getPackedPositionChild(id);
                        selectedSong = listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition);
                        // You now have everything that you would as if this was an OnChildClickListener()
                        // Add your logic here.

                        // Return true as we are handling the event.
                        //selectedSong = new SongID(songList.get(position).getID(), songList.get(position).getName(), songList.get(position).getSinger());
                        // send it to the Favorite list
                        // Toast.makeText(v.getContext(),"onLongListItemClick", Toast.LENGTH_SHORT).show();
                        mQuickAction.show(view);
                        mQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);
                        /*
                        Toast.makeText(
                                getActivity().getApplicationContext(),
                                listDataHeader.get(groupPosition)
                                        + " * "
                                        + listDataChild.get(
                                        listDataHeader.get(groupPosition)).get(
                                        childPosition), Toast.LENGTH_SHORT)
                                .show();
                        */
                        return true;
                    }

                    return false;
                }
            });
            expListAdapter.notifyDataSetChanged();
        }
        else {
            // full list
    	    // listAdapter.notifyDataSetChanged();
  	        listAdapter = new CustomListAdapter(/* myCtx */ getActivity().getApplicationContext(), R.layout.custom_song_list , songList);
  	        listView.setAdapter(listAdapter);
  	        //Log.d(TAG, "Set adapter completed");
  	        listView.setTextFilterEnabled(true);
            // Make sure we clear the selection indicator before clearing selection list
            if (selectedSongList.size() > 0) {
                clearSelectIndicators(listAdapter);
            }
  	        listView.setOnItemClickListener(new OnItemClickListener() {
  	    	public void onItemClick(AdapterView<?> parent, View view,
  	    										int position, long id) {
  	    		// When clicked, show a toast with the TextView text
  	    		//Toast.makeText(getApplicationContext(),
  	    		//		((TextView) view).getText(), Toast.LENGTH_SHORT).show();
                toggleSongSelection(position);
  	    		//SongID song = songList.get(position);
  	    		//String songSelected = song.getID() + " " + song.getName();
  	    		//longToast(myCtx, songSelected);
  	    		//Log.d(TAG, "OnItemClickListener: position = " + String.valueOf(position));
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
	
	private static void longToast(Context ctx, CharSequence message) {
		//Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Toast toast = Toast.makeText(ctx,message, Toast.LENGTH_SHORT);
       // toast.setGravity(Gravity.CENTER, toast.getXOffset() / 2, toast.getYOffset());

        TextView textView = new TextView(ctx);
        textView.setBackgroundColor(Color.DKGRAY);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(25);
        Typeface typeface = Typeface.create("serif", Typeface.BOLD);
        textView.setTypeface(typeface);
        textView.setPadding(10, 10, 10, 10);
        textView.setText(message);
        toast.setView(textView);
        toast.show();
	}

    static void clearSelectIndicators(final CustomListAdapter adapter) {

        for (int i = 0; i < selectedSongList.size(); i++) {
             final SongID s = songList.get(selectedSongList.get(i));
             s.unselectSong();

        }
        adapter.notifyDataSetChanged();
        selectedSongList.clear();
    }

    void toggleSongSelection(int pos) {
        // toggle the selection bit of the current song
        // need to search through the selectedSongList to check to see if the song
        // is already selected. if the song is already selected, don't add another one
        // Note: don't want to add to the list the song that has been deselected also

        final SongID s = songList.get(pos);
        if (s.isSongSelected()) {
            Log.i(TAG, "SongID: " + s.getID() + " Pos: " + pos + " was previously selected - now un-select");
            s.unselectSong();
            // goto our list and delete the node
            for (int i = 0; i < selectedSongList.size(); i++) {
                if (selectedSongList.get(i) == pos) {
                    // Log.i(TAG, "Removing " + pos + " from select list");
                    selectedSongList.remove(i);     // delete this node
                }
            }
        }
        else {
            s.selectSong();
            //String songSelected = s.getID() + " " + s.getName();
            //longToast(myCtx, songSelected);
            Log.i(TAG, "Adding SongID: " + s.getID() + " to select list");
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
        SongID s;
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
                songList.get(n1).selectSong();
                n1++;
            }
            listAdapter.notifyDataSetChanged();
        }
        multiSelectMode = false;
    }

    private static void onLongListItemClick(View v, int position,long id) {
		selectedSong = new SongID(songList.get(position).getID(), songList.get(position).getName(), songList.get(position).getSinger());
  		// send it to the Favorite list
  		// Toast.makeText(v.getContext(),"onLongListItemClick", Toast.LENGTH_SHORT).show();
		mQuickAction.show(v);
		mQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);
	}

    // =========================================================================================
    // build a new song list based on search criteria
    // 2013/10/02: implement range search option:
    // if the search criteria contain | char as the first char, it will be evaluate as follows
    //  ie.|A-C  : all songs start with an 'A' or 'a' to songs start with 'C' or 'c' inclusively
    //     |C-   : all songs from 'C' or 'c' to end of list
    //     |A    : all songs starts with an 'A' or 'a'
    //     |-A   : all songs from the beginning to 'A' or 'a' inclusively
    // otherwise, normal search
    // =========================================================================================

    private void BuildSongList(ListSearchMode mode, String text) {
    	songList.clear();
    	if (text.isEmpty() || (mode == ListSearchMode.None)) {
            // Collections.copy(songList, originalSongList);
    		// restore original song list
            for (SongID s : originalSongList)
            	songList.add(s);
    	}
        else if (text.charAt(0) == '|') {
            // range search
            char [] srchChArray = text.toCharArray();
            int srchArySize = text.length();
            boolean beginToChar = false, fromCharToEnd = false, seeSecondChar = false, seeFirstChar = false;
            boolean fromCharToChar = false;
            for (int i = 1; i < srchArySize; i++ ) {
                switch (srchChArray[i]) {
                    case '-':
                        if (i == 1) {
                            beginToChar = true;
                            //Log.i(TAG, "Seeing beginToChar");
                        }
                        else if (i == 2) {
                             fromCharToEnd = true;
                            //Log.i(TAG, "Seeing fromCharToEnd");
                        }

                        break;
                    default:
                        if (i == 1) {
                            seeFirstChar = true;
                            //Log.i(TAG, "Seeing seeFirstChar");
                        }
                        else if (i == 2) {
                            seeSecondChar = true;
                            //Log.i(TAG, "Seeing seeSecondChar");
                        }
                        else if (i == 3) {
                            fromCharToChar = true;
                            //Log.i(TAG, "Seeing fromCharToChar");
                        }
                        break;
                }
            }
            if (seeFirstChar && fromCharToEnd && fromCharToChar) {
                // |A-C  : all songs start with an 'A' or 'a' to songs start with 'C' or 'c' inclusively
                Log.i(TAG, "|A-C  : all songs start with an 'A' or 'a' to songs start with 'C' or 'c' inclusively");
                char startChar = Character.toUpperCase(srchChArray[1]);
                char endChar = Character.toUpperCase(srchChArray[3]);
                int j = 0;
                if (startChar > endChar) {
                    char tmp = startChar;
                    startChar = endChar;
                    endChar = tmp;
                }
                while (j < originalSongList.size()) {
                    if (Character.toUpperCase(originalSongList.get(j).getName().charAt(0)) < startChar)
                        j++;
                    else
                        break;
                }
                for (int k = j; k < originalSongList.size(); k++) {
                    SongID s = originalSongList.get(k);
                    if (Character.toUpperCase(s.getName().charAt(0)) <= Character.toUpperCase(endChar))
                        songList.add(s);
                    else
                        break;

                }
            }
            else if (seeFirstChar && fromCharToEnd) {
                //  |C-   : all songs from 'C' or 'c' to end of list
                Log.i(TAG, "|C-   : all songs from 'C' or 'c' to end of list");
                char startChar = Character.toUpperCase(srchChArray[1]);
                int j = 0;
                while (j < originalSongList.size()) {
                    if (Character.toUpperCase(originalSongList.get(j).getName().charAt(0)) < startChar)
                        j++;
                    else
                        break;
                }
                for (int k = j; k < originalSongList.size(); k++) {
                    songList.add(originalSongList.get(k));
                }
            }
            else if (seeFirstChar) {
                // search for all songs starting with Char
                // |A    : all songs starts with an 'A' or 'a'
                Log.i(TAG, "|A    : all songs starts with an 'A' or 'a'");
                for (SongID s :  originalSongList) {
                    // search for both song names and singers
                    if (s.getName().toUpperCase().startsWith(Character.toString(srchChArray[1]).toUpperCase()))
                        songList.add(s);
                }
            }
            else if (beginToChar && seeSecondChar) {
                //  |-A   : all songs from the beginning to 'A' or 'a' inclusively
                Log.i(TAG, "|-A   : all songs from the beginning to 'A' or 'a' inclusively");
                for (int j = 0; j < originalSongList.size(); j++) {
                    SongID s = originalSongList.get(j);
                     if (Character.toUpperCase(s.getName().charAt(0)) <= Character.toUpperCase(srchChArray[2]))
                        songList.add(s);
                    else
                        break;

                }

            }

        }
    	else {
            for (SongID s :  originalSongList) {
                // search for both song names and singers
                if (TextUtil.flattenToAscii(s.getName()).toUpperCase().contains(text.toUpperCase()))
                    songList.add(s);
                if (TextUtil.flattenToAscii(s.getSinger()).toUpperCase().contains(text.toUpperCase()))
                    songList.add(s);
    		}
    	}
	    redefineListAdapter();
    }


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.song_view, menu);

     }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        final MenuItem i = item;

		switch(i.getItemId()) {
            /*
            case R.id.rmt_sound:
            case R.id.rmt_ff:
            case R.id.rmt_bwd:

                ArrayList<SongID> sl = new ArrayList<SongID>();
                sl.add(new SongID());   // don't care about song content
                SelectedSongCmd cmd;
                switch (i.getItemId()) {
                    case R.id.rmt_ff:
                        cmd = SelectedSongCmd.SkipForward;
                        break;
                    case R.id.rmt_bwd:
                        cmd = SelectedSongCmd.SkipBackward;
                        break;
                    default:
                        cmd = SelectedSongCmd.ToggleSound;
                        break;
                }
                songSelectedListener.onSongSelected(sl, cmd);

                break;
            */
			case R.id.song_sort:
                performSort(R.id.song_sort, i);
				break;
		    	
		    case R.id.singer_sort:
                performSort(R.id.singer_sort, i);
		    	break;
		    		
		    case R.id.search:
                // want to switch over to flat view first
                Log.i(TAG, "1. song_search:");
                if (expDisplayMode) {
                    // was in expanded display mode
                    expDisplayMode = false;
                    switcher.showNext();
                    // 20141025: dql - want to start search with full list of songs
                    songList = new ArrayList<SongID>(originalSongList);
                    redefineListAdapter();
                }
                item.setActionView(R.layout.search_param);
                final AutoCompleteTextView txtSearch = (AutoCompleteTextView) item.getActionView().findViewById(R.id.search_edit_text);
                //txtSearch.setAdapter(listAdapter);
                /**
                 * Enabling Search Filter
                 * */
                txtSearch.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                        // When user changed the Text
                        listAdapter.getFilter().filter(cs);
                    }

                    @Override
                    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                                  int arg3) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void afterTextChanged(Editable arg0) {
                        // TODO Auto-generated method stub
                    }
                });
                txtSearch.setTextColor(Color.BLACK);
                txtSearch.requestFocus();
                // Setting an action listener
                txtSearch.setOnEditorActionListener(new OnEditorActionListener(){
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        Log.i(TAG, "onEditorAction:");
                        if ((actionId == EditorInfo.IME_ACTION_SEARCH) || ( event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                                    txtSearch.getWindowToken(), 0);
                            i.collapseActionView();
                            String s = v.getText().toString();
                            StringBuilder searchString = new StringBuilder();

                            boolean seenSpaceBefore = false;
                            for (int i = 0; i < s.length(); i++) {
                                if (s.charAt(i) != ' ') {
                                    searchString.append(s.charAt(i));
                                    seenSpaceBefore = false;
                                }
                                else {
                                    if (!seenSpaceBefore) {
                                        // ignore if there is more than one space chars in a row
                                        seenSpaceBefore = true;
                                        if (i != 0) {
                                            // throw away leading spaces
                                            searchString.append(' ');
                                        }
                                    }
                                }
                            }
                            //Log.i(TAG, "search text = " + searchString.toString());
                            BuildSongList(ListSearchMode.Songs, searchString.toString());
                            listAdapter.notifyDataSetChanged();
                            return true;
                        }
                        else {
                            return false;
                        }
                    }

                });
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
		    	break;
            case R.id.multi_select:
                if (expDisplayMode) {
                    Toast.makeText(getActivity(),"M-S Only works in flat view mode", Toast.LENGTH_SHORT).show();
                }
                else {
                    multiSelectMode = !multiSelectMode;
                    if (multiSelectMode) {
                        if (selectedSongList.size() > 2) {
                            Toast.makeText(getActivity(),"Too many selections in list!", Toast.LENGTH_SHORT).show();
                            multiSelectMode = false;
                        }
                        if (selectedSongList.size() == 2) {
                            // got 2 entries -- expand them out
                            expandSelectionList();
                        }
                        else
                            Toast.makeText(getActivity(),"Multi Selection ON", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.songbook_info:
                // display number of songs in db
                Log.i(TAG, "Songbook Info");
                new AlertDialog.Builder(getActivity())
                        .setIcon(R.drawable.ic_get_info)
                        .setTitle("Songbook Info")
                        .setMessage("There are " + originalSongList.size() + " song(s) in the current collection")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
            case R.id.new_favsongbook:
                songbookCmdListener.onSongbookCmd(SongbookCmd.NewFavoriteBook);
                break;
		    		
		    default:
			   	return super.onOptionsItemSelected(item);
	    	}
	    	return true;
	    	
	    }

    private void performSort(int sortMode, final MenuItem item) {
        // want to switch over to flat view first
        if (expDisplayMode) {
            // was in expanded display mode
            expDisplayMode = false;
            switcher.showNext();
            redefineListAdapter();
        }
        PerformSortTask sortTask = new PerformSortTask();
        sortTask.sortItem = item;
        sortTask.execute(sortMode);
    }

    private class PerformSortTask extends AsyncTask<Integer, Void, Void> {
        ProgressDialog progressDialog;
        //private int progress;			// 100 is completed
        int sortMode;
        MenuItem sortItem;

        @Override
        protected void onPreExecute() {
            // perhaps show a dialog with a progress bar
            // to let your users know something is happening
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Sorting Songbook...");
            progressDialog.setCancelable(false);
            progressDialog.show();


        }

        @Override
        protected Void doInBackground(Integer... params) {
            sortMode = params[0];
            switch (sortMode) {
                case R.id.song_sort:
                    Log.i(TAG, "song_sort:");
                    if (songSortAscendingOrder)
                        Collections.sort(songList);
                    else
                        Collections.sort(songList, Collections.reverseOrder());
                    songSortAscendingOrder = !songSortAscendingOrder;
                    break;
                case R.id.singer_sort:
                    Log.i(TAG, "singer_sort:");
                    if (singerSortAscendingOrder)
                        Collections.sort(songList, SongID.SongIDComparator);
                    else
                        Collections.sort(songList, Collections.reverseOrder(SongID.SongIDComparator));
                    singerSortAscendingOrder = !singerSortAscendingOrder;
                    break;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void Void) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }

                    switch (sortMode) {

                        case R.id.song_sort:
                            if (sortItem != null) {
                                // change icon display
                                if (songSortAscendingOrder) {
                                    sortItem.setIcon(R.drawable.ic_sort_ascending);
                                }
                                else {
                                    sortItem.setIcon(R.drawable.ic_sort_descending);
                                }
                            }
                            break;
                        case R.id.singer_sort:
                            if (sortItem != null) {
                                // change icon display
                                if (singerSortAscendingOrder) {
                                    sortItem.setIcon(R.drawable.ic_people_sort_ascending);
                                }
                                else {
                                    sortItem.setIcon(R.drawable.ic_people_sort_descending);
                                }
                            }
                            break;

                    }
                    listAdapter.notifyDataSetChanged();

                }
            });
        }

    }
}

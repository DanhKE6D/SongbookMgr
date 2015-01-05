package com.dql.songbookmgr;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.dql.dbutil.DatabaseHandler;
import com.dql.dbutil.FavSongID;
import com.dql.dbutil.FavoriteDBHandler;
import com.dql.dbutil.SongID;
import com.dql.dbutil.SongbookCfg;
import com.dql.filechooser.FileChooser;
import com.dql.songbookmgr.FavSongbookLayout.SelectedFavSongCmd;
import com.dql.songbookmgr.SongbookLayout.SelectedSongCmd;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class SongbookMgrActivity extends FragmentActivity implements
	SongbookLayout.OnSongSelectedListener, SongbookLayout.OnSongbookCmdListener,
    FavSongbookLayout.OnSongSelectedListener, FavSongbookLayout.OnFavSongbookCmdListener,
    PlaylistLayout.OnSongSelectedListener {

    static final String TAG = "SongbookMgrActivity";
    static final String FAVBOOK_TABNAME = "FAV BOOK";
    protected static final int LOAD_SONGBOOK = 100;          // loader ID for main songbook
    public static final int LOADER_PLAYLIST = 110;           // loader ID for playlist -- loader ID for favorite
                                                             // songbook starts at 0
    public static final int IMPORT_SONGBOOK        = 1;
    public static final int SONGINDEX_CFG          = 2;
    public static final int RESTORE_SONGBOOK_DB    = 3;
    public static final int BACKUP_SONGBOOK_DB     = 4;
    public static final int RESTORE_FAVSONGBOOK_DB = 5;
    public static final int BACKUP_FAVSONGBOOK_DB  = 6;
    static final String HELP_FILENAME       = "SongIndexHelpFile.txt";
    static final float DEF_FONT_SIZE        =  12.0f;

    //private static final String TERMINAL     = "TERM";


    //static final int MAIN_SONGBOOK_ENTRY = 1;

    static final int MIN_NUM_OF_TABS     = 3;
    static final int PLAYBACK_ENTRY      = 1;
    static final int PLAYLIST_ENTRY      = 1;
    static final int MAIN_SONGBOOK_ENTRY = 2;

    ArrayList<String> favDBNames = new ArrayList<String>();

	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;

    private enum FragmentLayoutType {
        FragUtils, FragManualEntry, FragMainSongbook, FragFavoriteSongbook
    };

    class TabListInfo {
        FragmentLayoutType type;
        Class <?> clss;
        String    tabTitle;
        String    fragmentTag;
        String    dbFileName;         // for favorite songbook
        int loaderID;                 // unique loader ID for this tag
        TabListInfo(FragmentLayoutType t, Class <?> clss, String title, int id) {
            this.type = t;
            this.clss = clss;
            this.tabTitle = title;
            this.fragmentTag = null;
            this.loaderID = id;

        }

        Class<?> getClss() {
            return this.clss;
        }

        String getTitle() {
            return this.tabTitle;
        }

        void setTitle(String title) {
            // want to get rid of the last period indicating the filename extension
            int index = title.lastIndexOf(".");
            if (index < 0) {
                index = title.length();
            }
            this.tabTitle = title.substring(0, index);
        }

        void setFragmentTag(String tag) {
            this.fragmentTag = tag;
        }

        String getFragmentTag() {
            return this.fragmentTag;
        }

        void setDBFileName(String s) {
            this.dbFileName = s;
        }

        String getDBFileName() {
            return this.dbFileName;
        }
        void setLoaderID(int id) {
            this.loaderID = id;
        }

        int getLoaderID() {
            return this.loaderID;
        }
    }

    class FavBookListInfo {
        String title;
        String tag;

        FavBookListInfo (String t, String tag) {
            this.title = t;
            this.tag = tag;
        }
        String getTitle() {
            return this.title;
        }
        String getTag() {
            return this.tag;
        }

    }

    ArrayList <TabListInfo> fragmentTabList = new ArrayList<TabListInfo>();

    // static int prevKeyCode = KeyEvent.KEYCODE_0; // anything but KEYCODE_BACK

	SongbookCfg cfg = SongbookCfg.getInstance();
    KeygenSendThread sendTask = null;

    static int bookNumber = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // we should also read all the FavBooks DB names
	    readConfigFromSharePreferences();
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		//setContentView(R.layout.activity_songbook_mgr);
	    //setUpView();
	    //setTab();
        // Inflate your custom layout
        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(
                R.layout.actionbar_layout,
                null);

        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.pager);
        setContentView(mViewPager);
        
        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        /*
        // dql--original
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        bar.setHomeButtonEnabled (true);
        */
        bar.setDisplayShowHomeEnabled(false);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowCustomEnabled(true);
        bar.setCustomView(actionBarLayout);

        fragmentTabList.add(new TabListInfo(FragmentLayoutType.FragUtils, UtilsLayout.class, "UTILITIES", 0));
        fragmentTabList.add(new TabListInfo(FragmentLayoutType.FragManualEntry, ManualEntryLayout.class, "MANUAL ENTRY", 0));
        fragmentTabList.add(new TabListInfo(FragmentLayoutType.FragMainSongbook, SongbookLayout.class, "SONGBOOK", LOAD_SONGBOOK));
        if (favDBNames.size() > 0) {
            bookNumber = 1;
            // recreate all the fav book tabs that was open in previous sessions
            for (int i = 0; i < favDBNames.size(); i++) {
                TabListInfo t = new TabListInfo(FragmentLayoutType.FragFavoriteSongbook, FavSongbookLayout.class,
                                                FAVBOOK_TABNAME+bookNumber, bookNumber);
                bookNumber++;
                t.setDBFileName(favDBNames.get(i));
                t.setTitle(favDBNames.get(i));
                fragmentTabList.add(t);
            }
        }
        //fragmentTabList.add(new TabListInfo(FavSongbookLayout.class, "FAV SONGBOOK"));

        mTabsAdapter = new TabsAdapter(this, mViewPager);

        for (TabListInfo i : fragmentTabList) {
            mTabsAdapter.addTab(bar.newTab().setText(i.getTitle()), i.getClss(), null );
        }

        /*
        mTabsAdapter.addTab(bar.newTab().setText("UTILITIES"),
                UtilsLayout.class, null);
        mTabsAdapter.addTab(bar.newTab().setText("SONGBOOK"),
                SongbookLayout.class, null);
        mTabsAdapter.addTab(bar.newTab().setText("FAV SONGBOOK"),
        		FavSongbookLayout.class, null);
        */
        mViewPager.setCurrentItem(MAIN_SONGBOOK_ENTRY);

        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
        // start the keygen send task for the first time
        sendTask = KeygenSendThreadFactory.createKeygenSendThread(cfg.getMediaServerType());
        sendTask.setContext(SongbookMgrActivity.this);
        sendTask.start();
	}

    public void onHomeClick( View view ) {
        // onClick code goes here.
        // Tell the main songbook layout to re-read its songbook
        mViewPager.setCurrentItem(MAIN_SONGBOOK_ENTRY);
        String tag = fragmentTabList.get(MAIN_SONGBOOK_ENTRY).getFragmentTag();
        SongbookLayout fsl = (SongbookLayout) getSupportFragmentManager().findFragmentByTag(tag);
        if (fsl != null) {
            fsl.restoreOrignalBook();
            //Toast.makeText(SongbookMgrActivity.this, "Restore Songbook", Toast.LENGTH_SHORT).show();
        }
    }

    public void onSoundClick( View view ) {
        sendTask.send(KeygenSendThread.SOUND_TOGGLE);
        Toast.makeText(SongbookMgrActivity.this, "Toggle Sound", Toast.LENGTH_SHORT).show();
    }

    public void onForwardClick( View view ) {
        // onClick code goes here.
        sendTask.send(KeygenSendThread.FORWARD);
        Toast.makeText(SongbookMgrActivity.this, "Forward", Toast.LENGTH_SHORT).show();
    }

    public void onRewindClick( View view ) {
        // onClick code goes here.
        sendTask.send(KeygenSendThread.REWIND);
        Toast.makeText(SongbookMgrActivity.this, "Rewind", Toast.LENGTH_SHORT).show();
    }

    void saveFavSongboksTabInfoToSharePref() {
        int numOfFavbooks = getNumberOfFavoriteBooks();

        SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE).edit();
        prefs.putInt("NoOfFavBooks", numOfFavbooks);
        if (numOfFavbooks > 0 ) {
            int curBookNum = 1;
            for (TabListInfo ti : fragmentTabList ) {
                if (ti.type == FragmentLayoutType.FragFavoriteSongbook) {
                    prefs.putString(FAVBOOK_TABNAME+curBookNum, ti.getDBFileName());
                    curBookNum++;
                }
            }
        }
        prefs.commit();
    }

    /* Issue 29472: 	ViewPager / ActionBar, Menu Items not displaying */
    /* http://code.google.com/p/android/issues/detail?id=29472 */
    @Override
    public void supportInvalidateOptionsMenu() {
        mViewPager.post(new Runnable() {

            @Override
            public void run() {
                SongbookMgrActivity.super.supportInvalidateOptionsMenu();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendTask.stopSendThread();
        Log.i(TAG, "SongbookMgrActivity is being destroyed. Saving Fav Books DB names");
        // going to save the database name to SharePreferences
        saveFavSongboksTabInfoToSharePref();
    }

    /////////////////////////////////////////////////////////////////
    //
    // Tab/ViewPager adapter/helper stuff
    //
    /////////////////////////////////////////////////////////////////

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    public class TabsAdapter extends FragmentPagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }
        static final int MAX_NUMBER_OF_PAGER_TO_KEEP = 5;

        public TabsAdapter(FragmentActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = activity.getActionBar();
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
            mViewPager.setOffscreenPageLimit(MAX_NUMBER_OF_PAGER_TO_KEEP);        // dql - want to keep 5 screens without reloading
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            Log.i(TAG, "addTab, mTabs.size() = " + mTabs.size());
            mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        public void changeTabTitle(int position, String newTitle) {
            Log.i(TAG, "changeTabTitle, position = " + position);
            Tab tab = mActionBar.getTabAt(position);
            tab.setText(newTitle);
        }

        public void removeTab(int position) {
            Log.i(TAG, "removeTab, position = " + position);
            Tab tab = mActionBar.getTabAt(position);
            mTabs.remove(position);
            notifyDataSetChanged();         // Must notify dataset change before removing the tab/pager
            mActionBar.removeTab(tab);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // TODO Auto-generated method stub
            super.destroyItem(container, position, object);
            Log.i(TAG, "destroyItem, position = " + position + " Tag = " + object.toString());
            FragmentManager manager = /*((Fragment) object). getFragmentManager() */ getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction trans = manager.beginTransaction();
            trans.remove((Fragment) object);
            trans.commit();
            // dql: hack -- set the fragment tag to null also because the fragment got destroyed
            //if (position > MAIN_SONGBOOK_ENTRY) {
            //    TabListInfo i = fragmentTabList.get(position);
            //    i.setFragmentTag(null);
            //}
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // TODO Auto-generated method stub

        }

        @Override
        public int getItemPosition(Object object) {
            // Log.i(TAG, "getItemPosition");
            /*
            Fragment fragment = (Fragment) object;
            for (int i = 0; i < mTabs.size(); i++) {
                if (mTabs.get(i).clss == fragment.getClass()) {
                    Log.i(TAG, "getItemPosition, i = " + i);
                    return i;
                }
            }
            */
            // The tab must have been removed
            return POSITION_NONE;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPageSelected(int position) {
            // TODO Auto-generated method stub
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            Object tag = tab.getTag();
            Log.i(TAG, "onTabSelected,  mTabs.size() =  " + mTabs.size());
            for (int i = 0; i < mTabs.size(); i++) {
                if (mTabs.get(i) == tag) {
                    Log.i(TAG, "onTabSelected,  found the tag for this tab, i  =  " + i);
                    mViewPager.setCurrentItem(i);
                    break;
                }
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            // TODO Auto-generated method stub

        }

        @Override
        public Fragment getItem(int position) {
            Log.i(TAG, "TabsAdapter.getItem: position = " + position);
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

    }

    /////////////////////////////////////////////////////////////////
    //
    // These helper tag functions are used by the SongbookMgrActivity
    // to tell the correct fragments to update its data
    //
    /////////////////////////////////////////////////////////////////

    int getNumberOfFavoriteBooks() {
        int i = 0;
        for (TabListInfo ti : fragmentTabList) {
            if (ti.type == FragmentLayoutType.FragFavoriteSongbook) {
                i++;
            }
        }
        return i;
    }



    void updateDBNameInFragmentTabList(final String clientTag, final String newDBFileName) {
        Log.i(TAG, "updateDBNameInFragmentTabList: clientTag = " + clientTag);
        for (int index = 0; index < fragmentTabList.size(); index++) {
            TabListInfo i = fragmentTabList.get(index);
            if (i.type == FragmentLayoutType.FragFavoriteSongbook) {
                Log.i(TAG, "updateDBNameInFragmentTabList: index = " + index + " Tag = " + i.getFragmentTag());
                if (i.getFragmentTag().compareTo(clientTag) == 0) {
                    i.setDBFileName(newDBFileName);
                    i.setTitle(newDBFileName);
                    mTabsAdapter.changeTabTitle(index, i.getTitle());
                    break;
                }
            }
        }
    }

    public void setTabPlaylistFragment(String t) {
        TabListInfo tab = fragmentTabList.get(PLAYLIST_ENTRY);
        tab.setFragmentTag(t);
    }

    public void setTabSongbookFragment(String t) {
        TabListInfo tab = fragmentTabList.get(MAIN_SONGBOOK_ENTRY);
        tab.setFragmentTag(t);
    }

    public void setTabFavSongbookFragment(String t) {
        // go through the fragmentTabList, find the one that did not has fragmentTag set
        // and set that fragment tag
        Log.i(TAG, "setTabFavSongbookFragment: tag  = " + t);
        for (TabListInfo i : fragmentTabList) {
            if (i.type == FragmentLayoutType.FragFavoriteSongbook) {
                // it is a favorite songbook type
                if (i.getFragmentTag() == null) {
                    Log.i(TAG, "Setting fragment tag for Favorite songbook, tag  = " + t);
                    i.setFragmentTag(t);
                    break;
                }
                else if (i.getFragmentTag().compareTo(t) == 0) {
                    Log.i(TAG, "This fragment tag already exist, tag  = " + t + " DB filename = " + i.getDBFileName());
                    break;

                }
            }
        }
    }

    public int getLoaderID (String tag) {
        Log.i(TAG, "getLoaderID, tag  = " + tag);
        for (TabListInfo i : fragmentTabList) {
            if (i.getFragmentTag() != null) {
                if (i.getFragmentTag().compareTo(tag) == 0) {
                    // found it
                    Log.i(TAG, "getLoaderID, tag  = " + tag + " loader ID = " + i.getLoaderID());
                    return i.getLoaderID();
                }
            }

        }
        return LOAD_SONGBOOK;
    }

    public String getFavBookDBName(String tag) {
        Log.i(TAG, "getFavBookDBName, tag  = " + tag);
        for (TabListInfo i : fragmentTabList) {
            if (i.type == FragmentLayoutType.FragFavoriteSongbook) {
                // it is a favorite songbook type
                if (i.getFragmentTag() != null) {
                    if (i.getFragmentTag().compareTo(tag) == 0) {
                        // found it
                        Log.i(TAG, "getFavBookDBName, tag  = " + tag + " DB filename = " + i.getDBFileName());
                        return i.getDBFileName();
                    }
                }
            }
        }
        return FavoriteDBHandler.DEFAULT_DATABASE_NAME;
    }

    private ArrayAdapter<String> favBooksAdapter(String favBooksArray[]) {

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, favBooksArray) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                // setting the ID and text for every items in the list
                String item = getItem(position);
                // visual settings for the list item
                TextView listItem = new TextView(SongbookMgrActivity.this);

                listItem.setText(item);
                //listItem.setTextSize(22);
                listItem.setPadding(10, 10, 10, 10);
                listItem.setTextColor(Color.WHITE);
                return listItem;
            }
        };

        return adapter;
    }

    boolean addToFavoriteSongbook(final ArrayList<SongID> l) {
        // first find out if the favorite songbook tab is available
        if (fragmentTabList.size() > 2) {
            // there is at least one -- just add to first one right now
            //FavSongbookLayout f = (FavSongbookLayout) getSupportFragmentManager().findFragmentByTag(tag);
            //if (f != null)
            //    f.addToFavSongbook(song);
            //else
            //    Log.d(TAG, "Unable to find fragment tag for this Favorite songbook, tag = " + tag);
            final ArrayList<FavBookListInfo> favSongbookList = new ArrayList<FavBookListInfo>();
            for (TabListInfo i : fragmentTabList) {
                if (i.type == FragmentLayoutType.FragFavoriteSongbook) {
                    // it is a favorite songbook type
                    favSongbookList.add(new FavBookListInfo(i.getTitle(), i.getFragmentTag()));
                }
            }
            if (favSongbookList.size() == 1) {
                // there is only one Favorite book, add to this one then
                FavSongbookLayout f = (FavSongbookLayout) getSupportFragmentManager().
                        findFragmentByTag(favSongbookList.get(0).getTag());
                if (f != null) {
                    for (SongID s : l) {
                        f.addToFavSongbook(s);
                    }
                    Toast.makeText(SongbookMgrActivity.this, l.size() + " song(s) added to Fav Book", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                // construct a popup window then let the user select which one to add to
                // convert to simple array
                String popUpContents[] = new String[favSongbookList.size()];
                int index = 0;
                Log.i(TAG, "addToFavoriteSongbook: list size = " + favSongbookList.size() );
                for (FavBookListInfo i : favSongbookList) {
                    Log.i(TAG, "index = " + index + " title = " + i.getTitle() );
                    popUpContents[index++] = i.getTitle();
                }
                final PopupWindow favBookPopupWindow = new PopupWindow(this);
                 // the drop down list is a list view
                ListView listViewFavBooks = new ListView(this);

                // set our adapter and pass our pop up window contents
                listViewFavBooks.setAdapter(favBooksAdapter(popUpContents));

                // set the item click listener
                listViewFavBooks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // click on the list
                        // there is only one Favorite book, add to this one then
                        Log.i(TAG, "addToFavoriteSongbook: Add single");
                        FavSongbookLayout f = (FavSongbookLayout) getSupportFragmentManager().
                                findFragmentByTag(favSongbookList.get(position).getTag());
                        if (f != null) {
                            for (SongID s : l) {
                                f.addToFavSongbook(s);
                            }
                            Toast.makeText(SongbookMgrActivity.this, l.size() + " song(s) added to Fav Book", Toast.LENGTH_SHORT).show();
                        }
                        favBookPopupWindow.dismiss();
                    }
                });

                // some other visual settings
                favBookPopupWindow.setFocusable(true);
                favBookPopupWindow.setWidth(300);
                favBookPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

                // set the list view as pop up window content
                favBookPopupWindow.setContentView(listViewFavBooks);
                //favBookPopupWindow.showAsDropDown(listViewFavBooks, -5, 0);
                favBookPopupWindow.showAtLocation(listViewFavBooks, Gravity.CENTER, 0, 0);

            }


        }
        else {
            Log.e(TAG,"No favorite songbook available");
            // 2013-09-12: need more work here. If the book is not available, create new one but can't insert yet
            // There is not one available. Need to create one
            onNewFavoriteSongbook();
            //FavSongbookLayout f = (FavSongbookLayout) getSupportFragmentManager().findFragmentByTag(tabFavSongbookFragment);
            //f.addToFavSongbook(song);
            return false;
        }
        return true;

    }

    @Override
    public SongbookLayout.SongSelectedListener_CmdStatus onSongSelected(ArrayList<SongID> l, SelectedSongCmd cmd) {
        // make sure we get the latest parameters
        boolean longFmtSongID = SongbookCfg.getInstance().longFormatSongID();
        SongbookLayout.SongSelectedListener_CmdStatus status = SongbookLayout.SongSelectedListener_CmdStatus.CmdStatus_OK;
        Log.i(TAG, "onSongSelected:");
        switch (cmd) {
            case AddToFav:
                // 2013-09-10. Add to favorite. If the favorite songbook is not there, need to create
                // a new one before adding the song it.
                if (!addToFavoriteSongbook(l))
                    status = SongbookLayout.SongSelectedListener_CmdStatus.CmdStatus_BookNotExist;
                break;
            /*
            case ToggleSound:
                cmdQueue.add(SOUND_TOGGLE);
                break;
            case SkipBackward:
                cmdQueue.add(REWIND);
                break;
            */
            case AddToPlaylist:
                String songCmd;
                for (SongID s : l) {
                    if (longFmtSongID)
                        songCmd = String.format("1%05d", s.getID());
                    else
                        songCmd = String.format("1%04d", s.getID());
                    Log.i(TAG, "AddToPlaylist: Song Name = " + s.getName() + ", ID = " + s.getID());
                    sendTask.send(songCmd);
                    Toast.makeText(SongbookMgrActivity.this, s.getName() + " added to playlist", Toast.LENGTH_SHORT).show();
                }
                break;
            case PlaybackPause:
                sendTask.send(KeygenSendThread.PAUSE);
                break;
            case SkipForward:
                sendTask.send(KeygenSendThread.FORWARD);
                break;
            case StartOnAltTrack:
                sendTask.send(KeygenSendThread.START_ON_TRACK2);
                break;

        }
        return status;

    }

    @Override
    public void onFavSongbookCmd(FavSongbookLayout.FavSongbookCmd cmd, String tag) {
        switch (cmd) {
            case DeleteMyPage:
                Log.i(TAG, "onFavSongbookCmd: DeleteMyPage, tag = " + tag);
                for (int index = 2; index < fragmentTabList.size(); index++) {
                    TabListInfo i = fragmentTabList.get(index);
                    Log.i(TAG, "onFavSongbookCmd: DeleteMyPage, i = " + index + " Tag = " + i.getFragmentTag());
                    if (i.type == FragmentLayoutType.FragFavoriteSongbook) {
                        // it is a favorite songbook type
                        if (i.getFragmentTag().compareTo(tag) == 0) {
                            Log.i(TAG, "onFavSongbookCmd: removing tab position = " + index);
                            // 2013/09/21 - dql
                            // I don't understand how removing fragment from viewpager. Tried all kind
                            // of methods without success. This hack is going to remove the last Fav Book
                            // I does not look pretty, but it works.
                            if (index != fragmentTabList.size() - 1) {
                                for (int j = 2; j < fragmentTabList.size() - 1; j++) {
                                    Log.i(TAG, "onFavSongbookCmd: shifting filename up, j = " + j);
                                    fragmentTabList.get(j).setDBFileName(fragmentTabList.get(j+1).getDBFileName());
                                    fragmentTabList.get(j).setTitle(fragmentTabList.get(j + 1).getTitle());
                                    // 2013/09/27 - dql - need to move the loaderID over so that loading is correct
                                    // after deletion
                                    fragmentTabList.get(j).setLoaderID(fragmentTabList.get(j + 1).getLoaderID());
                                    mTabsAdapter.changeTabTitle(j, fragmentTabList.get(j).getTitle());
                                }
                                index = fragmentTabList.size() - 1;
                            }
                            fragmentTabList.remove(index);
                            mTabsAdapter.removeTab(index);
                            return;

                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onSongbookCmd(SongbookLayout.SongbookCmd cmd) {
        switch (cmd) {
            case NewFavoriteBook:
                onNewFavoriteSongbook();
                break;
        }
    }

    @Override
    public void onFavSongSelected(String fragmentTag, FavSongID song, SelectedFavSongCmd cmd) {
        // make sure we get the latest parameters
        boolean longFmtSongID = SongbookCfg.getInstance().longFormatSongID();

        //Log.i(TAG, "onFabSongSelected: song = " + song.getSong().toString());
        switch (cmd) {
            /*
            case ToggleSound:
                cmdQueue.add(SOUND_TOGGLE);
                break;
            case SkipBackward:
                cmdQueue.add(REWIND);
                break;
            */
            case AddToPlaylist:
                // 2013/11/22 - search db to get more accurate ID for the selected song
                DatabaseHandler db = DatabaseHandler.getInstance(getApplicationContext(),
                        SongbookCfg.getInstance().getPackageName());
                int songID = db.getSongID(song.getSong().getName(), song.getSong().getSinger());
                if (songID > 0) {
                    String songCmd;
                    if (longFmtSongID)
                        songCmd = String.format("1%05d", songID);
                    else
                        songCmd = String.format("1%04d", songID);
                    sendTask.send(songCmd);

                }
                else
                    Toast.makeText(SongbookMgrActivity.this, song.getSong().getName() + " is NOT in DB", Toast.LENGTH_SHORT).show();

                break;
            case PlaybackPause:
                sendTask.send(KeygenSendThread.PAUSE);
                break;

            case SkipForward:
                sendTask.send(KeygenSendThread.FORWARD);
                break;

        }

    }

    public void sendSongIDManually(int songID) {
        boolean longFmtSongID = SongbookCfg.getInstance().longFormatSongID();
        String songCmd;
        if (longFmtSongID)
            songCmd = String.format("1%05d", songID);
        else
            songCmd = String.format("1%04d", songID);
        sendTask.send(songCmd);
        Toast.makeText(SongbookMgrActivity.this, songID + " added to playlist", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSongSelected(String fragmentTag, SongID song) {
        // make sure we get the latest parameters
        boolean longFmtSongID = SongbookCfg.getInstance().longFormatSongID();

        DatabaseHandler db = DatabaseHandler.getInstance(getApplicationContext(),
                SongbookCfg.getInstance().getPackageName());
        int songID = db.getSongID(song.getName(), song.getSinger());
        if (songID > 0) {
            String songCmd;
            if (longFmtSongID)
                songCmd = String.format("1%05d", songID);
            else
                songCmd = String.format("1%04d", songID);
            sendTask.send(songCmd);
            Toast.makeText(SongbookMgrActivity.this, song.getName() + " added to playlist", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(SongbookMgrActivity.this, song.getName() + " is NOT in DB", Toast.LENGTH_SHORT).show();
     }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Handle the back button

        if (keyCode == KeyEvent.KEYCODE_BACK && isTaskRoot()) {
            //Ask the user if they want to quit
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_alert)
                    .setTitle("SongbookMgr")
                    .setMessage("Do you really want to quit?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Unlock screen rotation and stop the activity
                            //saveFavSongboksTabInfoToSharePref();
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;

        }
        else {
            return super.onKeyDown(keyCode, event);
        }

    }


    void onNewFavoriteSongbook() {
        Log.i(TAG, "onNewFavoriteSongbook:");
        TabListInfo t = new TabListInfo(FragmentLayoutType.FragFavoriteSongbook, FavSongbookLayout.class,
                                            FAVBOOK_TABNAME+bookNumber, bookNumber);
        bookNumber++;
        t.setDBFileName(FavoriteDBHandler.DEFAULT_DATABASE_NAME);
        t.setTitle(FavoriteDBHandler.DEFAULT_DATABASE_NAME);
        fragmentTabList.add(t);
        final ActionBar bar = getActionBar();
        mTabsAdapter.addTab(bar.newTab().setText(t.getTitle()), t.getClss(), null );
        mTabsAdapter.notifyDataSetChanged();
    }

    void readConfigFromSharePreferences() {

        //////////////////////////////
        //
        // get server configuration
        //
        //////////////////////////////

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String serName = prefs.getString("ServerName", cfg.LOCAL_IP_ADDR);
        int serPortNo = prefs.getInt("ServerPortNo", cfg.VNC_PORT_NO);
        String userName = prefs.getString("UserName", "");
        String userPassword = prefs.getString("UserPassword", "");
        // have to patch this code because of a previous bug
        boolean enableRemote = false;
        try {
            enableRemote = prefs.getBoolean("RemoteEnable", false);
        }
        catch(Exception e) {
            // must be the boolean/int bug that I made on earlier version
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("RemoteEnable").commit();
        }
        boolean useVNCServer = prefs.getBoolean("useVNCServer", false);
        cfg.setMediaServerType(useVNCServer ? SongbookCfg.MediaServerType.VNC_ServerType :
                SongbookCfg.MediaServerType.SBCM_ServerType);
        float textSize = prefs.getFloat("FontSize", DEF_FONT_SIZE);
        float textSizeSinger = prefs.getFloat("FontSizeSinger", DEF_FONT_SIZE);
        Log.i(TAG, "FontSize = (" + textSize + ", " + textSizeSinger + ")");
        cfg.updateServerCfgParams(serName, serPortNo, enableRemote, userName, userPassword);
        cfg.updatePackageName(getApplicationContext().getPackageName());
        Log.i(TAG, "Package Name = " + cfg.getPackageName());
        cfg.setDisplayTextSize(textSize, textSizeSinger);
        boolean fiveDigitSongID = prefs.getBoolean("fiveDigitSongID", false);
        cfg.setSongIDFormat(fiveDigitSongID);

        //////////////////////////////
        //
        // get favorite songbook dbnames from previous session
        //
        //////////////////////////////

        int numOfFavbooks = prefs.getInt("NoOfFavBooks", 0);
        Log.i(TAG, "NoOfFavBooks = " + numOfFavbooks);
        String dbFileName;
        if (numOfFavbooks > 0) {
            for (int i = 1; i <= numOfFavbooks; i++) {
                dbFileName = prefs.getString(FAVBOOK_TABNAME+i, FavoriteDBHandler.DEFAULT_DATABASE_NAME);
                Log.i(TAG, FAVBOOK_TABNAME+i + " DBNames = " + dbFileName);
                favDBNames.add(dbFileName);
            }

        }
    }

    void saveConfigToSharePreferences() {

        //////////////////////////////
        //
        // save server configuration
        //
        //////////////////////////////

        boolean remote = cfg.remoteEnable();
        String sn = cfg.getServerName();
        int pn = cfg.getServerPortNumber();
        String un = cfg.getUserName();
        String up = cfg.getUserPassword();
        boolean songIDFmt = cfg.longFormatSongID();
        boolean useVNCServer = cfg.getMediaServerType() == SongbookCfg.MediaServerType.VNC_ServerType;
        Log.i(TAG,"Server: " + sn + " Port: " + pn + " Remote = " + remote + " User: " + un + " Password: " + up);
        SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE).edit();
        prefs.putString("ServerName", sn);
        prefs.putInt("ServerPortNo", pn);
        prefs.putString("UserName", un);
        prefs.putString("UserPassword", up);
        prefs.putBoolean("RemoteEnable", remote);

        //////////////////////////////
        //
        // save display text size configuration
        //
        //////////////////////////////
        float fontSize = cfg.getDisplayTextSize();
        float fontSizeSinger = cfg.getSingerDisplayTextSize();
        Log.i(TAG, "FontSize = " + fontSize + ", " + fontSizeSinger);
        prefs.putFloat("FontSize", fontSize);
        prefs.putFloat("FontSizeSinger", fontSizeSinger);
        prefs.putBoolean("fiveDigitSongID", songIDFmt);
        prefs.putBoolean("useVNCServer", useVNCServer);
        prefs.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // See which child activity is calling us back.
    	Log.d(TAG, "onActivityResult: requestCode = " + requestCode);
   		if (resultCode == RESULT_OK) {
            FavSongbookLayout f;
   			switch (requestCode) {
   				case IMPORT_SONGBOOK:
   				case RESTORE_SONGBOOK_DB:
   					{
   					// Tell the main songbook layout to re-read its songbook
                    String tag = fragmentTabList.get(MAIN_SONGBOOK_ENTRY).getFragmentTag();
   					SongbookLayout fsl = (SongbookLayout) getSupportFragmentManager().findFragmentByTag(tag);
                    if (fsl != null)
   					    fsl.updateSongbook();
   					}
   				    break;
   				case SONGINDEX_CFG:
                    saveConfigToSharePreferences();
                    // update favorite songbook display
                    //f = (FavSongbookLayout) getSupportFragmentManager().findFragmentByTag(tabFavSongbookFragment);
                    //f.updateSongbookDisplay();
                    if (KeygenSendThreadFactory.getCurrentServerType() != cfg.getMediaServerType()) {
                        // need to stop the current send thread before starting a different one
                        sendTask.stopSendThread();
                        // should wait a bit before starting new one?
                        sendTask = KeygenSendThreadFactory.createKeygenSendThread(cfg.getMediaServerType());
                        sendTask.setContext(SongbookMgrActivity.this);
                        sendTask.start();
                    }
                    break;

   				case RESTORE_FAVSONGBOOK_DB:
   					{
                    // need to retrieve the clientTag and new db filename
                    String clientTag = data.getStringExtra(FavSongbookDBRestoreActivity.CLIENT_TAG);
                    String newDBFileName = data.getStringExtra(FileChooser.GET_FILE_NAME);
                    Log.i(TAG, "ClientTag = " + clientTag + " New DB filename = " + newDBFileName);
                    // Update our table to keep track of the new DB filename
                    updateDBNameInFragmentTabList(clientTag, newDBFileName);
   					// Tell the tagged fav songbook layout to re-read its songbook
   					f = (FavSongbookLayout) getSupportFragmentManager().findFragmentByTag(clientTag);
   					f.updateSongbook(newDBFileName);
   					}
   					break;
   				case BACKUP_SONGBOOK_DB:
  				case BACKUP_FAVSONGBOOK_DB:
  					// do nothing
   					break;
					  					
   				default:
   					Log.i(TAG, "Unrecognized requestCode = " + requestCode);
   					break;
   			}

   		}
   		else {
			Log.i(TAG, "resultCode is NOT OK, requestCode = " + requestCode);   			
   		}
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.songbook_mgr, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
    		
			case R.id.app_about:
		    	{
		        final Dialog dialog = new Dialog(SongbookMgrActivity.this);
		        dialog.setContentView(R.layout.about_dialog);
		        dialog.setTitle("About SongbookMgr");
		        dialog.setCancelable(true);
		        //there are a lot of settings, for dialog, check them all out!

		        //set up text
		        TextView text = (TextView) dialog.findViewById(R.id.TextView01);
		        text.setText(R.string.app_info);

		        text = (TextView) dialog.findViewById(R.id.AppVersion);

		        String versionName;
		        try {
		        	versionName = getApplicationContext().getPackageManager()
		            			.getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
		        } catch (NameNotFoundException e) {
		        	// TODO Auto-generated catch block
		            versionName = "Not Available";
		            e.printStackTrace();
		        }
		        text.setText("Version: " + versionName);

		            
		        //set up image view
		        ImageView img = (ImageView) dialog.findViewById(R.id.AppImageView);
		        img.setImageResource(R.drawable.ic_songbookmgr_large);

		        //set up button
		        ImageButton button = (ImageButton) dialog.findViewById(R.id.Cfm_btn);
		        button.setOnClickListener(new OnClickListener() {
		        	@Override
		            public void onClick(View v) {
		            	dialog.dismiss();
		            }
		        });
		        //now that the dialog is set up, it's time to show it    
		        dialog.show();
		    	}
		    	break;
		    case R.id.app_help:
		    	doAppHelp();
		    	break;

		    default:
		    	return 	super.onOptionsItemSelected(item);
		    		
	    	}
	    	
	    	return true;
	    	
	    }

	    void doAppHelp() {
	   	 /**
	   	  * read the help file  from local assets-folder then display into help dialog box
	   	  * */
	  
	   	 
	   		//Open your local db as the input stream
	   		InputStream myInput = null;
	   		String result = null;
			try {
				myInput = getAssets().open(HELP_FILENAME);
				result= convertStreamToString(myInput);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	        final Dialog dialog = new Dialog(SongbookMgrActivity.this);
	        dialog.setContentView(R.layout.help_dialog);
	        dialog.setTitle("SongbookMgr Quick Help");
	        dialog.setCancelable(true);
	        //there are a lot of settings, for dialog, check them all out!

	        //set up text
	        TextView text = (TextView) dialog.findViewById(R.id.help_text);
	        text.setText(result);

	        //set up button
	        ImageButton button = (ImageButton) dialog.findViewById(R.id.Cfm_btn);
	        button.setOnClickListener(new OnClickListener() {
	        	@Override
	            public void onClick(View v) {
	        		dialog.dismiss();
	            }
	        });
	        //now that the dialog is set up, it's time to show it    
	        dialog.show(); 	 
	   	}

	    static String convertStreamToString(InputStream is)
	                throws IOException {
	    	Writer writer = new StringWriter();
	        char[] buffer = new char[1024];
	        try {
	        	Reader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
	            int n;
	            while ((n = reader.read(buffer)) != -1) {
	            	writer.write(buffer, 0, n);
	            }
	        } finally {
	        	is.close();
	        }
	        String text = writer.toString();
	        return text;
	    }
}

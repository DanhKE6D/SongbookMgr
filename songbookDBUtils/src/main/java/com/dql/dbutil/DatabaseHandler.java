package com.dql.dbutil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

	// From AndroidDesignPatterns.com -- use singleton to help
	// from leaking when more than one is opened
	private static DatabaseHandler mInstance = null;
	  
	// Database Version
	private static final int DATABASE_VERSION = 1;
	//private static String DB_PATH = "/data/data/com.dql.tksongindex/databases/";
	private static final String DATA_PATH = "/data/data/";
	private static final String DATABASE_DIR = "/databases/";
	private static String packageName = "com.dql.tksongindex";
	// Database Name
	private static final String DATABASE_NAME = "songList.db";

	// SongID table name
	private static final String TABLE_SONG_ID = "SongNames";

	// Contacts Table Columns names
	private static final String KEY_ID = "id";
	private static final String KEY_NAME = "name";
	private static final String KEY_SINGER = "singer";
		
	private static final String TAG = "DatabaseHandler";
	private SQLiteDatabase songDataBase; 
	private final Context ctx;	
	    
   
	public static DatabaseHandler getInstance(Context ctx, final String pkgName) {
	        
		// Use the application context, which will ensure that you
	    // don't accidentally leak an Activity's context.
	    // See this article for more information: http://bit.ly/6LRzfx

	    if (mInstance == null) {
	    	mInstance = new DatabaseHandler(ctx.getApplicationContext(), pkgName);
	    }
	    return mInstance;

	 }
	         
	 private DatabaseHandler(Context context, final String pkgName) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.ctx = context;
		this.packageName = pkgName;		
	}
	 
	public static String getDBPath() {
		return DATA_PATH + packageName + DATABASE_DIR;
	}
	
	public static String getDBName() {
		return DATABASE_NAME;
	}
	
	/**
	 * Creates a empty database on the system and rewrites it with your own database.
	**/
	 
	public void createDataBase() throws IOException {
		boolean dbExist = checkDataBase();
			  
		if (dbExist) {
			 //do nothing - database already exist
		}
		else {
			  
			//By calling this method and empty database will be created into the default system path
			//of your application so we are gonna be able to overwrite that database with our database.
			this.getReadableDatabase();
			  
			try {
				copyDataBase();
			  
			} catch (IOException e) {
				throw new Error("Error copying database");
			  
			}
		}
			  
	}
	
	 /**
	  * Check if the database already exist to avoid re-copying the file each time you open the application.
	  * @return true if it exists, false if it doesn't
	  */
	private boolean checkDataBase() {
	 
		SQLiteDatabase checkDB = null;
	 
		try {
			String myPath = getDBPath() + DATABASE_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
	 
		} catch (SQLiteException e) {
	 
			//database does't exist yet.
	 
		}
	 
		if (checkDB != null) {
			checkDB.close();
	 	}
	 
		return (checkDB != null) ? true : false;
	}

	
	 /**
	  * Copies your database from your local assets-folder to the just created empty database in the
	  * system folder, from where it can be accessed and handled.
	  * This is done by transfering bytestream.
	  * */
	private void copyDataBase() throws IOException {
	 
		//Open your local db as the input stream
		InputStream myInput = ctx.getAssets().open(DATABASE_NAME);
	 
		// Path to the just created empty db
		String outFileName = getDBPath() + DATABASE_NAME;
		//Log.i(TAG, "Database does not exist - Copying database from asset to " + outFileName);	 
		//Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);
	 
		//transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}
		//Log.i(TAG, "Done copying... ");		 
		//Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();
	 
	}
	
	public void openDataBase() throws SQLException {
		 
		//Open the database
		String myPath = getDBPath() + DATABASE_NAME;
		songDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		//Log.i(TAG, "openDataBase: " + myPath);		 
	}
		 
	@Override
	public synchronized void close() {

		if (songDataBase != null)
			songDataBase.close();

	super.close();

	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	// Add your public helper methods to access and get content from the database.
	// You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
	// to you to create adapters for your views.

	
    /*
	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_SONG_TABLE = "CREATE TABLE " + TABLE_SONG_ID + "("
				+ KEY_ID + " TEXT PRIMARY KEY," + KEY_NAME + " TEXT,"
				+ KEY_SINGER + " TEXT" + ")";
		db.execSQL(CREATE_SONG_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONG_ID);

		// Create tables again
		onCreate(db);
	}

	//
	// All CRUD(Create, Read, Update, Delete) Operations
	//

	// Adding new song
	void addSong(SongID song) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_ID, song.getID());
		values.put(KEY_NAME, song.getName()); // Song Name
		values.put(KEY_SINGER, song.getSinger()); // Singer of this song

		// Inserting Row
		db.insert(TABLE_SONG_ID, null, values);
		db.close(); // Closing database connection
	}

	// Updating single contact
	public int updateSong(SongID song) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_NAME, song.getName());
		values.put(KEY_SINGER, song.getSinger());

		// updating row
		return db.update(TABLE_SONG_ID, values, KEY_ID + " = ?", new String[] { song.getID() });
	}

	// Deleting single contact
	public void deleteSong(SongID song) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_SONG_ID, KEY_ID + " = ?",
				new String[] { song.getID() });
		db.close();
	}

	*/
	
		
	// Getting single song
	public SongID getSong(int id) {
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query(TABLE_SONG_ID, new String[] { KEY_ID,
				KEY_NAME, KEY_SINGER }, KEY_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
        SongID song;
        try {
		    song = new SongID(Integer.parseInt(cursor.getString(0)),
				cursor.getString(1), cursor.getString(2));
        } catch (CursorIndexOutOfBoundsException e) {
            song = null;
        }
		return song;
	}
		
	// Getting All songs
	public List<SongID> getAllSongs() {
		//Log.i(TAG, "getAllSongs: ");
		List<SongID> songList = new ArrayList<SongID>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_SONG_ID;

		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				SongID song = new SongID();
				song.setID(Integer.parseInt(cursor.getString(0)));
				song.setName(cursor.getString(1));
				song.setSinger(cursor.getString(2));
				songList.add(song);
				//Log.i(TAG, song.toString());
			} while (cursor.moveToNext());
		}
    	return songList;
	}

	// Getting song count
	public int getSongCount() {
	    int count = 0;
		String countQuery = "SELECT  * FROM " + TABLE_SONG_ID;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		if (cursor != null && !cursor.isClosed()) {
			count = cursor.getCount();
			cursor.close();
		}

		//Log.d(TAG, "getSongCount " + count);
		// return count
	    return count;
	}

	// clear all table in the db
	public void clearAll() {
		//Log.i(TAG, "In clearAll");
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONG_ID);
		// Create tables again
		createNewDataBase();
	}
		
	/**
	  * Creates a empty database on the system and rewrites it with your own database.
	  * */

	public void createNewDataBase() {
		//Log.i(TAG, "In createNewDataBase");
		SQLiteDatabase db = this.getWritableDatabase();

		String CREATE_SONG_TABLE = "CREATE TABLE " + TABLE_SONG_ID + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
				+ KEY_SINGER + " TEXT" + ")";
		db.execSQL(CREATE_SONG_TABLE);
		//Log.i(TAG, "Done createNewDataBase");
		}

	// Adding new song
	public void addSong(SongID song) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_ID, song.getID());
		values.put(KEY_NAME, song.getName()); // Song Name
		values.put(KEY_SINGER, song.getSinger()); // Singer of this song

		// Inserting Row
		db.insert(TABLE_SONG_ID, null, values);
		db.close(); // Closing database connection
	}

    // insert a block of records into the songbook database
    // Using SQLite compile statement to improve performance
    // a 10K records improve performance from 3 min to 10 secs!
    public void addAllSongs(ArrayList<SongID> l) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "INSERT INTO "+ TABLE_SONG_ID +" VALUES (?,?,?);";
        SQLiteStatement statement = db.compileStatement(sql);
        db.beginTransaction();
        try {
            for (SongID i : l) {
                statement.clearBindings();
                statement.bindLong(1, i.getID());
                statement.bindString(2, i.getName());
                statement.bindString(3, i.getSinger());
                statement.execute();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Getting song ID based on song name and singer
    public int getSongID(String songName, String singerName) {
        int songID = -1;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(true, TABLE_SONG_ID,
                new String[] {
                        KEY_ID
                },
                KEY_NAME + "=?" + " AND " + KEY_SINGER + "=?",
                new String[] {
                        songName,
                        singerName
                },
                null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            try {
                songID = Integer.parseInt(cursor.getString(0));
            } catch (CursorIndexOutOfBoundsException e) {
                // probably not found in DB
                songID = -1;
            }
        }
        Log.i(TAG, "songID(" + songName + "," + singerName + ") = " + songID);
        return songID;
    }

}


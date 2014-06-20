package com.dql.dbutil;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FavoriteDBHandler extends SQLiteOpenHelper {

	// Database Version
	private static final int DATABASE_VERSION = 1;
	//private static String DB_PATH = "/data/data/com.dql.tksongindex/databases/";
	private static final String DATA_PATH = "/data/data/";
	private static final String DATABASE_DIR = "/databases/";
	private static String packageName = "com.dql.tksongindex";
	// Database Name
	public static final String DEFAULT_DATABASE_NAME = "FavSongbook.dbf";
    String databaseName;

	// SongID table name
	private static final String TABLE_SONG_ID = "SongNames";

	// Contacts Table Columns names
	private static final String KEY_ID = "id";
	private static final String KEY_NAME = "name";
	private static final String KEY_SINGER = "singer";
	private static final String KEY_COUNT = "count";
		
	private static final String TAG = "FavoriteDBHandler";


	public FavoriteDBHandler(Context context, final String pkgName, final String databaseName) {
		super(context, databaseName, null, DATABASE_VERSION);
		this.packageName = pkgName;
        Log.d(TAG, "Database Name is " + databaseName);
        this.databaseName = databaseName;

	}
	 
	public static String getDBPath() {
		return DATA_PATH + packageName + DATABASE_DIR;
	}
	
	public String getDBName() {
		return this.databaseName;
	}
	
	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_SONG_TABLE = "CREATE TABLE " + TABLE_SONG_ID + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
				+ KEY_SINGER + " TEXT, " + KEY_COUNT + " INTEGER"+ ")";
		db.execSQL(CREATE_SONG_TABLE);
        Log.d(TAG, "onCreate: ");
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONG_ID);

		// Create tables again
		onCreate(db);
	}
	// Getting single song
	public FavSongID getSong(int id) {
        FavSongID favSong = null;
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query(TABLE_SONG_ID, new String[] { KEY_ID,
				KEY_NAME, KEY_SINGER, KEY_COUNT }, KEY_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
        try {
		    if (cursor != null)
			    cursor.moveToFirst();

		    favSong = new FavSongID(Integer.parseInt(cursor.getString(0)),cursor.getString(1),
										cursor.getString(2), Integer.parseInt(cursor.getString(3)));
        }
        finally {
            cursor.close();
        }
        db.close();
		return favSong;
	}
	
	// Getting All songs
	public List<FavSongID> getAllSongs() {
		List<FavSongID> songList = new ArrayList<FavSongID>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_SONG_ID;
		Log.d(TAG, "getAllSong:");
		SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        try {
		    // looping through all rows and adding to list
		    if (cursor.moveToFirst()) {
			    do {
				    FavSongID song = new FavSongID();
				    song.setSong(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2));
				    song.setAllStatusBits(Integer.parseInt(cursor.getString(3)));
				    songList.add(song);
				    //Log.d(TAG, song.toString());
			    } while (cursor.moveToNext());
		    }
        }
        finally {
            cursor.close();
        }
        db.close();
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
        db.close();
	    return count;
	}
	//
	// All CRUD(Create, Read, Update, Delete) Operations
	//

	// Adding new song
	public void addSong(FavSongID song) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		SongID s = song.getSong();
		values.put(KEY_ID, s.getID());
		values.put(KEY_NAME, s.getName()); // Song Name
		values.put(KEY_SINGER, s.getSinger()); // Singer of this song
		values.put(KEY_COUNT, song.getStatusBits());

		// Inserting Row
		db.insert(TABLE_SONG_ID, null, values);
		db.close(); // Closing database connection
	}
	
	// Updating single contact
	public int updateSong(FavSongID song) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		SongID s = song.getSong();
		values.put(KEY_NAME, s.getName());
		values.put(KEY_SINGER, s.getSinger());
		values.put(KEY_COUNT, song.getStatusBits());

		// updating row
		return db.update(TABLE_SONG_ID, values, KEY_ID + " = ?", new String[] { String.valueOf(s.getID()) });
	}

	// Deleting single contact
	public void deleteSong(FavSongID song) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_SONG_ID, KEY_ID + " = ?",
				new String[] { String.valueOf(song.getSong().getID()) });
		db.close();
	}
}

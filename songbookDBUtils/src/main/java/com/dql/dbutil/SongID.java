package com.dql.dbutil;

import java.util.Comparator;

public class SongID implements Comparable<SongID> {

    public static int SONG_SELECTED                = 1 << 31;       // using the most significant bit to do selection

	//private variables
	int id;
	String name;
	String singer;

	// Empty constructor
	public SongID() {

	}
	// constructor
	public SongID(int id, String name, String singer){
		this.id = id;
		this.name = name;
		this.singer = singer;
	}

	// getting ID
	public int getID() {
		return this.id & (~SONG_SELECTED);   // make sure the MSB bit is clear
	}

	// setting id
	public void setID(int id ){
		this.id = id & (~SONG_SELECTED);    // make sure the MSB bit is clear
	}

	// getting name
	public String getName(){
		return this.name;
	}

	// setting name
	public void setName(String name){
		this.name = name;
	}

	// getting singer for this song
	public String getSinger(){
		return this.singer;
	}

	// setting singer for this song
	public void setSinger(String singer){
		this.singer = singer;
	}

    public void unselectSong() {
        this.id = this.id & (~SONG_SELECTED);
    }

    public void selectSong() {
        this.id = this.id | SONG_SELECTED;
    }

    public boolean isSongSelected() {
        return ((this.id & SONG_SELECTED) != 0) ;
    }

	public int compareTo(SongID o) {
		if(this.name != null)
			return this.name.toUpperCase().compareTo(o.getName().toUpperCase());
		else
			throw new IllegalArgumentException();
	}
	
	public static Comparator<SongID> SongIDComparator
    						= new Comparator<SongID>() {

			public int compare(SongID s1, SongID s2) {

		String songName1 = s1.getSinger().toUpperCase();
		String songName2 = s2.getSinger().toUpperCase();

		//ascending order
		return songName1.compareTo(songName2);

		// descending order
		// return songName2.compareTo(songName1);
		}

		};

	@Override
	public String toString() {
		return String.format("%04d", this.id & (~SONG_SELECTED)) + ":" + this.name + "--" + this.singer;
	}
}

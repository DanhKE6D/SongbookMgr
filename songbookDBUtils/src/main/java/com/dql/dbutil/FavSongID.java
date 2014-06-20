package com.dql.dbutil;

public class FavSongID implements Comparable<FavSongID> {
	SongID song;
	int   statusFlag;

	public static int SONG_GRAYED_OUT              = 1 << 0;
	public static int SONG_PLAYED_IN_REVERSE_TRACK = 1 << 1;
    public static int SONG_SELECTED                = 1 << 2;
	
	// Empty constructor
	public FavSongID() {
		
	}
	// constructor
	public FavSongID(int id, String name, String singer, int status) {
		this.song = new SongID(id, name, singer);
		this.statusFlag = status;
	}
	
	public SongID getSong() {
		return new SongID(song.getID(), song.getName(), song.getSinger());
	}
	
	public void setSong(int id, String name, String singer) {
		this.song = new SongID(id, name, singer);
	}
	
	public void clearAllStatusBits() {
		this.statusFlag = 0;
	}
	
	public int getStatusBits() {
		return this.statusFlag;
	}
	
	public void setStatusBit(int bit) {
		this.statusFlag = this.statusFlag | bit;
	}

	public void setAllStatusBits(int bits) {
		this.statusFlag = bits;
	}
	
	public void clearStatusBit(int bit) {
		this.statusFlag = this.statusFlag & ~bit;
	}
	
	public boolean getBit(int bit) {
		return ((this.statusFlag & bit) != 0) ;
	}

    public int compareTo(FavSongID o) {
        if(this.song.name != null)
            return this.song.compareTo(o.getSong());
        else
            throw new IllegalArgumentException();
    }
}

package com.dql.dbutil;

import android.util.Log;

public class SongbookCfg {
    public static final int TCP_DEF_CONN_TIMEOUT = 20000;          // 20s timeout -- seems faster though
    public static final int SBCM_PORT_NO           = 9000;
    public static final int VNC_PORT_NO            = 5900;
    public static final String LOCAL_IP_ADDR       = "192.168.0.1";

    public enum MediaServerType {
        SBCM_ServerType, VNC_ServerType
    };
	private String packageName;
	private String serverName;
	private int serverPortNumber;
	private String userName;
	private String userPassword;
	private boolean enableRemote;
	float displayTextSize;
    float displaySingerTextSize;
    boolean longFmtSongID;
    MediaServerType serverType;
	private static SongbookCfg mInstance = null;
	static final String TAG = "ServerCfg";	
	
	// Empty constructor
	private SongbookCfg() {
		this.packageName = "";
		this.serverName = LOCAL_IP_ADDR;
		this.serverPortNumber = VNC_PORT_NO;
		this.enableRemote = false;
		this.userName = "";						// anonymous
		this.userPassword = "";
		this.displayTextSize = 10.0f;
        this.displaySingerTextSize = 8.0f;
        this.serverType = MediaServerType.SBCM_ServerType;
	}
	
	public static SongbookCfg getInstance() {
        
		// want to make this a singleton

	    if (mInstance == null) {
	    	mInstance = new SongbookCfg();
	    }
	    return mInstance;

	 }
	
	public void updateServerCfgParams(String sn, int pn, boolean remote, String un, String up) {
		this.serverName = sn;
		this.serverPortNumber = pn;
		this.enableRemote = remote;
		this.userName = un;	
		this.userPassword = up;	
		Log.i(TAG,"Server: " + sn + " Port: " + pn + " Remote = " + remote + " User: " + un + " Password: " + up);
	}
	
	public void updatePackageName(final String pkgName) {
		this.packageName = pkgName;
	}
	
	public String getPackageName() {
		return this.packageName;
	}
	
	public String getServerName() {
		return this.serverName;
	}
	

	public int getServerPortNumber() {
		return this.serverPortNumber;
	}
	
	public boolean remoteEnable() {
		return this.enableRemote;
	}
	
	public String getUserName() {
		return this.userName;
	}
	
	public String getUserPassword() {
		return this.userPassword;
	}

    public void setDisplayTextSize(float songTextsize, float singerTextsize) {
        Log.i(TAG,"Text size = " + songTextsize + ", " + singerTextsize);
        this.displayTextSize = songTextsize;
        this.displaySingerTextSize = singerTextsize;
    }

    public float getDisplayTextSize() {
        return this.displayTextSize;
    }
    public float getSingerDisplayTextSize() {
        return this.displaySingerTextSize;
    }
    public void setSongIDFormat(boolean longFormat) {
        this.longFmtSongID = longFormat;
    }
    public boolean longFormatSongID() {
        return this.longFmtSongID;
    }
    public void setMediaServerType(MediaServerType type) {this.serverType = type; }
    public MediaServerType getMediaServerType() { return this.serverType; }
}

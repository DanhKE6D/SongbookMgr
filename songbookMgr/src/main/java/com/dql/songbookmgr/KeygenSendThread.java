package com.dql.songbookmgr;

import android.app.Activity;

/**
 * Created by dql on 1/31/14.
 */
public abstract class KeygenSendThread extends Thread {
    public static final String SOUND_TOGGLE        = "ctrl_h";
    public static final String REWIND              = "ctrl_r";
    public static final String FORWARD             = "ctrl_n";
    public static final String PAUSE		        = "ctrl_p";
    public static final String SONG_INFO		    = "ctrl_i";
    public static final String START_ON_TRACK2     = "ctrl_2";

    public abstract void setContext(Activity ctx);
    public abstract void send(String cmd);
    public abstract void stopSendThread();
}

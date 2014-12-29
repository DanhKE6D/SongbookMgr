package com.dql.songbookmgr;

import android.app.Activity;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    protected Activity myCtx = null;
    protected boolean keygenThreadStopping = false;            // set to true in destroy to let the task die
    // This ConcurrentLinked queue is used to hold all the vnc/sbmc commands that
    // needed to send to vnc/sbmc server.
    protected Queue<String> cmdQueue = new ConcurrentLinkedQueue<String>();

    public void setContext(Activity ctx) {
        this.myCtx = ctx;
    }
    public void send(String cmd) {
        cmdQueue.add(cmd);
    }

    public void stopSendThread() {
        synchronized (this) {
            keygenThreadStopping = true;
        }
    }
}

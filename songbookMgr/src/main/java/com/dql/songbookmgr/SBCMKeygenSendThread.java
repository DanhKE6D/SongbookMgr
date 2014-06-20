package com.dql.songbookmgr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import com.dql.dbutil.SongbookCfg;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by dql on 1/31/14.
 */
public class SBCMKeygenSendThread extends KeygenSendThread {
    // SBCM protocol connection

    static final String TAG = "SBCMKeygenSendThread";
    Activity myCtx = null;
    boolean keygenThreadStopping = false;            // set to true in destroy to let the task die
    // This ConcurrentLinked queue is used to hold all the SBCM commands that
    // needed to send to SBCM server.
    Queue<String> cmdQueue = new ConcurrentLinkedQueue<String>();

    @Override
    public void setContext(Activity ctx) {
        this.myCtx = ctx;
    }

    @Override
    public void send(String cmd) {
        cmdQueue.add(cmd);
    }

    @Override
    public void stopSendThread() {
        keygenThreadStopping = true;
    }
    // **********************************
    // Public methods
    // **********************************
    // This method is run when the thread
    // is started by calling start().

    @Override
    public void run() {
        int nCmdSize;
        String response = null;
        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        BufferedReader inFromServer = null;
        Log.i(TAG, "Starting SBCMKeygenSendThread");
        while (!keygenThreadStopping){
            try {
                if (cmdQueue.isEmpty()){
                    Thread.sleep(1000);
                }
                else {
                    // got at least one command -- connect to server
                    while ((nCmdSize = cmdQueue.size()) > 0) {
                        // The command for TKaraoke either starts out with a 1NNNN for selecting songs
                        // or a ctrl_ for keyboard shortcut
                        String cmd = cmdQueue.poll();
                        try {
                            Log.i(TAG, "SBCMKeygenSendThread: Sending " + cmd);
                            clientSocket = new Socket();
                            clientSocket.connect(new InetSocketAddress(SongbookCfg.getInstance().getServerName(),
                                    SongbookCfg.SBCM_PORT_NO), SongbookCfg.TCP_DEF_CONN_TIMEOUT);
                            //clientSocket = new Socket(serverName, connServerListenPortNumber);
                            outToServer = new DataOutputStream(clientSocket.getOutputStream());
                            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            outToServer.writeBytes("SELECT" + cmd + '\n');
                            response = inFromServer.readLine();
                        } catch (IOException e) {
                            // clear the queue -- no point of going forward
                            cmdQueue.clear();
                            myCtx.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Ui Stuff here
                                    new AlertDialog.Builder(myCtx)
                                            .setIcon(R.drawable.ic_stop)
                                            .setTitle("SBCM SendThread")
                                            .setMessage("Unable to communicate with SBCM server")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                            .show();
                                }
                            });
                            Log.e(TAG,"Exception raised - connection failed!");
                            e.printStackTrace();
                            continue;
                        }
                        // need to slow down or the last one will take over the rest
                        if (nCmdSize > 1) {
                            // Tkaraoke is really slow. No need to sleep 2 seconds on the last command
                            Thread.sleep(2000);
                        }
                    }
                }

            }
            catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG,"Exception raised - unable to get from Cmd Queue");
            }
        }
        Log.i(TAG, "SBCMKeygenSendThread: Stopping");
    }
}


package com.dql.songbookmgr;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import com.dql.dbutil.SongbookCfg;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by dql on 1/31/14.
 */
public class SBCMKeygenSendThread extends KeygenSendThread {
    static final String TAG = "SBCMKeygenSendThread";

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
                        }
                        catch (IOException e) {
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
                        }
                        finally {
                            try {
                                clientSocket.close();
                                clientSocket = null;
                            }
                            catch (IOException ioe) {
                                Log.e(TAG,"Exception raised - unable to close socket!");
                            }
                        }
                        // need to slow down or the last one will take over the rest
                        if (nCmdSize > 1) {
                            // simulate human keyboard input to TKaraoke. If there is more than one cmd, sleep 2 seconds before
                            // pushing another command
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
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException ioe) {
                Log.e(TAG, "Exception raised - unable to close socket!");
            }
        }
    }
}


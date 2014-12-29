package com.dql.songbookmgr;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import com.dql.dbutil.SongbookCfg;
import java.io.IOException;


/**
 * Created by dql on 1/31/14.
 */
public class VNCKeygenSendThread extends KeygenSendThread {
    static final String TAG = "VNCKeygenSendThread";
    // VNC protocol connection
    RfbProto rfb = null;

    // **********************************
    // Public methods
    // **********************************
    // This method is run when the thread
    // is started by calling start().

    @Override
    public void run() {
        int nCmdSize;
        Log.i(TAG, "Starting VNCSendTask");
        while (!keygenThreadStopping){
            try {
                if (cmdQueue.isEmpty()){
                    Thread.sleep(1000);
                }
                else {
                    // got at least one command -- connect to server
                    try {
                        rfb = connectToVNCServer(SongbookCfg.getInstance().getUserName(), SongbookCfg.getInstance().getUserPassword());
                        //doProtocolInitialisation(rfb);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        if (rfb != null) {
                            rfb.close();
                            rfb = null;
                        }
                        // clear the queue -- no point of going forward
                        cmdQueue.clear();
                        myCtx.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run() {
                                // Ui Stuff here
                                new AlertDialog.Builder(myCtx)
                                        .setIcon(R.drawable.ic_stop)
                                        .setTitle("VNC SendTask")
                                        .setMessage("Unable to communicate with VNC server")
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
                    if (rfb == null) {
                        // remote not enable
                        cmdQueue.clear();
                        continue;
                    }
                    while ((rfb != null) && (nCmdSize = cmdQueue.size()) > 0) {
                        String cmd = cmdQueue.poll();
                        if (cmd != null) {
                            // send it
                            Log.i(TAG, "Got a command, cmd = " + cmd);
                            // The command for TKaraoke either starts out with a 1NNNN for selecting songs
                            // or a ctrl_ for keyboard shortcut
                            Log.i(TAG, "VNCSendTask: Sending " + cmd);
                            if (cmd.startsWith("1")) {
                                // a song select command
                                sendText(rfb, cmd + "\n");
                            }
                            else
                                sendMetaKey(rfb, cmd);

                            // need to slow down or the last one will take over the rest
                            if (nCmdSize > 1) {
                                // Tkaraoke is really slow. No need to sleep 2 seconds on the last command
                                Thread.sleep(2000);
                            }

                        }
                    }
                    if ((rfb != null) && SongbookCfg.getInstance().remoteEnable()) {
                        rfb.close();
                        rfb = null;
                    }
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG,"Exception raised - unable to get from Cmd Queue");
            }
        }
        Log.i(TAG, "VNCSendTask: Stopping");
        if (rfb != null) {
            rfb.close();
        }
    }

    synchronized RfbProto connectToVNCServer(String us,String pw) throws Exception {
        Log.i(TAG, "Connecting to " + SongbookCfg.getInstance().getServerName() + ", port " + SongbookCfg.getInstance().getServerPortNumber() + "...");

        if (!SongbookCfg.getInstance().remoteEnable())
            return null;
        else {

            // connection and authentication
            RfbProto rfb = new RfbProto(SongbookCfg.getInstance().getServerName(),
                                SongbookCfg.getInstance().getServerPortNumber(), SongbookCfg.TCP_DEF_CONN_TIMEOUT);
            Log.i(TAG, "Connected to server");
            rfb.readVersionMsg();
            Log.i(TAG, "RFB server supports protocol version " + rfb.serverMajor + "." + rfb.serverMinor);

            rfb.writeVersionMsg();
            Log.i(TAG, "Using RFB protocol version " + rfb.clientMajor + "." + rfb.clientMinor);

            int bitPref=0;
            if (us.length() > 0)
                bitPref|=1;
            //Log.d("debug","bitPref="+bitPref);
            int secType = rfb.negotiateSecurity(bitPref);
            int authType;
            if (secType == RfbProto.SecTypeTight) {
                rfb.initCapabilities();
                rfb.setupTunneling();
                authType = rfb.negotiateAuthenticationTight();
            } else if (secType == RfbProto.SecTypeUltra34) {
                rfb.prepareDH();
                authType = RfbProto.AuthUltra;
            } else {
                authType = secType;
            }

            switch (authType) {
                case RfbProto.AuthNone:
                    Log.i(TAG, "No authentication needed");
                    rfb.authenticateNone();
                    break;
                case RfbProto.AuthVNC:
                    Log.i(TAG, "VNC authentication needed");
                    rfb.authenticateVNC(pw);
                    break;
                case RfbProto.AuthUltra:
                    rfb.authenticateDH(us,pw);
                    break;
                default:
                    throw new Exception("Unknown authentication scheme " + authType);
            }
            // Protocol initialization
            rfb.writeClientInit();
            rfb.readServerInit();

            Log.i(TAG, "Desktop name is " + rfb.desktopName);
            Log.i(TAG, "Desktop size is " + rfb.framebufferWidth + " x " + rfb.framebufferHeight);


            return rfb;
        }

    }

    private void sendText(RfbProto rfb, String s) {

        Log.i(TAG, "sendText: string = " + s);
        if (SongbookCfg.getInstance().remoteEnable()) {
            int l = s.length();
            for (int i = 0; i<l; i++) {
                char c = s.charAt(i);
                int meta = 0;
                int keysym = c;
                if (Character.isISOControl(c)) {
                    if (c=='\n')
                        keysym = 0xFF0D /* MetaKeyBean.keysByKeyCode.get(KeyEvent.KEYCODE_ENTER).keySym */;
                    else
                        continue;
                }
                try {
                    rfb.writeKeyEvent(keysym, meta, true);
                    rfb.writeKeyEvent(keysym, meta, false);
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    private void sendMetaKey(RfbProto rfb, String s) {

        Log.i(TAG, "sendMetaKey: string = " + s);
        if (SongbookCfg.getInstance().remoteEnable()) {
            int meta = 4;
            int keysym = 0;
            if (s.equalsIgnoreCase(SOUND_TOGGLE))
                keysym = 0x0068;
            else if (s.equalsIgnoreCase(FORWARD))
                keysym = 0x006e;
            else if (s.equalsIgnoreCase(REWIND))
                keysym = 0x0072;
            else if (s.equalsIgnoreCase(PAUSE))
                keysym = 0x0070;
            else if (s.equalsIgnoreCase(SONG_INFO))
                keysym = 0x0069;
            else if (s.equalsIgnoreCase(START_ON_TRACK2))
                keysym = 0x0032;
            try {
                rfb.writeKeyEvent(keysym, meta, true);
                rfb.writeKeyEvent(keysym, meta, false);
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }
}

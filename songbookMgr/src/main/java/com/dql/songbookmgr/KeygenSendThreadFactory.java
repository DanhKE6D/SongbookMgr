package com.dql.songbookmgr;

import com.dql.dbutil.SongbookCfg;

/**
 * Created by dql on 1/31/14.
 */
public class KeygenSendThreadFactory {
    private KeygenSendThreadFactory() {
        // don't want to instantiate this object
    }
    static SongbookCfg.MediaServerType currentServerType = SongbookCfg.MediaServerType.SBCM_ServerType;

    public static SongbookCfg.MediaServerType getCurrentServerType() { return currentServerType; }

    public static KeygenSendThread createKeygenSendThread(SongbookCfg.MediaServerType serverType) {
        currentServerType = serverType;
        switch (serverType) {
            case SBCM_ServerType:
                return new SBCMKeygenSendThread();
            default:
            case VNC_ServerType:
                return new VNCKeygenSendThread();
        }
    }
}

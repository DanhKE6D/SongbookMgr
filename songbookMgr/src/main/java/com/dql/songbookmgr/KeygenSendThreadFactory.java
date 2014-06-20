package com.dql.songbookmgr;

import com.dql.dbutil.SongbookCfg;

/**
 * Created by dql on 1/31/14.
 */
public class KeygenSendThreadFactory {
    static SongbookCfg.MediaServerType currentServerType = SongbookCfg.MediaServerType.SBCM_ServerType;

    public static SongbookCfg.MediaServerType getCurrentServerType() { return currentServerType; }

    public static KeygenSendThread createKeygenSendThread(SongbookCfg.MediaServerType serverType) {
        currentServerType = serverType;
        switch (serverType) {
            case SBCM_ServerType:
                return new SBCMKeygenSendThread();
            case VNC_ServerType:
                return new VNCKeygenSendThread();
        }
        return null;
    }
}

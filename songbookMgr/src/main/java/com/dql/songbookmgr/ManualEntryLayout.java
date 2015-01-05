package com.dql.songbookmgr;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dql.dbutil.SongbookCfg;
import com.dql.quickaction.QuickAction;

/**
 * Created by dql on 12/29/14.
 */
public class ManualEntryLayout extends Fragment  {

    private static final String TAG = "ManualEntryLayout";
    String myTag;
    SongbookMgrActivity act;
    String songIDToSend = "";
    int maxSongIDValue = 9999;          // default to 4 digit song ID
    TextView songIdDisp;
    Button[] keypadButtons = new Button[10];
    Button backBtn, clearBtn, sendBtn;

   @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        act = (SongbookMgrActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // set up tag so that the host activity can call me back
        myTag = getTag();
        Log.i(TAG, "ManualEntryLayout.onCreate: myTag = " + myTag);
        ((SongbookMgrActivity) getActivity()).setTabPlaylistFragment(myTag);
        if (SongbookCfg.getInstance().longFormatSongID())
            maxSongIDValue = 99999;             // max 5 digit song ID
    }

    void handleNumericButtonPressed(String digitText) {
        Log.i(TAG, "handleNumericButtonPressed() digit = " + digitText);
        String oldSongID = songIDToSend;
        songIDToSend = songIDToSend + digitText;
        if (Integer.valueOf(songIDToSend) > maxSongIDValue) {
            // don't want to add more digit if the value is going to be larger than max number of
            // digits allow
            songIDToSend = oldSongID;
        }
        songIdDisp.setText(songIDToSend);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.rmt_cmd_screen, null);
        songIdDisp = (TextView) root.findViewById(R.id.songid_display);
        songIdDisp.setText(songIDToSend);
        keypadButtons[0] = (Button) root.findViewById(R.id.button0);
        keypadButtons[1] = (Button) root.findViewById(R.id.button1);
        keypadButtons[2] = (Button) root.findViewById(R.id.button2);
        keypadButtons[3] = (Button) root.findViewById(R.id.button3);
        keypadButtons[4] = (Button) root.findViewById(R.id.button4);
        keypadButtons[5] = (Button) root.findViewById(R.id.button5);
        keypadButtons[6] = (Button) root.findViewById(R.id.button6);
        keypadButtons[7] = (Button) root.findViewById(R.id.button7);
        keypadButtons[8] = (Button) root.findViewById(R.id.button8);
        keypadButtons[9] = (Button) root.findViewById(R.id.button9);
        backBtn = (Button) root.findViewById(R.id.buttonback);
        clearBtn = (Button) root.findViewById(R.id.buttonclear);
        sendBtn = (Button) root.findViewById(R.id.buttonsend);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int strLen = songIDToSend.length();
                if (strLen > 1)
                    songIDToSend = songIDToSend.substring(0, strLen - 1);
                else
                    songIDToSend = "";
                songIdDisp.setText(songIDToSend);
            }
        });
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                songIDToSend = "";
                songIdDisp.setText(songIDToSend);
            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (songIDToSend.length() > 0) {
                    act.sendSongIDManually(Integer.valueOf(songIDToSend));
                }
            }
        });
        for (int i = 0; i <= 9; i++) {
            keypadButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Button b = (Button)v;
                    handleNumericButtonPressed(b.getText().toString());
                }
            });
        }
        return root;
    }
}

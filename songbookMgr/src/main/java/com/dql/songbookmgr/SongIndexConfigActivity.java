package com.dql.songbookmgr;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ContentHandler;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.dql.dbutil.SongbookCfg;
import com.dql.dbutil.SongID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.apache.http.Header;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.protocol.HTTP;

public class SongIndexConfigActivity extends Activity {


	static final String TAG = "SongIndexConfigActivity";
	private final float DEF_FONT_SIZE = 8.0f;
	ListView listView;
    private static TestListAdapter listAdapter = null;
    private static ArrayList<SongID> testSongList = null;
    final SongbookCfg cfg = SongbookCfg.getInstance();
    float textSongFontSize = cfg.getDisplayTextSize();
    float textSingerFontSize = cfg.getSingerDisplayTextSize();
    // make the radio button check on song name to start
    float textFontSize = textSongFontSize;
    private int textSizeProgress = (int) (textFontSize - DEF_FONT_SIZE);
	Button saveCfgBtn;
	CheckBox enableRemote, longFmtSongID, useVNCServer, checkServerConnection;
    RadioGroup textSizeRadioGroup;
    RadioButton songNameTextSizeBtn, singerNameTextSizeBtn;
	EditText serverName, portNumber, userName, password;
	SeekBar textSizeSeekBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.app_config);
		saveCfgBtn = (Button) findViewById(R.id.savecfg);
		enableRemote = (CheckBox) findViewById(R.id.remoteenable);
        checkServerConnection = (CheckBox) findViewById(R.id.checkforconnection);
        useVNCServer = (CheckBox) findViewById(R.id.use_vnc_server);
        textSizeRadioGroup = (RadioGroup) findViewById(R.id.size_change_radio_group);
        checkServerConnection.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkServerConnection.isChecked()){
                    checkServerConnection.setChecked(false);
                    performConnectionTest(serverName.getText().toString(), cfg.SBCM_PORT_NO);
                }
            }
        });
        songNameTextSizeBtn = (RadioButton) findViewById(R.id.song_text_size);
        singerNameTextSizeBtn = (RadioButton) findViewById(R.id.singer_text_size);
        songNameTextSizeBtn.setChecked(true);
        textSizeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup arg0, int id) {
                RadioButton checkedRadioButton = (RadioButton) textSizeRadioGroup.findViewById(id);
                if (checkedRadioButton.isChecked()) {
                    if (id == R.id.song_text_size) {
                        // load the seek bar textsize for song name
                        textFontSize = textSongFontSize;
                        textSizeProgress = (int) (textFontSize - DEF_FONT_SIZE);
                    }
                    else {
                        // singer name
                        textFontSize = textSingerFontSize;
                        textSizeProgress = (int) (textFontSize - DEF_FONT_SIZE);
                    }
                    textSizeSeekBar.setProgress(textSizeProgress);
                    listAdapter.notifyDataSetChanged();

                }
            }
        });
		serverName = (EditText) findViewById(R.id.serveraddr);
		portNumber = (EditText) findViewById(R.id.serverportno);
		userName = (EditText) findViewById(R.id.username);
		password = (EditText) findViewById(R.id.password);
		textSizeSeekBar = (SeekBar) findViewById(R.id.textsize_seekbar);		
		enableRemote.setChecked(cfg.remoteEnable());
        useVNCServer.setChecked(cfg.getMediaServerType() == SongbookCfg.MediaServerType.VNC_ServerType);
		serverName.setText( cfg.getServerName()) ;
		portNumber.setText(Integer.toString(cfg.getServerPortNumber()));
		userName.setText(cfg.getUserName());
		password.setText(cfg.getUserPassword());
        longFmtSongID = (CheckBox) findViewById(R.id.songid_5digit);
        longFmtSongID.setChecked(cfg.longFormatSongID());

 		Log.i(TAG, "textSizeProgress = " + textSizeProgress);
		textSizeSeekBar.setProgress(textSizeProgress);
		textSizeSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
   	    listView = (ListView) findViewById(R.id.testListView);		
        testSongList = new ArrayList<SongID>();
        saveCfgBtn.setOnClickListener(new OnClickListener() {           
	         @Override
	         public void onClick(View v) {
		    	cfg.updateServerCfgParams(serverName.getText().toString(), Integer.parseInt(portNumber.getText().toString()),
		    				enableRemote.isChecked(),
		    				userName.getText().toString(), password.getText().toString());
                cfg.setDisplayTextSize(textSongFontSize, textSingerFontSize);
                cfg.setSongIDFormat(longFmtSongID.isChecked());
                cfg.setMediaServerType(useVNCServer.isChecked() ? SongbookCfg.MediaServerType.VNC_ServerType :
                        SongbookCfg.MediaServerType.SBCM_ServerType);
				Intent intent = new Intent();
			    setResult(RESULT_OK, intent);
	        	finish();
	         }
	    });
        
        for (int i = 1; i < 6; i++)
  	    	testSongList.add(new SongID(i,"THIS IS A LONG SONG NAME  # " + String.valueOf(i), "SINGER # " + String.valueOf(i)));

  	    listAdapter = new TestListAdapter(this, R.layout.custom_song_list , testSongList);
  	    listView.setAdapter(listAdapter);
	}

	class TestListAdapter extends ArrayAdapter<SongID> {

	    private Context mContext;
	    private int id;
	    private List <SongID>items ;

	    public TestListAdapter(Context context, int textViewResourceId , List<SongID> list ) 
	    {
	        super(context, textViewResourceId, list);           
	        mContext = context;
	        id = textViewResourceId;
	        items = list ;
	    }

	    @Override
	    public View getView(int position, View v, ViewGroup parent)
	    {
	        View mView = v ;
	        if (mView == null) {
	            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            //LayoutInflater vi = LayoutInflater.from(mContext);
	            mView = vi.inflate(id, null);
	        }

	        if (items.get(position) != null ) {
	            TextView songID  = (TextView) mView.findViewById(R.id.songID);
	            TextView songName  = (TextView) mView.findViewById(R.id.songName);
	            TextView singer  = (TextView) mView.findViewById(R.id.singer);
	            
	        	songID.setTextSize(textSingerFontSize);
	        	songID.setTextColor(Color.BLACK);
	        	//songID.setTypeface(null,Typeface.BOLD);
	        	songID.setText(String.format("%04d", items.get(position).getID()));

	        	songName.setTextSize(textSongFontSize);
	        	songName.setTextColor(Color.BLACK);
	        	songName.setTypeface(null,Typeface.BOLD);
	        	songName.setText(items.get(position).getName());

	        	singer.setTextSize(textSingerFontSize);
	        	singer.setTextColor(Color.BLACK);
	        	singer.setText(items.get(position).getSinger());
		    }

	        return mView;
	    }

	    }

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener()
    {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			textSizeProgress = textSizeSeekBar.getProgress();
			// smallest is 8.0 and largest is 50
			textFontSize = (float) textSizeProgress + DEF_FONT_SIZE;
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
            int id = textSizeRadioGroup.getCheckedRadioButtonId();
            if (id == R.id.song_text_size) {
                textSongFontSize = textFontSize;
            }
            else {
                // must be singer name size
                textSingerFontSize = textFontSize;
            }
            listAdapter.notifyDataSetChanged();
        }


    };
    private void performConnectionTest(String serverName, int serverPortNumber) {

        PerformConnectTestTask task = new PerformConnectTestTask();
        String[] connectionParam = new String[2];
        connectionParam[0] = serverName;
        connectionParam[1] = String.valueOf(serverPortNumber);
        task.execute(connectionParam);
    }

    private class PerformConnectTestTask extends AsyncTask<String, Void, Boolean> {
        private ProgressDialog progressDialog;
        StringBuilder errString = new StringBuilder();
        String serverName, pw;
        int serverPortNumber, sbcm_ServerPortNumber;

        @Override
        protected void onPreExecute() {
            // perhaps show a dialog with a progress bar
            // to let your users know something is happening
        }

        @Override
        protected Boolean doInBackground(String... params) {
            serverName = params[0];
            sbcm_ServerPortNumber = Integer.valueOf(params[1]);

            boolean bResult = true;
            final String cmd = "CONNECTION_TEST";
            String response = null;
            Socket clientSocket = null;
            DataOutputStream outToServer = null;
            BufferedReader inFromServer = null;
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    // change UI elements here
                    progressDialog = new ProgressDialog(SongIndexConfigActivity.this);
                    progressDialog.setIndeterminate(true);
                    progressDialog.setMessage("Checking for SBCM connectivity at " + serverName + ":" + sbcm_ServerPortNumber);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            });
            try {
                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(serverName, sbcm_ServerPortNumber), SongbookCfg.TCP_DEF_CONN_TIMEOUT);
                //clientSocket = new Socket(serverName, connServerListenPortNumber);
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                outToServer.writeBytes(cmd + '\n');
                response = inFromServer.readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                errString.append("Exception occurred during connection test\n");
            }
            if (response != null)
                //Log.i(TAG, "Response from server: " + response);
                errString.append("* SBCM server responses: " + response + "\n");
            else
                errString.append("* Unable to communicate with SBCM server\n");

            try {
                clientSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                errString.append("Exception occurred in closing client socket\n");
            }
            return bResult;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            final boolean res = result;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;

                    }

                    // something failed during read
                    AlertDialog.Builder builder = new AlertDialog.Builder(SongIndexConfigActivity.this);
                    builder.setTitle("Connection Test Result");
                    builder.setMessage(errString.toString());
                    // Setting Icon to Dialog
                    builder.setIcon(R.drawable.ic_get_info);
                    builder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });

                    // create alert dialog
                    final AlertDialog alertDialog = builder.create();
                    // show it
                    alertDialog.show();
                }

            });
        }
    }
}

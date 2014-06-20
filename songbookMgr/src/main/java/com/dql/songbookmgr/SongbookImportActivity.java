package com.dql.songbookmgr;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.dql.dbutil.DatabaseHandler;
import com.dql.dbutil.SongID;
import com.dql.dbutil.SongbookCfg;
import com.dql.filechooser.FileChooser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

public class SongbookImportActivity extends Activity {

	private static final int REQUEST_FILENAME = 1;
	static final String appDirInSDCard = "TKSongIndex";
	static final String songIndexDBDir = "DB";
	private String curPathName = Environment.getExternalStorageDirectory().getPath() + "/" + appDirInSDCard;
	private String curFileName;
	private static final String TAG = "SongbookImportActivity";
    private ImageButton importButton, importFromServerButton;
	private Button fileBrowseButton;
    CheckBox longFmtSongID;
    boolean longFmt = false;
	private EditText fileNameText, leftDelimiter, rightDelimiter;
	Context myCtx = SongbookImportActivity.this;

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	//setTitle(getResources().getString(R.string.songbook_import));
        setContentView(R.layout.songbook_import); 
        findAllViewsById();
        importFromServerButton.requestFocus();
        importFromServerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // import from server

                // Make sure that if the file exist, let the user back out or over write
                AlertDialog.Builder builder = new AlertDialog.Builder(myCtx);
                builder.setTitle("Import From Server Confirmation");
                builder.setIcon(R.drawable.ic_alert);
                builder.setMessage("Make sure that sbConnectionMgr is running on media server computer");
                builder.setPositiveButton("Proceed",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                performSongImportFromServer();
                            }
                        });

                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                // create alert dialog
                AlertDialog alertDialog = builder.create();
                // show it
                alertDialog.show();

            }

        });
        importButton.setOnClickListener(new OnClickListener() {           
	         @Override
	         public void onClick(View v) {

	        	//	Log.i(TAG, "Import button clicked");
	        	// import file
	        	curFileName = fileNameText.getText().toString();
                Log.i(TAG, "Song book file name = " + curFileName);
                longFmt = longFmtSongID.isChecked();
	            performSongLoad(curFileName);

	         }
	    });
        
        fileBrowseButton.setOnClickListener(new OnClickListener() {
	         @Override
	         public void onClick(View v) {
                //	Log.i(TAG, "Browse button clicked. CurPathName = " + curPathName); 
	         	Intent intent1 = new Intent(v.getContext(), FileChooser.class);
	            intent1.putExtra(FileChooser.START_PATH, curPathName);
	        	startActivityForResult(intent1,REQUEST_FILENAME);
	        	}
	    });        

    }
	
	private void findAllViewsById() {
		leftDelimiter = (EditText) findViewById(R.id.LeftDelimiter);
		rightDelimiter = (EditText) findViewById(R.id.RightDelimiter);
		fileNameText = (EditText) findViewById(R.id.SongFileName);
		fileBrowseButton = (Button) findViewById(R.id.BrowseButton);
        importFromServerButton = (ImageButton) findViewById(R.id.import_server_btn);
        importButton = (ImageButton) findViewById(R.id.ImportButton);
        longFmtSongID = (CheckBox) findViewById(R.id.songid_5digit);
	} 
   
 // Listen for results.
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // See which child activity is calling us back.
    	if (requestCode == REQUEST_FILENAME) {
    		if (resultCode == RESULT_OK) {
    			curPathName = data.getStringExtra(FileChooser.GET_PATH);
    			curFileName = data.getStringExtra(FileChooser.GET_FILE_NAME);
            	String fileName = curPathName + "/" + curFileName;
        		fileNameText.setText(fileName);
                //   Log.i(TAG, "New file name = " + fileName); 
      		}
        }
    }

    protected void errorMesgBox(Context ctx, String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(title);
        builder.setMessage(msg);
        // Setting Icon to Dialog
        builder.setIcon(R.drawable.ic_stop);
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // send failed
                        Intent intent = new Intent();
                        setResult(RESULT_CANCELED, intent);
                        finish();
                    }
                });

        // create alert dialog
        final AlertDialog alertDialog = builder.create();
        // show it
        alertDialog.show();
    }

    private void performSongImportFromServer() {

        final PerformLoadSongImportFromServerTask task = new PerformLoadSongImportFromServerTask();
        task.execute();
        //setting timeout thread for async task
        Thread timerThread = new Thread(){
            public void run(){
                try {
                    task.get(60000, TimeUnit.MILLISECONDS);  //set time in milisecond(in this timeout is 30 seconds

                } catch (Exception e) {
                    task.cancel(true);
                    task.cancelDialog();
                    runOnUiThread(new Runnable()
                    {
                        public void run() {
                            // send failed -- should add some notification here
                        errorMesgBox(myCtx, "Songbook Import Error",
                                        "Timeout error during importing songbook");
                        }
                    });

                }
            }
        };
        timerThread.start();
    }


    private class PerformLoadSongImportFromServerTask extends AsyncTask<String, Void, Boolean> {
        private ProgressDialog progressDialog;
        StringBuilder errString = new StringBuilder();
        private int progress;			// 100 is completed
        Socket clientSocket = null;

        boolean getServerInfo() {

            final String cmd = "GET_SERVER_INFO";
            String response = null;
            DataOutputStream outToServer = null;
            BufferedReader inFromServer = null;
            boolean result = false;

            try {
                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(SongbookCfg.getInstance().getServerName(),
                        SongbookCfg.SBCM_PORT_NO), SongbookCfg.TCP_DEF_CONN_TIMEOUT);
                //clientSocket = new Socket(serverName, connServerListenPortNumber);
                //Log.i(TAG, "Open new socket at " + serverName + ":" + connServerListenPortNumber);
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                outToServer.writeBytes(cmd + '\n');
                Log.i(TAG, "Sent to server: " + cmd);
                response = inFromServer.readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                result = false;
            }
            if (response != null) {
                Log.i(TAG, "Response from server: " + response);
                if (/* (response.contains("VLC"))  || */ (response.contains("TKaraoke")) ) {
                    // Only TKaraoke is allowed
                    result = true;
                }
                else
                    result = false;
            }
            else
                result = false;
            try {
                clientSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                result = false;
            }
            finally {
                clientSocket = null;
            }
            return result;
        }

        ByteArrayOutputStream receiveSongbookFromServer() {

            final String cmd = "GET_SONGBOOK";
            boolean result = true;

            DataOutputStream outToServer = null;
            DataInputStream inFromServer = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int fileLength, bytesCounter = 0;
            try {
                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(SongbookCfg.getInstance().getServerName(),
                        SongbookCfg.SBCM_PORT_NO), SongbookCfg.TCP_DEF_CONN_TIMEOUT);
                //Log.i(TAG, "Open new socket at " + serverName + ":" + connServerListenPortNumber);
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                inFromServer = new DataInputStream(clientSocket.getInputStream());
                outToServer.writeBytes(cmd + '\n');
                Log.i(TAG, "Sent to server: " + cmd);
                Log.i(TAG, "Waiting for server to send file...");

                // now wait for the server to send the file back

                if (inFromServer != null) {
                    byte[] buffer = new byte[8192];     // 4k optimized for android???
                    int bytesRead;
                    try {
                        Log.i(TAG, "Open file for writing...");
                        //Step 1: Read length
                        fileLength = inFromServer.readInt();
                        Log.i(TAG, "File length = " + fileLength);
                        // Step 2: Read file - one buffer at a time until EOF (byteRead < 0)
                        while (bytesCounter < fileLength) {
                            bytesRead = inFromServer.read(buffer);
                            if (bytesRead > 0) {
                                bytesCounter += bytesRead;
                                baos.write(buffer, 0, bytesRead);

                                progress = (int) (bytesCounter * 100.0 / fileLength);
                                if ((progress % 5) == 0)
                                    progressDialog.setProgress(progress);
                            }
                            // don't want to break out because server may not finish
                            //else {
                            //    // end of file
                            //    break;
                            //}
                        }

                        Log.i(TAG, "Done reading from network!");
                    } catch (IOException ex) {
                        // Do exception handling
                        ex.printStackTrace();
                        Log.i(TAG, "Exception occurred during file transfer!");
                        baos = null;
                    }
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.i(TAG, "Exception occurred during TCP communication!");
                baos = null;
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                baos = null;
            }
            finally {
                clientSocket = null;
            }
            return baos;
        }

        @Override
        protected void onPreExecute() {
            // perhaps show a dialog with a progress bar
            // to let your users know something is happening
            //progressDialog = ProgressDialog.show(m_activity,
            //		"Please wait...", "Loading song list from database", true, true);
            //final String TAG = "onPreExecute";
            //Log.i(TAG, "About to display progress dialog box");

            progressDialog = new ProgressDialog(myCtx);
            //progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(true);
            //progressDialog.setMessage("Loading songs into database...");
            progressDialog.setMessage("Checking media server type...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            final String TAG = "doInBackground";
            ByteArrayOutputStream xlmStream;

            Log.i(TAG, "Getting server info...");
            if (getServerInfo()) {
                // connection is successful, change message to loading file
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        // change UI elements here
                        //progressDialog.setIndeterminate(false);
                        progressDialog.dismiss();
                        progressDialog = new ProgressDialog(myCtx);
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialog.setMessage("Waiting for server to send songbook catalog");
                        progressDialog.setCancelable(false);
                        progressDialog.show();
                        progress = 0;
                    }
                });
                xlmStream = receiveSongbookFromServer();
                if (xlmStream != null) {
                    boolean bResult = true;
                    ArrayList<SongID> mrlList = null;
                    XmlParser xmlParser = new XmlParser();
                    mrlList = xmlParser.parseSongbookXmlStream(XmlParser.XmlInputType.XMLString, xlmStream.toString());
                    if (mrlList == null) {
                        errString.append("Exception occurred during parsing XML stream");
                        bResult = false;
                    }
                    if (bResult) {
                        // save to database
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run(){
                                // change UI elements here
                                //progressDialog.setIndeterminate(true);
                                progressDialog.dismiss();
                                progressDialog = new ProgressDialog(myCtx);
                                progressDialog.setMessage("Loading songs into database...");
                                progressDialog.setIndeterminate(true);
                                progressDialog.setCancelable(false);
                                progressDialog.show();
                            }
                        });
                        Log.i(TAG, "Saving to database");
                        DatabaseHandler db = DatabaseHandler.getInstance(myCtx, SongbookCfg.getInstance().getPackageName());
                        db.clearAll();
                        //int counter = 1, dbsize = mrlList.size();

                        //for (FullMrlID i : mrlList) {
                        //    db.addSong(i.getSong(), i.getMrl(), i.getDVD());
                        //    progress = (int) (counter++ * 100.0 / dbsize);
                        //    if ((progress % 5) == 0)
                        //        progressDialog.setProgress(progress);
                        //}
                        // going from inserting 1 record at a time to bulk insert using sql compile statement
                        // reducing db insertion time from 140 secs min to 5 secs (timed on the Galaxy Tab2)
                        // very big improvement -- dql -- 2013/11/21
                        db.addAllSongs(mrlList);
                        db.close();
                    }
                    return bResult;
                }
                else {
                    errString.append("Unable to receive songbook from server");
                    return false;
                }
            }
            errString.append("Media server type is incorrect!");
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            final boolean res = result;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cancelDialog();
                    if (!res) {
                        // something failed during read
                        errorMesgBox(myCtx, "Songbook Import Error", errString.toString());
                    }
                    else {
                        Intent intent = new Intent();
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }
            });
        }

        public void cancelDialog() {
            //Log.i(TAG, "task cancelled...");
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;

            }
            // Have to do this because if the timeout error occurred during
            // loading songbook, the socket was not close and can caused the
            // port to be unavailable
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                finally {
                    clientSocket = null;
                }
            }
        }
    }
	private void performSongLoad(String songFileName) {

	    PerformLoadSongTask task = new PerformLoadSongTask();
	    String[] songImportParam = new String[3];
	    songImportParam[0] = songFileName;
	    songImportParam[1] = leftDelimiter.getText().toString();
	    songImportParam[2] = rightDelimiter.getText().toString();	    
	    task.execute(songImportParam);
	}

	private class PerformLoadSongTask extends AsyncTask<String, Void, Boolean> {
		private ProgressDialog progressDialog;
    	//private int progress;			// 100 is completed
        StringBuilder errString;
		
		@Override
		protected void onPreExecute() {
            // perhaps show a dialog with a progress bar
            // to let your users know something is happening
		   	//progressDialog = ProgressDialog.show(m_activity,
	        //		"Please wait...", "Loading song list from database", true, true);
			//final String TAG = "onPreExecute";
            //Log.i(TAG, "About to display progress dialog box");

			progressDialog = new ProgressDialog(myCtx);
			//progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(true);
			progressDialog.setMessage("Loading songs into database...");
			progressDialog.setCancelable(false);
			progressDialog.show();
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			String songFileName = params[0];
			String lDelimiter = params[1];
			String rDelimiter = params[2];
			
		    File songFile;
		    long fileLength, curFileRead = 0;
		    Boolean bResult = true;
            ArrayList<SongID> songList = new ArrayList<SongID>();

			final String TAG = "doInBackground";
            //String tagLine;


            Log.i(TAG, "New file name = " + songFileName + " lDelim = " + lDelimiter + " rDelim = " + rDelimiter); 

			//songList.clear();
			songFile = new File(songFileName);

            Log.i(TAG, "Creating new song file descriptor"); 
			fileLength = songFile.length();
			
			FileInputStream in = null;
			BufferedReader bufferedReader = null;
			try {
				in = new FileInputStream(songFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    if (in != null) {
		    	InputStreamReader inputStreamReader = new InputStreamReader(in);
		    	bufferedReader = new BufferedReader(inputStreamReader);
		    }
		    

		    //StringBuilder sb = new StringBuilder();
		    String line, newLine, songID, songName, singers;
            int songIDSize;
		    boolean hasLDelim, hasRDelim;
            if (longFmt)
                songIDSize = 5;     // 5 digit song ID
            else
                songIDSize = 4;     // 4 digit song ID
		    try {
				while ((line = bufferedReader.readLine()) != null) {
                    errString = new StringBuilder();
                    errString.append("Input Text: ");
                    errString.append(line);
                    errString.append("\n\n");
				    //sb.append(line);
					// Our song file will have the format below
					// nnnn song name (singer|singers)
					// where nnnn is the song ID in string
					// song name is the song name (leading spaces should be removed -- after the nnnn digits
					//                             trailing spaces until the ( character should also be removed)
					// Error checking: need to test for line integrity before parsing
					hasLDelim = line.lastIndexOf(lDelimiter) >= 0;
                    if (hasLDelim == false) {
                        errString.append("Error: Missing ");
                        errString.append(lDelimiter);
                        errString.append(" for Singer column\n");
                        bResult = false;
                        break;

                    }
					hasRDelim = line.lastIndexOf(rDelimiter) >= 0;
                    if (hasRDelim == false) {
                        errString.append("Error: Missing ");
                        errString.append(rDelimiter);
                        errString.append(" for Singer column\n");
                        bResult = false;
                        break;

                    }
					if (hasLDelim && hasRDelim) {
						songID = line.substring(0, songIDSize);
						newLine = line.substring(songIDSize, line.indexOf(lDelimiter));
						songName = newLine.trim();
						newLine = line.substring(line.indexOf(lDelimiter) + 1, line.lastIndexOf(rDelimiter));
						singers = newLine.trim();
						// Check for '?' in the singer field and replace it with UNKNOWN
						if (singers.equalsIgnoreCase("?"))
							singers = "UNKNOWN";
						// don't want to display debug info every single line
					    //if ((numOfRecs % 20) == 0) {
						//tagLine = "ID:" + songID + "|Name:" + songName + "|Singer(s):" + singers;
						//Log.i(TAG, tagLine);
						//}
                        // There are cases where the delimiters got stuck in the singer column because
                        // the parser takes the first lDelim and last rDelim. Need to go through and
                        // convert any left over delims to ' '
                        char [] singerChArray = singers.toCharArray();
                        for (int i = 0; i < singers.length(); i++) {
                            if ( (singerChArray[i] == lDelimiter.charAt(0)) ||
                                    (singerChArray[i] == rDelimiter.charAt(0)))
                                singerChArray[i] = ' ';
                        }

                        if (songName.compareTo("") == 0) {
                            // invalid ID or song file name
                            errString.append("Error: Invalid song name\n");
                            bResult = false;
                            break;

                        }
                        int sID = Integer.parseInt(songID);
                        if ((sID <= 0) || (sID > 99999)) {
                            // invalid ID or song file name
                            errString.append("Error: Invalid song ID\n");
                            bResult = false;
                            break;
                        }
						SongID s = new SongID(sID, songName, new String(singerChArray));
                        songList.add(s);
						curFileRead += line.length();
						//progress = (int) (curFileRead * 100.0 / fileLength);
						//if ((progress % 5) == 0)
						//	progressDialog.setProgress(progress);
					}
					else {
						Log.d(TAG, line + " has no delimiters");
						bResult = false;
						break;
					}
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				bResult = false;
                errString.append("Error: Unable to parse song ID\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				bResult = false;
                errString.append("Error: Exception occurred during parsing\n");
			}
		    
		    if (in != null)
		        try {
				    in.close();
			    } catch (IOException e1) {
				    // TODO Auto-generated catch block
				    e1.printStackTrace();
				    bResult = false;
                    errString.append("Error: Unable to close input file\n");
			    }
		    if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					bResult = false;
                    errString.append("Error: Unable to close bufferedReader\n");
				}
		    }
            // Save to database. Using this method will improve db insert dramatically
            DatabaseHandler db = DatabaseHandler.getInstance(myCtx, SongbookCfg.getInstance().getPackageName());
            db.clearAll();
            db.addAllSongs(songList);
            db.close();     // close the database to prevent leak
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
		    		if (!res) {
		    			// something failed during read
                        errorMesgBox(myCtx, "Songbook Import Error", errString.toString());
		    		}
                    else {
				   	    Intent intent = new Intent();
			            setResult(RESULT_OK, intent);
		         	    finish();
                    }
		    	}
		    });
		}
		
	}
}

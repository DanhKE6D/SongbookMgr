package com.dql.songbookmgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


import com.dql.dbutil.DatabaseHandler;

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
import android.widget.EditText;


public class SongbookDBBackupActivity extends Activity {

	private static final int REQUEST_FILENAME = 1;
	static final String appDirInSDCard = "TKSongIndex";
	static final String songIndexDBDir = "DB";
	private String curPathName = Environment.getExternalStorageDirectory().getPath() + "/" + appDirInSDCard + "/" + songIndexDBDir;
	private String curFileName;
	private static final String TAG = "SongbookDBBackupActivity";
	private static final String defSongbookDBName = "Songbook.db";
	private Button fileBackupButton;
	private Button fileBowseButton;
	private EditText fileNameText;
	Context myCtx = SongbookDBBackupActivity.this;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	setTitle(getResources().getString(R.string.songbookdb_backup));
        setContentView(R.layout.db_export); 
		fileNameText = (EditText) findViewById(R.id.SongBookDBName);
		fileBowseButton = (Button) findViewById(R.id.FileBrowseButton);
		fileBackupButton = (Button) findViewById(R.id.BackupButton);
		fileNameText.setText(curPathName + "/" + defSongbookDBName);
        fileBackupButton.setOnClickListener(new OnClickListener() {           
	         @Override
	         public void onClick(View v) {

	        	//	Log.i(TAG, "Import button clicked");
	        	// import file
	        	curFileName = fileNameText.getText().toString();
    			// find out if the file extension is ".db"
				int n = curFileName.lastIndexOf(".");
				if (n > 0) {
					String fileType = curFileName.substring(n, curFileName.length());
					if (!fileType.equalsIgnoreCase(".db")) {
						curFileName = curFileName + ".db";
					}
				}
				else {
					curFileName = curFileName + ".db";					
				}
						
                Log.i(TAG, "Songbook DB file name = " + curFileName);
        	    File file = new File(curFileName);
           	    if (file.exists()) {
           	    	// Make sure that if the file exist, let the user back out or over write 
        			AlertDialog.Builder builder = new AlertDialog.Builder(myCtx);
                	builder.setTitle("DB Overwrite Confirmation");
                	builder.setIcon(R.drawable.ic_alert);
                	builder.setMessage("OK to overwite " + curFileName + "?");                	           	
                	builder.setPositiveButton("Overwrite",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                               	performSongbookDBBackup(curFileName);
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
           	    else {
           	    	// go for it
                    Log.i(TAG, "Backing up DB = " + curFileName); 
                   	performSongbookDBBackup(curFileName);
                }

	         }
	    });
        
        fileBowseButton.setOnClickListener(new OnClickListener() {           
	         @Override
	         public void onClick(View v) {
                //	Log.i(TAG, "Browse button clicked. CurPathName = " + curPathName); 
	         	Intent intent1 = new Intent(v.getContext(), FileChooser.class);
	            intent1.putExtra(FileChooser.START_PATH, curPathName);
	        	startActivityForResult(intent1,REQUEST_FILENAME);
	        	}
	    });        

    }
	
 
 // Listen for results.
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // See which child activity is calling us back.
    	if (requestCode == REQUEST_FILENAME) {
    		if (resultCode == RESULT_OK) {
    			curPathName = data.getStringExtra(FileChooser.GET_PATH);
    			curFileName = data.getStringExtra(FileChooser.GET_FILE_NAME);
    			if (curFileName == null)
    				curFileName = defSongbookDBName;

    			
            	String fileName = curPathName + "/" + curFileName;
        		fileNameText.setText(fileName);
                //   Log.i(TAG, "New file name = " + fileName); 
      		}
        }
    }
    
	private void performSongbookDBBackup(String songFileName) {
	
		performSongbookDBBackupTask task = new performSongbookDBBackupTask();
	    task.execute(songFileName);
	}

	private class performSongbookDBBackupTask extends AsyncTask<String, Void, Void> {
		private ProgressDialog progressDialog;
    	private int progress;			// 100 is completed
		
		@Override
		protected void onPreExecute() {
            // perhaps show a dialog with a progress bar
            // to let your users know something is happening
		   	//progressDialog = ProgressDialog.show(m_activity,
	        //		"Please wait...", "Loading song list from database", true, true);
			final String TAG = "onPreExecute";
            Log.i(TAG, "About to display progress dialog box");

			progressDialog = new ProgressDialog(myCtx);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage("Backing up current songbook DB...");
			progressDialog.setCancelable(false);
			progressDialog.show();
		}
		
		@Override
		protected Void doInBackground(String... params) {
			final String TAG = "doInBackground";
			
			String songFileName = params[0];
		    // ArrayList<SongID> songList = new ArrayList<SongID>();
		    File songFile;
		    //String dbPath = DatabaseHandler.getDBPath();
		    //String dbName = DatabaseHandler.getDBName();
		    String fullDBName = DatabaseHandler.getDBPath() + DatabaseHandler.getDBName();
		    long fileLength, curFileRead = 0;

            Log.i(TAG, "DB file name = " + fullDBName);
            Log.i(TAG, "Backup songbook DB name = " + songFileName );
            
        
    		//Open your local db as the input stream
			songFile = new File(fullDBName);
			fileLength = songFile.length();
			FileInputStream in = null;
			try {
				in = new FileInputStream(songFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
    		//Open the output stream
    		OutputStream myOutput = null;
			try {
				myOutput = new FileOutputStream(songFileName);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	 
    		//transfer bytes from the inputfile to the outputfile

            Log.i(TAG, "Backing up DB from " + fullDBName + " to " + songFileName); 

    		byte[] buffer = new byte[1024];
    		int length;
    		try {
				while ((length = in.read(buffer)) > 0) {
					myOutput.write(buffer, 0, length);
					curFileRead += length;
					progress = (int) (curFileRead * 100.0 / fileLength);
					progressDialog.setProgress(progress);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	 
    		//Close the streams
    		try {
				myOutput.flush();
	    		myOutput.close();
	    		in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            Log.i(TAG, "Done backing up DB from " + fullDBName + " to " + songFileName); 
    		
			return null;
		}

		
		@Override
		protected void onPostExecute(Void Void) {			
			runOnUiThread(new Runnable() {
		    	@Override
		    	public void run() {
		    		if (progressDialog != null) {
		    			progressDialog.dismiss();
		    			progressDialog = null;
		    		
		    		}
				   	Intent intent = new Intent();
			        setResult(RESULT_OK, intent);
		         	finish();
		    	}
		    });
		}
		
	}
}
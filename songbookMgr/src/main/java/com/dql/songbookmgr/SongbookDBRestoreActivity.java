package com.dql.songbookmgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


import com.dql.dbutil.DatabaseHandler;
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

public class SongbookDBRestoreActivity extends Activity {
	
	static final String TAG = "SongbookDBRestoreActivity";
	private static final int REQUEST_FILENAME = 1;
	static final String appDirInSDCard = "TKSongIndex";
	static final String songIndexDBDir = "DB";
	private String curPathName = Environment.getExternalStorageDirectory().getPath() + "/" + appDirInSDCard + "/" + songIndexDBDir;
	private String curFileName;
	Context myCtx = SongbookDBRestoreActivity.this;	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	setTitle(getResources().getString(R.string.dbrestore));
        setContentView(R.layout.blank_form);
		Intent intent;
		Log.i(TAG, "db_backup Button Clicked");
	 	intent = new Intent(myCtx, FileChooser.class);
	    intent.putExtra(FileChooser.START_PATH, curPathName);
		startActivityForResult(intent, REQUEST_FILENAME);

	}
	
	
	// Listen for results.
   	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
           // See which child activity is calling us back.
    	Log.d(TAG, "onActivityResult: requestCode = " + String.valueOf(requestCode));
    	if (requestCode == REQUEST_FILENAME) {
    		if (resultCode == RESULT_OK) {
    			// start a task to copy the new db into old one
    			curPathName = data.getStringExtra(FileChooser.GET_PATH);
    			curFileName = data.getStringExtra(FileChooser.GET_FILE_NAME);

            	// make sure that the file selected has a ".db" extension
				int n = curFileName.lastIndexOf(".");
				if (n > 0) {
					String fileType = curFileName.substring(n, curFileName.length());
					if (fileType.equalsIgnoreCase(".db")) {
						final String fileName = curPathName + "/" + curFileName;
						Log.d(TAG, "onActivityResult: fileName = " + fileName);
		       			AlertDialog.Builder builder = new AlertDialog.Builder(myCtx);
	                	builder.setTitle("DB Restore Confirmation");
	                	builder.setIcon(R.drawable.ic_alert);
	                	builder.setMessage("OK to restore " + curFileName + "?");                	           	
	                	builder.setPositiveButton("Yes",
	                        new DialogInterface.OnClickListener() {
	                            public void onClick(DialogInterface dialog, int id) {
	        						performSongDBCopy(fileName);
	                            }
	                        });

	                	builder.setNegativeButton("No",
	                        new DialogInterface.OnClickListener() {
	                            public void onClick(DialogInterface dialog, int id) {
	                                dialog.cancel();
	            				   	Intent intent = new Intent();
	            			        setResult(RESULT_OK, intent);
	            		         	finish();
	                            }
	                        });
	        			// create alert dialog
	        			AlertDialog alertDialog = builder.create();
	        			// show it
	        			alertDialog.show();

					}
					else {
						cancelDBRestoreDialog(myCtx, curFileName);
					}
				}
				else {
					// not a valid file extension
					cancelDBRestoreDialog(myCtx, curFileName);
				}

            }
    		else {
    			// RESULT_CANCELED from file chooser
    			finish();
    		}
    	}
    }
   	
   	void cancelDBRestoreDialog(Context ctx, String fileName) {
		AlertDialog.Builder builder = new AlertDialog.Builder(myCtx);
        builder.setTitle("Invalid Songbook DB");
        builder.setIcon(R.drawable.ic_alert);
        builder.setMessage(curFileName + " is not a valid songbook DB!");                	           	
        builder.setPositiveButton("OK",
        new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int id) {
            	dialog.cancel();
			   	Intent intent = new Intent();
		        setResult(RESULT_OK, intent);
	         	finish();
            	}
        	});

		// create alert dialog
		AlertDialog alertDialog = builder.create();
		// show it
		alertDialog.show();		
   	}

   	private void performSongDBCopy(String songDBFileName) {

   		PerformSongDBCoypTask task = new PerformSongDBCoypTask();
	    task.execute(songDBFileName);
	}

	private class PerformSongDBCoypTask extends AsyncTask<String, Void, Void> {
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
			progressDialog.setMessage("Restoring songbook database...");
			progressDialog.setCancelable(false);
			progressDialog.show();
		}
		
		@Override
		protected Void doInBackground(String... params) {
			String songFileName = params[0];
		    // ArrayList<SongID> songList = new ArrayList<SongID>();
		    File songFile;
		    //String dbPath = DatabaseHandler.getDBPath();
		    //String dbName = DatabaseHandler.getDBName();
		    String fullDBName = DatabaseHandler.getDBPath() + DatabaseHandler.getDBName();
		    long fileLength, curFileRead = 0;
			final String TAG = "doInBackground";

			DatabaseHandler.getInstance(myCtx, SongbookCfg.getInstance().getPackageName()).close();

	        
            Log.i(TAG, "New file name = " + songFileName);
            Log.i(TAG, "New db file name = " + fullDBName);
            
        
    		//Open your local db as the input stream
			songFile = new File(songFileName);
			fileLength = songFile.length();
			FileInputStream in = null;
			try {
				in = new FileInputStream(songFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
    		//Open the empty db as the output stream
    		OutputStream myOutput = null;
			try {
				myOutput = new FileOutputStream(fullDBName);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	 
    		//transfer bytes from the inputfile to the outputfile

            Log.i(TAG, "Copypying new db from " + songFileName + " to " + fullDBName); 

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

            Log.i(TAG, "Done copying new db from " + songFileName + " to " + fullDBName);
    		
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


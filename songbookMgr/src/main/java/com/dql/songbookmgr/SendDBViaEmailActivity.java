package com.dql.songbookmgr;

import com.dql.filechooser.FileChooser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SendDBViaEmailActivity extends Activity {
	private static final int REQUEST_FILENAME = 1;
	private static final String TAG = "SendDBViaEmailActivity";
	static final String appDirInSDCard = "TKSongIndex";
	static final String songIndexDBDir = "DB";
	private String curPathName = Environment.getExternalStorageDirectory().getPath() + "/" + appDirInSDCard + "/" + songIndexDBDir;
	private Button sendEmailButton;
	private Button fileBowseButton;
	private EditText fileNameText, toRecipient, subjectText;
	private String curFileName, subject, recipient;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	//setTitle(getResources().getString(R.string.email_db));
    	setContentView(R.layout.songbook_db_email); 
        findAllViewsById();
        sendEmailButton.setOnClickListener(new OnClickListener() {           
	         @Override
	         public void onClick(View v) {

	        	//	Log.i(TAG, "Import button clicked");
	        	// import file
	        	curFileName = fileNameText.getText().toString();
                subject = subjectText.getText().toString();
                recipient = toRecipient.getText().toString();
                Log.i(TAG, "Songbook DB name = " + curFileName +" To: " + recipient + " Subject: " + subject);  
                
                //====================================================
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
                emailIntent.setType("application/*");

                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {recipient}); 
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject); 
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,"Here is the Song Book DB.\n\nEnjoy");
                Log.i(TAG, "attachment =" + Uri.parse("file://"+ curFileName));
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ curFileName));
                startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                //====================================================
                
                
			   	//Intent intent = new Intent();
		        //setResult(RESULT_OK, intent);
	         	finish();

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
	
	private void findAllViewsById() {

		fileNameText = (EditText) findViewById(R.id.SongDBName);
		fileBowseButton = (Button) findViewById(R.id.DBBrowseButton);
		sendEmailButton = (Button) findViewById(R.id.SendEmail_btn);
		toRecipient = (EditText) findViewById(R.id.recipient);	
		subjectText = (EditText) findViewById(R.id.subject);
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
	
}

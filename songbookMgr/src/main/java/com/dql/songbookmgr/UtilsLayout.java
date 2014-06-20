package com.dql.songbookmgr;


import java.io.File;

import com.dql.songbookmgr.R;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class UtilsLayout extends Fragment {

	static final String TAG = "UtilsLayout";
	static final String appDirInSDCard = "TKSongIndex";
	static final String songIndexDBDir = "DB";
	private String curPathName = Environment.getExternalStorageDirectory().getPath() + "/" + appDirInSDCard + "/" + songIndexDBDir;
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		createDirIfNotExists(appDirInSDCard);
		createDirIfNotExists(appDirInSDCard +"/"+ songIndexDBDir);
		ViewGroup root = (ViewGroup) inflater.inflate(R.layout.utilities, null);
		
        ImageButton importButton = (ImageButton) root.findViewById(R.id.import_btn);
        importButton.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		// do import
        		Intent intent = new Intent(getActivity(), SongbookImportActivity.class);
        		getActivity().startActivityForResult(intent, SongbookMgrActivity.IMPORT_SONGBOOK); 	
        	}
        });		
        TextView tv1 = (TextView) root.findViewById(R.id.textView1);
        tv1.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		// do import
        		Intent intent = new Intent(getActivity(), SongbookImportActivity.class);
        		getActivity().startActivityForResult(intent, SongbookMgrActivity.IMPORT_SONGBOOK); 	
        }
        });		       
        ImageButton dbRestoreButton = (ImageButton) root.findViewById(R.id.restore_btn);
        dbRestoreButton.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		// do restore
        		Intent intent = new Intent(getActivity(), SongbookDBRestoreActivity.class);
        		getActivity().startActivityForResult(intent, SongbookMgrActivity.RESTORE_SONGBOOK_DB); 
        	}
        });
        TextView tv2 = (TextView) root.findViewById(R.id.textView1_2);
        tv2.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		// do restore
        		Intent intent = new Intent(getActivity(), SongbookDBRestoreActivity.class);
        		getActivity().startActivityForResult(intent, SongbookMgrActivity.RESTORE_SONGBOOK_DB); 
        	}
        });	       
        ImageButton dbBackupButton = (ImageButton) root.findViewById(R.id.backup_btn);
        dbBackupButton.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		// do backup
        		Intent intent = new Intent(getActivity(), SongbookDBBackupActivity.class);
           		getActivity().startActivityForResult(intent, SongbookMgrActivity.BACKUP_SONGBOOK_DB); 
        	}
        });
        TextView tv3 = (TextView) root.findViewById(R.id.textView1_3);
        tv3.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		// do backup
        		Intent intent = new Intent(getActivity(), SongbookDBBackupActivity.class);
           		getActivity().startActivityForResult(intent, SongbookMgrActivity.BACKUP_SONGBOOK_DB); 
        	}
        });	       
        ImageButton dbEmailButton = (ImageButton) root.findViewById(R.id.email_btn);
        dbEmailButton.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		// do email
        		Intent intent = new Intent(getActivity(), SendDBViaEmailActivity.class);
        		getActivity().startActivity(intent);           	
            }
        });
        TextView tv4 = (TextView) root.findViewById(R.id.textView1_4);
        tv4.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		// do email
        		Intent intent = new Intent(getActivity(), SendDBViaEmailActivity.class);
        		getActivity().startActivity(intent);           	
            }
        });	      
        ImageButton appConfigButton = (ImageButton) root.findViewById(R.id.Configuration);
        appConfigButton.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		Intent intent = new Intent(getActivity(), SongIndexConfigActivity.class);
        		getActivity().startActivityForResult(intent, SongbookMgrActivity.SONGINDEX_CFG);
            }
        });      
        TextView tv7 = (TextView) root.findViewById(R.id.textView1_4_1);
        tv7.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		Intent intent = new Intent(getActivity(), SongIndexConfigActivity.class);
        		getActivity().startActivityForResult(intent, SongbookMgrActivity.SONGINDEX_CFG);
        	}
        });
		return root;
	}
	
    boolean createDirIfNotExists(String dir) {
   	    boolean ret = true;

   	    File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + dir);
   	    if (!file.exists()) {
   	        if (!file.mkdirs()) {
   	            Log.e(TAG, "Problem creating " + dir + " folder");
   	            ret = false;
   	        }
   	    }
   	    return ret;
   	}
}

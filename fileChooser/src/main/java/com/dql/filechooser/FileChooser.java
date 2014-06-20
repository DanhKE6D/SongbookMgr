package com.dql.filechooser;


import java.io.File;
import java.sql.Date;
import java.util.ArrayList; 
import java.util.Collections;
import java.util.List;
import java.text.DateFormat; 

import android.os.Bundle; 
import android.os.Environment;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent; 
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView; 
import android.widget.TextView;

public class FileChooser extends ListActivity {
	//static final String TAG = "FileChooser";
	//start path
	private String startPath;
	public static final String START_PATH = "START_PATH";
	public static final String FILE_NAME = "FileName";
	public static final String GET_PATH = "GetPath";
	public static final String GET_FILE_NAME = "GetFileName";

	private File currentDir;
    private FileArrayAdapter adapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
		startPath = getIntent().getStringExtra(START_PATH);
		startPath = startPath != null ? startPath : Environment.getExternalStorageDirectory().getPath() + "/";
        currentDir = new File(startPath);
        fill(currentDir); 

        /* dql */
        // long click directory return the directory name
        // ListActivity has a ListView, which you can get with:
        ListView lv = getListView();

        lv.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
             @Override
             public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                 onLongListItemClick(v,pos,id);
                 return true;
             }

        });

        /* dql */
    }
 
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
	        // start new Activity here
            //Log.i(TAG, "Back key is pressed!");
		   	Intent intent = new Intent();
	        setResult(RESULT_CANCELED, intent);
	    	finish();
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
    private void fill(File f)
    {
    	 File[] dirs = f.listFiles(); 
		 this.setTitle("Current Dir: " + f.getName());
		 List<Item> dir = new ArrayList<Item>();
		 List<Item> fls = new ArrayList<Item>();
		 try {
			 for (File ff: dirs)
			 { 
				Date lastModDate = new Date(ff.lastModified()); 
				DateFormat formater = DateFormat.getDateTimeInstance();
				String date_modify = formater.format(lastModDate);
				if (ff.isDirectory()) {
					File[] fbuf = ff.listFiles(); 
					int buf = 0;
					if (fbuf != null) { 
						buf = fbuf.length;
					} 
					else
						buf = 0; 
					String num_item = String.valueOf(buf);
					if (buf == 0)
						num_item = num_item + " item";
					else
						num_item = num_item + " items";
					
					//String formated = lastModDate.toString();
					dir.add(new Item(ff.getName(),num_item,date_modify,ff.getAbsolutePath(),"directory_icon")); 
				}
				else {
					fls.add(new Item(ff.getName(),ff.length() + " Byte", date_modify, ff.getAbsolutePath(),"file_icon"));
				}
			 }
		 } catch(Exception e) {    
			 
		 }
		 Collections.sort(dir);
		 Collections.sort(fls);
		 dir.addAll(fls);
		 if (!f.getName().equalsIgnoreCase("sdcard"))
			 dir.add(0,new Item("..","Parent Directory","",f.getParent(),"directory_up"));
		 adapter = new FileArrayAdapter(FileChooser.this,R.layout.file_view,dir);
		 this.setListAdapter(adapter); 
    }
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		Item o = adapter.getItem(position);
		if (o.getImage().equalsIgnoreCase("directory_icon")|| o.getImage().equalsIgnoreCase("directory_up")) {
			currentDir = new File(o.getPath());
			fill(currentDir);
		}
		else {
			onFileClick(o);
		}
	}
    

	private void onLongListItemClick(View v, int position,long id) {
		// TODO Auto-generated method stub
		//Toast.makeText(FileChooser.this, "onLongListItemClick works!", Toast.LENGTH_SHORT).show();
		Item o = adapter.getItem(position);
		if (o.getImage().equalsIgnoreCase("directory_icon")|| o.getImage().equalsIgnoreCase("directory_up")) {
			// directory, just return the directory name -- file name will be NULL
			currentDir = new File(o.getPath());
			String emptyFilename = null;
		   	Intent intent = new Intent();
	        intent.putExtra(GET_PATH,currentDir.toString());
	        intent.putExtra(GET_FILE_NAME,emptyFilename);
	        setResult(RESULT_OK, intent);
	        finish();			
		}
	}

    
    private void onFileClick(Item o) {
    	//Toast.makeText(this, "Folder Clicked: "+ currentDir, Toast.LENGTH_SHORT).show();
    	Intent intent = new Intent();
        intent.putExtra(GET_PATH,currentDir.toString());
        intent.putExtra(GET_FILE_NAME,o.getName());
        setResult(RESULT_OK, intent);
        finish();
    }
    
    static class FileArrayAdapter extends ArrayAdapter<Item> {

    	private Context c;
    	private int id;
    	private List<Item> items;
    	
    	public FileArrayAdapter(Context context, int textViewResourceId,
    			List<Item> objects) {
    		super(context, textViewResourceId, objects);
    		c = context;
    		id = textViewResourceId;
    		items = objects;
    	}
    	public Item getItem(int i)	 {
    		return items.get(i);
    	}
    	
    	@Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
            	LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(id, null);
	            ViewHolder holder = new ViewHolder();
	            holder.imgIcon = (ImageView) v.findViewById(R.id.fd_Icon1);
	            holder.t1 = (TextView) v.findViewById(R.id.TextView01);
	            holder.t2 = (TextView) v.findViewById(R.id.TextView02);
	            holder.t3 = (TextView) v.findViewById(R.id.TextViewDate);
	            v.setTag(holder);
            }
                   
            /* create a new view of my layout and inflate it in the row */
           	//convertView = ( RelativeLayout ) inflater.inflate( resource, null );
           		
            final Item o = items.get(position);
            if (o != null) {
            	ViewHolder holder = (ViewHolder) v.getTag();
                // Take the ImageView from layout and set the icon image
    	        String uri = "drawable/" + o.getImage();
    	        int imageResource = c.getResources().getIdentifier(uri, null, c.getPackageName());
    	        Drawable image = c.getResources().getDrawable(imageResource);
    	        holder.imgIcon.setImageDrawable(image);
                           
                if (holder.t1!=null)
                	holder.t1.setText(o.getName());
                if (holder.t2!=null)
                	holder.t2.setText(o.getData());
                if (holder.t3!=null)
                	holder.t3.setText(o.getDate());
          
           }
           return v;
       }
    
	    static class ViewHolder {
	    	ImageView imgIcon;
	        TextView t1;
	        TextView t2;
	        TextView t3;
	    }

    }

}

package com.dql.songbookmgr;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dql.dbutil.FavSongID;
import com.dql.dbutil.SongbookCfg;
import com.dql.dbutil.SongID;

public class CustomListAdapter extends ArrayAdapter<SongID> implements Filterable {

    private Context mContext;
    private int id;
    private List <SongID>items ;
    private List <SongID> origData;
    static final String TAG = "CustomListAdapter";
    static final int MAX_SINGER_NAME_LENGTH = 30;

    public CustomListAdapter(Context context, int textViewResourceId , List<SongID> list ) {
        super(context, textViewResourceId, list);           
        mContext = context;
        id = textViewResourceId;
        items = list;
        origData = list;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Filter getFilter() {
        Filter myFilter =  new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence cs) {
                FilterResults results = new FilterResults();
                //If there's nothing to filter on, return the original data for your list
                if (cs == null || cs.length() == 0) {
                    results.values = origData;
                    results.count = origData.size();
                }
                else  {
                    ArrayList<SongID> filterResultsData = new ArrayList<SongID>();

                    for(SongID s : origData) {
                        //In this loop, you'll filter through originalData and compare each item to charSequence.
                        //If you find a match, add it to your new ArrayList
                        //I'm not sure how you're going to do comparison, so you'll need to fill out this conditional
                        //if (data.getName().toUpperCase().contains(cs.toString().toUpperCase()) ||
                        //    data.getSinger().toUpperCase().contains(cs.toString().toUpperCase())) {
                        //    Log.i(TAG, "data.getName() = " + data.getName() +
                        //            ", data.getSinger() = " + data.getSinger() + ", cs = " + cs.toString());
                        if (TextUtil.flattenToAscii(s.getName()).toUpperCase().contains(cs.toString().toUpperCase())) {
                            Log.i(TAG, "Matched Name: s.getName() = " + s.getName() + ", cs = " + cs.toString());
                            filterResultsData.add(s);
                        }
                        if (TextUtil.flattenToAscii(s.getSinger()).toUpperCase().contains(cs.toString().toUpperCase())) {
                            Log.i(TAG, "Matched Singer: s.getSinger() = " + s.getSinger() + ", cs = " + cs.toString());
                            filterResultsData.add(s);
                        }
                    }
                    results.values = filterResultsData;
                    results.count = filterResultsData.size();
                    Log.i(TAG, "filterResultsData.size() = " + filterResultsData.size());
                }
                Log.i(TAG, "results.count = " + results.count);
                return results;
            }

            @Override
            protected void publishResults(CharSequence cs, FilterResults filterResults) {
                if (filterResults.count == 0) {
                    notifyDataSetInvalidated();
                }
                else {
                    items = (ArrayList<SongID>) filterResults.values;
                    notifyDataSetChanged();
                }
            }
        };
        return myFilter;
    }

    public static void shrinkTextToFit(float availableWidth, TextView textView,
                                       float startingTextSize, float minimumTextSize) {

        CharSequence text = textView.getText();
        float textSize = startingTextSize;
        textView.setTextSize(startingTextSize);
        while (text != (TextUtils.ellipsize(text, textView.getPaint(),
                availableWidth, TextUtils.TruncateAt.END))) {
            textSize -= 1;
            if (textSize < minimumTextSize) {
                break;
            } else {
                textView.setTextSize(textSize);
            }
        }
    }

    @Override
    public View getView(int position, View v, ViewGroup parent)
    {
        View mView = v ;
    	float fontSize = SongbookCfg.getInstance().getDisplayTextSize();
        float fontSizeSinger = SongbookCfg.getInstance().getSingerDisplayTextSize();
        boolean longFmtSongID = SongbookCfg.getInstance().longFormatSongID();
        int backgroundColor;

        if (mView == null){
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //LayoutInflater vi = LayoutInflater.from(mContext);
            mView = vi.inflate(id, null);
            ViewHolder holder = new ViewHolder();
            holder.songID = (TextView) mView.findViewById(R.id.songID);
            holder.songName = (TextView) mView.findViewById(R.id.songName);
            holder.singer = (TextView) mView.findViewById(R.id.singer);
            holder.layout = (LinearLayout) mView.findViewById(R.id.custom_song_id);
            mView.setTag(holder);

        }

        if (items.get(position) != null ) {

            ViewHolder holder = (ViewHolder) mView.getTag();

            int backgroundColorforIDField = Color.parseColor(App.getContext().getResources().getString(R.color.app_background));
            holder.songID.setTextSize(fontSizeSinger);
            int c = Color.BLACK;

            if (items.get(position).isSongSelected()) {
                backgroundColor = Color.parseColor(App.getContext().getResources().getString(R.color.fav_selected_bg));
                backgroundColorforIDField = backgroundColor;
                holder.layout.setBackgroundColor(backgroundColor);
            }
            else {
                backgroundColor = Color.parseColor(App.getContext().getResources().getString(R.color.app_background));
                holder.layout.setBackgroundColor(backgroundColor);

            }

            holder.songID.setBackgroundColor(backgroundColorforIDField);
            holder.songID.setTextColor(c);
            if (longFmtSongID)
                holder.songID.setText(String.format("%05d", items.get(position).getID()));
            else
        	    holder.songID.setText(String.format("%04d", items.get(position).getID()));
            //holder.songID.setTypeface(null,Typeface.BOLD);


            holder.songName.setBackgroundColor(backgroundColor);
        	holder.songName.setTextSize(fontSize);
        	holder.songName.setTextColor(c);
        	holder.songName.setTypeface(null,Typeface.BOLD);
        	holder.songName.setText(items.get(position).getName());

            holder.singer.setBackgroundColor(backgroundColor);
            holder.singer.setTextSize(fontSizeSinger);
            holder.singer.setTextColor(c);
            if (items.get(position).getSinger().length() > MAX_SINGER_NAME_LENGTH) {
                holder.singer.setText( items.get(position).getSinger().substring(0, MAX_SINGER_NAME_LENGTH));
            }
            else
                holder.singer.setText(items.get(position).getSinger());
            // would like to fit the singer names into the available bound of this box
            //int viewWidth = holder.singer.getWidth();
            //int viewHeight = holder.singer.getHeight();
            //shrinkTextToFit((float) viewWidth, holder.singer, (float) fontSize, 8.0f);


	    }

        return mView;
    }

    static class ViewHolder {
        TextView songID;
        TextView songName;
        TextView singer;
        LinearLayout layout;
    }


}


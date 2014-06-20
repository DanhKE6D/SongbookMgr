package com.dql.songbookmgr;

/**
 * Created by dql on 12/30/13.
 */
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dql.dbutil.SongID;
import com.dql.dbutil.SongbookCfg;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    static final int MAX_SINGER_NAME_LENGTH = 30;
    private Context context;
    private List<String> listDataHeader; // header titles
    // child data in format of header title, child title
    private HashMap<String, List<SongID>> listDataChild;

    public ExpandableListAdapter(Context context, List<String> listDataHeader,
                                 HashMap<String, List<SongID>> listChildData) {
        this.context = context;
        this.listDataHeader = listDataHeader;
        this.listDataChild = listChildData;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this.listDataChild.get(this.listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View mView, ViewGroup parent) {

        final SongID s = (SongID) getChild(groupPosition, childPosition);
        float fontSize = SongbookCfg.getInstance().getDisplayTextSize();
        float fontSizeSinger = SongbookCfg.getInstance().getSingerDisplayTextSize();
        boolean longFmtSongID = SongbookCfg.getInstance().longFormatSongID();
        int backgroundColor;
        if (mView == null) {
            LayoutInflater vi = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = vi.inflate(R.layout.custom_song_list, null);
            ViewHolder holder = new ViewHolder();
            holder.songID = (TextView) mView.findViewById(R.id.songID);
            holder.songName = (TextView) mView.findViewById(R.id.songName);
            holder.singer = (TextView) mView.findViewById(R.id.singer);
            holder.layout = (LinearLayout) mView.findViewById(R.id.custom_song_id);
            mView.setTag(holder);

        }

        //TextView txtListChild = (TextView) convertView
        //        .findViewById(R.id.lblListItem);

        //txtListChild.setText(childText);
        if (s != null) {
            ViewHolder holder = (ViewHolder) mView.getTag();

            int backgroundColorforIDField = Color.parseColor(App.getContext().getResources().getString(R.color.app_background));
            holder.songID.setTextSize(fontSizeSinger);
            int c = Color.BLACK;

            if (s.isSongSelected()) {
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
                holder.songID.setText(String.format("%05d", s.getID()));
            else
                holder.songID.setText(String.format("%04d", s.getID()));
            //holder.songID.setTypeface(null,Typeface.BOLD);


            holder.songName.setBackgroundColor(backgroundColor);
            holder.songName.setTextSize(fontSize);
            holder.songName.setTextColor(c);
            holder.songName.setTypeface(null,Typeface.BOLD);
            holder.songName.setText(s.getName());

            holder.singer.setBackgroundColor(backgroundColor);
            holder.singer.setTextSize(fontSizeSinger);
            holder.singer.setTextColor(c);
            if (s.getSinger().length() > MAX_SINGER_NAME_LENGTH) {
                holder.singer.setText(s.getSinger().substring(0, MAX_SINGER_NAME_LENGTH));
            }
            else
                holder.singer.setText(s.getSinger());
            // would like to fit the singer names into the available bound of this box
            //int viewWidth = holder.singer.getWidth();
            //int viewHeight = holder.singer.getHeight();
            //shrinkTextToFit((float) viewWidth, holder.singer, (float) fontSize, 8.0f);


        }


        return mView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.listDataChild.get(this.listDataHeader.get(groupPosition))
                .size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        // see if we can display the first few chars of the first child if the list > 0
        if (getChildrenCount(groupPosition) > 0) {
            headerTitle = ((SongID) getChild(groupPosition, 0)).getName();
        }
        headerTitle =  headerTitle + " (" + getChildrenCount(groupPosition) + ")";
        if (convertView == null) {
            LayoutInflater inflator = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflator.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        // set it to 4 extra points larger??
        lblListHeader.setTextSize(SongbookCfg.getInstance().getDisplayTextSize() + 4.0f);
        lblListHeader.setTypeface(null, Typeface.BOLD|Typeface.ITALIC);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    static class ViewHolder {
        TextView songID;
        TextView songName;
        TextView singer;
        LinearLayout layout;
    }
}


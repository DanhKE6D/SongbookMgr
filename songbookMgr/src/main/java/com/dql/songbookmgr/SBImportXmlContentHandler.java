package com.dql.songbookmgr;

import android.util.Log;

import com.dql.dbutil.SongID;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

/**
 * Created by dql on 11/19/13.
 */
// Create an songbook catalog in XML format like below
//   	<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
//   	<tkSongbookCatalog root_dir = "xxxx">
//			<song id="1">
//				<dvd>dvd_name</dvd>
//				<singer>singer_name</singer>
//				<song_name>song_name</song_name>
//				<mrl>some_mrl</mrl>
//			</song>
//			<song id="2">
//				<dvd>dvd_name</dvd>
//				<singer>singer_name</singer>
//				<song_name>song_name</song_name>
//				<mrl>some_mrl</mrl>
//				</song>
//			.
//			.
//			.
//   </tkSongbookCatalog>

public class SBImportXmlContentHandler extends DefaultHandler {
    final String TAG = "SBImportXmlContentHandler";
    static final String ROOT_ELEM       = "tkSongbookCatalog";
    static final String SONG_ELEM       = "song";
    static final String SONG_ID         = "id";
    static final String SINGER_ELEM     = "singer";
    static final String SONG_NAME_ELEM  = "song_name";

    private StringBuffer buffer = new StringBuffer();
    ArrayList<SongID> mrlList = new ArrayList<SongID>();
    String song_name, singer_name, dvd_name, mrl;
    boolean seeSinger, seeSongName;
    int id = 1;

    @Override
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts) throws SAXException {

        buffer.setLength(0);
        if (localName.equals(ROOT_ELEM)) {
            mrlList.clear();
        }
        if (localName.equals(SONG_ELEM)) {
            seeSinger = seeSongName = false;
            String idValue = atts.getValue(SONG_ID);
            id = Integer.valueOf(idValue);
        }
        else if (localName.equals(SINGER_ELEM)) {
            seeSinger = true;
        }
        else if (localName.equals(SONG_NAME_ELEM)) {
            seeSongName = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)throws SAXException {

        if (localName.equals(SONG_ELEM)) {
            // end of song
            //Log.i(TAG, "id: " + id + " DVD: " + dvd_name + " song: " + song_name + " singer: " + singer_name);
            SongID songInfo = new SongID(id, song_name, singer_name);
            mrlList.add(songInfo);
        }
        if (localName.equals(ROOT_ELEM)) {
            // see if we can get the media root directory
        }
        else if (localName.equals(SONG_ID)) {
            // don't care
            // song.setID(Integer.parseInt( buffer.toString()) );
        }

        else if (localName.equals(SINGER_ELEM)) {
            if (seeSinger)
                singer_name = buffer.toString();
        }
        else if (localName.equals(SONG_NAME_ELEM)) {
            if (seeSinger)
                song_name = buffer.toString();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        buffer.append(ch, start, length);
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {

    }

    public ArrayList<SongID> retrieveFullMrlList() {
        return this.mrlList;
    }

}
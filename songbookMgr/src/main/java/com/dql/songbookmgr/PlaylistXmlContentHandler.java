package com.dql.songbookmgr;

import com.dql.dbutil.SongID;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.ArrayList;

/**
 * Created by dql on 11/19/13.
 */
// Parse an songbook playlist in XML format like below
//	<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
//	<PlaylistHistory>
//		<song>111</song>
//		<song>112</song>
//		<song>113</song>
//		..
//		..
//</PlaylistHistory>

public class PlaylistXmlContentHandler extends DefaultHandler {
    final String TAG = "PlaylistXmlContentHandler";
    static final String ROOT_ELEM       = "PlaylistHistory";
    static final String SONG_ELEM       = "song";


    private StringBuffer buffer = new StringBuffer();
    ArrayList<String> mrlList = new ArrayList<String>();
    String song_id;
    boolean seeSongID;

    @Override
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts) throws SAXException {

        buffer.setLength(0);
        if (localName.equals(ROOT_ELEM)) {
            mrlList.clear();
        }
        if (localName.equals(SONG_ELEM)) {
            seeSongID = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)throws SAXException {

        if (localName.equals(SONG_ELEM)) {
            // end of song
            song_id = buffer.toString();
            mrlList.add(song_id);
        }
        if (localName.equals(ROOT_ELEM)) {
            // see if we can get the media root directory
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

    public ArrayList<String> retrieveFullMrlList() {
        return this.mrlList;
    }
}

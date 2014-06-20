package com.dql.songbookmgr;

import com.dql.dbutil.SongID;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by dql on 11/19/13.
 */
public class XmlParser {

    public enum XmlInputType {
        XMLFile, XMLString
    };
    private XMLReader initializeReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // create a parser
        SAXParser parser = factory.newSAXParser();
        // create the reader (scanner)
        XMLReader xmlreader = parser.getXMLReader();
        return xmlreader;
    }

    private static String convertToFileURL(String filename) {
        String path = new File(filename).getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "file:" + path;
    }

    public ArrayList<SongID> parseSongbookXmlStream(String fileName) {

        try {
            XMLReader xmlreader = initializeReader();
            SBImportXmlContentHandler sbHandler = new SBImportXmlContentHandler();
            // assign our handler
            xmlreader.setContentHandler(sbHandler);
            // perform the synchronous parse
            xmlreader.parse(convertToFileURL(fileName));
            return sbHandler.retrieveFullMrlList();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public ArrayList<SongID> parseSongbookXmlStream(XmlInputType type, String source) {
        try {
            XMLReader xmlreader = initializeReader();
            SBImportXmlContentHandler sbHandler = new SBImportXmlContentHandler();
            // assign our handler
            xmlreader.setContentHandler(sbHandler);
            // perform the synchronous parse
            if (type == XmlInputType.XMLFile)
                xmlreader.parse(convertToFileURL(source));
            else if (type == XmlInputType.XMLString)
                xmlreader.parse(new InputSource(new StringReader(source)));
            else
                return null;
            return sbHandler.retrieveFullMrlList();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<String> parsePlaylistXmlStream(String source) {
        try {
            XMLReader xmlreader = initializeReader();
            PlaylistXmlContentHandler sbHandler = new PlaylistXmlContentHandler();
            // assign our handler
            xmlreader.setContentHandler(sbHandler);
            xmlreader.parse(new InputSource(new StringReader(source)));
            return sbHandler.retrieveFullMrlList();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

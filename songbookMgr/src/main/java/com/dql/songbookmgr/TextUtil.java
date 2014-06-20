package com.dql.songbookmgr;

import java.text.Normalizer;

/**
 * Created by dql on 5/25/14.
 */
public class TextUtil {
    // remove diacritics -- accent from the string
    // also want to change � -> D and d -> d
    public static String flattenToAscii(String string) {
        //System.out.println("in: flattenToAscii: source: " + string.length() + ":" + string);
        StringBuilder out = new StringBuilder();
        char ddCharUpperCase = 'Đ';
        char ddCharLowerCase = 'đ';
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        for (int i = 0, n = string.length(); i < n; ++i) {
            char c = string.charAt(i);
            if (c <= '\u007F')
                out.append(c);
            else if (c == ddCharUpperCase)
                out.append( 'D');
            else if (c == ddCharLowerCase)
                out.append( 'd');
        }
        String newString = out.toString();
        //System.out.println("out: flattenToAscii: source: " + newString.length() + ":" + newString);
        return newString;
    }
}

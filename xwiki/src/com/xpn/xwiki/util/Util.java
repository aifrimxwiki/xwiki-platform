/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 24 nov. 2003
 * Time: 17:04:20
 */
package com.xpn.xwiki.util;

import org.apache.commons.lang.StringUtils;
import org.apache.oro.text.*;
import org.apache.oro.text.perl.*;
import org.apache.oro.text.regex.*;

import java.util.Hashtable;
import java.io.*;

import com.xpn.xwiki.render.WikiSubstitution;


public class Util {

    private static PatternCache patterns = new PatternCacheLRU(20);
    private Perl5Matcher matcher = new Perl5Matcher();
    private Perl5Util p5util = new Perl5Util(getPatterns());

    public String substitute(String pattern, String text) {
        return getP5util().substitute(pattern, text);
    }

    public boolean match(String pattern, String text) {
        return getP5util().match(pattern, text);
    }

    public boolean matched() {
     return (getP5util().getMatch()!=null);
    }

    public String substitute(String pattern, String substitution, String text) {
        return (new WikiSubstitution(this, substitution)).substitute(text);
    }

    public Perl5Matcher getMatcher() {
        return matcher;
    }

    public void setMatcher(Perl5Matcher matcher) {
        this.matcher = matcher;
    }

    public Perl5Util getP5util() {
        return p5util;
    }

    public void setP5util(Perl5Util p5util) {
        this.p5util = p5util;
    }


    public static String cleanValue(String value) {
      value = StringUtils.replace(value,"\r\r\n", "%_N_%");
      value = StringUtils.replace(value,"\r\n", "%_N_%");
      value = StringUtils.replace(value,"\n\r", "%_N_%");
      value = StringUtils.replace(value,"\r", "\n");
      value = StringUtils.replace(value,"\n", "%_N_%");
      value = StringUtils.replace(value,"\"", "%_Q_%");
      return value;
    }

    public static String restoreValue(String value) {
      value = StringUtils.replace(value,"%_N_%", "\n");
      value = StringUtils.replace(value,"%_Q_%", "\"");
      return value;
    }

    /*
       Treats lines of format name="value1" name2="value2"...
    */
    public static Hashtable keyValueToHashtable(String keyvalue) throws IOException {
        Hashtable hash = new Hashtable();
        StreamTokenizer st = new StreamTokenizer(new BufferedReader(new StringReader(keyvalue)));
        st.resetSyntax();
        st.quoteChar('"');
        st.wordChars('a','z');
        st.wordChars('A','Z');
        // st.wordChars(' ',' ');
        st.whitespaceChars(' ',' ');
        st.whitespaceChars('=','=');
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            String key = st.sval;
            st.nextToken();
            String value = (st.sval!=null) ? st.sval : "";
            hash.put(key,restoreValue(value));
        }
        return hash;
    }

    public static PatternCache getPatterns() {
        return patterns;
    }

    public static void setPatterns(PatternCache patterns) {
        Util.patterns = patterns;
    }

}

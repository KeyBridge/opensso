package com.sun.identity.admin;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class Functions {

    public static String indexOf(List l, Object o) {
        return Integer.toString(l.indexOf(o));
    }

    public static String truncate(String s, int length) {
        if (length >= s.length()) {
            return s;
        }
        return s.substring(0, length - 1) + "...";
    }

    public static int size(Collection c) {
        if (c == null) {
            return 0;
        }
        return c.size();
    }

    public static int length(Object[] oa) {
        return oa.length;
    }

    public static int length(String s) {
        return s.length();
    }

    public static String scrape(String url) {
        try {
            Scraper s = new Scraper(url);
            String result = s.scrape();
            return result;
        } catch (IOException ioe) {
            Resources r = new Resources();
            return r.getString("scrapeError", url, ioe);
        }
    }


}

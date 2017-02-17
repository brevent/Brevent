package me.piebridge.brevent.server;

import android.text.TextUtils;

/**
 * Created by thom on 16/1/28.
 */
public class StringUtils {

    private static final String EMPTY = "";

    private StringUtils() {

    }

    public static String substring(String line, String start, String end) {
        int s;
        if (!TextUtils.isEmpty(start)) {
            s = line.indexOf(start);
            if (s < 0) {
                return EMPTY;
            }
            s += start.length();
        } else {
            s = 0;
        }
        if (TextUtils.isEmpty(end)) {
            return line.substring(s);
        }
        int e = line.indexOf(end, s);
        if (e < 0) {
            if (isBlank(end)) {
                return line.substring(s);
            } else {
                return EMPTY;
            }
        }
        return line.substring(s, e);
    }

    public static boolean isDigitsOnly(String s) {
        return !TextUtils.isEmpty(s) && TextUtils.isDigitsOnly(s.trim());
    }

    public static boolean isBlank(String s) {
        return s == null || TextUtils.isEmpty(s.trim());
    }

}

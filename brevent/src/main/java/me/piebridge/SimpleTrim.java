package me.piebridge;

import android.support.annotation.NonNull;

import java.util.regex.Pattern;

/**
 * Created by thom on 2018/2/3.
 */

public class SimpleTrim {

    // whitespace excluding \r\n\t\v\f and ' '(\u0020)
    private static final String WHITESPACE = "\u0085\u00a0\u1680" +
            "\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a" +
            "\u2028\u2029\u202f\u205f\u3000";

    private static final char[] WHITESPACE_ARRAY = WHITESPACE.toCharArray();

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[" + WHITESPACE + "]",
            Pattern.DOTALL);

    private static boolean isWhiteSpace(char s) {
        if (s <= ' ') {
            return true;
        }
        for (char c : WHITESPACE_ARRAY) {
            if (c == s) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static CharSequence trim(@NonNull CharSequence cs) {
        int last = cs.length() - 1;
        int start = 0;
        int end = last;
        while (start <= end && isWhiteSpace(cs.charAt(start))) {
            start++;
        }
        while (end >= start && isWhiteSpace(cs.charAt(end))) {
            --end;
        }
        if (start != 0 || end != last) {
            return cs.subSequence(start, end + 1);
        }
        return cs;
    }

    public static String removeWhiteSpace(@NonNull String s) {
        return WHITESPACE_PATTERN.matcher(s).replaceAll("");
    }

}

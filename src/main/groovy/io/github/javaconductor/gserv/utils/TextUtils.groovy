package io.github.javaconductor.gserv.utils

/**
 * Created by lcollins on 11/9/2014.
 */
class TextUtils {

    static def stripParenthesis(s) {
        def ss = s.trim()
        if (ss.startsWith('(') && ss.endsWith(')')) {
            return ss.substring(1, ss.length() - 1)
        }
        return ss
    }

    static def stripBackTicks(s) {
        def ss = s.trim()
        if (ss.startsWith('`') && ss.endsWith('`')) {
            return ss.substring(1, ss.length() - 1)
        }
        return ss
    }

    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
        }
        return false;
    }

    public static boolean isNumber(String str) {
        try {
            new BigDecimal(str)
            return true;
        } catch (Throwable nfe) {
        }
        return false;
    }

}
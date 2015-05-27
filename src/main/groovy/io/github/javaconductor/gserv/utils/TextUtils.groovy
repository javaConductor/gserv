/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 - 2015 Lee Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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

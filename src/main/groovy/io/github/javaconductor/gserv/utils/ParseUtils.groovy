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
 * Created by lcollins on 11/21/2014.
 */
class ParseUtils {

    def parsePath(path, elements = []) {
        if (!path)
            return elements
        if (path.startsWith('?'))
            return elements + path;
        def p = path
        if (p.startsWith('/'))
            p = p.substring(1)

        def el = parseElement(p)
        def element = el[0]
        def restOfString = el[1]

        elements << element
        parsePath(restOfString, elements)
    }

    def parseElement(path) {
        def p = path;
        def element = ""
        if (p.startsWith(':')) { // its a variable
            p = p.substring(1) // skip past the ':'
            element += ':'
            // get the name of the variable
            def name = p.takeWhile { it != ':' && it != '/' && it != '?' }
            element += name

            // determine whether it contains a type
            if (p.length() >= name.length() + 1 && p[name.length()] == ':') {// its a type
                if (p.length() > name.length() && p[name.length() + 1] == '`') {//reg ex
                    p = p.substring(name.length() + 1);
                    def reg = parseRegEx(p);
                    element = ":" + name + ":" + reg;
                } else {// not reg-ex its a type
                    p = p.substring(name.length() + 1);
                    def typeName = p.takeWhile { it != '/' && it != '?' }
                    element = ':' + name + ':' + typeName
                }
            }// its a type
//            return [element, path.substring(element.length())]
        } else {/// Not a Variable
            element = p.takeWhile { it != '/' && it != '?' }
        }
        return [element, path.substring(element.length())]
    }


    def parseSurrounded(String text, String startCh, String stopCh) {
        if (!text.startsWith(startCh))
            throw new IllegalArgumentException("Bad start char: ${text.charAt(0)}")
        def t = text.substring(1)
        def s = t.takeWhile { it != stopCh }
        startCh + s + stopCh
    }

    def parseRegEx(String text) {
        parseSurrounded(text, '`', '`')
    }

}

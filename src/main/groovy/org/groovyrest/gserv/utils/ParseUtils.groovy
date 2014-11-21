package org.groovyrest.gserv.utils

/**
 * Created by lcollins on 11/21/2014.
 */
class ParseUtils {

    def parsePath(path, elements = []) {
        if (!path || path.startsWith('?'))
            return elements
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
        if (p.startsWith(':')) {
            p = p.substring(1)
            element += ':'
            def name = p.takeWhile { it != ':' && it != '/' && it != '?' }
            element += name
            if (p.length() >= name.length() + 1 && p[name.length()] == ':') {// its a type
                if (p.length() > name.length() && p[name.length() + 1] == '`') {//reg ex
                    def reg = parseRegEx(p.substring(name.length() + 1));
                    element = ":" + name + ":" + reg;
                }
            }
            return [element, path.substring(element.length())]
        } else {
            element = p.takeWhile { it != '/' && it != '?' }
            return [element, path.substring(element.length())]
        }
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

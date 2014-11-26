package org.groovyrest.gserv.utils

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
                }else{// not reg-ex its a type
                    p = p.substring(name.length() + 1);
                    def typeName  = p.takeWhile { it != '/' && it != '?' }
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

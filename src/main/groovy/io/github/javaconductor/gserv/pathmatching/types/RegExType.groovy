package io.github.javaconductor.gserv.pathmatching.types

import java.util.regex.Pattern

/**
 * Created by lcollins on 1/28/2015.
 */
class RegExType extends PathElementType {
    String regEx

    RegExType(String regEx) {
        name = "RegEx"
        this.regEx = regEx
    }

    boolean validate(String s) {
        Pattern p = new Pattern(regEx, 0)
        p.matcher(s).matches()
    }

    Object toType(String s) {
        s
    }
}

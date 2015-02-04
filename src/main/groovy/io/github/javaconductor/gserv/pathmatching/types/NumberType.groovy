package io.github.javaconductor.gserv.pathmatching.types

import java.util.regex.Pattern

import static io.github.javaconductor.gserv.utils.TextUtils.isNumber

/**
 * Created by lcollins on 1/28/2015.
 */
class NumberType extends PathElementType {

    NumberType() {
        name = "Number"
    }

    boolean validate(String s) {
        isNumber(s)
    }

    Object toType(String s) {
        Double.parseDouble(s)
    }
}

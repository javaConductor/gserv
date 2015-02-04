package io.github.javaconductor.gserv.pathmatching.types

import static io.github.javaconductor.gserv.utils.TextUtils.isInteger

/**
 * Created by lcollins on 1/28/2015.
 */
class IntegerType extends PathElementType {

    IntegerType() {
        name = "Integer"
    }

    boolean validate(String s) {
        isInteger(s)
    }

    Object toType(String s) {
        Integer.parseInt(s)
    }
}

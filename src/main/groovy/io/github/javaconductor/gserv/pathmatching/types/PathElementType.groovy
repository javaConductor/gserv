package io.github.javaconductor.gserv.pathmatching.types

/**
 * Created by lcollins on 1/28/2015.
 */
abstract class PathElementType {
    abstract boolean validate(String s);

    abstract Object toType(String s);
    String name
}

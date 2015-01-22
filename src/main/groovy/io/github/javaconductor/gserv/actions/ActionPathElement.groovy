package io.github.javaconductor.gserv.actions

/**
 * Represents one element of the URL path - either fixed string or Variable
 */
class ActionPathElement {
    private def _pathSegment, _isVar, _elementType

    def ActionPathElement(String pathElement, boolean isVar, elementType = null) {
        _pathSegment = pathElement
        _isVar = isVar
        _elementType = elementType
    }

    def type() {
        _elementType
    }

    String text() { _pathSegment }

    boolean isVariable() { _isVar }

    String variableName() { _isVar ? _pathSegment.toString().substring(1) : "" }

    String toString() {
        return text()
    }
}

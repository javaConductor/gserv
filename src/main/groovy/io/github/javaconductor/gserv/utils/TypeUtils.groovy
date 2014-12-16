package io.github.javaconductor.gserv.utils

import java.util.regex.Pattern

/**
 * Misc Utils for Pattern matching
 */
trait TypeUtils {

    def numberType = [
            name    : "Number",
            validate: { s ->
                TextUtils.isNumber(s)
            },
            toType  : { s -> Double.parseDouble(s) }
    ]
    def integerType = [
            name    : "Integer",
            validate: { s ->
                TextUtils.isInteger(s)
            },
            toType  : { s -> Integer.parseInt(s) }
    ]

    def hasType(pathElement) {
        //:name:Number
        getType(pathElement) != null
    }

    def getType(pathElement) {
        //:name:Number
        // only look at variables
        if (!pathElement.startsWith(':')) {
            return null;
        }
        def parts = pathElement.split(":")
        parts = parts.findAll { it }// remove nulls
        if (parts.size() < 2)
            return null
        else
            return createType(parts[1])
    }

    def valueAsType(elementType, value) {
        return createType(elementType)?.toType(value) ?: value
    }

    def createRegExType(regEx) {
        def regExType = [
                name    : "RegEx",
                validate: { s ->
                    Pattern p = new Pattern(regEx, 0)
                    p.matcher(s).matches()
                },
                toType  : { s -> (s) }
        ]
        regExType
    }


    def createType(elementType) {
        switch (elementType) {
            case "Number":
            case "Integer":
//            case "List":
                createKnownType(elementType)
                break;
            default:
                def regEx = TextUtils.stripBackTicks(elementType)
                regEx ? createRegExType(regEx) : null
                break;
        }
    }
}

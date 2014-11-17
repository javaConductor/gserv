/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Lee Collins
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

package org.groovyrest.gserv

import org.groovyrest.gserv.filters.Filter
import org.groovyrest.gserv.filters.FilterType
import java.util.regex.Pattern

import static org.groovyrest.gserv.utils.TextUtils.*;

/**
 * Misc Utils for Pattern matching
 */
class Utils {

    static def removeRoute(List routeList, Route route) {
        routeList.findAll { rte ->
            !routesMatchEqual(rte, route)
        }

    }

    static def routesMatchEqual(Route a, Route b) {
        if (a.method() != b.method())
            return false;
        def matches = elementsMatchEqual(a.pathElements(), b.pathElements());
        if (!matches)
            return false;
        queriesMatchEqual(a.queryPattern(), b.queryPattern())
    }

    static def elementsMatchEqual(List<RoutePathElement> a, List<RoutePathElement> b) {

        if ((a.size() != b.size())) return false;
        if (a.size() == 0)
            return true;
        def aElement = a.head();
        def bElement = b.head();

        def matches = elementsMatchEqual(aElement, bElement);
        return matches && elementsMatchEqual(a.tail(), b.tail());
    }

    static def elementsMatchEqual(RoutePathElement aElement, RoutePathElement bElement) {

        if (aElement.isVariable() != bElement.isVariable())
            return false;
        if (!aElement.isVariable() && (aElement.text() != bElement.text()))
            return false;

        return true;
    }

    static def queriesMatchEqual(RouteQuery aQry, RouteQuery bQry) {
        def aKeys = aQry.queryKeys()
        def bKeys = bQry.queryKeys()
        if (aKeys.size() != bKeys.size()) return false;

        for (int x = 0; x > aKeys.size(); ++x) {
            if (aKeys[x] != bKeys[x])
                return false;
        }
        return true;
    }

    static def isValuePattern(name) {
        //returns true if name is matching pattern with ‘:’
        !(isMatchingPattern(name) || isDataOnlyPattern(name))
    }

    static def isMatchingPattern(name) {
        //returns true if name is matching pattern with ‘:’
        (name?.startsWith(':'))
    }

    static def isDataOnlyPattern(name) {
        //  returns true if name  will not be used for Route matching but is data-only.
        // Qry params with this designation will passed to the HTTP Method handler
        //  should start with ‘?:'
        (name?.startsWith('?:'))
    }

    static def queryStringToMap(qry) {
        def m = [:]
        if (qry) {
            def queries = qry.split("&")
            queries.each { pair ->
                if (pair) {
                    def kv = pair.split("=")
                    m[(kv[0])] = (kv.size() > 1 ? kv[1] : true)
                }
            }
        }
        m
    }

    static def hasType(pathElement) {
        //:name:Number
        getType(pathElement) != null
    }

    static def getType(pathElement) {
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

    static def createType(elementType) {
        switch (elementType) {
            case "Number":
            case "Integer":
//            case "List":
                createKnownType(elementType)
                break;
            default:
                def regEx = stripBackTicks(elementType)
                createRegExType(regEx)
                break;
        }
    }

    static def createKnownType(elementType) {

        def numberType = [
                name    : "Number",
                validate: { s ->
                    isNumber(s)
                },
                toType  : { s -> Double.parseDouble(s) }
        ]
        def integerType = [
                name    : "Integer",
                validate: { s ->
                    isInteger(s)
                },
                toType  : { s -> Integer.parseInt(s) }
        ]
//        def listType = [
//                name: "List",
//                validate: { ->  isInteger(it) },
//                toType: { -> Integer.parseInt(it)}
//        ]

        switch (elementType) {
            case "Number":
                return numberType
            case "Integer":
                return integerType
            default:
                return null
        }

    }

    static def createRegExType(regEx) {
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

    static def extractElement(pathElement) {
        def parts = pathElement.split(':')
        ":${parts[1]}"
    }
}

/**
 * Factory for Routes and Filters
 */
class RouteFactory {
    static methodCount = ["GET": 0, "PUT": 0, "POST": 0, "DELETE": 0]

    static def createURLPattern(method, uri, clozure) {
        createURLPattern(method, uri, [:], clozure)
    }

    static def createURLPattern(method, uri, options, clozure) {
        createURLPattern("$method-${++methodCount[method.toUpperCase()]}", method, uri, options, clozure)
    }

    static def createURLPattern(name, method, uri, options, clozure) {
        /// Example: /Thing/:id/?page=:pg&chapter=:chpt
        def u2 = new java.net.URI(uri)
        def paths = u2.path.split("/")

        //def paths = uri.split("/\\?")
        paths = paths.findAll { p -> p }
        //println "paths -> "+paths.toString()

        def pathPatterns = paths.collect { t ->
            def type
            def element = t
            if (Utils.hasType(t)) {
                type = Utils.getType(t)
                element = Utils.extractElement(t)
            }
            new RoutePathElement(element, Utils.isMatchingPattern(element), type)
        }
        def qryPattern
        def qry = uri.split('\\?', 2)
        if (qry.size() > 1) { // no query string
            qryPattern = new RouteQuery(qry[1])
        } else {
            qryPattern = new RouteQuery("")
        }
//        def qryPattern = new RouteQuery(u2.query)
        (name) ? new Route(name, method, pathPatterns, qryPattern, options ?: [passPathParams: true], clozure)
                : new Route(method, pathPatterns, qryPattern, options ?: [passPathParams: true], clozure)
    }

    static def createFilterURLPattern(name, method, uri, options, clozure) {
        /// Example: /Thing/:id/?page=:pg&chapter=:chpt
        if (!uri || uri == '/')
            uri = '/**'
        def u2 = new java.net.URI(uri)
        def paths = u2.path.split("/")
        paths = paths.findAll { p -> p }
        //println "paths -> "+paths.toString()
        def pathPatterns = paths.collect { t ->
            new RoutePathElement(t, Utils.isMatchingPattern(t))
        }
        def qryPattern = new RouteQuery(u2.query)
        new Filter(name, method, pathPatterns, qryPattern, options ?: [passPathParams: true], clozure)
    }

    static def createAfterFilterURLPattern(String name, method, uri, options, int order = 5, Closure clozure) {
        def ret = createFilterURLPattern(name, method, uri, options, clozure)
        ret.filterType = FilterType.After
        ret.order = order
        ret
    }

    static def createBeforeFilterURLPattern(String name, method, uri, options, int order = 5, Closure clozure) {
        def ret = createFilterURLPattern(name, method, uri, options, clozure)
        ret.filterType = FilterType.Before
        ret.order = order
        ret
    }
}

/**
 * Represents a URI/HttpMethod/Behavior Combination.  The encapsulation of a resource.
 */
class Route {

    private def _urlPatterns
    private def _queryPattern
    private def _handler, _method
    private def _options
    String name

    String toString() {
        return "$_method(/" + _urlPatterns.join("/") + ")"
    }

    def Route(method, urlPatterns, queryPattern, clHandler) {
        this(method, urlPatterns, queryPattern, [:], clHandler)
    }

    def Route(name, method, urlPatterns, RouteQuery queryPattern, Map options, clHandler) {
        this(method, urlPatterns, queryPattern, options, clHandler)
        this.name = name
    }

    def Route(method, urlPatterns, RouteQuery queryPattern, Map options, clHandler) {
        _queryPattern = queryPattern
        _urlPatterns = urlPatterns
        _handler = clHandler
        _method = method
        _options = options
    }

    //returns Closure passed to method function
    def requestHandler() {
        _handler
    }

    def method() { _method }

    Map options() { _options }
    //returns RoutePathElement representing const or var
    def path(idx) {
        (idx >= 0 && idx < _urlPatterns.size()) ? _urlPatterns[idx] : null
    }

    //returns number of elements in path
    def pathSize() {
        _urlPatterns.size()
    }

    //returns list of elements in path
    def pathElements() {
        (_urlPatterns as List).asImmutable()
    }

    //returns number of query values
    def queryPatternSize() {
        _queryPattern.size()
    }

    RouteQuery queryPattern() {
        _queryPattern
    }
}

/**
 * Represents the Query portion of a URI.  Needed when using the queryString as part of the pattern match.
 */
class RouteQuery {

    // we need to string - in order
    // which are the keys to the queryValues that are mapped

    private def _queryString = ""
    private def _matchQueryKeys = []/// list of the keys that have data
    private def _queryMap = [:]/// Map
    private def _dataQueryKeys = []

    def RouteQuery(qString) {
        if (!qString) {
            return
        }
        _queryString = qString
        def queries = _queryString.split("&")
        //println "queries -> " + queries.toString()
        _matchQueryKeys = queries.inject([]) { acc, qry ->
            def kv = qry.split("=")
            /// only record the ones that are used for matching:
            ///     -   nonOptional Query Params
            ///         and
            ///     -   fixed Query Param values
            /// the rest are NOT used in matching
            (Utils.isMatchingPattern(kv[1]) || Utils.isValuePattern(kv[1])) ?
                    (acc + [kv[0]]) : acc
        }

        _dataQueryKeys = queries.inject(_matchQueryKeys) { acc, qry ->
            def kv = qry.split("=")
            /// only record the ones that are used for data mapping:
            ///     -   optional Query Params
            (Utils.isDataOnlyPattern(kv[1])) ? (acc + [kv[0]]) : acc
        }
        _queryMap = Utils.queryStringToMap(_queryString)
    }

    String text() {
        _queryString
    }

    String toString() {
        return text()
    }

    Map<String, String> queryMap() {
        _queryMap
    }

    def queryMatchMap() {
        queryKeys().inject([:]) { dataMap, dk -> dataMap + ["$dk": _queryMap[dk]] }
    }

    def queryDataMap() {
        (queryKeys() + dataKeys()).inject([:]) { dataMap, dk -> dataMap + ["$dk": _queryMap[dk]] }
        //queryMap()
    }

    def queryKeys() {
        _matchQueryKeys
    }

    def dataKeys() {
        _dataQueryKeys
    }

}

/**
 * Represents one element of the URL path - either fixed string or Variable
 */
class RoutePathElement {
    private def _pathSegment, _isVar, _elementType

    def RoutePathElement(pathElement, isVar, elementType = null) {
        _pathSegment = pathElement
        _isVar = isVar
        _elementType = elementType
    }

    def type() {
        _elementType
    }

    def text() { _pathSegment }

    def isVariable() { _isVar }

    def variableName() { _isVar ? _pathSegment.toString().substring(1) : "" }

    //  def isRegEx() { _isRegEx }

    String toString() {
        return text()
    }
}

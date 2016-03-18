/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2014-2016 Lee Collins
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package io.github.javaconductor.gserv.factory

import io.github.javaconductor.gserv.actions.ActionPathElement
import io.github.javaconductor.gserv.actions.ActionPathQuery
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.filters.Filter
import io.github.javaconductor.gserv.filters.FilterType
import io.github.javaconductor.gserv.pathmatching.PathMatchingUtils
import io.github.javaconductor.gserv.utils.ParseUtils

/**
 * Factory for Routes and Filters
 */
class ResourceActionFactory {
    static methodCount = ["GET": 0, "PUT": 0, "POST": 0, "DELETE": 0]

    static def createAction(method, uri, clozure) {
        createAction(method, uri, [:], clozure)
    }

    static def createAction(method, uri, options, clozure) {
        createAction("$method-${++methodCount[method.toUpperCase()]}", method, uri, options, clozure)
    }

    static def createAction(name, method, uri, options, clozure) {
        def qry
        /// Example: /Thing/:id:Number/?page=:pg&chapter=:chpt
        List paths = new ParseUtils().parsePath(uri);//u2.path.split("/")
        if (!paths.empty && paths?.last()?.startsWith("?")) {
            qry = paths.last(); qry = qry.substring(1)
            paths = paths.subList(0, paths.size() - 1)

        }
        // remove empty entries - encode each element of the path
        paths = paths.findAll { p -> p }//.collect { URLEncoder.encode(it, "UTF-8") }

        def pathPatterns = paths.collect { t ->
            def type
            def element = t
            if (PathMatchingUtils.hasType(t)) {
                type = PathMatchingUtils.getType(t)
                element = PathMatchingUtils.extractElement(t)
            }
            new ActionPathElement(element, PathMatchingUtils.isMatchingPattern(element), type)
        }
        def qryPattern = qry ? new ActionPathQuery(qry) : new ActionPathQuery("")
        (name) ? new ResourceAction(name, method, pathPatterns, qryPattern, options ?: [passPathParams: true], clozure)
                : new ResourceAction(method, pathPatterns, qryPattern, options ?: [passPathParams: true], clozure)
    }

    static def createFilter(name, method, uri, options, clozure) {
        /// Example: /Thing/:id/?page=:pg&chapter=:chpt
        if (!uri || uri == '/')
            uri = '/**'
        def u2 = new java.net.URI(uri)
        def paths = u2.path.split("/")
        paths = paths.findAll { p -> p }
        def pathPatterns = paths.collect { t ->
            new ActionPathElement(t, PathMatchingUtils.isMatchingPattern(t))
        }
        def qryPattern = new ActionPathQuery(u2.query)
        new Filter(name, method, pathPatterns, qryPattern, options ?: [passPathParams: true], clozure)
    }

    static def createAfterFilter(String name, method, uri, options, int order = 5, Closure clozure) {
        def ret = createFilter(name, method, uri, options, clozure)
        ret.filterType = FilterType.After
        ret.order = order
        ret
    }

    static def createBeforeFilter(String name, method, uri, options, int order = 5, Closure clozure) {
        def ret = createFilter(name, method, uri, options, clozure)
        ret.filterType = FilterType.Before
        ret.order = order
        ret
    }
}

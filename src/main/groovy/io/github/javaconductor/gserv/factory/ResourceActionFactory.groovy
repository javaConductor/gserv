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
        def paths = new ParseUtils().parsePath(uri);//u2.path.split("/")
        if (paths?.last()?.startsWith("?")) {
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

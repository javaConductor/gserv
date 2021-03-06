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

package io.github.javaconductor.gserv.requesthandler

import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.delegates.FilterDelegate
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.filters.Filter
import io.github.javaconductor.gserv.filters.FilterType
import io.github.javaconductor.gserv.pathmatching.PathMatchingUtils

/**
 * Created by lcollins on 1/11/2015.
 */
@Slf4j
class FilterRunner {
    def _serverConfig

    def FilterRunner(GServConfig config) {
        _serverConfig = config
    }

/**
 *
 * @param filters
 * @param c
 * @return
 */
    RequestContext runFilters(List<Filter> filters, RequestContext c) {
        RequestContext context = c
        for (Filter f : filters) {
            log.trace("FilterRunner() running $f on $context")
            try {
                context = runFilter(f, context) ?: context
                if (context.isClosed()) {
                    log.trace("FilterRunner() filter $f closed context")
                    return context
                }
            } catch (Throwable e) {
                log.warn("FilterError: ${ex.message}")
                EventManager.instance().publish(Events.FilterError, [
                        requestId : rc.id(),
                        error     : e.message,
                        statusCode: 500,
                        filterName: f.name,
                        method    : rc.requestMethod,
                        uri       : rc.requestURI,
                        headers   : rc.requestHeaders])
            }
//            log.trace("FilterRunner() running $f on $context")
        }
//        log.trace("FilterRunner() running $f on $context")
        context
    }

/**
 *
 * @param rc
 * @param bytes
 * @return
 */
    byte[] runAfterFilters(RequestContext rc, byte[] bytes) {
        def requestId = rc.id()
        log.trace "Running ppList for ${rc.getRequestURI().path} - req #${requestId}"
        /// run the PostProcess List
        def ppList = rc.getAttribute(GServ.contextAttributes.postProcessList).toList()
        ppList.each { fn ->
            try {
                log.trace("Processing Filter Fn: ${fn.delegate.$this.name}  ")
                bytes = fn(rc, bytes) ?: bytes
            } catch (Throwable ex) {
                log.warn("FilterError: ${ex.message}")
                EventManager.instance().publish(Events.FilterError, [
                        requestId : requestId,
                        error     : ex.message,
                        statusCode: 500,
                        filterName: fn.delegate.$this.name,
                        method    : rc.requestMethod,
                        uri       : rc.requestURI,
                        headers   : rc.requestHeaders])
            }
        }
        bytes
    }

    /**
     *
     * @param theFilter
     * @param requestContext
     * @return
     */
    RequestContext runFilter(Filter theFilter, RequestContext requestContext) {

        switch (theFilter.filterType) {
            case FilterType.After:
                //////////////////////////////////////////////////////////////////////
                /// We will set the delegate for the closure before we put in the list
                //////////////////////////////////////////////////////////////////////

                def fn = theFilter.requestHandler()
                fn.delegate = prepareDelegate(theFilter, requestContext)

                /// add after closure to PostProcessList
                def ppList = requestContext.getAttribute(GServ.contextAttributes.postProcessList) ?: []
                log.trace "FilterRunner: Scheduling after filter ${theFilter.name} for req #${requestContext.id()} "
                ppList << fn
                requestContext.setAttribute(GServ.contextAttributes.postProcessList, ppList)
                return requestContext

            case FilterType.Before:
            case FilterType.Normal:
                try {
                    // run the filter
                    // replace with new context if returned
                    requestContext = process(theFilter, requestContext) ?: requestContext
                } catch (Throwable ex) {
                    //TODO must handle this better -
                    log.trace("FilterRunner: $theFilter threw exception: ${ex.message}.", ex)
                    log.error("FilterRunner: $theFilter threw exception: ${ex.message}.")
                }
                return requestContext
        }//switch
    }

    /**
     *
     * @param theFilter
     * @param context
     * @return
     */
    RequestContext process(Filter theFilter, RequestContext context) {
        Closure cl = theFilter.requestHandler()//_handler
        FilterDelegate dgt = prepareDelegate(theFilter, context)
        def errorHandlingWrapper = { clozure, List argList ->
            try {
                return clozure(context, argList)
            } catch (Throwable e) {
                log.error("FilterRunner filter: [$theFilter] error: ${e.message}", e)
                e.printStackTrace(System.err)
                dgt.error(500, e.message)
                context
            }
        }

        //TODO Should be a cleaner way to do this
        cl.delegate = dgt
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        /// Pass the route variables to the behavior if option is set
        /// Else there will be no args
        List args = prepareArguments(context, theFilter)

        /// Execute the behavior for this filter
        def ret = errorHandlingWrapper(cl, args)
        ret
    }

/**
 *
 * @param filter
 * @param requestContext
 * @param templateEngineName
 * @return
 */
    private def prepareDelegate(filter, requestContext, templateEngineName = "") {
        new FilterDelegate(filter, requestContext, _serverConfig, templateEngineName)
    }

    /**
     * Set up the args to be sent to a FilterBehaviorClosure
     *
     * @param context
     * @param theFilter
     * @return
     */
    private def prepareArguments(context, theFilter) {
        def uri = context.requestURI
        def args = []
        def method = theFilter.method()
        if (method == "PUT" || method == "POST") {
            // add the data before the other args
            // data is a byte[]
            args.add(context.getRequestBody())
        }

        def pathElements = uri.path.split("/").findAll { !(!it) }
        //// loop thru the Patterns getting the corresponding uri path elements
        for (int i = 0; i != theFilter.pathSize(); ++i) {
            def p = theFilter.path(i)
            def pathElement = pathElements[i];
            if (p.isVariable()) {
                args.add(pathElement)
            }
        }

        def qmap = PathMatchingUtils.queryStringToMap(uri.query)
        theFilter.queryPattern().queryKeys().each { k ->
            args.add(qmap[k])
        }
        args
    }

}

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

package io.github.javaconductor.gserv.filters

import com.sun.net.httpserver.Filter
import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.FilterMatcher
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.GServFactory
import io.github.javaconductor.gserv.Utils
import io.github.javaconductor.gserv.delegates.FilterDelegate
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 * User: javaConductor
 * Date: 1/7/14
 * Time: 2:04 AM
 * This is a Proxy from gserv filters to the com.sun....Filter
 */
@Log4j
class FilterProxy extends Filter {

    private List<io.github.javaconductor.gserv.filters.Filter> _filterList
    private def _templateEngineName
    def m = new FilterMatcher();
    private def _serverConfig
    def eventMgr = EventManager.instance()
    static def requestId = 1

    def FilterProxy(filterList, serverConfig) {
        _filterList = filterList
        _templateEngineName = serverConfig.templateEngineName()
        _serverConfig = serverConfig
    }

    /**
     *
     * @param httpExchange
     * @param chain
     * @throws IOException
     */
    @Override
    void doFilter(HttpExchange httpExchange, com.sun.net.httpserver.Filter.Chain chain) throws IOException {

        /// here we do the get the ResourceContect from the Exchange
        RequestContext requestContext = httpExchange.getAttribute(GServ.contextAttributes.requestContext)

        // if none create it
        if ( !requestContext ){
            requestContext = new GServFactory().createRequestContext(httpExchange)
            httpExchange.setAttribute(GServ.contextAttributes.requestContext, requestContext)
            requestContext.setAttribute(GServ.contextAttributes.receivedMS, "" + new Date().time)
            synchronized (requestId){
                requestContext.setAttribute(GServ.contextAttributes.requestId, requestId++)
            }
        }

        def currentRequestId =  requestContext.getAttribute(GServ.contextAttributes.requestId)
        println requestContext.properties

        def theFilter = m.matchAction ( _filterList, requestContext )
        if (theFilter) {
            eventMgr.publish(Events.RequestMatchedFilter, [
                    filter       : theFilter.name ?: "-",
                    requestTimeMs: requestContext.getAttribute(GServ.contextAttributes.receivedMS),
                    action        : theFilter.toString(),
                    path         : requestContext.requestURI.path,
                    method       : requestContext.requestMethod
            ]
            );
            //  if route is required  for this filter and not matched  return immediately
            if (theFilter.options()[FilterOptions.MatchedActionsOnly]) {
                if (!_serverConfig.requestMatched(requestContext)) {
                    log.trace "FilterProxy: Request($currentRequestId) : Not matched. Calling chain." 
                    chain.doFilter(httpExchange);
                    log.trace "FilterProxy: Request($currentRequestId) :Not matched. Called chain:" 
                    return
                }
            }

            switch (theFilter.filterType) {
                case FilterType.After:
                    //////////////////////////////////////////////////////////////////////
                    /// We will set the delegate for the closure before we put in the list
                    //////////////////////////////////////////////////////////////////////

                    def fn = theFilter.requestHandler()
                    //TODO the After filter will have a potentially OLD exchange injected but the current one passed in
                    fn.delegate = prepareDelegate(theFilter, requestContext, chain)

                    /// add after closure to PostProcessList
                    def ppList = requestContext.getAttribute(GServ.contextAttributes.postProcessList) ?: []
                    log.trace "FilterProxy: Scheduling after filter ${theFilter.name} for req @${requestContext.getAttribute(GServ.contextAttributes.receivedMS)} "
                    ppList << fn
                    //println "adding $fn to ppList"
                    requestContext.setAttribute( GServ.contextAttributes.postProcessList, ppList )
                    log.trace "FilterProxy: Request($currentRequestId) :After Filter. Calling chain."
                    chain.doFilter( httpExchange )
                    log.trace "FilterProxy: Request($currentRequestId) :After Filter. Called chain."
                    requestContext
                    break

                case FilterType.Before:
                case FilterType.Normal:
                    try {
                        // run the filter
                        // replace with new one
                        requestContext = filter(requestContext, chain, theFilter) ?: requestContext
                    } catch (Throwable ex) {
                        log.trace( "Filter $theFilter threw exception: ${ex.message}.", ex )
                        log.error( "Filter $theFilter threw exception: ${ex.message}." )
                        eventMgr.publish(Events.FilterError, [
                                filter       : (theFilter.name ?: "-"),
                                requestTimeMs: requestContext.getAttribute(GServ.contextAttributes.receivedMS),
                                message      : ex.message,
                                path         : requestContext.requestURI.path,
                                method       : requestContext.requestMethod])
                    } finally {
                        eventMgr.publish(Events.FilterComplete, [filter       : theFilter.name ?: "-",
                                                                 requestTimeMs: requestContext.getAttribute(GServ.contextAttributes.receivedMS),
                                                                 path         : requestContext.requestURI.path,
                                                                 method       : requestContext.requestMethod])
                    }
                    requestContext
                    break;
            }//switch
        } else {
                    log.trace "FilterProxy: Request($currentRequestId) :Filter Not Matched. Calling chain."
            chain.doFilter(httpExchange)
                    log.trace "FilterProxy: Request($currentRequestId) :Filter Not Matched. Called chain."
            httpExchange
        }
    }//fn

    @Override
    String description() {
        return "FilterProxy. Proxies requests from com.sun.net.http.Filter to matched gServ Filter"
    }

    private def prepareDelegate(filter, requestContext, chain, templateEngineName = _templateEngineName) {
        new FilterDelegate(filter, requestContext, chain, _serverConfig, templateEngineName)
    }

    /**
     * Set up the args to be sent to a FilterBehaviorClosure
     *
     * @param exchange
     * @param chain
     * @param theFilter
     * @return
     */
    private def prepareArguments(context, chain, theFilter) {
        def uri = context.requestURI
        def args = []
        def method = theFilter.method()
        if (FilterType.Normal == theFilter.filterType && chain)
            args.add(chain)
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

        def qmap = Utils.queryStringToMap(uri.query)
        theFilter.queryPattern().queryKeys().each { k ->
            args.add(qmap[k])
        }
        args
    }

    /**
     * This function executes the FilterBehaviorClosure
     *
     * @param context
     * @param chain
     * @param theFilter
     * @return
     */
    def filter(RequestContext context, chain, theFilter) {
        Closure cl = theFilter.requestHandler()//_handler
        def options = theFilter.options()
        FilterDelegate dgt = prepareDelegate(theFilter, context, chain)
        def errorHandlingWrapper = { clozure, List argList ->
            try {
                eventMgr.publish(Events.FilterProcessing, [filter       : theFilter.name ?: "-",
                                                           requestTimeMs: context.getAttribute(GServ.contextAttributes.receivedMS),
                                                           action        : theFilter.toString(), path: context.requestURI.path, args: argList])
                return clozure(*argList)
            } catch (Throwable e) {
                log.error("FilterProxy error: ${e.message}", e)
                e.printStackTrace(System.err)
                eventMgr.publish(Events.FilterError, [filter       : theFilter.name ?: "-",
                                                      requestTimeMs: context.getAttribute(GServ.contextAttributes.receivedMS),
                                                      action        : theFilter.toString(), path: context.requestURI.path, args: argList, error: e.message])
                dgt.error(500, e.message)
                context
            }
            finally {
                // println "FilterProxy.filter(): closureWrapper: DONE!"
            }
        }
        //TODO Should be a cleaner way to do this
        cl.delegate = dgt
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        /// Pass the route variables to the behavior if option is set
        /// Else there will be no args
        List  args = options[FilterOptions.PassRouteParams] ? prepareArguments(context, chain, theFilter) : [];
        /// Execute the behavior for this filter
        def ret = errorHandlingWrapper(cl, args)
        ret
    }
}

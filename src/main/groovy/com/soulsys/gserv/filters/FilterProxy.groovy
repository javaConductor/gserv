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

package com.soulsys.gserv.filters

import com.soulsys.gserv.FilterMatcher
import com.soulsys.gserv.GServ
import com.soulsys.gserv.Utils
import com.soulsys.gserv.delegates.FilterDelegate
import com.soulsys.gserv.events.EventManager
import com.soulsys.gserv.events.Events
import com.soulsys.gserv.wrapper.ExchangeWrapper
import com.sun.net.httpserver.Filter
import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Log4j

/**
 * User: javaConductor
 * Date: 1/7/14
 * Time: 2:04 AM
 * This is a Proxy from gserv filters to the com.sun....Filter
 */
@Log4j
class FilterProxy extends Filter {

    private def _filterList
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

    @Override
    void doFilter(HttpExchange httpExchange, com.sun.net.httpserver.Filter.Chain chain) throws IOException {
        /// Check the request against the filter's  route.
        synchronized (requestId) {
            if (!httpExchange.getAttribute(GServ.exchangeAttributes.requestId))
                httpExchange.setAttribute(GServ.exchangeAttributes.requestId, requestId++)
        }

        def theFilter = m.matchRoute(_filterList, httpExchange)
        if (theFilter) {
            eventMgr.publish(Events.RequestMatchedFilter, [
                    filter   : theFilter.name ?: "-",
                    requestId: httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                    route    : theFilter.toString(),
                    path     : httpExchange.requestURI.path,
                    method   : httpExchange.requestMethod
            ]
            );
            //  if route is required and not matched  return immediately
            if (theFilter.options()[FilterOptions.MatchedRoutesOnly]) {
                if (!_serverConfig.requestMatched(httpExchange)) {
                    chain.doFilter(httpExchange);
                    return
                }
            }

            if (!(httpExchange instanceof ExchangeWrapper)) {
                //println "Wrapping the Exchange - filter: ${theFilter.name}"
                // wrap the Exchange
                httpExchange = new ExchangeWrapper(httpExchange)
                httpExchange.setAttribute(GServ.exchangeAttributes.postProcessList, [])
                def baos = new FilterByteArrayOutputStream({ _this ->
                    println "Running ppList for ${httpExchange.requestURI.path} - req #${httpExchange.getAttribute(GServ.exchangeAttributes.requestId)}"
                    /// run the PostProcess List
                    def ppList = httpExchange.getAttribute(GServ.exchangeAttributes.postProcessList).toList()

                    def s = _this
                    /// get bytes from BAOS
                    def bytes = s.toByteArray()

                    ppList.each { fn ->
                        try {
                            println("Processing Filter Fn: ${fn.delegate.$this.name}  ")
                            bytes = fn(httpExchange, bytes) ?: bytes
                        } catch (Throwable ex) {
                            System.err.println("FilterError: ${ex.message}")
                            ex.printStackTrace(System.err)
//                            eventMgr.publish(Events.FilterError, [filter   : theFilter.name ?: "-",
//                                                                  requestId: httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
//                                                                  path     : httpExchange.requestURI.path,
//                                                                  method   : httpExchange.requestMethod])
                        }
                    }

                    httpExchange.writeIt(bytes)
                })
                httpExchange.setStreams(httpExchange.requestBody, baos)
            } else {
                //println "The Exchange is already wrapped - filter: ${theFilter.name}"
            }

            switch (theFilter.filterType) {
                case FilterType.After:
                    //////////////////////////////////////////////////////////////////////
                    /// We will set the delegate for the closure before we put in the list
                    //////////////////////////////////////////////////////////////////////
                    def fn = theFilter.requestHandler()
                    fn.delegate = prepareDelegate(theFilter, httpExchange, chain)

                    /// add after closure to PostProcessList
                    def ppList = httpExchange.getAttribute(GServ.exchangeAttributes.postProcessList) ?: []
                    log.debug  "FilterProxy: adding ${theFilter.name} afterClosure to ppList"
                    ppList << fn
                    //println "adding $fn to ppList"
                    httpExchange.setAttribute(GServ.exchangeAttributes.postProcessList, ppList)
                    chain.doFilter(httpExchange)
                    httpExchange
                    break

                case FilterType.Before:
                case FilterType.Normal:
                    try {
                        // run the filter
                        // replace with new one
                        httpExchange = filter(httpExchange, chain, theFilter) ?: httpExchange
                    } catch (Throwable ex) {
                        ex.printStackTrace(System.err)
                        eventMgr.publish(Events.FilterError, [
                                filter   : (theFilter.name ?: "-"),
                                requestId: httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                                message  : ex.message,
                                path     : httpExchange.requestURI.path,
                                method   : httpExchange.requestMethod])
                    } finally {
                        eventMgr.publish(Events.FilterComplete, [filter   : theFilter.name ?: "-",
                                                                 requestId: httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                                                                 path     : httpExchange.requestURI.path,
                                                                 method   : httpExchange.requestMethod])
                    }
                    httpExchange
                    break;
            }//switch
        } else {
            chain.doFilter(httpExchange)
            httpExchange
        }
    }

    @Override
    String description() {
        return "FilterProxy. Proxies requests from com.sun.net.http.Filter to matched gServ Filter"
    }

    private def prepareDelegate(filter, httpExchange, chain, templateEngineName = _templateEngineName) {
        new FilterDelegate(filter, httpExchange, chain, _serverConfig, templateEngineName)
    }

    /**
     * Set up the args to be sent to a FilterBehaviorClosure
     *
     * @param exchange
     * @param chain
     * @param theFilter
     * @return
     */
    private def prepareArguments(exchange, chain, theFilter) {
        def uri = exchange.requestURI
        def args = []
        def method = theFilter.method()
        if (FilterType.Normal == theFilter.filterType && chain)
            args.add(chain)
        if (method == "PUT" || method == "POST") {
            // add the data before the other args
            // data is a byte[]
            args.add(exchange.getRequestBody())
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
     * @param exchange
     * @param chain
     * @param theFilter
     * @return
     */
    def filter(HttpExchange exchange, chain, theFilter) {
        Closure cl = theFilter.requestHandler()//_handler
        def options = theFilter.options()
        FilterDelegate dgt = prepareDelegate(theFilter, exchange, chain)
        def errorHandlingWrapper = { clozure, List argList ->
            try {
                eventMgr.publish(Events.FilterProcessing, [filter   : theFilter.name ?: "-",
                                                           requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                                                           route    : theFilter.toString(), path: exchange.requestURI.path, args: argList])
                return clozure(*argList)
            } catch (Throwable e) {
                log.debug("FilterProxy error: ${e.message}", e)
                e.printStackTrace(System.err)
                eventMgr.publish(Events.FilterError, [filter   : theFilter.name ?: "-",
                                                      requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                                                      route    : theFilter.toString(), path: exchange.requestURI.path, args: argList, error: e.message])
                dgt.error(500, e.message)
                exchange
            }
            finally {
                // println "FilterProxy.filter(): closureWrapper: DONE!"
            }
        }
        //TODO Should be a cleaner way to do this
        cl.delegate = dgt
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        List args = prepareArguments(exchange, chain, theFilter)
        /// Pass the route variables to the behavior if option is set
        /// Else there will be no args
        args = options[FilterOptions.PassRouteParams] ? args : [];
        /// Execute the behavior for this filter
        def ret = errorHandlingWrapper(cl, args)
        ret
    }
}

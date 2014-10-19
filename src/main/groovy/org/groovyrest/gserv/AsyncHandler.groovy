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

import org.groovyrest.gserv.delegates.HttpMethodDelegate
import org.groovyrest.gserv.events.EventManager
import org.groovyrest.gserv.events.Events
import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Log4j
import groovyx.gpars.actor.DynamicDispatchActor

/**
 * Created with IntelliJ IDEA.
 * User: lcollins
 * Date: 1/5/14
 * Time: 10:13 PM
 */
@Log4j
class AsyncHandler extends DynamicDispatchActor {
    EventManager _evtMgr = EventManager.instance()
    private def _staticRoots
    private def _templateEngineName
    private def _cfg
    private def _seq

    static long Seq = 0;

    def AsyncHandler(cfg) {
        this(cfg.staticRoots(), cfg.templateEngineName())
        _cfg = cfg
    }

    def AsyncHandler(staticRoots, templateEngineName) {
        _staticRoots = staticRoots
        _templateEngineName = templateEngineName
        _seq = ++Seq
    }
/** This method is called when this Actor receives a message
 *
 * @param request
 * @return void
 */
    def onMessage(request) {
        def pattern
        try {
            def exchange = request.exchange
            pattern = request.pattern
//            println "$this recieved req #${exchange.getAttribute(GServ.exchangeAttributes.requestId)} ${exchange.requestURI.path}"
            log.debug "$this received req #${exchange.getAttribute(GServ.exchangeAttributes.requestId)} ${exchange.requestURI.path}"
            process(exchange, pattern)
        } catch (Throwable e) {
            _evtMgr.publish(Events.ResourceProcessingError,
                    [requestId: request.exchange.getAttribute(GServ.exchangeAttributes.requestId),
                     path     : pattern.toString(),
                     error    : e.message])
            //          e.printStackTrace(System.err)
            log.error("AsyncHandler($_seq) req #${request.exchange.getAttribute(GServ.exchangeAttributes.requestId)}: Error processing request: ${e.message} ", e)
        }
    }

    def prepareDelegate(httpExchange, route) {
//        _cfg.delegateMgr.createHttpMethodDelegate(httpExchange, _staticRoots, _templateEngineName)
        _cfg.delegateMgr.createHttpMethodDelegate(httpExchange, route, _cfg)
    }

    def prepareArguments(uri, istream, pattern) {
        def args = []
        def method = pattern.method()
        if (method == "PUT" || method == "POST") {
            // add the data before the other args
            // data is a byte[]
            args.add(istream)
        }

        def pathElements = uri.path.split("/").findAll { !(!it) }
        //// loop thru the Patterns getting the corresponding uri path elements
        for (int i = 0; i != pattern.pathSize(); ++i) {
            def p = pattern.path(i)
            def pathElement = pathElements[i];
            if (p.isVariable()) {
                args.add(pathElement)
            }
        }

        def qmap = Utils.queryStringToMap(uri.query)
        pattern.queryPattern().queryKeys().each { k ->
            args.add(qmap[k])
        }
        //println "AsyncHandler.prepareArguments(): $args"
        // log.debug "AsyncHandler.prepareArguments(): $args"
        args
    }

    def prepareClosure(exchange, pattern) {
        def cl = pattern.requestHandler()//_handler
        HttpMethodDelegate dgt = prepareDelegate(exchange, pattern)
        cl.delegate = dgt
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl
    }

    def process(HttpExchange exchange, pattern) {
        Closure cl = prepareClosure(exchange, pattern)
        def args = prepareArguments(exchange.requestURI, exchange.requestBody, pattern)
//        println "AsyncHandler.process(): Calling errorHandlingWrapper w/ args: $args"
        ({ clozure, argList ->
            try {
                EventManager.instance().publish(Events.ResourceProcessing, [
                        requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                        uri      : exchange.requestURI.path,
                        msg      : "Resource Processing."])
                //println "AsyncHandler.process(): closureWrapper: Calling request handler w/ args: $argList"
                log.debug "Running AsyncHandler(${this._seq}) for req#${exchange.getAttribute(GServ.exchangeAttributes.requestId)} ${exchange.requestURI.path}"
                clozure(*argList)
                log.debug "AsyncHandler(${this._seq}) for req#${exchange.getAttribute(GServ.exchangeAttributes.requestId)} ${exchange.requestURI.path} - Finished."
            } catch (Throwable e) {
                EventManager.instance().publish(Events.ResourceProcessingError, [
                        requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                        uri      : exchange.requestURI.path, msg: e.message, e: e])
                cl.delegate.error(500, e.message)
                log.error "Error Running AsyncHandler(${this._seq}) for req#${exchange.getAttribute(GServ.exchangeAttributes.requestId)} ${exchange.requestURI.path} : ${e.message}", e
                exchange
            }
            finally {
                EventManager.instance().publish(Events.ResourceProcessed, [
                        requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                        uri      : exchange.requestURI.path, msg: 'Resource processing done.'])
            }
        })(cl, args);

    }

    /**
     *
     * @return
     */
    @Override
    String toString() {
        return "AsyncHandler($_seq)"
    }
}

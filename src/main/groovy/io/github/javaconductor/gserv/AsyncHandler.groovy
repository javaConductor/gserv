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

package io.github.javaconductor.gserv

import io.github.javaconductor.gserv.delegates.HttpMethodDelegate
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Log4j
import groovyx.gpars.actor.DynamicDispatchActor
import io.github.javaconductor.gserv.requesthandler.ActionRunner
import io.github.javaconductor.gserv.utils.TypeUtils

/**
 * Created with IntelliJ IDEA.
 * User: lcollins
 * Date: 1/5/14
 * Time: 10:13 PM
 */
@Log4j
class AsyncHandler extends DynamicDispatchActor implements TypeUtils {
    EventManager _evtMgr = EventManager.instance()
    private def _staticRoots
    private def _templateEngineName
    private def _cfg
    private def _seq
    ActionRunner r

    static long Seq = 0;

    def AsyncHandler(cfg) {
        this(cfg.staticRoots(), cfg.templateEngineName())
        _cfg = cfg
        r = new ActionRunner(_cfg)
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
            log.trace "$this received req #${exchange.getAttribute(GServ.exchangeAttributes.requestId)} ${exchange.requestURI.path}"
            r.process(exchange, pattern)
        } catch (Throwable e) {
            _evtMgr.publish(Events.ResourceProcessingError,
                    [requestId: request.exchange.getAttribute(GServ.exchangeAttributes.requestId),
                     path     : pattern.toString(),
                     error    : e.message])
            //          e.printStackTrace(System.err)
            log.error("AsyncHandler($_seq) req #${request.exchange.getAttribute(GServ.exchangeAttributes.requestId)}: Error processing request: ${e.message} ", e)
        }
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

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
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.PGroup
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events

/**
 * Created with IntelliJ IDEA.
 * User: lcollins
 * Date: 1/5/14
 * Time: 10:13 PM
 *
 * The AsyncHandler receives request and an action from the
 * AsyncDispatcher and processes the request by executing the action.
 */
@Slf4j
class AsyncHandler {// extends DynamicDispatchActor {//implements TypeUtils {
    EventManager _evtMgr = EventManager.instance()
    private def _cfg
    private def _seq
    ActionRunner r
    static long Seq = 0;

    def AsyncHandler(GServConfig cfg, PGroup pGroup, DataflowQueue handlerQ) {
        _cfg = cfg

        r = new ActionRunner(_cfg)
        _seq = ++Seq
        Thread.start("AsyncHandler") {
            while (true) {
                def val = handlerQ.getVal()
                log.trace("$this got message: $val : ${new Date().getTime()}")
                pGroup.execute([
                        run: {
                            handler val
                        }
                ] as Runnable)
            }
        }
        log.debug("Created $this ")
    }

    def handler = { Map request ->
        onMessage(request)
    }

    /** This method is called when this Actor receives a message
     *
     * @param request
     * @return void
     */
    def onMessage(request) {
        RequestContext context = request.requestContext
        def currentReqId = context.id()
        // log.trace "$this received req #$currentReqId: ${context.requestBody.available()} bytes from input: ${context.requestBody} "
        def action
        try {
            action = request.action
            log.trace "$this  received req #$currentReqId ${context.requestURI.path}"
            //log.trace "$this received req #$currentReqId bytes from input: ${context.requestBody.bytes.size()} "
            r.process(context, action)
        } catch (Throwable e) {
            _evtMgr.publish(Events.ResourceProcessingError,
                    [requestId: currentReqId,
                     path     : action.toString(),
                     error    : e.message])
            log.error("$this req #$currentReqId: Error processing request: ${e.message} ", e)
        }
        finally {
            log.trace "$this  processed req #$currentReqId ${context.requestURI.path}"
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

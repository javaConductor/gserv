/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 - 2015 Lee Collins
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

package io.github.javaconductor.gserv.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import groovy.util.logging.Slf4j
import groovyx.gpars.group.DefaultPGroup
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.exceptions.HttpErrorException
import io.github.javaconductor.gserv.factory.GServFactory
import io.github.javaconductor.gserv.filters.FilterOptions
import io.github.javaconductor.gserv.pathmatching.FilterMatcher
import io.github.javaconductor.gserv.requesthandler.AsyncHandler
import io.github.javaconductor.gserv.requesthandler.FilterRunner
import io.github.javaconductor.gserv.requesthandler.RequestContext
import io.github.javaconductor.gserv.utils.ActorPool

import java.util.concurrent.atomic.AtomicLong

/**
 * This is the Handler that is called for each request by the Java 1.6 HttpServer
 *
 */
@Slf4j
class GServHandler implements HttpHandler {
    private def _factory = new GServFactory();
    private def _actions
    private def _staticRoots
    private def _templateEngineName
    private def _dispatcher, _handler
    private GServConfig _cfg
    private def _nuHandler, _nuDispatcher
    Calendar cal = new GregorianCalendar();
    Long reqId = 1;
    FilterMatcher m = new FilterMatcher()
    FilterRunner filterRunner

    /**
     * Create a handler with a specific configuration
     *
     * @param cfg
     * @return
     */
    def GServHandler(GServConfig cfg) {
        _cfg = cfg;
        filterRunner = new FilterRunner(_cfg)
        this._actions = cfg.actions()
        this._staticRoots = cfg.staticRoots()
        this._templateEngineName = cfg.templateEngineName()

        _nuHandler = {
            new AsyncHandler(cfg)
        }
        _handler = _nuHandler()

        def actors = new ActorPool(1, 2, new DefaultPGroup(1), _nuHandler);
        _nuDispatcher = {
            _factory.createDispatcher(actors, _cfg);
        }
        _dispatcher = _nuDispatcher();
        _dispatcher.start()
    }

//    static Long requestId = 0L
    AtomicLong requestId = new AtomicLong(0L)
    /**
     * This method is called for each request
     * This is called after the Filters and done.
     *
     * @param httpExchange
     */
    void handle(HttpExchange httpExchange) {
        RequestContext context = new GServFactory().createRequestContext(_cfg, httpExchange)
        def r = new Long(requestId.addAndGet(1L))
        context.setAttribute(GServ.contextAttributes.requestId, r)
        log.trace("ServerHandler.handle(${httpExchange.requestURI.path}) #$r")
        def currentReqId = r

        EventManager.instance().publish(Events.RequestRecieved, [
                when     : new Date(),
                requestId: currentReqId,
                requestContext: context,
                method   : context.requestMethod,
                uri      : context.requestURI,
                headers  : context.requestHeaders])

        try {
            log.trace("ServerHandler.handle(${httpExchange.requestURI.path})  #$requestId: Finding filters... ")
            context.setAttribute(GServ.contextAttributes.serverConfig, _cfg)
            boolean matched = _cfg.requestMatched(context)
            def filters = m.matchFilters(_cfg.filters(), context)
            filters = filters.findAll { theFilter ->
                (!theFilter.options()[FilterOptions.MatchedActionsOnly]) ||
                        (theFilter.options()[FilterOptions.MatchedActionsOnly] && matched)
            }

            log.trace("ServerHandler.handle(${httpExchange.requestURI.path})  #$requestId: Running filters -> $filters")
            context = filterRunner.runFilters(filters, context)
            def t = "${(context.isClosed() ? ' Context CLOSED!' : '')}"
            log.trace("ServerHandler.handle(${httpExchange.requestURI.path})  #$requestId: After filters $t ")

            if (context.isClosed()) {
                EventManager.instance().publish(Events.RequestProcessed, [
                        requestId: currentReqId])
                return;
            }

            log.trace("ServerHandler.handle(${httpExchange.requestURI.path}) #$requestId: unharmed by filters. ")
            //        log.debug("ServerHandler.handle(${httpExchange.requestURI.path}) instream: ${httpExchange.requestBody} outstream: ${httpExchange.responseBody}")
            currentReqId = context.id()
            def start = cal.getTimeInMillis();
            _handle(context)
            EventManager.instance().publish(Events.RequestDispatched, [
                    requestId: currentReqId,
                    method   : context.requestMethod,
                    uri      : context.requestURI,
                    headers  : context.requestHeaders])
        } catch (HttpErrorException e) {
            def msg = e.message
            log.error(msg, e)
            EventManager.instance().publish(Events.RequestProcessingError, [
                    requestId: currentReqId,
                    error    : e.message,
                    statusCode : e.httpStatusCode,
                    method   : context.requestMethod,
                    uri      : context.requestURI,
                    headers  : context.requestHeaders])
            context.sendResponseHeaders(e.httpStatusCode, msg.bytes.size())
            context.responseBody.write(msg.bytes)
            context.responseBody.close()
            context.close()
        } catch (Throwable e) {
            def msg = "Error req #${currentReqId} ${e.message ?: e.class.name} "
            log.error(msg, e)
            EventManager.instance().publish(Events.RequestProcessingError, [
                    requestId: currentReqId,
                    error    : "${msg}",
                    method   : context.requestMethod,
                    uri      : context.requestURI,
                    headers  : context.requestHeaders])
            context.sendResponseHeaders(500, msg.bytes.size())
            context.responseBody.write(msg.bytes)
            context.responseBody.close()
            context.close()
        }
    }

    private void _handle(RequestContext context) {
        log.trace("ServerHandler._handle(${context})")
        //TODO be ready to stop/start the actor when it returns with IllegalState (actor cannot recv messages)
        try {
            _dispatcher << [requestContext: context]
        } catch (IllegalStateException e) {
            _dispatcher = _nuDispatcher()
            _handle(context)
        }
        log.trace("ServerHandler._handle(${context}) Sent to dispatcher.")

    }
}

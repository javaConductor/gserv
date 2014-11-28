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

package io.github.javaconductor.gserv.gserv

import io.github.javaconductor.gserv.gserv.GServ
import io.github.javaconductor.gserv.gserv.Matcher
import io.github.javaconductor.gserv.gserv.Route
import io.github.javaconductor.gserv.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.gserv.events.EventManager
import io.github.javaconductor.gserv.gserv.events.Events
import io.github.javaconductor.gserv.gserv.utils.ActorPool
import io.github.javaconductor.gserv.gserv.utils.Filename
import io.github.javaconductor.gserv.gserv.utils.MimeTypes
import io.github.javaconductor.gserv.gserv.utils.StaticFileHandler
import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Log4j
import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.ResizeablePool
import org.apache.commons.io.IOUtils

/**
 * Created with IntelliJ IDEA.
 * User: javaConductor
 * Date: 1/5/14
 * Time: 10:13 PM
 */
@Log4j
class AsyncDispatcher extends DynamicDispatchActor {
    private def _routes = []
    private def _staticRoots = []
    private def _matcher = new Matcher()
    private def _templateEngineName = ""
    private def _handler
    private def _staticFilehandler = new StaticFileHandler()
    private def _useResourceDocs
    private def _actorPool
    private GServConfig _config

    def AsyncDispatcher(actorPool, patterns, staticRoots, templateEngineName, useResourceDocs) {
        this.setParallelGroup(new DefaultPGroup(new ResizeablePool(false, 10)))
        _routes = patterns
        _staticRoots = staticRoots
        _templateEngineName = templateEngineName
        _useResourceDocs = useResourceDocs
        _actorPool = actorPool
    }

    def AsyncDispatcher(ActorPool actorPool, GServConfig config) {
        this.setParallelGroup(new DefaultPGroup(new ResizeablePool(false, 10)))
        _actorPool = actorPool
        _config = config;
    }

    def AsyncDispatcher() {

    }

    /**
     * Search Application resources when resolving request paths
     *
     * @param bUseResourceDocs
     * @return
     *
     */
    @Deprecated
    def useResourceDocs(bUseResourceDocs) {
        _config.useResourceDocs(bUseResourceDocs)
    }

    def useResourceDocs() { _useResourceDocs }

    ////// Actor Pool /////////
    /**
     * Sets the pool of actors for this dispatcher.
     *
     * @param pool
     * @return
     */
    def actorPool(pool) {
        _actorPool = pool
        this.setParallelGroup(new DefaultPGroup(_actorPool))
    }

    def actorPool() {
        _actorPool
    }

    ////// TemplateEngineName /////////
    def templateEngineName(templateName) {
        _templateEngineName = templateName
    }

    def templateEngineName() {
        _templateEngineName
    }

    ////// Routes /////////
    /**
     * Returns the routes available to this dispatcher
     *
     * @param routes
     * @return
     */
    def routes(routes) {
        _routes = routes
    }

    def routes() { _routes }

    ////// Static Roots /////////

/**
 *
 * @param staticRoots
 * @return
 */
    def staticRoots(staticRoots) {
        _staticRoots.addAll(staticRoots);
        _staticRoots = _staticRoots.unique();
    }

    def staticRoots() {
        _staticRoots
    }

    void onMessage(Map request) {
        process(request.exchange)
    }

    def evtMgr = EventManager.instance()

    def process(HttpExchange httpExchange) {

        log.trace "Processing exchange(${httpExchange.dump()})"
        Route pattern = _matcher.matchRoute(_routes, httpExchange)
        if (pattern) {
            httpExchange.setAttribute(GServ.exchangeAttributes.matchedRoute, pattern)
            evtMgr.publish(Events.RequestMatchedDynamic, [requestId: httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                                                          routePath: pattern.toString(), method: httpExchange.getRequestMethod()])
            httpExchange.setAttribute(GServ.exchangeAttributes.currentRoute, pattern)
            def actr
            try {
                //// here we use the next actor
                actr = (_actorPool.next())
                actr << [exchange: httpExchange, pattern: pattern]
            } catch (IllegalStateException e) {
                evtMgr.publish(Events.RequestProcessingError, [
                        requestId   : httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                        routePath   : pattern.toString(),
                        errorMessage: e.message,
                        method      : httpExchange.getRequestMethod()])

                if (e.message.startsWith("The actor cannot accept messages at this point.")) {
                    //TODO needs new handler
                    log.warn "Actor in bad state: replacing."
                    _actorPool.replaceActor(actr)
                    log.warn "Actor in bad state: replaced and reprocessed!"
                    process(httpExchange)
                } else {
                    evtMgr.publish(Events.RequestProcessingError, [
                            requestId   : httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                            routePath   : pattern.toString(),
                            errorMessage: e.message,
                            method      : httpExchange.getRequestMethod()])

                }
            }
            //println "AsyncDispatcher.process(): route $pattern sent to processor."
            return
        }
        log.trace "AsyncDispatcher.process(): No matching dynamic resource for: ${httpExchange.requestURI}"
        //// if its a GET then try to match it to a static resource
        //
        if (httpExchange.requestMethod == "GET") {
            InputStream istream = _staticFilehandler.resolveStaticResource(httpExchange.requestURI.path, _staticRoots, _useResourceDocs)
            if (istream) {
                //TODO test this well!!
                def mimeType = MimeTypes.getMimeType(fileExtensionFromPath(httpExchange.requestURI.path))
                //header("Content-Type", mimeType)
                httpExchange.getResponseHeaders().add("Content-Type", mimeType)
                evtMgr.publish(Events.RequestMatchedStatic, [requestId: httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                                                             routePath: httpExchange.requestURI.path, method: httpExchange.getRequestMethod()])
//                println "AsyncDispatcher.process(): Found static resource: ${httpExchange.requestURI.path}: seems to be ${istream.available()} bytes."
                httpExchange.sendResponseHeaders(200, istream.available())
                sendStream(istream, httpExchange.responseBody)
                httpExchange.responseBody.close()
                log.trace "AsyncDispatcher.process(): Static resource ${httpExchange.requestURI.path} was sent for req #${httpExchange.getAttribute(GServ.exchangeAttributes.requestId)}."
                return
            }
        }
//        println "AsyncDispatcher.process(): No matching static resource for: ${httpExchange.requestURI}"
        evtMgr.publish(Events.RequestNotMatchedStatic, [requestId: httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                                                        routePath: httpExchange.requestURI.path, method: httpExchange.getRequestMethod()])

        ////TODO Externalize this message!!!!!!!
        def msg = "No such thang: ${httpExchange.requestURI}";
        httpExchange.sendResponseHeaders(404, msg.getBytes().size())
        httpExchange.responseBody.write(msg.getBytes())
        httpExchange.responseBody.close()
        //println "AsyncDispatcher.process(): ALL done for pattern: ${httpExchange.requestURI}"
    }

    private fileExtensionFromPath(path) {
        new Filename(path, '/', '.').extension()
    }

    /**
     * Copies from inputStream to outputStream
     *
     * @param inStream
     * @param outStream
     */
    def sendStream(inStream, outStream) {
        IOUtils.copy(inStream, outStream)
    }
}


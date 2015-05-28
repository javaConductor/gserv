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

package io.github.javaconductor.gserv.requesthandler

import groovy.util.logging.Slf4j
import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.group.DefaultPGroup
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.pathmatching.Matcher
import io.github.javaconductor.gserv.utils.ActorPool
import io.github.javaconductor.gserv.utils.Filename
import io.github.javaconductor.gserv.utils.MimeTypes
import io.github.javaconductor.gserv.utils.StaticFileHandler
import org.apache.commons.io.IOUtils

/**
 * Created with IntelliJ IDEA.
 * User: javaConductor
 * Date: 1/5/14
 * Time: 10:13 PM
 */
@Slf4j
class AsyncDispatcher extends DynamicDispatchActor {
    private def _matcher = new Matcher()
    private def _handler
    private def _staticFilehandler = new StaticFileHandler()
    private def _actorPool
    private GServConfig _config

    def AsyncDispatcher(ActorPool actorPool, GServConfig config) {
        this.setParallelGroup(new DefaultPGroup(2))
        _actorPool = actorPool
        _config = config;
    }

    ////// Actor Pool /////////
    /**
     * Sets the pool of actors for this dispatcher.
     *
     * @param pool
     * @return
     */
    void actorPool(pool) {
        _actorPool = pool
        this.setParallelGroup(new DefaultPGroup(1))
    }

    ActorPool actorPool() {
        _actorPool
    }

    void onMessage(Map request) {
        log.trace "AsyncDispatcher.onMessage $request: Processing"
        process(request.requestContext)
        log.trace "AsyncDispatcher.onMessage $request: Done"
    }

    def evtMgr = EventManager.instance()

    def process(RequestContext context) {
        def currentReqId = context.id()
        //log.trace "AsyncDispatcher.process($currentReqId) "
        log.debug "AsyncDispatcher.process($currentReqId)  "
        ResourceAction action = _matcher.matchAction(_config.actions(), context)
        if (action) {
            context.setAttribute(GServ.contextAttributes.matchedAction, action)
            evtMgr.publish(Events.RequestMatchedDynamic, [requestId : currentReqId,
                                                          actionPath: action.toString(), method: context.getRequestMethod()])
            context.setAttribute(GServ.contextAttributes.currentAction, action)
            def actr
            try {
                //// here we use the next actor
                actr = (_actorPool.next())
                log.trace "Processing request(${currentReqId}) dispatching to $action"
                //log.trace "Processing request(${currentReqId}) requestContext has ${ context.requestBody } bytes to read."

                actr << [requestContext: context, action: action]
            } catch (IllegalStateException e) {
                log.trace "Error Processing request(${currentReqId}) dispatching to $action: ${e.message}", e
                evtMgr.publish(Events.RequestProcessingError, [
                        requestId   : currentReqId,
                        actionPath  : action.toString(),
                        errorMessage: e.message,
                        method      : context.requestMethod])

                if (e.message.startsWith("The actor cannot accept messages at this point.")) {
                    //TODO needs new handler
                    log.warn "Actor in bad state: replacing."
                    _actorPool.replaceActor(actr)
                    log.warn "Actor in bad state: replaced and reprocessed!"
                    process(context)
                } else {
                    evtMgr.publish(Events.RequestProcessingError, [
                            requestId   : currentReqId,
                            actionPath  : action.toString(),
                            errorMessage: e.message,
                            method      : context.requestMethod])
                }
            }
            //println "AsyncDispatcher.process(): action $action sent to processor."
            return
        }
        log.trace "AsyncDispatcher.process(): No matching dynamic resource for: ${context.requestURI}"
        //// if its a GET then try to match it to a static resource
        //
        if (context.requestMethod == "GET") {
            InputStream istream = _staticFilehandler.resolveStaticResource(context.requestURI.path, _config.staticRoots(), _config.useResourceDocs())
            if (istream) {
                //TODO test this well!!
                def mimeType = MimeTypes.getMimeType(fileExtensionFromPath(context.requestURI.path))
                //header("Content-Type", mimeType)
                context.getResponseHeaders().put("Content-Type", [mimeType])
                evtMgr.publish(Events.RequestMatchedStatic, [requestId : currentReqId,
                                                             actionPath: context.requestURI.path, method: context.getRequestMethod()])
//                println "AsyncDispatcher.process(): Found static resource: ${httpExchange.requestURI.path}: seems to be ${istream.available()} bytes."
                context.sendResponseHeaders(200, istream.available())
                sendStream(istream, context.responseBody)
                context.responseBody.close()
                context.close()
                log.trace "AsyncDispatcher.process(): Static resource ${context.requestURI.path} was sent for req #${context.id()}."
                return
            }
        }
//        println "AsyncDispatcher.process(): No matching static resource for: ${httpExchange.requestURI}"
//        evtMgr.publish(Events.RequestNotMatchedStatic, [requestId: context.getAttribute(GServ.contextAttributes.requestId),
//                                                        actionPath: context.requestURI.path, method: context.getRequestMethod()])

        ////TODO Externalize this message!!!!!!!
        def msg = "No such resource: ${context.requestURI}";
        context.sendResponseHeaders(404, msg.getBytes().size())
        context.responseBody.write(msg.getBytes())
        context.responseBody.close()
        context.close()
        //println "AsyncDispatcher.process(): ALL done for action: ${httpExchange.requestURI}"
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


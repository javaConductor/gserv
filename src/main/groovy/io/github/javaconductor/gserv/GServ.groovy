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

import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.filters.FilterMatcher
import io.github.javaconductor.gserv.filters.FilterOptions
import io.github.javaconductor.gserv.filters.FilterRunner
import io.github.javaconductor.gserv.plugins.IPlugin
import io.github.javaconductor.gserv.requesthandler.AsyncHandler
import io.github.javaconductor.gserv.requesthandler.RequestContext
import io.github.javaconductor.gserv.utils.ActorPool
import io.github.javaconductor.gserv.utils.LinkBuilder
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import groovy.util.logging.Log4j
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.ResizeablePool
import io.github.javaconductor.gserv.delegates.*

/**
 * gServ main class
 */
@Log4j
class GServ {
    def factory = new GServFactory();
    static def contextAttributes = [
            "receivedMS"     : 'g$$when',
            "serverConfig"   : 'g$$serverConfig',
            "currentAction"   : 'g$$route',
            "requestId"      : 'g$$requestId',
            "isWrapper"      : 'g$$wrapper',
            "matchedAction"   : 'g$$matchedAction',
            "postProcessList": 'g$$postProcessList',
            "requestContext": 'g$$requestContext'
    ]
    static def returnCodes = [
            Normal                  : 0,
            InstanceCompilationError: -1,
            ResourceCompilationError: -2,
            GeneralError            : -3
    ]

    def serverPlugins = new gServPlugins()// has no plugins

    /**
     * Defines the plugins to be used with this ServerInstance
     *
     * @param definitionClosure
     * @return
     */
    def plugins(Closure definitionClosure) {
        def dgt = new PluginsDelegate();
        definitionClosure = definitionClosure.rehydrate(dgt, this, this)
        definitionClosure.resolveStrategy = Closure.DELEGATE_FIRST
        definitionClosure()
        serverPlugins = definitionClosure.delegate.plugins ?: serverPlugins
        return this
    }

    /**
     *
     * Convenience
     *
     * @param basePath URL pattern prefix
     * @param definitionClosure The definition of the resource. All methods of ResourceDelegate are available.
     *
     * @return gServResource
     */
    static def Resource(basePath, Closure definitionClosure) {
        GServResource.Resource(basePath, definitionClosure)
    }

    static def Resource(basePath, ResourceObject target) {
        GServResource.Resource(basePath, target)
    }

    /**
     *
     * Defines a resource that may be added to a ServerInstance
     *
     * @param basePath URL pattern prefix
     * @param definitionClosure The definition of the resource. All methods of ResourceDelegate are available.
     *
     * @return gServResource
     */
    def resource(basePath, Closure definitionClosure) {
        Resource(basePath, definitionClosure)
    }

    /**
     *
     * Defines a ServerInstance
     *
     * @param definitionClosure The definition of the resource. All methods of ResourceDelegate are available.
     *
     * @return GServInstance
     */
    def http(Closure instanceDefinition) {
        http([:], instanceDefinition)
    }

/**
 *
 * @param configFile
 * @param instanceDefinition
 * @param https
 * @return
 */
    def http(String configFile, Closure instanceDefinition, https = false) {
        Properties p = new Properties();
        InputStream is = new FileInputStream(new File(configFile))
        p.load(is)
        is.close()
        http(p, instanceDefinition, https)
    }

    /**
     *
     * Defines a ServerInstance
     *
     * @param options Options:
     * @param definitionClosure The definition of the resource. All methods of ResourceDelegate are available.
     *
     * @return GServInstance
     */
    def http(Map options, Closure instanceDefinition, https = false) {
        def tmpActions = []
        def tmpFilters = []
        def tmpStaticRoots = []
        def tmpAuthenticator
        def tmpName
        /// create the initial config
        GServConfig cfg = factory.createGServConfig(tmpActions)
                .linkBuilder(new LinkBuilder(""))
                .delegateManager(new DelegatesMgr())
                .authenticator(tmpAuthenticator)
                .addStaticRoots(tmpStaticRoots)
                .actions(tmpActions)
                .addFilters(tmpFilters);
        /// each plugin is applied to the configuration
        cfg = serverPlugins.applyPlugins(cfg);
        if (options.https) {
            cfg.applyHttpsConfig(options.https)
        } else {
            //TODO Check for a global https setting  and apply it
        }

        /// get the delegate
        instanceDefinition.delegate = cfg.delegateMgr.createHttpDelegate();
        instanceDefinition.resolveStrategy = Closure.DELEGATE_FIRST
        // run the config closure
        instanceDefinition()
        boolean _useResourceDocs
        def templateEngineName
        def lBuilder
        (instanceDefinition.delegate).with {
            //// Gather data from the closure we just ran
            tmpName = name()
            tmpActions = actions()
            _useResourceDocs = useResourceDocs()
            tmpFilters = filters()
            tmpStaticRoots = staticRoots()
            templateEngineName = templateEngine ?: "default"
            lBuilder = linkBuilder()
        }

        /// add this info to the config

        cfg.addServerIP(options.serverIP)
        .addFilters(tmpFilters)
        .addStaticRoots(tmpStaticRoots)
        .addActions(tmpActions)
        .useResourceDocs(_useResourceDocs)
        .templateEngineName(templateEngineName)
        .name(tmpName)
        .linkBuilder(lBuilder)
        //}
        factory.createHttpInstance(cfg)
    }

    def https(Closure instanceDefinition) {
        https([:], instanceDefinition)
    }

    def https(Map options, Closure instanceDefinition) {
        http(options, instanceDefinition, true);
    }

}

/**
 * This is the Handler that is called for each request by the Java 1.6 HttpServer
 *
 */
@Log4j
class gServHandler implements HttpHandler {
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
    def gServHandler(GServConfig cfg) {
        _cfg = cfg;
        filterRunner = new FilterRunner(_cfg)
        this._actions = cfg.actions()
        this._staticRoots = cfg.staticRoots()
        this._templateEngineName = cfg.templateEngineName()

        _nuHandler = {
            new AsyncHandler(cfg)
        }
        _handler = _nuHandler()

        def actors = new ActorPool(10, 40, new DefaultPGroup(new ResizeablePool(false)), _nuHandler);
        _nuDispatcher = {
            _factory.createDispatcher(actors, _cfg );
        }
        _dispatcher = _nuDispatcher();
        _dispatcher.start()
    }

    static Long requestId = 0L
    /**
     * This method is called for each request
     * This is called after the Filters and done.
     *
     * @param httpExchange
     */
    void handle(HttpExchange httpExchange) {
        RequestContext context = new GServFactory().createRequestContext(httpExchange)
        synchronized (requestId){
            context.setAttribute(GServ.contextAttributes.requestId, ++requestId)
            log.trace("ServerHandler.handle(${httpExchange.requestURI.path}) #$requestId")
        }

        log.trace("ServerHandler.handle(${httpExchange.requestURI.path})  #$requestId: Finding filters... ")
        context.setAttribute(GServ.contextAttributes.serverConfig, _cfg)
        boolean matched = _cfg.requestMatched(context)
        def filters = m.matchFilters(_cfg.filters(),context)
        filters = filters.findAll {theFilter ->
            (!theFilter.options()[FilterOptions.MatchedActionsOnly]) ||
                    (theFilter.options()[FilterOptions.MatchedActionsOnly] && matched)
                }

        log.trace("ServerHandler.handle(${httpExchange.requestURI.path})  #$requestId: Running filters -> $filters")
        context = filterRunner.runFilters(filters, context)
        def t = "${(context.isClosed() ? ' Context CLOSED!' : '')}"
        log.trace("ServerHandler.handle(${httpExchange.requestURI.path})  #$requestId: After filters $t ")

        if (context.isClosed())
            return;

        log.trace("ServerHandler.handle(${httpExchange.requestURI.path}) #$requestId: unharmed by filters. ")
//        log.debug("ServerHandler.handle(${httpExchange.requestURI.path}) instream: ${httpExchange.requestBody} outstream: ${httpExchange.responseBody}")
        def currentReqId = context.getAttribute(GServ.contextAttributes.requestId)
        try {
            EventManager.instance().publish(Events.RequestRecieved, [
                    requestId: currentReqId,
                    method   : context.requestMethod,
                    uri      : context.requestURI,
                    headers  : context.requestHeaders])
            def start = cal.getTimeInMillis();
            _handle(context)
            EventManager.instance().publish(Events.RequestDispatched, [
                    requestId: currentReqId,
                    method   : context.requestMethod,
                    uri      : context.requestURI,
                    headers  : context.requestHeaders])
        } catch (Throwable e) {
            def msg = "Error req #${currentReqId} ${e.message} "
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

/**
 *
 * Container for the application of plugins.
 *
 */
class gServPlugins {
    def plugins = []

    def add(IPlugin p) {
        plugins.add(p)
    }

    /**
     * Apply the plugins to a ServerConfiguration
     *
     * @param serverConfig
     * @return GServConfig
     */
    def applyPlugins(GServConfig serverConfig) {
        def delegates = prepareAllDelegates(DefaultDelegates.delegates)
        serverConfig.delegateManager(new DelegatesMgr(delegates))
        /// for each plugin we add to the actions, filters, and staticRoots
        //TODO plugins MAY also contribute to the Type formatter (to)
        plugins.each {
            serverConfig.addActions(it.actions())
                    .addFilters(it.filters())
                    .addStaticRoots(it.staticRoots())
        }
        serverConfig.delegateTypeMap(delegates)
        serverConfig
    }

    /**
     * Apply each plugin to each delegate
     *
     * @param delegates
     * @return preparedDelegates
     */
    def prepareAllDelegates(delegates) {
        delegates.each { kv ->
            def delegateType = kv.key
            def delegateExpando = kv.value
            plugins.each { plugin ->
                if (!plugin) {
                    println "Skipping null plugin in list"
                } else {
                    plugin.decorateDelegate(delegateType, delegateExpando)// get the side-effect
                }
            }
        }
        delegates
    }
}

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

import org.groovyrest.gserv.configuration.GServConfig
import org.groovyrest.gserv.events.EventManager
import org.groovyrest.gserv.events.Events
import org.groovyrest.gserv.plugins.IPlugin
import org.groovyrest.gserv.utils.ActorPool
import org.groovyrest.gserv.utils.LinkBuilder
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import groovy.util.logging.Log
import groovy.util.logging.Log4j
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.ResizeablePool
import org.groovyrest.gserv.delegates.*

/**
 * gServ main class
 */
@Log
class GServ {
    def factory = new GServFactory();
    static def exchangeAttributes = [
            "receivedMS"     : 'g$$when',
            "serverConfig"   : 'g$$serverConfig',
            "currentRoute"   : 'g$$route',
            "requestId"      : 'g$$requestId',
            "isWrapper"      : 'g$$wrapper',
            "matchedRoute"   : 'g$$matchedRoute',
            "postProcessList": 'g$$postProcessList'
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
        def _patterns = []
        def _filters = []
        def _staticRoots = []
        def _authenticator

        /// create the initial config
        GServConfig cfg = factory.createGServConfig(_patterns)
                .linkBuilder(new LinkBuilder(""))
                .delegateManager(new DelegatesMgr())
                .authenticator(_authenticator)
                .addStaticRoots(_staticRoots)
                .routes(_patterns)
                .addFilters(_filters);

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
        def _useResourceDocs
        def templateEngineName
        def lBuilder
        (instanceDefinition.delegate).with {
            //// Gather data from the closure we just ran
            _patterns = patterns()
            _useResourceDocs = useResourceDocs()
            _filters = filters()
            _staticRoots = staticRoots()
            templateEngineName = templateEngine ?: "default"
            lBuilder = linkBuilder()
        }

        cfg.with {
            /// add this info to the config
            addServerIP(options.serverIP)
                    .addFilters(_filters)
                    .addStaticRoots(_staticRoots)
                    .addRoutes(_patterns)
                    .useResourceDocs(_useResourceDocs)
                    .templateEngineName(templateEngineName)

            linkBuilder(lBuilder)

        }
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
    private def _routes
    private def _staticRoots
    private def _templateEngineName
    private def _dispatcher, _handler
    private def _cfg
    private def _nuHandler, _nuDispatcher
    Calendar cal = new GregorianCalendar();
    Long reqId = 1;

    /**
     * Create a handler with a specific configuration
     *
     * @param cfg
     * @return
     */
    def gServHandler(GServConfig cfg) {
        _cfg = cfg;
        this._routes = cfg.routes()
        this._staticRoots = cfg.staticRoots()
        this._templateEngineName = cfg.templateEngineName()

        _nuHandler = {
            new AsyncHandler(cfg)
        }
        _handler = _nuHandler()

        def actors = new ActorPool(10, 40, new DefaultPGroup(new ResizeablePool(false)), _nuHandler);
        _nuDispatcher = {
            _factory.createDispatcher(actors, _routes, cfg.staticRoots(),
                    _templateEngineName,
                    _cfg.bUseResourceDocs);
        }
        _dispatcher = _nuDispatcher();
        _dispatcher.start()
    }

    /**
     * This method is called for each request
     * This is called after the Filters and done.
     *
     * @param httpExchange
     */
    void handle(HttpExchange httpExchange) {
        try {
            httpExchange.setAttribute(GServ.exchangeAttributes.serverConfig, _cfg)
            EventManager.instance().publish(Events.RequestRecieved, [
                    requestId: httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                    method   : httpExchange.requestMethod,
                    uri      : httpExchange.requestURI,
                    headers  : httpExchange.requestHeaders])
            def start = cal.getTimeInMillis();
            _handle(httpExchange)
            EventManager.instance().publish(Events.RequestDispatched, [
                    requestId: httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                    method   : httpExchange.requestMethod,
                    uri      : httpExchange.requestURI,
                    headers  : httpExchange.requestHeaders])
        } catch (Throwable e) {
            def msg = "Error req #${httpExchange.getAttribute(GServ.exchangeAttributes.requestId)}${e.message} "
            log.error(msg, e)
            EventManager.instance().publish(Events.RequestProcessingError, [
                    requestId: httpExchange.getAttribute(GServ.exchangeAttributes.requestId),
                    error    : "${msg}",
                    method   : httpExchange.requestMethod,
                    uri      : httpExchange.requestURI,
                    headers  : httpExchange.requestHeaders])
            httpExchange.sendResponseHeaders(500, msg.bytes.size())
            httpExchange.responseBody.write(msg.bytes)
            httpExchange.responseBody.close()
        }
    }

    private void _handle(HttpExchange httpExchange) {
        //TODO be ready to stop/start the actor when it returns with IllegalState (actor cannot recv messages)
        _dispatcher << [exchange: httpExchange]
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
        /// for each plugin we add to the patterns, filters, and staticRoots
        //TODO plugins MAY also contribute to the Type formatter (to)
        plugins.each {
            serverConfig.addRoutes(it.routes())
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

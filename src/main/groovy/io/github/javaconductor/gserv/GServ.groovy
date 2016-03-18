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

package io.github.javaconductor.gserv

import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.converters.InputStreamTypeConverter
import io.github.javaconductor.gserv.delegates.DelegatesMgr
import io.github.javaconductor.gserv.delegates.PluginsDelegate
import io.github.javaconductor.gserv.factory.GServFactory
import io.github.javaconductor.gserv.resources.GServResource
import io.github.javaconductor.gserv.resources.ResourceObject
import io.github.javaconductor.gserv.server.GServPlugins
import io.github.javaconductor.gserv.utils.LinkBuilder

/**
 * gServ main class
 */
@Slf4j
class GServ {
    def factory = new GServFactory();
    static def contextAttributes = [
            "receivedMS"     : 'g$$when',
            "serverConfig"   : 'g$$serverConfig',
            "currentAction"  : 'g$$route',
            "requestId"      : 'g$$requestId',
            "isWrapper"      : 'g$$wrapper',
            "matchedAction"  : 'g$$matchedAction',
            "postProcessList": 'g$$postProcessList',
            "requestContext" : 'g$$requestContext'
    ]
    static def returnCodes = [
            Normal                  : 0,
            InstanceCompilationError: -1,
            ResourceCompilationError: -2,
            GeneralError            : -3
    ]

    def serverPlugins = new GServPlugins()// has no plugins

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
        if (!options.https && https) {
            throw new IllegalStateException("No HTTPS configuration found.")
        }
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
                .addFilters(tmpFilters)

        /// each plugin is applied to the configuration
        cfg = serverPlugins.applyPlugins(cfg);
        if (options.https) {
            cfg.applyHttpsConfig(options.https)
        } else {
            //TODO Check for a global https setting  and apply it
        }

        /// get the delegate
        if (https) {
            instanceDefinition.delegate = cfg.delegateMgr.createHttpsDelegate();

        } else {
            instanceDefinition.delegate = cfg.delegateMgr.createHttpDelegate();
        }

        instanceDefinition.resolveStrategy = Closure.DELEGATE_FIRST
        // run the config closure
        instanceDefinition()
        boolean _useResourceDocs
        def templateEngineName
        def lBuilder
        def defaultRes
        def statusPg, statusPth
        InputStreamTypeConverter inputStreamTypeConverter
        (instanceDefinition.delegate).with {
            //// Gather data from the closure we just ran
            tmpName = name()
            tmpActions = actions()
            _useResourceDocs = useResourceDocs()
            tmpFilters = filters()
            tmpStaticRoots = staticRoots()
            templateEngineName = templateEngine ?: "default"
            lBuilder = linkBuilder()
            inputStreamTypeConverter = converter()
            defaultRes = defaultResource()
            statusPg = statusPage()
            statusPth = statusPath()

        }

        /// add this info to the config
        cfg.addServerIP(options.serverIP)
                .addFilters(tmpFilters)
                .addStaticRoots(tmpStaticRoots)
                .statusPath(statusPth)
                .statusPage(statusPg)
                .addActions(tmpActions)
                .useResourceDocs(_useResourceDocs)
                .templateEngineName(templateEngineName)
                .name(tmpName)
                .linkBuilder(lBuilder)
                .converter(inputStreamTypeConverter)
                .defaultResource(defaultRes)
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

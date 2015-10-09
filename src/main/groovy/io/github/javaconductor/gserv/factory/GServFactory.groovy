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

package io.github.javaconductor.gserv.factory

import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.configuration.GServConfigFile
import io.github.javaconductor.gserv.configuration.scriptloader.ScriptLoader
import io.github.javaconductor.gserv.requesthandler.AbstractRequestContext
import io.github.javaconductor.gserv.requesthandler.Jdk16RequestContext
import io.github.javaconductor.gserv.requesthandler.RequestContext
import io.github.javaconductor.gserv.resourceloader.ResourceLoader
import io.github.javaconductor.gserv.resources.GServResource
import io.github.javaconductor.gserv.server.GServInstance
import io.github.javaconductor.gserv.server.gServHttpsInstance

/**
 *
 * @author javaConductor
 */
@Slf4j
class GServFactory {

    GServConfig createGServConfig() {
        new GServConfig()
    }

    GServConfig createGServConfig(actions) {
        new GServConfig().addActions(actions)
    }

    GServConfig createGServConfig(GServConfig orig) {
        orig.clone()
    }

    GServInstance createHttpInstance(GServConfig cfg) {
        log.debug("$cfg is ${(cfg.https()) ? 'HTTPS' : 'HTTP'}")
        if (!cfg.httpsConfig() && cfg.https()) {
            throw new IllegalStateException("HTTPS configuration is required in ServerConfig!")
        }
        cfg.https() ? new gServHttpsInstance(cfg)
                : new GServInstance(cfg)
    }

//
//    AsyncDispatcher createDispatcher( GServConfig cfg, Closure getHandlerFn) {
//        new AsyncDispatcher(cfg, getHandlerFn)
//    }

    /**
     * Parses a gserv Config file
     *
     * @param cfgFile
     * @return GServConfig instances that were created from the parsing.
     */
    List<GServConfig> createConfigs(File cfgFile) {
        assert cfgFile
        try {
            return new GServConfigFile().parse(cfgFile);// also assembles the httpsConfig
        } catch (Exception ex) {
            log.error("Could not create application from configuration file: ${cfgFile.absolutePath}", ex)
            throw ex;
        }
    }//createConfigs

//    /**
//     * Loads a gserv config file from a Groovy script file
//     *
//     * @param cfgScriptFile
//     * @return GServConfig .
//     */
//    GServConfig createConfig(File cfgScriptFile) {
//        assert cfgScriptFile
//        try {
//            new ResourceLoader().loadInstanceConfig(cfgScriptFile, [])
//        } catch (Throwable ex) {
//            log.error("Could not create application from gserv config script: ${cfgScriptFile.absolutePath}: ${ex.message}")
//            throw ex;
//        }
//    }//createConfigs

    /**
     * Creates a gserv Config
     *
     * @param staticRoot
     * @param port
     * @param defaultResource
     * @param instanceScript
     * @param resourceScripts
     * @return list of configs (containing one config)
     */
    List<GServConfig> createConfigs(String staticRoot, String bindAddress,
                                    int port, String defaultResource, String instanceScript,
                                    List<String> resourceScripts,
                                    boolean statusPage,
                                    String statusPath,
                                    List<String> classpath,
                                    displayName = "gServ Application") {
        GServConfig cfg
        def resources = []
        ResourceLoader resourceLoader = new ResourceLoader()
        ScriptLoader scriptLoader = new ScriptLoader()

        try {
            /// if there is an instance script then use it to create the config
            if (instanceScript) {
                cfg = resourceLoader.loadInstanceConfig(new File(instanceScript), classpath)
            }
            // if we didn't get one from the instance then create one
            cfg = cfg ?: createGServConfig()
            // if there are resoucrs scripts - load them
            if (resourceScripts) {
                resources = scriptLoader.loadResources(resourceScripts, classpath)
            }
        } catch (Throwable ex) {
            log.error("Could not load resource script: ${ex.message}")
            println ex.message
            throw ex
        }

        createConfigs(staticRoot, bindAddress, port, defaultResource,
                cfg, resources, statusPage, statusPath, classpath, displayName)

    }//createConfigs

    GServInstance createInstance(String staticRoot, String bindAddress,
                                 int port, String defaultResource, String instanceScript,
                                 List<String> resourceScripts,
                                 boolean statusPage,
                                 String statusPath,
                                 List<String> classpath,
                                 displayName = "gServ Application") {
        GServConfig cfg
        def resources = []
        ResourceLoader resourceLoader = new ResourceLoader()
        ScriptLoader scriptLoader = new ScriptLoader()

        try {
            /// if there is an instance script then use it to create the config
            GServInstance instance = resourceLoader.loadInstance(new File(instanceScript), classpath)
            instance.config().port(port)
            if (resourceScripts) {
                resources = scriptLoader.loadResources(resourceScripts, classpath)
                instance.config().addResources(resources)
            }

            if (staticRoot) {
                instance.config().addStaticRoots([staticRoot])
            }

            if (bindAddress) {
                instance.config().bindAddress(new InetSocketAddress(bindAddress, port))
            }

            if (defaultResource) {
                instance.config().defaultResource(defaultResource)
            }

            if (statusPage) {
                instance.config().statusPage(statusPage)
                if (statusPath)
                    instance.config().statusPath(statusPath)
            }
            if (displayName) {
                instance.config().name(displayName)
            }

            instance
            // if we didn't get one from the instance then create one
        } catch (Throwable ex) {
            log.error("Could not load resource script: ${ex.message}")
            println ex.message
            throw ex
        }

    }//createInstance

    /**
     * Creates a gserv Config
     *
     * @param staticRoot
     * @param port
     * @param defaultResource
     * @param cfg GServConfig
     * @param resources List<GServResource>
     * @return list of configs (containing one config)
     */
    def createConfigs(String staticRoot, String bindAddress,
                      int port, String defaultResource,
                      GServConfig cfg, List<GServResource> resources,
                      boolean statusPage,
                      String statusPath,
                      List<String> classpath, displayName = "gServ Application") {
        cfg = cfg ?: createGServConfig()
        if (resources) {
            cfg.addResources(resources)
        }
        if (staticRoot) {
            cfg.addStaticRoots([staticRoot])
        }

        if (defaultResource) {
            cfg.defaultResource(defaultResource)
        }

        if (bindAddress) {
            def addr = InetAddress.getByName(bindAddress);
            def socketAddr = new InetSocketAddress(addr, port);
            cfg.bindAddress(socketAddr);
        }

        if (displayName) {
            cfg.name(displayName)
        }

        cfg.statusPath(statusPath)
        cfg.statusPage(statusPage)

        [cfg
                 .port(port)
        ];

    }//createConfigs

    RequestContext createRequestContext(GServConfig config, HttpExchange httpExchange) {
        new Jdk16RequestContext(config, httpExchange);
    }

    @Deprecated
    RequestContext createRequestContext(String method, URI uri, Map headers) {
        def context = new AbstractRequestContext(null) {
            boolean closed = false

            @Override
            void setResponseHeaders(Map<String, List> responseHeaders) {

            }

            @Override
            void sendResponseHeaders(int responseCode, long size) {

            }

            @Override
            def close() {
                closed = true
                return null
            }

            @Override
            def isClosed() {
                return closed
            }

            @Override
            def setStreams(InputStream is, OutputStream os) {
                return null
            }
        }
        context.requestHeaders = headers
        context.requestURI = uri
        context.requestMethod = method
        context
    }

}

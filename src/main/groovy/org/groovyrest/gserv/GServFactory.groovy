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

import groovy.util.logging.Log4j
import org.groovyrest.gserv.configuration.GServConfig
import org.groovyrest.gserv.configuration.GServConfigFile
import org.groovyrest.gserv.configuration.scriptloader.ScriptLoader
import org.groovyrest.gserv.resourceloader.ResourceLoader

/**
 *
 * @author lcollins
 */
@Log4j
class GServFactory {

    def createGServConfig() {
        new GServConfig()
    }

    def createGServConfig(routes) {
        new GServConfig().addRoutes(routes)
    }

    def createGServConfig(GServConfig orig) {
        orig.clone()
    }

    def createHttpInstance(cfg) {
        log.debug("$cfg is ${(cfg.https()) ? 'HTTPS' : 'HTTP'}")
        cfg.https() ? new gServHttpsInstance(cfg)
                : new GServInstance(cfg)
    }

    def createDispatcher(actors, routes, staticRoots, templateEngineName, bUseResourceDocs) {
        new AsyncDispatcher(actors, routes, staticRoots, templateEngineName, bUseResourceDocs);
    }

    def createDispatcher() {
        new AsyncDispatcher()
    }

    /**
     * Parses a gserv Config file
     *
     * @param cfgFile
     * @return GServConfig instances that were created from the parsing.
     */
    def createConfigs(File cfgFile) {
        try {
            return new GServConfigFile().parse(cfgFile);// also assembles the httpsConfig
        } catch (Exception ex) {
            log.error("Could not create application from configuration file: ${cfgFile.absolutePath}", ex)
            throw ex;
        }
    }//createConfigs

    /**
     * Created a gserv Config
     *
     * @param staticRoot
     * @param port
     * @param defaultResource
     * @param instanceScript
     * @param resourceScripts
     * @return list of configs (containing one config)
     */
    def createConfigs(staticRoot, bindAddress, port, defaultResource, instanceScript, resourceScripts, classpath) {
        GServConfig cfg
        ResourceLoader resourceLoader = new ResourceLoader()
        ScriptLoader scriptLoader = new ScriptLoader()

        if (instanceScript) {
            cfg = resourceLoader.loadInstance(new File(instanceScript), classpath)
        }

        cfg = cfg ?: createGServConfig()
        if (resourceScripts) {
            try {
                def resources = scriptLoader.loadResources(resourceScripts, classpath)
                cfg.addResources(resources)
            } catch (Throwable ex) {
                log.error("Could not load resource script: ${ex.message}")
                println ex.message
                throw ex
            }
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

        [cfg
                 .port(port)
        ];

    }//createConfigs
}

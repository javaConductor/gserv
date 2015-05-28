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

package io.github.javaconductor.gserv.configuration

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.exceptions.ConfigException
import io.github.javaconductor.gserv.factory.GServFactory
import io.github.javaconductor.gserv.plugins.PluginMgr
import io.github.javaconductor.gserv.resourceloader.ResourceLoader

/**
 * Created by javaConductor on 8/14/2014.
 */
@Slf4j
class GServConfigFile {
    ResourceLoader resourceLoader = new ResourceLoader()
    GServFactory factory = new GServFactory()

    /**
     *
     *
     * @param configFile
     * @return list of GServConfig
     */
    List<GServConfig> parse(File configFile) {
        def cfg
        try {
            cfg = new JsonSlurper().parse(configFile);
        } catch (Exception e) {
            def msg = "Could not parse file: ${configFile.absolutePath}: ${e.message}";
            log.error(msg, e)
            throw new ConfigException(msg, e)
        }

        ///// CREATE the initial GServConfig from the file values
        if (!cfg.apps) {//else
            System.err.println("Error in gserv config(${configFile.absolutePath}) - No apps specified.  At least one is required.")
            return [];
        }

        GServConfig newCfg;
        def configs = cfg.apps.collect { app ->

            try {
                newCfg = appToConfig(app, cfg.classpath)
                if (app.https && cfg.https) {
                    newCfg.applyHttpsConfig(cfg.https)
                }
            } catch (Exception ex) {
                log.error("Unable to instantiate configuration", ex)
                throw ex;
            }
            newCfg
        }//collect
        configs
    }//parse

    def addResources(resourceScripts, config) {
        def resources = []
        if (resourceScripts) {
            resources = resourceScripts.collect { scriptFileName ->
                def f = new File(scriptFileName)
                if (!f.exists()) {
                    System.err.println("No resourceScript: ${f.absolutePath}")
                    return []
                }
                return (resourceLoader.loadResources(f) ?: [])
            }.flatten()
        }
        config.addResources(resources)
        config;
    }////

    def appToConfig(app, classpath = []) {

        def config = factory.createGServConfig()
        app.with {
            // declare plugins first so they are available for instance and resources
            registerPlugins(plugins)
            if (instanceScript) {
                File f = new File(instanceScript)
                if (!f.exists()) {
                    throw new ConfigException("Instance script: $instanceScript not found.")
                }
                config = resourceLoader.loadInstance(f, classpath)
            }

            try {
                config = addResources(resourceScripts, config);
            } catch (Exception ex) {
                log.debug("Could not load resource scripts.", ex)
                throw ex
            }

            if (defaultResource) {
                config.defaultResource defaultResource
            }
            if (port) {
                config.port(port)
            }
            if (static_roots) {
                config.addStaticRoots(static_roots)
            }
            if (name) {
                config.name(name)
            }

        }
        config
    }//appToConfig

    def registerPlugins(plugins) {
        if (plugins) {
            def pluginMgr = PluginMgr.instance()
            plugins.keySet.each { pname ->
                pluginMgr.register(pname, Class.forName(plugins[pname]))
            }
        }
    }
}

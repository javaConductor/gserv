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

package io.github.javaconductor.gserv.gserv.configuration

import io.github.javaconductor.gserv.gserv.GServ
import io.github.javaconductor.gserv.GServFactory
import io.github.javaconductor.gserv.gserv.exceptions.ConfigException
import io.github.javaconductor.gserv.resourceloader.ResourceLoader
import groovy.json.JsonSlurper
import io.github.javaconductor.gserv.gserv.plugins.PluginMgr
import groovy.util.logging.Log4j

/**
 * Created by javaConductor on 8/14/2014.
 */
@Log4j
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
        HttpsConfig httpsCfg
        if (cfg.https) {
            cfg.applyHttpsConfig(cfg.https)
        }

        if (!cfg.apps) {//else
            System.err.println("Error in gserv config(${configFile.absolutePath}) - No apps specified.  At least one is required.")
            return [];
        }
        ClassLoader classLoader = GServ.classLoader
        ///// Get the classpath and add those jars to the Classpath
        if (cfg.classpath) {
            classLoader = addClasspath(classLoader, cfg.classpath)
        }

        GServConfig newCfg;
        def configs = cfg.apps.collect { app ->

            try {
                newCfg = appToConfig(app)
                if (app.https) {

                    newCfg.httpsConfig(cfg.httpsConfig())
                }
            } catch (Exception ex) {
                log.error("Unable to instantiate configuration", ex)
                throw ex;
            }
            if (httpsCfg && app.https) {
                newCfg.httpsConfig(httpsCfg);
            }
            newCfg
        }
        configs
    }//parse

    ClassLoader addClasspath(ClassLoader classLoader, List classpath) {
        def urls = classpath.collect { jar ->
            new File(jar).toURI().toURL()
        }
        URLClassLoader.newInstance(urls, classLoader)
    }

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
    };////

    def appToConfig(app) {
        def config = factory.createGServConfig()
        app.with {

            try {
                config = addResources(resourceScripts, config);
            } catch (Exception ex) {
                log.debug("Could not load resource scripts.", ex)
                throw ex
            }

            registerPlugins(plugins)
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

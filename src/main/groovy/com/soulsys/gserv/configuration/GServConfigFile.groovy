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

package com.soulsys.gserv.configuration

import com.soulsys.gserv.GServFactory
import com.soulsys.gserv.exceptions.ConfigException
import com.soulsys.gserv.resourceloader.ResourceLoader
import groovy.json.JsonSlurper
import com.soulsys.gserv.plugins.PluginMgr

/**
 * Created by lcollins on 8/14/2014.
 */
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
            throw new ConfigException("Could not parse file: ${configFile.absolutePath}: ${e.message}")
        }
        ///// CREATE the initial GServConfig from the file values
        HttpsConfig httpsCfg
        if (cfg.https) {
            if (!cfg.https.password)
                throw new IllegalArgumentException("Password is required for HTTPS.")
            httpsCfg = new HttpsConfig()
            httpsCfg.password = cfg.https.password
            httpsCfg.keyManagerAlgorithm = cfg.https.keyManagerAlgorithm
            httpsCfg.keyStoreFilePath = cfg.https.keyStoreFilePath ?: (System.getProperty("user.home") + "/gserv.keystore")

            httpsCfg.keyStoreImplementation = cfg.https.keyStoreImplementation
            httpsCfg.trustManagerAlgorithm = cfg.https.trustManagerAlgorithm
            httpsCfg.sslProtocol = cfg.https.sslProtocol
        }

        if (!cfg.apps) {//else
            System.err.println("Error in gserv config(${configFile.absolutePath}) - No apps specified.  At least one is required.")
            return [];
        }
        GServConfig newCfg;
        def configs = cfg.apps.collect { app ->
            newCfg = appToConfig(app)
            if (httpsCfg && app.https) {
                newCfg.httpsConfig(httpsCfg);
            }
            newCfg
        }
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
    };////


    def appToConfig(app) {
        def config = factory.createGServConfig()
        app.with {
            config = addResources(resourceScripts, config);
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

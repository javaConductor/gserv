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

package com.soulsys.gserv

import com.soulsys.gserv.configuration.GServConfig
import com.soulsys.gserv.configuration.GServConfigFile
import com.soulsys.gserv.configuration.scriptloader.ScriptLoader
import com.soulsys.gserv.exceptions.ConfigException
import com.soulsys.gserv.resourceloader.ResourceLoader
import org.apache.commons.cli.Option

import static groovyx.gpars.GParsPool.withPool

/**
 * Runs instances of gserv  as specified by the command-line args.
 */
class GServRunner {
    def factory = new GServFactory();
    def scriptLoader = new ScriptLoader();
    def resourceLoader = new ResourceLoader();
    CliBuilder cli = new CliBuilder(
            usage: 'gserv  -c configFile  or -s Server Root -p port -d defaultStaticResource -i instanceDefinitionScript -r resourceScripts',
            header: '\nAvailable options (use -h for help):\n',
            footer: '\nInformation provided via above options is used to generate printed'
    );

    def GServRunner() {
        cli.c(longOpt: 'configFilePath', 'gserv config file path', args: 1, required: false)
        cli.i(longOpt: 'instanceScript', 'gserv instance script', args: 1, required: false)
        cli.s(longOpt: 'serverRoot', 'Server Static Root (used only when configFilePath is not present)', args: 1, required: false)
        cli.p(longOpt: 'port', 'Port (used only when configFilePath is not present)', args: 1, required: false)
        cli.d(longOpt: 'defaultStaticResource', 'Default file to load when no file is specified', required: false)
        cli.j(longOpt: 'classpath', 'Classpath. Commas separated list of jars.', required: false, args: Option.UNLIMITED_VALUES, valueSeparator: ',')
        //d(longOpt: 'defaultStaticResource', 'Default file to load when no file is specified', required: false, args: Option.UNLIMITED_VALUES, valueSeparator: ',')
        cli.r(longOpt: 'resourceScripts', 'Resource Scripts', required: false, args: Option.UNLIMITED_VALUES, valueSeparator: ',')
        cli.usage()
    }

    /**
     * Parses a gserv Config file
     *
     * @param cfgFile
     * @return  GServConfig instances that were created from the parsing.
     */
    def createConfigs(File cfgFile) {
        //TODO must add the [get('\', file(defaultPage) )] to the server config.
        /// read the config to get the 'https' and 'apps' info
        GServConfigFile configFile = new GServConfigFile()
        List<GServConfig> configs = configFile.parse(cfgFile);
        configs
    }//createConfig

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
    def createConfigs(staticRoot, port, defaultResource, instanceScript, resourceScripts, classpath) {
        GServConfig cfg
        ClassLoader classLoader = GServ.classLoader

        if (classpath){
            classLoader = this.addClasspath(classLoader, classpath)
        }

        if (instanceScript) {
            cfg = resourceLoader.loadInstance(new File(instanceScript), classLoader)
        }

        cfg = cfg ?: factory.createGServConfig()
        if (resourceScripts) {
            def resources = scriptLoader.loadResources(resourceScripts, classLoader)
            cfg.addResources(resources)
        }
        if (staticRoot) {
            cfg.addStaticRoots([staticRoot])
        }

        if (defaultResource) {
            cfg.defaultResource(defaultResource)
        }

        [cfg
                 .port(port)
        ];

    }//createConfig

    /**
     * Make sure user entered valid command-line args
     *
     * @param options
     * @return
     */
    def validateOptions(options){
        def ok = (options.i && options.p) || (options.r && options.p) || (options.s && options.p) || options.c;
        ok
    }

    /**
     * Starts gserv instance(s) based on the args
     *
     * @param cliArgs
     * @return NONE - call never returns
     */
    def start(cliArgs) {
        def options = cli.parse(cliArgs)
        if (!options || !validateOptions(options)) {
            System.out.println(cli.usage)
            return;
        }

        def configs
        def configFile
        def configFilename = options.c;
        if (!configFilename) {
            def staticRoot, port, classpath, defaultResource, resourceScripts, instanceScript;
            if (!options.p)
                throw new ConfigException("Port is required for gserv cli.")

            staticRoot = options.s;
            port = options.p as int;
            defaultResource = options.d;
            resourceScripts = options.rs;
            instanceScript = options.i;
            classpath = options.js;
            configs = createConfigs(
                    staticRoot,
                    port,
                    defaultResource,
                    instanceScript,
                    resourceScripts,
            classpath);
        } else {   // use ONLY the config file and ignore everything else on the cmdLine
            configFile = new File(configFilename);
            if (!configFile.exists()) {
                throw new IllegalArgumentException("ConfigFile $configFilename not found!");
            }
            configs = createConfigs(configFile)
        }

        def stopFns = configs.collect { cfg ->
                def instance = factory.createHttpInstance(cfg);
                instance.start(cfg.port())
            }

        Thread.sleep(300)

        return ({ ->
            stopFns.each { stopFn -> stopFn() }
        });

    }//start

    ClassLoader addClasspath(ClassLoader classLoader, List classpath){
        def urls = classpath.collect { jar ->
            new File(jar).toURI().toURL()
        }
        URLClassLoader.newInstance(urls, classLoader)
    }

}//class

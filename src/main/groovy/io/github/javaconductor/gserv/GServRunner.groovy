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
import io.github.javaconductor.gserv.configuration.GServConfigFile
import io.github.javaconductor.gserv.configuration.scriptloader.ScriptLoader
import io.github.javaconductor.gserv.exceptions.ConfigException
import io.github.javaconductor.gserv.resourceloader.InstanceScriptException
import io.github.javaconductor.gserv.resourceloader.ResourceLoader
import io.github.javaconductor.gserv.resourceloader.ResourceScriptException
import groovy.util.logging.Log4j
import org.apache.commons.cli.Option

import static groovyx.gpars.GParsPool.withPool

/**
 * Runs instances of gserv  as specified by the command-line args.
 */
@Log4j
class GServRunner {
    def factory = new GServFactory();
    def scriptLoader = new ScriptLoader();
    def resourceLoader = new ResourceLoader();
    CliBuilder cli = new CliBuilder(
            usage: 'gserv [options]',
            header: '\nAvailable options (use -h for help):\n',
            footer: '\ngServ © 2014 Lee Collins.  All rights reserved.'
    );

    def GServRunner() {
        cli.c(longOpt: 'configFilePath', 'gserv config file path', args: 1, required: false)
        cli.i(longOpt: 'instanceScript', 'gserv instance script', args: 1, required: false)
        cli.s(longOpt: 'serverRoot', 'Server Static Root (used only when configFilePath is not present)', args: 1, required: false)
        cli.p(longOpt: 'port', 'Port (used only when configFilePath is not present)', args: 1, required: false)
        cli.d(longOpt: 'defaultStaticResource', 'Default file to load when no file is specified', args: 1, required: false)
        cli.a(longOpt: 'bindAddress', 'If specified, the App will only respond to requests for the specified IP address. By default, responds to requests for all IP addresses on the machine.', args: 1, required: false)
        cli.j(longOpt: 'classpath', 'Classpath. Commas separated list of jars.', required: false, args: Option.UNLIMITED_VALUES, valueSeparator: ',')
        cli.r(longOpt: 'resourceScripts', 'Resource Scripts', required: false, args: Option.UNLIMITED_VALUES, valueSeparator: ',')
        cli.n(longOpt: 'appName', 'The name of the Application', args: 1, required: false)
    }

    /**
     * Make sure user entered valid command-line args
     *
     * @param options
     * @return
     */
    def validateOptions(options) {
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
            cli.usage()
            return;
        }

        def configs
        def configFile
        def configFilename = options.c;
        try {
            if (!configFilename) {
                def staticRoot, bindAddress, port, classpath, defaultResource, resourceScripts, instanceScript;
                if (!options.p)
                    throw new ConfigException("Port is required for gserv cli.")

                staticRoot = options.s;
                bindAddress = options.a;
                port = options.p as int;
                defaultResource = options.d;
                resourceScripts = options.rs;
                instanceScript = options.i;
                def appName = options.n;
                classpath = options.js;
                configs = factory.createConfigs(
                        staticRoot ?: "",
                        bindAddress ?: "",
                        port,
                        defaultResource ?: "",
                        instanceScript ?: "",
                        resourceScripts ?: [],
                        classpath ?: [], appName ?: null);
            } else {   // use ONLY the config file and ignore everything else on the cmdLine
                configFile = new File(configFilename);
                if (!configFile.exists()) {
                    throw new IllegalArgumentException("ConfigFile $configFilename not found!");
                }
                configs = factory.createConfigs(configFile)
            }
        } catch (ResourceScriptException ex) {
            log.error("Could not start app.", ex)
            //System.exit(GServ.returnCodes.ResourceCompilationError)
            throw ex;
        } catch (InstanceScriptException ex) {
            log.error("Could not start app.", ex)
            //System.exit(GServ.returnCodes.InstanceCompilationError)
            throw ex;
        } catch (Throwable ex) {
            log.error("Could not start app.", ex)
            //System.exit(GServ.returnCodes.GeneralError)
            throw ex;
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

}//class

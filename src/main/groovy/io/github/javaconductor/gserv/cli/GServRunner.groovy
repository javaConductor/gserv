

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

package io.github.javaconductor.gserv.cli

import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.exceptions.ConfigException
import io.github.javaconductor.gserv.factory.GServFactory
import org.apache.commons.cli.Option

/**
 * Runs instances of gserv  as specified by the command-line args.
 */
@Slf4j
class GServRunner {
    def factory = new GServFactory();
    def version = getGServVersion()
    CliBuilder cli = new CliBuilder(
            usage: 'gserv [options]',
            header: '\nAvailable options (use -h for help):\n',
            footer: "\ngServ $version - Copyright 2014-2015 Lee Collins.  All rights reserved."
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
        cli.x(longOpt: 'maxThreads', 'The max number of threads used to respond to requests', args: 1, required: false)
        cli.v(longOpt: 'version', 'Prints the current gServ version', args: 0, required: false)
        cli.g(longOpt: 'no-status-page', 'Disables the status page.', args: 0, required: false)
        cli.t(longOpt: 'status-path', 'Path to use for the status page.', args: 0, required: false)
        cli._(longOpt: 'appProperties', 'Application properties file.', args: 1, required: false)
    }

    /**
     * Make sure user entered valid command-line args
     *
     * @param options
     * @return
     */
    //TODO do some data type validation
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
        if (options?.v) {
            println "gServ ${getGServVersion()}"
            return
        }
        if (!options || !validateOptions(options)) {
            cli.usage()
            return;
        }

        def maxThreads
        def statusPage
        def statusPath
        def appPropertiesFilename
        def configs
        def configFile
        def configFilename = options.c;
        def nuInstance
        try {
            if (!configFilename) {
                def staticRoot, bindAddress, port, classpath, defaultResource, resourceScripts, instanceScript;
                if (!options.p)
                    throw new ConfigException("Port not specified.")

                staticRoot = options.s;
                bindAddress = options.a;
                port = options.p as int;
                defaultResource = options.d;
                resourceScripts = options.rs;
                instanceScript = options.i;
                def appName = options.n;
                classpath = options.js;
                maxThreads = options.x;
                statusPath = options.t
                statusPage = !options.g
                appPropertiesFilename = options.appProperties

                if (instanceScript) {
                    nuInstance = factory.createInstance(
                            staticRoot ?: "",
                            bindAddress ?: "",
                            port,
                            defaultResource ?: "",
                            instanceScript ?: "",
                            resourceScripts ?: [],
                            maxThreads ? maxThreads as int : GServConfig.defaultMaxThreads(),
                            statusPage,
                            statusPath ?: "",
                            appPropertiesFilename,
                            classpath ?: [],
                            appName ?: null)
                } else {
                    configs = factory.createConfigs(
                            staticRoot ?: "",
                            bindAddress ?: "",
                            port,
                            defaultResource ?: "",
                            instanceScript ?: "",
                            resourceScripts ?: [],
                            maxThreads ? maxThreads as int : GServConfig.defaultMaxThreads(),
                            statusPage,
                            statusPath ?: "",
                            appPropertiesFilename,
                            classpath ?: [],
                            appName ?: null);
                }
            } else {   // use ONLY the config file and ignore everything else on the cmdLine Except: appProperties

                //TODO we need to use the appProperties file as the values for config file vars eg: $properties.timeServiceHost
                //TODO $properties is Map of values from properties file ???? LAC

                configFile = new File(configFilename);
                if (!configFile.exists()) {
                    throw new IllegalArgumentException("ConfigFile $configFilename not found!");
                }
                configs = factory.createConfigs(configFile)
            }
        } catch (Throwable ex) {
            log.trace("Could not start app.", ex)
            throw ex;
        }
        def stopFns
        if (nuInstance) {
            stopFns = [nuInstance.start(nuInstance.config().port())]
        } else {
            stopFns = configs.collect { cfg ->
                log.debug("Creating HTTP Instance")
                def inst = factory.createHttpInstance(cfg);
                inst.start(cfg.port())
            }
        }
        Thread.sleep(300)
        return ({ ->
            stopFns.each { stopFn -> stopFn() }
        });

    }//start

    String getGServVersion() {
        this.class.package.implementationVersion
    }

}//class

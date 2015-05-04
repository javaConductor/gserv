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

package io.github.javaconductor.gserv.server

import com.sun.net.httpserver.HttpContext
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsServer
import groovy.jmx.builder.JmxBuilder
import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.factory.ResourceActionFactory
import io.github.javaconductor.gserv.filters.Filter
import io.github.javaconductor.gserv.filters.FilterByteArrayOutputStream
import io.github.javaconductor.gserv.jmx.GServJMX
import io.github.javaconductor.gserv.requesthandler.RequestContext
import io.github.javaconductor.gserv.requesthandler.wrapper.RequestContextWrapper
import io.github.javaconductor.gserv.status.StatisticsMgr

import javax.management.MBeanServer
import javax.management.ObjectName
import javax.net.ssl.*
import java.lang.management.ManagementFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.text.NumberFormat

/**
 * An HTTP Server Instance.  Pass a port number to the start() method to start the instance on that port.
 */
@Log4j
class GServInstance {
    protected def _authenticator
    protected com.sun.net.httpserver.HttpHandler _handler
    protected def _executor
    protected def _filters
    protected def _templateEngineName;
    protected GServConfig _cfg;
    def mbean
    static def requestId = 1L;

    /**
     *
     * @param cfg
     * @return
     */
    def GServInstance(cfg) {
        _cfg = cfg
        _handler = new GServHandler(cfg);
        _authenticator = cfg.authenticator();
        _filters = cfg.filters();
        _templateEngineName = cfg.templateEngineName();
    }

    /**
     *
     * @param actualPort
     * @param jmxBean
     * @return
     */
    def exportMBean(actualPort, jmxBean) {

        System.properties.put('com.sun.management.jmxremote.ssl', true);
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("com.gserv.$actualPort:type=GServJMX");
        JmxBuilder jmx = new JmxBuilder(mbs)
        def connection = jmx.server(port: 8090)

        def beans = jmx.export(policy: "replace") {
            bean(
                    target: jmxBean,
                    name: name
            )
        }

        this.mbean = (GroovyMBean) beans[0]
        this.mbean
    }

    /**
     *
     * @return
     */
    def createInitFilter() {
        def initFilter = ResourceActionFactory.createBeforeFilter("gServInit", "*", "/*", [:], -1) { requestContext, args ->
            def requestId = requestContext.id()
            log.trace("initFilter(#${requestId} context: $requestContext")
            /// here we should check for a blank file name
            /// if file name is blank and we have a defaultResource then we use that.
            URI uri = applyDefaultResourceToURI(config().defaultResource(),
                    requestContext.requestURI);

            // wrap the Context
            RequestContext rc = new RequestContextWrapper(requestContext)
            rc.requestURI = uri
            rc.setAttribute(GServ.contextAttributes.postProcessList, [])
            rc.setStreams(rc.requestBody, new ByteArrayOutputStream())
            rc
        }
        initFilter
    }

    /**
     *
     * @return The Status Filter
     */
    Filter createStatusFilter() {
        new StatisticsMgr().createStatusFilter(_cfg)
    }

    /**
     * This method will start the server on 'port'.
     *
     * @param port
     * @return Function to close the server
     */
    def start(port = null) {
        def actualPort = (port ?: _cfg.port())

        exportMBean(actualPort,
                new GServJMX()
        );

        ///// Underlying Server Impl -
        HttpServer server = HttpServer.create((_cfg.bindAddress() ?: new InetSocketAddress(actualPort as Integer)), actualPort as Integer);
        HttpContext context = server.createContext("/", _handler);

        ////////////////////////////////
        /// create and add the InitFilter
        ////////////////////////////////
        def initFilter = createInitFilter()
        _filters = _filters ?: []
        _filters.add(initFilter);

        ////////////////////////////////
        /// create and add the StatusFilter
        ////////////////////////////////
        if (_cfg.statusPage()) {
            _filters.add(createStatusFilter())
        }

        _cfg._filters = _filters.sort({ a, b -> a.order - b.order })
        context.authenticator = _authenticator;
        server.executor = _executor;
        def appName = _cfg.name() ?: "gserv"

        def bindStr = bindAddrString(_cfg.bindAddress())
        if (bindStr)
            println "$appName starting HTTP ${bindStr}"
        else
            println "$appName starting HTTP on port ${server.address.port}"

        EventManager.instance().publish(Events.ServerStarted, [port: actualPort])
        Thread.start {
            server.start();
        }
        return ({ ->
            server.stop(0);
        });
    };

    /**
     *
     * @param bindAddress
     * @return
     */
    def bindAddrString(bindAddress) {
        (bindAddress && !bindAddress.toString().contains("0:0:0:0")) ? "bound to ${bindAddress.toString().substring(1)} " : ""
    }

    /**
     *
     * @return
     */
    GServConfig config() {
        return _cfg;
    }

    /**
     * Takes a URI pointing to the  root and returns a URI pointing to the defaultResource
     *
     * @param defaultResource
     * @param uri
     * @return
     */
    URI applyDefaultResourceToURI(defaultResource, uri) {
        if (!defaultResource || (!(uri.path == '/')))
            return uri;

        new URI(
                uri.scheme, uri.userInfo, uri.host,
                uri.port, config().defaultResource(),
                uri.query, uri.fragment);
    }

}

@Log4j
class gServHttpsInstance extends GServInstance {

    gServHttpsInstance(cfg) {
        super(cfg)
        if (!cfg.https())
            throw new IllegalArgumentException("HTTPS must be configured for this Instance.")
    }

    /**
     * This method will start the server on 'port'.
     *
     * @param port
     */
    def start(port = null) {
        def actualPort = (port ?: _cfg.port())

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("com.gserv.$actualPort:type=GServJMX");
        GServJMX mbean = new GServJMX();
        mbs.registerMBean(mbean, name);

        def httpsConfig = this._cfg.httpsConfig()
        assert (httpsConfig.password)
        def keyManagerAlgorithm = httpsConfig.keyManagerAlgorithm ?: "SunX509"
        def trustManagerAlgorithm = httpsConfig.trustManagerAlgorithm ?: "SunX509"
        def keyStoreFilePath = httpsConfig.keyStoreFilePath ?: (System.getProperty("user.home") + "/gserv.keystore")
        def keyStoreImplementation = httpsConfig.keyStoreImplementation ?: "JKS"
        def sslProtocol = httpsConfig.sslProtocol ?: "TLS"
        def httpsPassword = httpsConfig.password
        def server = HttpsServer.create(new InetSocketAddress(actualPort as Integer), 0);
        SSLContext sslContext = SSLContext.getInstance(sslProtocol);

        // initialise the keystore
        char[] password = httpsPassword.toCharArray();
        KeyStore ks = KeyStore.getInstance(keyStoreImplementation);
        FileInputStream fis = new FileInputStream(keyStoreFilePath);
        ks.load(fis, password);

        // setup the key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerAlgorithm);
        kmf.init(ks, password);

        // setup the trust manager factory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(trustManagerAlgorithm);
        tmf.init(ks);

        // setup the HTTPS context and parameters
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        server.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(sslContext) {

            @Override
            void configure(com.sun.net.httpserver.HttpsParameters params) {
                log.info("HTTPSConfigurator.onConfigure($params) - handling");
                try {
                    // initialise the SSL context
                    SSLContext c = sslContext;//.getDefault();
                    SSLEngine engine = c.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    // get the default parameters
                    SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                    params.setSSLParameters(defaultSSLParameters);
                    super.configure(params)
                }
                catch (Exception ex) {
                    log.error("Failed to create HTTPS port: ", ex);
                } finally {
                    log.info("HTTPSConfigurator.onConfigure($params) - handled!");
                }
            }
        });
        def context = server.createContext("/", _handler);

        ////////////////////////////////
        /// create and add the InitFilter
        ////////////////////////////////
        def initFilter = createInitFilter()

        _filters = _filters ?: []
        _filters.add(initFilter);
        //TODO ADD Status Filter
        /// If  (_cfg .statusPage)  then create/add StatusFilter
        // _filters.add ( createStatusFilter( cfg, statusPath) )

        _cfg._filters = _filters.sort({ a, b -> a.order - b.order })
        context.authenticator = _authenticator;
        server.executor = _executor;
        def appName = _cfg.name() ?: "gserv"
        def bindStr = bindAddrString(_cfg.bindAddress())
        if (bindStr)
            println "$appName starting HTTPS ${bindStr}"
        else
            println "$appName starting HTTPS on port ${server.address.port}"

        EventManager.instance().publish(Events.ServerStarted, [port: actualPort])
        Thread.start {
            server.start();
        }
        return ({ ->
            server.stop(0);
        });
    }

}

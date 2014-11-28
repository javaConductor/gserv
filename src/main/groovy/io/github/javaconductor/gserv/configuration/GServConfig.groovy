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

import io.github.javaconductor.gserv.*
import io.github.javaconductor.gserv.delegates.DefaultDelegates
import io.github.javaconductor.gserv.delegates.DelegatesMgr
import io.github.javaconductor.gserv.GServResource
import io.github.javaconductor.gserv.utils.LinkBuilder
import io.github.javaconductor.gserv.utils.StaticFileHandler
import groovy.transform.AutoClone
import groovy.transform.AutoCloneStyle

//@AutoClone(style = AutoCloneStyle.COPY_CONSTRUCTOR)
class HttpsConfig {
    def keyManagerAlgorithm = 'SunX509'
    def trustManagerAlgorithm = 'SunX509'
    def keyStoreFilePath = "/Users/lcollins/gserv.keystore"
    def keyStoreImplementation = "JKS"
    def password
    def sslProtocol
}

/**
 *
 * @author lcollins
 *
 * gServ Server Instance Configuration
 *
 */
@AutoClone(style = AutoCloneStyle.CLONE)
class GServConfig {

    private def _name = 'gserv App';
    private def _actions = [];
    def _staticRoots = [], _filters = [], _authenticator,
        _templateEngineName, bUseResourceDocs, _defaultPort
    def delegateTypeMap
    def delegateMgr
    def linkBuilder
    def _defaultResource
    InetSocketAddress _bindAddress
    StaticFileHandler _staticFileHandler = new StaticFileHandler()
    io.github.javaconductor.gserv.Matcher matcher = new Matcher()
    def serverIPs = []
    HttpsConfig _https

    GServConfig() {
        this.delegateManager(new DelegatesMgr(DefaultDelegates.getDelegates()))
    }

    /**
     *
     * @return the BindAddress for Instances using this gServ Config
     */
    def bindAddress() {
        return _bindAddress;// ?: new Inet4Address('0.0.0.0', null)
    }

    /**
     *
     * @param addr
     * @return 'this' - Fluent Interface
     */
    def bindAddress(InetSocketAddress addr) {
        /// always use 0.0.0.0 for the default
        _bindAddress = addr;//?: new InetSocketAddress()  ('0.0.0.0', null);
        return this;
    }

    def name() {
        _name
    }

    def name(nm) {
        _name = nm
    }

    def httpsConfig(HttpsConfig httpsConfig) {
        _https = httpsConfig
    }

    def httpsConfig() {
        _https
    }

    def https() {
        !!_https
    }

    def authenticator(authenticator) {
        this._authenticator = authenticator;
        this
    }

    def authenticator() { _authenticator }

    def delegateManager(delegateMgr) {
        this.delegateMgr = delegateMgr;
        this
    }

    def delegateManager() { this.delegateMgr }

    def linkBuilder(linkBuilder) {
        this.linkBuilder = linkBuilder;
        this
    }

    def linkBuilder() {
        this.linkBuilder = this.linkBuilder ?: new LinkBuilder();
        this.linkBuilder
    }

    def matchAction(exchange) {
        matcher.matchAction(_actions, exchange)
    }

    def requestMatched(exchange) {
        !!(matchAction(exchange) || _staticFileHandler.resolveStaticResource(
                exchange.requestURI.path,
                _staticRoots,
                bUseResourceDocs))
    }

    /**
     * Sets the template engine for this configuration
     *
     * @param ten Template Engine Name
     *
     */
    def templateEngineName(ten) {
        _templateEngineName = ten;
        this
    }

    def templateEngineName() { _templateEngineName }

    /**
     *
     * @param b if true, resource docs will be scanned for static content requests
     *
     */
    def useResourceDocs(b) {
        bUseResourceDocs = b;
        this
    }

    boolean useResourceDocs() {
        bUseResourceDocs
    }

    /**
     * Sets the delegates to use for this configuration.
     *
     * @param dtm Map( delegateType -> delegate )
     *
     */
    def delegateTypeMap(dtm) {
        delegateTypeMap = dtm;
        this
    }

    /**
     *
     */
    def port(int p) {
        this._defaultPort = p;
        this
    }

    /**
     *
     */
    def port() {
        this._defaultPort
    }

    def addResource(GServResource resource) {
        resource.actions.each this.&addAction
        this
    }

    def addResources(resources) {
        resources.each this.&addResource
        this
    }

    /**
     * Add actions to this Configuration
     *
     * @param List < ResourceAction >  List of Actions
     * @return GServConfig
     */
    def addActions(rlist) {
        actions(rlist);
    }

    def actions(rlist) {
        this._actions.addAll(rlist);
        this
    }

    /**
     * Add actions to this Configuration
     *
     * @param List < ResourceAction >  List of Actions
     * @return GServConfig
     */
    def addAction(actions) {
        this._actions.add(actions);
        this
    }

    def actions() {
        this._actions;
    }

    def addServerIP(serverIp) {
        this.serverIPs.add(serverIp);
        this
    }

    /**
     * Add staticRoots to this Configuration
     *
     * @param List < String >  List of directories
     * @return List < String >
     */
    def addStaticRoots(roots) {
        this._staticRoots.addAll(roots);
        this
    }

    def staticRoots() {
        _staticRoots
    }

    /**
     *
     * @param filters List<io.github.javaconductor.gserv.filters.Filter>
     * @return List < io.github.javaconductor.gserv.filters.Filter >
     */
    def addFilter(filter) {
        this._filters.add(filter);
        this
    }
    /**
     *
     * @param filters List<io.github.javaconductor.gserv.filters.Filter>
     * @return List < io.github.javaconductor.gserv.filters.Filter >
     */
    def addFilters(filters) {
        this._filters.addAll(filters);
        this;
    }

    def filters() { _filters }

    def defaultResource(def defResource) {
        _defaultResource = defResource
    }

    def defaultResource() {
        _defaultResource
    }

    def applyHttpsConfig(cfgObj) {
        if (cfgObj.password) {
            def httpsCfg = new HttpsConfig()
            httpsCfg.password = cfgObj.password
            httpsCfg.keyManagerAlgorithm = cfgObj.keyManagerAlgorithm
            httpsCfg.keyStoreFilePath = cfgObj.keyStoreFilePath ?: (System.getProperty("user.home") + "/gserv.keystore")

            httpsCfg.keyStoreImplementation = cfgObj.keyStoreImplementation
            httpsCfg.trustManagerAlgorithm = cfgObj.trustManagerAlgorithm
            httpsCfg.sslProtocol = cfgObj.sslProtocol
            httpsConfig(httpsCfg)
        } else {
            throw new IllegalArgumentException("Password is required for HTTPS.")
        }

    }
}

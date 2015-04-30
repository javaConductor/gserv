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

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.converters.InputStreamTypeConverter
import io.github.javaconductor.gserv.delegates.DefaultDelegates
import io.github.javaconductor.gserv.delegates.DelegatesMgr
import io.github.javaconductor.gserv.pathmatching.Matcher
import io.github.javaconductor.gserv.resources.GServResource
import io.github.javaconductor.gserv.filters.Filter
import io.github.javaconductor.gserv.requesthandler.RequestContext
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
 * @author javaConductor
 *
 * gServ Server Instance Configuration
 *
 */
@AutoClone(style = AutoCloneStyle.CLONE)
class GServConfig {

    private def _name = 'gserv App';
    private def _actions = [];
    private def _staticRoots = [], _filters = [], _authenticator,
        _templateEngineName, _defaultPort
    private def _defaultResource
    private InetSocketAddress _bindAddress
    private StaticFileHandler _staticFileHandler = new StaticFileHandler()
    private HttpsConfig _https
    def delegateTypeMap
    boolean bUseResourceDocs
    def delegateMgr
    def linkBuilder
    Matcher matcher = new Matcher()
    def serverIPs = []
    def _statusPage = true
    String _statusPath = "/status"
    InputStreamTypeConverter inputStreamTypeConverter = new InputStreamTypeConverter()

    GServConfig() {
        this.delegateManager(new DelegatesMgr(DefaultDelegates.getDelegates()))
    }

    /**
     *
     * @return the BindAddress for Instances using this gServ Config
     */
    InetSocketAddress bindAddress() {
        return _bindAddress;// ?: new Inet4Address('0.0.0.0', null)
    }

    /**
     *
     * @param addr
     * @return 'this' - Fluent Interface
     */
    GServConfig bindAddress(InetSocketAddress addr) {
        /// always use 0.0.0.0 for the default
        _bindAddress = addr;//?: new InetSocketAddress()  ('0.0.0.0', null);
        return this;
    }

    String name() {
        _name
    }

    GServConfig name(nm) {
        _name = nm
        this
    }

    GServConfig httpsConfig(HttpsConfig httpsConfig) {
        _https = httpsConfig
        this
    }

    def httpsConfig() {
        _https
    }

    def https() {
        !!_https
    }

    GServConfig authenticator(authenticator) {
        this._authenticator = authenticator;
        this
    }

    def authenticator() { _authenticator }

    GServConfig delegateManager(delegateMgr) {
        this.delegateMgr = delegateMgr;
        this
    }

    def delegateManager() { this.delegateMgr }

    GServConfig linkBuilder(linkBuilder) {
        this.linkBuilder = linkBuilder;
        this
    }

    LinkBuilder linkBuilder() {
        this.linkBuilder = this.linkBuilder ?: new LinkBuilder();
        this.linkBuilder
    }

    ResourceAction matchAction(RequestContext context) {
        matcher.matchAction(_actions, context)
    }

    boolean requestMatched(RequestContext context) {
        !!(matchAction(context) || _staticFileHandler.resolveStaticResource(
                context.requestURI.path,
                _staticRoots,
                bUseResourceDocs))
    }

    /**
     * Sets the template engine for this configuration
     *
     * @param ten Template Engine Name
     *
     */
    GServConfig templateEngineName(ten) {
        _templateEngineName = ten;
        this
    }

    def templateEngineName() { _templateEngineName }

    /**
     *
     * @param b if true, resource docs will be scanned for static content requests
     *
     */
    GServConfig useResourceDocs(boolean b) {
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
    GServConfig delegateTypeMap(Map dtm) {
        delegateTypeMap = dtm;
        this
    }

    /**
     *
     */
    GServConfig port(int p) {
        this._defaultPort = p;
        this
    }

    /**
     *
     */
    int port() {
        this._defaultPort
    }

    GServConfig addResource(GServResource resource) {
        resource.actions.each this.&addAction
        this
    }

    GServConfig addResources(List<GServResource> resources) {
        resources.each this.&addResource
        this
    }

    /**
     * Add actions to this Configuration
     *
     * @param List < ResourceAction >  List of Actions
     * @return GServConfig
     */
    GServConfig addActions(List<ResourceAction> rlist) {
        actions(rlist);
        this
    }

    GServConfig actions(List<ResourceAction> rlist) {
        this._actions.addAll(rlist);
        this
    }

    /**
     * Add actions to this Configuration
     *
     * @param List < ResourceAction >  List of Actions
     * @return GServConfig
     */
    GServConfig addAction(ResourceAction action) {
        this._actions.add(action);
        this
    }

    List<ResourceAction> actions() {
        this._actions;
    }

    GServConfig addServerIP(String serverIp) {
        this.serverIPs.add(serverIp);
        this
    }

    /**
     * Add staticRoots to this Configuration
     *
     * @param List < String >  List of directories
     * @return List < String >
     */
    GServConfig addStaticRoots(List<String> roots) {
        this._staticRoots.addAll(roots);
        this
    }

    List<String> staticRoots() {
        _staticRoots
    }

    /**
     *
     * @param filters List<io.github.javaconductor.gserv.filters.Filter>
     * @return List < io.github.javaconductor.gserv.filters.Filter >
     */
    GServConfig addFilter(Filter filter) {
        this._filters.add(filter);
        this
    }
    /**
     *
     * @param filters List<io.github.javaconductor.gserv.filters.Filter>
     * @return List < io.github.javaconductor.gserv.filters.Filter >
     */
    GServConfig addFilters(List<Filter> filters) {
        this._filters.addAll(filters);
        this;
    }

    List<Filter> filters() { _filters }

    GServConfig defaultResource(String defResource) {
        _defaultResource = defResource
        this
    }

    String defaultResource() {
        _defaultResource
    }

    GServConfig applyHttpsConfig(cfgObj) {
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

    def converter() { this.inputStreamTypeConverter }

    def converter(InputStreamTypeConverter inputStreamTypeConverter) {
        this.inputStreamTypeConverter = inputStreamTypeConverter
        this
    }

    boolean statusPage() {
        _statusPage
    }

    GServConfig statusPage(boolean b) {
        _statusPage = b
        this
    }

    String statusPath() {
        _statusPath
    }

    GServConfig statusPath(String s) {
        _statusPath = s
        this
    }

}

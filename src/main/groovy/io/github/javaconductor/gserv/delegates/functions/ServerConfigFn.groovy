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

package io.github.javaconductor.gserv.delegates.functions

import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.resources.GServResource
import io.github.javaconductor.gserv.factory.ResourceActionFactory
import io.github.javaconductor.gserv.resources.ResourceObject
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.filters.FilterOptions
import io.github.javaconductor.gserv.utils.LinkBuilder
import io.github.javaconductor.gserv.utils.StaticFileHandler
import org.apache.commons.io.IOUtils

/**
 *
 * @author javaConductor
 */
trait ServerConfigFn {

    def name(nm) {
        this.value "name", nm
        this
    }

    def name() {
        this.value("name")
    }

    def addAction(action) {
        this.value("actionList").add(action)
        this
    }

    def addResource(GServResource resource) {
        resource.actions.each(this.&addAction)
        this
    }

    def addFilter(f) {
        value("filterList").add(f)
        this
    }

    def addStaticRoot(r) {
        value("staticRoots").add(r)
        this
    }

    def addLink(name, action) {
        value("linkBuilder").add(name, action)
        this
    }

    /**
     * Imports a Resource into a ServerInstance
     *
     * @param r The Resource to import
     *
     */
    def resource(GServResource r) {
        if (r instanceof ResourceObject) {
            r = GServResource.Resource(r.basePath, r)
        }
        addResource(r)
        addLinkBuilder r.linkBuilder
        this
    }

    private addLinkBuilder(LinkBuilder lb) {
        value("linkBuilder", value("linkBuilder") + lb)
        this
    }

    /**
     *
     * @return true if using resource Docs else false
     */
    def useResourceDocs() {
        value("useResourceDocs")
    }
    /**
     * Allows/disallows the use of classPath resources inside the jar file.
     *
     * @param b boolean
     * @return
     */
    def useResourceDocs(boolean b) {
        value("useResourceDocs", b)
    }

    /**
     * Creates a named filter for this url/method combination.
     *
     * @param name The name of this filter - for logging and debugging
     * @param url URL on which this filter will be applied
     * @param method HttpMethod -> GET, POST, PUT, DELETE
     * @param clozure Filter behavior
     * @return
     */
    def filter(name, url, method, clozure) {
        filter(name, url, method, [:], clozure)
    }

    /**
     * Creates a named filter for this url/method combination.
     *
     * @param name The name of this filter - for logging and debugging
     * @param url URL on which this filter will be applied
     * @param method HttpMethod -> GET, POST, PUT, DELETE
     * @param options Filter Options:
     *                   passRouteParams :boolean -> if true the path variables will be passed to 'clozure'
     * @param clozure Filter behavior
     *
     */
    def filter(name, url, method, options, clozure, order) {
        method = method ?: '*'
        //println("serverInitClosure: filter($name) $method, url=$url")
        addFilter(ResourceActionFactory.createFilter(name, method, url, options, clozure))
    }

    /**
     *
     * @param name
     * @param url
     * @param method
     * @param options
     * @param clozure fn(requestContext, byte[] data)
     * @return
     *
     */
    def after(name, url, method, options, order, clozure) {
        method = method ?: '*'
        //println("serverInitClosure: after($name) $method, url=$url, list=${this._filterList}")
        def theFilter = ResourceActionFactory.createAfterFilter(name, method, url, options, order, clozure)
        addFilter(theFilter)
        this
    }

    /**
     *
     * @param name
     * @param url
     * @param method
     * @param options
     * @param clozure fn(requestContext, byte[] data)
     * @return this
     */
    def before(name, url, method, options, order, clozure) {
        method = method ?: '*'
        def theFilter = ResourceActionFactory.createBeforeFilter(name, method, url, options, order, clozure)
        addFilter(theFilter)
        this
    }

/**
 *
 * @param methods
 * @param path
 * @param realm
 * @param challengeFn
 * @return
 */
    def basicAuthentication(methods, path, realm, challengeFn) {

        assert challengeFn;

        def options = [(FilterOptions.PassRouteParams): false, (FilterOptions.MatchedActionsOnly): true];
        methods.each { method ->
            before("basicAuth($method->$path)", path, method, options, 2) { ->
                log.trace("basicAuth before()");

                def userPswd = getBasicAuthUserPswd(requestContext);
                log.trace("basicAuth before(): userPswd:$userPswd");
                if (!userPswd || userPswd.length < 2) {
                    log.trace("basicAuth before(): userPswd:$userPswd");
                    (requestContext).responseHeaders.put("WWW-Authenticate", "Basic realm=$realm")
                    error(401, "Authentication Required");
                } else {
                    def bAuthenticated = _authenticated(userPswd[0], userPswd[1], requestContext, challengeFn);
                    log.trace("basicAuth after(): Auth: ${bAuthenticated ? 'Successful' : 'Failed'}!");
                    if (!bAuthenticated) {
                        error(403, "Bad credentials for path ${requestContext.requestURI.path}.");
                    }
                }
                return (requestContext);
            }
        }
    }//basicAuthentication

    def getBasicAuthUserPswd(requestContext) {
        def basic = requestContext.requestHeaders.get("Authorization");
        //println "basic: $basic"
        if (!basic)
            return null;
        basic = basic[0];// we get a list as response but we only need the first one
        //" Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
        def base64 = basic.trim().substring(6);
        def authString = new String(base64.toString().decodeBase64());
        return authString.split(':')
    }

    private boolean _authenticated(user, pswd, requestContext, challengeFn) {
        return (challengeFn(user, pswd, requestContext));
    }

    /**
     * Creates a closure that returns file 'filename'
     *
     * @param filename
     * @return
     */
    def file(filename) {
        file(null, filename)
    }

    /**
     * Creates a closure that writes file 'filename' to the outputStream
     *
     * @param mimeType Optional contentType
     * @param filename
     * @return Closure that will return the file
     *
     */
    def file(mimeType, filename) {
        def staticHandler = new StaticFileHandler()
        if (!mimeType) {
            mimeType = URLConnection.guessContentTypeFromName(filename)
        }

        { ->
            EventManager.instance().publish(Events.ResourceProcessing, [
                    requestId: requestContext.getAttribute(GServ.contextAttributes.requestId),
                    mimeType : mimeType,
                    msg      : "Sending static file.",
                    path     : "$filename"])
            /// search the staticRoots
            InputStream is = getFile(value('staticRoots'), filename)
            if (is) {
                def sz = is.available();
                requestContext.responseHeaders.put("Content-Type", mimeType)
                requestContext.sendResponseHeaders(200, sz)
                IOUtils.copy(is, requestContext.responseBody)
            } else {
                def msg = "No such file: $filename"
                def ab = msg.getBytes()
                requestContext.sendResponseHeaders(404, ab.size())
                requestContext.responseBody.write(ab);
            }
            requestContext.responseBody.close();
            requestContext.close()
            requestContext
        }
    }

    /**
     * Declares a directory from which to serve static content.
     *
     * @param directory
     * @return
     */
    def static_root(directory) {
        addStaticRoot(directory)
    }

    def conversion(Class c, Closure fn) {
        value('inputStreamTypeConverter').add(c, fn)
    }
}

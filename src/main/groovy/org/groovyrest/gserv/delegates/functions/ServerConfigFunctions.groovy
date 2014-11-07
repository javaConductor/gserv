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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.groovyrest.gserv.delegates.functions

import groovy.util.logging.Log4j
import org.groovyrest.gserv.GServ
import org.groovyrest.gserv.ResourceObject
import org.groovyrest.gserv.RouteFactory
import org.groovyrest.gserv.events.EventManager
import org.groovyrest.gserv.events.Events
import org.groovyrest.gserv.filters.FilterOptions
import org.groovyrest.gserv.GServResource
import org.groovyrest.gserv.utils.LinkBuilder
import org.groovyrest.gserv.utils.StaticFileHandler
import org.apache.commons.io.IOUtils
import sun.misc.BASE64Decoder

/**
 *
 * @author lcollins
 */
@Log4j
class ServerConfigFunctions {

    def addRoute(rte) {
        this.value("routeList").add(rte)
        //println("addRoute($rte)")
    }

    def addResource(GServResource resource) {
        resource.routes.each(this.&addRoute)
    }

    def addFilter(f) {
        value("filterList").add(f)
    }

    def addStaticRoot(r) {
        value("staticRoots").add(r)
    }

    def addLink(name, route) {
        value("linkBuilder").add(name, route)
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
    }

    private addLinkBuilder(LinkBuilder lb) {
        value("linkBuilder", value("linkBuilder") + lb)
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
    def filter(name, url, method, options, clozure, order = 5) {
        method = method ?: '*'
        //println("serverInitClosure: filter($name) $method, url=$url")
        addFilter(RouteFactory.createFilterURLPattern(name, method, url, options, clozure))
    }

    /**
     *
     * @param name
     * @param url
     * @param method
     * @param options
     * @param clozure fn(exchange, byte[] data)
     * @return
     *
     */
    def after(name, url, method, options, order = 5, clozure) {
        method = method ?: '*'
        //println("serverInitClosure: after($name) $method, url=$url, list=${this._filterList}")
        def theFilter = RouteFactory.createAfterFilterURLPattern(name, method, url, options, order, clozure)
        addFilter(theFilter)
        this
    }

    /**
     *
     * @param name
     * @param url
     * @param method
     * @param options
     * @param clozure fn(exchange, byte[] data)
     * @return this
     */
    def before(name, url, method, options, order = 5, clozure) {
        method = method ?: '*'
        def theFilter = RouteFactory.createBeforeFilterURLPattern(name, method, url, options, order, clozure)
        addFilter(theFilter)
        this
    }

    def basicAuthentication(methods, path, realm, challengeFn) {

        assert challengeFn;

        def options = [(FilterOptions.PassRouteParams): false, (FilterOptions.MatchedRoutesOnly): true];
        methods.each { method ->
            before("basicAuth($method->$path)", path, method, options, 2) { ->
                log.trace("basicAuth before()");
//                exchange.requestHeaders.keySet().each {
//                    println "h: $it -> ${exchange.requestHeaders[it]}";
//                }
                def userPswd = getBasicAuthUserPswd(exchange);
                log.trace("basicAuth before(): userPswd:$userPswd");
                if (!userPswd) {
                    exchange.responseHeaders.add("WWW-Authenticate", "Basic realm=$realm")
                    error(401, "Authentication Required");
                } else {
                    def bAuthenticated = _authenticated(userPswd[0], userPswd[1], challengeFn);
                    if (bAuthenticated) {
                        nextFilter();
                        return (exchange);
                    } else {
                        error(403, "Bad credentials for path ${exchange.requestURI.path}.");
                    }
                }
            }
        }
        this
    }//basicAuthentication

    def getBasicAuthUserPswd(exchange) {
        def basic = exchange.requestHeaders.get("Authorization");
        //println "basic: $basic"
        if (!basic)
            return null;
        basic = basic[0];// we get a list as response but we only need the first one
        //" Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
        def base64 = basic.trim().substring(6);
        def authString = new String(new BASE64Decoder().decodeBuffer(base64));
        return authString.split(':')
    }

    private boolean _authenticated(user, pswd, challengeFn) {
        return (challengeFn(user, pswd));
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
                    requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                    mimeType : mimeType,
                    msg      : "Sending static file.",
                    path     : "$filename"])
            /// search the staticRoots
            InputStream is = getFile(value('staticRoots'), filename)
            if (is) {
                def sz = is.available();
                exchange.responseHeaders.add("Content-Type", mimeType)
                exchange.sendResponseHeaders(200, sz)
                IOUtils.copy(is, exchange.responseBody)
            } else {
                def msg = "No such file: $filename"
                def ab = msg.getBytes()
                exchange.sendResponseHeaders(404, ab.size())
                exchange.responseBody.write(ab);
            }
            exchange.responseBody.close()
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

}


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

package org.groovyrest.gserv.delegates.functions

import org.groovyrest.gserv.RouteFactory

/**
 *
 * @author lcollins
 */
class ResourceFunctions {

    /**
     *
     * @param name
     * @param url
     * @param clozure
     * @return
     */
    def handle(String name, url, clozure) {
        if (name.equalsIgnoreCase("get")) {
            get(url, clozure)
        } else if (name.equalsIgnoreCase("put")) {
            put(url, clozure)
        } else if (name.equalsIgnoreCase("post")) {
            post(url, clozure)
        } else if (name.equalsIgnoreCase("delete")) {
            delete(url, clozure)
        } else throw new IllegalArgumentException("HTTP method $name either not supported or no such method.")

    }
    /**
     *
     * @param methods
     * @param url
     * @param clozure
     * @return
     */
    def handle(List<String> methods, url, clozure) {
        methods.each { methodName ->
            handle(methodName, url, clozure)
        }
    }
    /**
     * Declares a handler for an HTTP GET request for the specified URL.
     *
     * @param url /example/:name/:age
     * @param clozure clozure(name, age )
     * @return
     */
    def get(url, clozure) {
        get(null, url, clozure)
    }

    /**
     * Declares a handler for an HTTP GET request for the specified URL.
     *
     * @param routeName optional name of the Route
     * @param url /example/:name/:age
     * @param clozure clozure(name, age )
     * @return
     */
    def get(routeName, url, clozure) {
        log.trace("serverInitClosure: get ${routeName ?: ''} base=${(value("path"))} url=$url")
        def rte = _addUrlToPatternList("GET", url, clozure)
        if (routeName)
            value("linkBuilder").addLink(routeName, rte)
        rte
    }

    /**
     * Declares a handler for an HTTP PUT request for the specified URL.
     *
     * @param url /example/:name/:age
     * @param clozure clozure(inputStream, name, age )
     * @return
     */
    def put(url, clozure) {
        log.trace("serverInitClosure: put url=$url")
        def rte = _addUrlToPatternList("PUT", url, clozure)
        //if(routeName) linkBuilder.add(routeName, rte)
        rte
    }

    /**
     * Declares a handler for an HTTP POST request for the specified URL.
     *
     * @param url /example/:name/:age
     * @param clozure clozure(inputStream, name, age )
     * @return
     */
    def post(url, clozure) {
        log.trace("serverInitClosure: post url=$url")
        _addUrlToPatternList("POST", url, clozure)
    }

    /**
     * Declares a handler for an HTTP DELETE request for the specified URL.
     *
     * @param url /example/:name/:age
     * @param clozure clozure(name, age )
     * @return
     */
    def delete(url, clozure) {
        log.trace("serverInitClosure: delete url=$url")
        _addUrlToPatternList("DELETE", url, clozure)
    }

    def _addUrlToPatternList(method, url, clozure) {
        def absUrl = value("path") ?: "";
        if (url) {
            if (!url.startsWith("/")) {
                url = ("/" + url)
            }
            absUrl += url
        }
        log.trace "resourceInitClosure: _addUrlToPatternList url=$absUrl".toString();

        def rte = RouteFactory.createURLPattern(method, absUrl, clozure)
        value("routeList").add(rte)
        rte
    }

}


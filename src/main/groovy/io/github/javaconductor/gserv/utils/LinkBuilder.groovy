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

package io.github.javaconductor.gserv.utils

import io.github.javaconductor.gserv.actions.ActionPathElement
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.pathmatching.PathMatchingUtils
import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 * A LinkBuilder is used to create URLs to resources
 *
 */
class LinkBuilder {
    def prefix
    def actions = [:]
    private def _linksFunctions = [:]

    def LinkBuilder(String prefix = null) {
        this.prefix = prefix
    }

    def LinkBuilder(String prefix, Map actions) {
        this(prefix)
        this.actions = actions
    }

    /**
     * Creates a new LinkBuilder combining actions and retaining the prefix from 'this'
     *
     * @param lb LinkBuilder to merge
     * @return New LinkBuilder
     */
    LinkBuilder plus(LinkBuilder lb) {
        lb ? new LinkBuilder(prefix: prefix,
                actions: actions + lb.actions,
                _linksFunctions: _linksFunctions + lb._linksFunctions)
                : this
    }

    /**
     * Add route by name
     *
     * @param name The name LinkBuilder will associate with the Route.
     * @param action
     * @return
     */
    @Deprecated
    def add(String name, ResourceAction action) {
        addLink(name, action)
    }
    /**
     * Add action by name
     *
     * @param name The name LinkBuilder will associate with the Action.
     * @param route
     * @return
     */
    def addLink(String name, ResourceAction action) {
        actions[name] = action
    }

    /**
     *
     * @param name
     * @param c
     * @return
     */
    def addLinksFunction(String name, Closure c) {
        _linksFunctions[name] = c
    }

    List linksFunctions() {
        _linksFunctions.collect { name, fn ->
            [name: name, linksFunction: fn]
        }
    }

    /**
     *
     * @param name Route name
     * @param data Data used to interpolate the resulting URL
     * @return URI to a named route
     */
    String link(String name, Map data) {
        // find the action in actions
        ResourceAction action = actions[name]
        if (!action) {
            throw new IllegalArgumentException("No such action '$name'. Be sure to add a value for 'actionName' in your action definition.")
        }

        // loop thru the path and on non-var we append it and on var we append the value from data
        def link = ""
        for (int i = 0; i != action.pathSize(); ++i) {
            ActionPathElement element = action.path(i)
            if (element._isVar) {
                def d = data[element.variableName()]
                if (!d) {
                    throw new IllegalArgumentException("${element.variableName()} not found in data. data must contain values for all action variables. ")
                }
                link += "/$d"
            } else {
                link += "/${element.text()}"
            }
        }

        action.queryPattern().queryMap().eachWithIndex { it, i ->
            def k = it.key, v = it.value

            if (PathMatchingUtils.isMatchingPattern(v)) {
                def d = data[k]
                if (!d) {
                    throw new IllegalArgumentException("${k} not found in data. data must contain values for all action variables. ")
                }
                link += ((i == 0 ? '?' : '&') + "$k=$d")

            } else if (PathMatchingUtils.isValuePattern(v)) {
                link += ((i == 0 ? '?' : '&') + "$k=$v")
            }
        }
        link
    }

    static List expandLinksIfNeeded(RequestContext rc, List links) {
        def protocol = rc.protocol
        def host = rc.localAddress.hostName == '0:0:0:0:0:0:0:1' ? 'localhost' : rc.localAddress.hostName
        def port = rc.localAddress.port

        links.collect { link ->
            def href = link.href
            href = expandLinkIfNeeded(protocol, host, port, href)
            [
                    href  : href,
                    method: link.method,
                    rel   : link.rel
            ]
        }

    }

    static def expandLinkIfNeeded(String protocol, String host, int port, String href) {
        URI uri = new URI(href)
        def newProtocol = uri.scheme ?: protocol.split('/')[0]
        def newHost = uri.host ?: host
        def newPort = uri.port > 0 ? uri.port : port
        def newPath = "${uri.path}?${uri.query ?: ''}"

        new URL(newProtocol, newHost, newPort, newPath).toExternalForm()
    }

}

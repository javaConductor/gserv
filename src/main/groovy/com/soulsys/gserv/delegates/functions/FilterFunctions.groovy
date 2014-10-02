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

package com.soulsys.gserv.delegates.functions

import com.soulsys.gserv.utils.StaticFileHandler

/**
 *
 * @author lcollins
 */
@Mixin(StaticFileHandler)
class FilterFunctions {

    /**
     *
     * @param code
     * @param message
     */
    void error(int code, String message) {
        message = message ?: "Error!"
        println "requestHandlerDelegate.error($code, $message)"
        exchange.sendResponseHeaders(code, message.bytes.size())
        exchange.getResponseBody().write(message.bytes)
        exchange.getResponseBody().close()
    }

    /**
     * Sends a HTTP Redirect (302) w/ updated 'Location' header
     * @param url
     */
    void redirect(url) {
        def message = "Resource has moved to: $url"
        exchange.getHeaders().add("Location", url)
        exchange.sendResponseHeaders(302, message.bytes.size())
        exchange.getResponseBody().write(message)
        exchange.getResponseBody().close()
    }


    def matchedRoute(exchange) {
        value("serverConfig").matchRoute(exchange)
    }

    def requestMatched(exchange) {
        value("serverConfig").matchRoute(exchange) || resolveStaticResource(
                exchange.requestURI.path,
                value("serverConfig").staticRoots,
                value("serverConfig").useResourceDocs())
    }


    def _nextFilterCalled = false

    def nextFilter() {
        if (_nextFilterCalled)
            throw new IllegalStateException("nextFilter() has already been called.")
        value("chain").doFilter(exchange)
        _nextFilterCalled = true
    }
}


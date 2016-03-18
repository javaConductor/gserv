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

package io.github.javaconductor.gserv.requesthandler

import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events

/**
 * Created by lcollins on 12/26/2014.
 */
@Slf4j
class Jdk16RequestContext extends AbstractRequestContext {

    HttpExchange _exchange
    boolean _closed = false

    Jdk16RequestContext(GServConfig config, HttpExchange exchange) {
        super(config)
        assert exchange
        this.requestBody = exchange.requestBody
        this.responseBody = exchange.responseBody
        this.requestHeaders = exchange.requestHeaders as Map
        this.responseHeaders = exchange.responseHeaders as Map
        this.requestURI = exchange.requestURI
        this.requestMethod = exchange.requestMethod
        this.localAddress = exchange.localAddress
        this.remoteAddress = exchange.remoteAddress
        this.principal = exchange.principal
        this.protocol = exchange.protocol
        _exchange = exchange
    }

    def isClosed() {
        _closed
    }

    /**
     *
     * @param responseCode
     * @param size
     */
    void sendResponseHeaders(int responseCode, long size) {
        if (!_closed) {
            _exchange.responseHeaders.putAll(responseHeaders)
            log.trace("Sending headers ($this) : ${_exchange.responseHeaders} ")
            _exchange.sendResponseHeaders(responseCode, size)
        } else {
            log.warn("sendResponseHeaders() called twice.")
        }
    }

    def setStreams(InputStream is, OutputStream os) {
        requestBody = is
        responseBody = os
    }

    @Override
    def close() {
        if (!_closed) {
            _exchange.close()
            _exchange.setAttribute(GServ.contextAttributes.requestContext, null)

            EventManager.instance().publish(Events.ResourceProcessed, [
                    requestId: id(),
                    requestContext: this,
                    when     : new Date()])
        }
        _closed = true
    }

    String dump() {
        ""
    }

    Object nativeObject() {
        _exchange
    }

    @Override
    String toString() {
        "#${id()} -> $requestMethod:$requestURI"
        return super.toString()
    }

}

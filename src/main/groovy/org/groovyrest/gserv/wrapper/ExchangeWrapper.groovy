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

package org.groovyrest.gserv.wrapper

import org.groovyrest.gserv.GServ
import org.groovyrest.gserv.events.EventManager
import org.groovyrest.gserv.events.Events
import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpContext
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpPrincipal

/**
 * Exchange Wrapper usually used for Filters
 */
class ExchangeWrapper extends HttpExchange {
    HttpExchange _exchange
    Headers _requestHdrs, _responseHdrs
    URI _uri
    int _code
    OutputStream _originalOutputStream
    InputStream _originalInputStream
    boolean _closed

    def ExchangeWrapper(HttpExchange exchange) {
        if (!exchange)
            throw new IllegalArgumentException("exchange must NOT be null. Should be valid HttpExchange impl.")
        _exchange = exchange
        _requestHdrs = _exchange.requestHeaders
        _responseHdrs = _exchange.responseHeaders
        _responseHdrs.putAll(_exchange.responseHeaders)
        _originalOutputStream = _exchange.responseBody
        _originalInputStream = _exchange.requestBody
        _uri = _exchange.requestURI
        setAttribute(GServ.exchangeAttributes.isWrapper, true)
    }

    def originalOutputStream() { _originalOutputStream }

    def originalInputStream() { _originalInputStream }

    @Override
    Headers getRequestHeaders() {
        return _requestHdrs
    }

    @Override
    Headers getResponseHeaders() {
        return _responseHdrs
    }

    @Override
    URI getRequestURI() {
        return _uri
    }

    /**
     * Should not change the host - just the path
     *
     * @param uri
     * @return
     */
    def setRequestURI(URI uri) {
        _uri = uri
    }

    @Override
    String getRequestMethod() {
        return _exchange.getRequestMethod()
    }

    @Override
    HttpContext getHttpContext() {
        return _exchange.getHttpContext()
    }

    @Override
    void close() {
        _closed = true
//        _exchange.close()
    }

    @Override
    InputStream getRequestBody() {
        return _exchange.requestBody
    }

    @Override
    OutputStream getResponseBody() {
        return _exchange.responseBody
    }

    @Override
    void sendResponseHeaders(int statusCode, long dataLength) throws IOException {

        /// must NOT send anything YET
        _code = statusCode
//        _exchange.sendResponseHeaders(statusCode, dataLength)
    }

    @Override
    InetSocketAddress getRemoteAddress() {
        return _exchange.remoteAddress
    }

    @Override
    int getResponseCode() {
        return _code
    }

    @Override
    InetSocketAddress getLocalAddress() {
        return _exchange.localAddress
    }

    @Override
    String getProtocol() {
        return _exchange.protocol
    }

    @Override
    Object getAttribute(String s) {
        _exchange.getAttribute(s)
    }

    @Override
    void setAttribute(String s, Object o) {
        _exchange.setAttribute(s, o)
    }

    @Override
    void setStreams(InputStream inputStream, OutputStream outputStream) {
        /// swap the streams on underlying exchange
        /// this.requestBody = inputStream
        /// this.responseBody = outputStream
        _exchange.setStreams(inputStream, outputStream)
    }

    @Override
    HttpPrincipal getPrincipal() {
        return _exchange.getPrincipal()
    }

    def isClosed = false
    /**
     * Sends the Headers and writes the bytes to the original stream
     * Return code from the intercepted sendResponseHeaders() call or 200 is used as the statusCode in the response
     *
     * @param bytes
     */
    synchronized def writeIt(bytes) {
        println "Wrapper.writeIt(): Writing response($_code) for req #${getAttribute(GServ.exchangeAttributes.requestId)} ${requestMethod}( ${requestURI.path} ) size=${bytes.size()}"
        if (!isClosed) {
            EventManager.instance().publish(Events.FilterProcessing, [
                    stream   : this.class.name,
                    requestId: _exchange.getAttribute(GServ.exchangeAttributes.requestId),
                    message  : "Writing ${bytes.size()} Bytes on stream.close()"])

            _exchange.responseHeaders.putAll this._responseHdrs
            _exchange.sendResponseHeaders(_code, bytes.size())
            try {
                originalOutputStream().write(bytes)
                println "Wrote response($_code) for req #${getAttribute(GServ.exchangeAttributes.requestId)} ${requestMethod}( ${requestURI.path}) size=${bytes.size()}"
            } catch (Throwable ex) {
                println "Error writing response($_code) for req #${getAttribute(GServ.exchangeAttributes.requestId)} ${requestURI.path} size=${bytes.size()} : Exception: ${ex.message}"
            } finally {
            }
            // println "Wrote response($_code) for ${requestURI.path} size=${bytes.size()}"
            originalOutputStream().close()
            if (_closed)
                _exchange.close()
            isClosed = true
        } else {
            EventManager.instance().publish(Events.FilterProcessing, [
                    stream   : this.class.name,
                    requestId: _exchange.getAttribute(GServ.exchangeAttributes.requestId),
                    message  : "Can't Write Bytes - already closed!"])
        }
    }
}

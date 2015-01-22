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

package io.github.javaconductor.gserv.requesthandler.wrapper

import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpContext
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpPrincipal
import io.github.javaconductor.gserv.filters.FilterByteArrayOutputStream

/**
 * Exchange Wrapper usually used for Filters
 */
@Log4j
class ExchangeWrapper extends HttpExchange {
    HttpExchange _exchange
    Headers _requestHdrs, _responseHdrs
    URI _uri
    int _code
    OutputStream _originalOutputStream, _responseBody
    InputStream _originalInputStream, _requestBody
    boolean _closed
    boolean testMode
    String _requestMethod;

    byte[] testData;

    def ExchangeWrapper(Map requestInfo) {
        testMode = true
        _exchange = this

        if (!requestInfo.uri)
            throw new IllegalArgumentException("'path' element is required.")
        _uri = requestInfo.uri

        if (!requestInfo.method)
            throw new IllegalArgumentException("'method' element is required.")
        _requestMethod = requestInfo.method

        if (requestInfo.headers) {
            requestInfo.entrySet().each { h ->
                _requestHdrs.add(h.key, h.value)
            }
        }

        if (requestInfo.inputData) {
            testData = requestInfo.inputData
            _requestBody = _originalInputStream = new ByteArrayInputStream(testData)
        } else {
            _requestBody = new ByteArrayInputStream(new byte[0])
        }

        _responseBody = new ByteArrayOutputStream()
        setAttribute(GServ.contextAttributes.isWrapper, true)
    }

    def ExchangeWrapper(HttpExchange exchange, URI uri = null) {
        if (!exchange)
            throw new IllegalArgumentException("exchange must NOT be null. Should be valid HttpExchange impl.")

        _exchange = exchange
        _requestHdrs = _exchange.requestHeaders
        _responseHdrs = _exchange.responseHeaders
        _responseHdrs.putAll(_exchange.responseHeaders)
        _originalOutputStream = _exchange.responseBody
        _requestMethod = _exchange.requestMethod
        _requestBody = _originalInputStream = _exchange.requestBody
        _responseBody = new FilterByteArrayOutputStream(defaultClose)
        _uri = uri ?: _exchange.requestURI
        setAttribute(GServ.contextAttributes.isWrapper, true)
    }
    def defaultClose = { _this ->
        writeIt(_responseBody.bytes)
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
        return _requestMethod
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

    /**
     * Done POORLY!!!
     * MUST wrap the stream or whats the point
     * @return
     */
    @Override
    InputStream getRequestBody() {
        return _requestBody
    }

    @Override
    OutputStream getResponseBody() {
        return _responseBody
    }

    @Override
    void sendResponseHeaders(int statusCode, long dataLength) throws IOException {

        /// must NOT send anything YET
        _code = statusCode
        //dataLength may change before the data is sent
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
        _requestBody = inputStream
        _responseBody = outputStream
        //lets not bother the original        _exchange.setStreams(inputStream, outputStream)
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
        log.trace "Wrapper.writeIt(): Writing response($_code) for req #${getAttribute(GServ.contextAttributes.requestId)} ${requestMethod}( ${requestURI.path} ) size=${bytes.size()}"
        if (!isClosed) {
            EventManager.instance().publish(Events.FilterProcessing, [
                    stream   : this.class.name,
                    requestId: _exchange.getAttribute(GServ.contextAttributes.requestId),
                    message  : "Writing ${bytes.size()} Bytes on stream.close()"])

            _exchange.responseHeaders.putAll this._responseHdrs
            _exchange.sendResponseHeaders(_code ?: 200, bytes.size())
            try {
                originalOutputStream().write(bytes)
                log.trace "Wrote response($_code) for req #${getAttribute(GServ.contextAttributes.requestId)} ${requestMethod}( ${requestURI.path}) size=${bytes.size()}"
            } catch (Throwable ex) {
                log.error "Error writing response($_code) for req #${getAttribute(GServ.contextAttributes.requestId)} ${requestURI.path} size=${bytes.size()} : Exception: ${ex.message}"
            }
            // println "Wrote response($_code) for ${requestURI.path} size=${bytes.size()}"
            originalOutputStream().close()
            if (_closed)
                _exchange.close()
            isClosed = true
        } else {
            EventManager.instance().publish(Events.FilterProcessing, [
                    stream   : this.class.name,
                    requestId: _exchange.getAttribute(GServ.contextAttributes.requestId),
                    message  : "Can't Write Bytes - already closed!"])
        }
    }
}

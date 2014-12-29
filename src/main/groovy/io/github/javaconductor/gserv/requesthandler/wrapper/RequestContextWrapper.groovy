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

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpContext
import com.sun.net.httpserver.HttpPrincipal
import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.filters.FilterByteArrayOutputStream
import io.github.javaconductor.gserv.requesthandler.AbstractRequestContext
import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 * Exchange Wrapper usually used for Filters
 */
@Log4j
class RequestContextWrapper extends AbstractRequestContext {
    RequestContext _context
    Map _requestHdrs=[:], _responseHdrs=[:]
    int _code
    OutputStream _originalOutputStream, _responseBody
    InputStream _originalInputStream
    boolean _closed
    String _requestMethod;
    def _wasClosed = false

    def RequestContextWrapper(RequestContext context, URI uri = null) {
        if (!context)
            throw new IllegalArgumentException("context must NOT be null. Should be valid RequestContext impl.")

        _context = context
        this.requestURI = _context.requestURI
        this.principal = _context.principal
        this.requestHeaders = _context.requestHeaders as  Map
        this.responseHeaders.putAll(_context.responseHeaders)
        this.requestMethod = _context.requestMethod
        _originalOutputStream = _context.responseBody
       _originalInputStream = _context.requestBody
        this.attributes = _context.attributes as Map
        this.responseCode = _context.responseCode

        this.remoteAddress = _context.remoteAddress
        this.localAddress = _context.localAddress

        this.responseBody =  _responseBody = new FilterByteArrayOutputStream(defaultClose)
        setAttribute(GServ.contextAttributes.isWrapper, true)
    }

    def defaultClose = { _this ->
        writeIt(_responseBody.toByteArray())
    }

    def originalOutputStream() { _originalOutputStream }

    def originalInputStream() { _originalInputStream }
    @Override
    def close() {
        _wasClosed = true
//        _context.close()
    }

    @Override
    void sendResponseHeaders(int statusCode, long dataLength) throws IOException {
        /// must NOT send anything YET
        _code = statusCode
        //dataLength may change before the data is sent
//        _context.sendResponseHeaders(statusCode, dataLength)
    }

    int getResponseCode() {
        return _code
    }

    @Override
    String dump() {
        attributes.toString()
    }

    @Override
    Object nativeObject() {
        _context.nativeObject()
    }

    @Override
    def setStreams(InputStream is, OutputStream os) {
        _context.setStreams(is ,os)
    }
/**
     * Sends the Headers and writes the bytes to the original stream
     * Return code from the intercepted sendResponseHeaders() call or 200 is used as the statusCode in the response
     *
     * @param bytes
     */
    synchronized def writeIt(bytes) {
        log.trace "Wrapper.writeIt(): Writing response($_code) for req #${getAttribute(GServ.contextAttributes.requestId)} ${requestMethod}( ${requestURI.path} ) size=${bytes.size()}"
        if (!_closed) {
            EventManager.instance().publish(Events.FilterProcessing, [
                    stream   : this.class.name,
                    requestId: _context.getAttribute(GServ.contextAttributes.requestId),
                    message  : "Writing ${bytes.size()} Bytes on stream.close()"])

            _context.responseHeaders.putAll this._responseHdrs
            _context.sendResponseHeaders(_code ?: 200, bytes.size())
            try {
                originalOutputStream().write(bytes)
                log.trace "Wrote response($_code) for req #${getAttribute(GServ.contextAttributes.requestId)} ) size=${bytes.size()}"
            } catch (Throwable ex) {
                log.error "Error writing response($_code) for req #${getAttribute(GServ.contextAttributes.requestId)} ${requestURI.path} size=${bytes.size()} : Exception: ${ex.message}"
            }
            // println "Wrote response($_code) for ${requestURI.path} size=${bytes.size()}"
            originalOutputStream().close()
            _closed = true
        } else {
            EventManager.instance().publish(Events.FilterProcessing, [
                    stream   : this.class.name,
                    requestId: _context.getAttribute(GServ.contextAttributes.requestId),
                    message  : "Can't Write Bytes - already closed!"])
        }
    }
}

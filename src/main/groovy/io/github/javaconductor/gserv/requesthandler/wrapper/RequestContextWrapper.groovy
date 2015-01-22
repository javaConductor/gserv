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
import io.github.javaconductor.gserv.configuration.GServConfig
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
    Map _requestHdrs = [:], _responseHdrs = [:]
    int _code
    InputStream _originalInputStream
    OutputStream _originalOutputStream, _responseBody
    def _wasClosed = false

    def RequestContextWrapper(RequestContext context) {
        if (!context)
            throw new IllegalArgumentException("context must NOT be null. Should be valid RequestContext impl.")

        if (context instanceof RequestContextWrapper)
            throw new IllegalArgumentException("context must NOT be Wrapper.")

        _context = context
        this.requestURI = _context.requestURI
        this.principal = _context.principal
        this.requestHeaders = _context.requestHeaders as Map
        this.responseHeaders.putAll(_context.responseHeaders)
        this.requestMethod = _context.requestMethod
        _originalOutputStream = _context.responseBody
        _originalInputStream = _context.requestBody
        this.requestBody = _context.requestBody
        this.attributes = _context.attributes as Map
        this.responseCode = _context.responseCode

        this.remoteAddress = _context.remoteAddress
        this.localAddress = _context.localAddress
        def currentReqId = getAttribute(GServ.contextAttributes.requestId)

        this.responseBody = _responseBody = new ByteArrayOutputStream()
        log.trace("RequestContext(#$currentReqId) -> $_context ")

        setAttribute(GServ.contextAttributes.isWrapper, true)
    }

    @Override
    def isClosed() {
        _wasClosed
    }

    def originalOutputStream() { _originalOutputStream }

    @Override
    def close() {
        def currentReqId = getAttribute(GServ.contextAttributes.requestId)
        if (_wasClosed) {
            log.warn("RequestContext(#$currentReqId) close() called multiple times.")
            return
        }
        log.debug("RequestContext(#$currentReqId) is closing... ")
        writeIt(_responseBody.toByteArray())
        _wasClosed = true
        log.debug("RequestContext(#$currentReqId) has been closed.")
    }

    void sendResponseHeaders(int statusCode, long dataLength) throws IOException {
        /// must NOT send anything YET
        _code = statusCode
        //dataLength may change before the data is sent
//        _context.sendResponseHeaders(statusCode, dataLength)
    }

    int getResponseCode() {
        return _code
    }

    String dump() {
        attributes.toString()
    }

    Object nativeObject() {
        _context.nativeObject()
    }

    def setStreams(InputStream is, OutputStream os) {
        _context.setStreams(is, os)
    }
/**
 * Sends the Headers and writes the bytes to the original stream
 * Return code from the intercepted sendResponseHeaders() call or 200 is used as the statusCode in the response
 *
 * @param bytes
 */
    synchronized def writeIt(bytes) {
        def currentReqId = getAttribute(GServ.contextAttributes.requestId)
        log.trace "Wrapper.writeIt(): Writing response($_code) for req #${currentReqId} ${requestMethod}( ${requestURI.path} ) size=${bytes.size()}"
        if (!_wasClosed) {
            EventManager.instance().publish(Events.FilterProcessing, [
                    stream   : this.class.name,
                    requestId: currentReqId,
                    message  : "Writing ${bytes.size()} Bytes on stream.close()"])

            _context.responseHeaders.putAll this._responseHdrs
            try {
                _context.sendResponseHeaders(_code ?: 200, bytes.size())
                originalOutputStream().write(bytes)
                log.trace "Wrote response($_code) for req #$currentReqId ) size=${bytes.size()}"
            } catch (Throwable ex) {
                log.error "Error writing response($_code) for req #${currentReqId} ${requestURI.path} size=${bytes.size()} : Exception: ${ex.message}"
            }
            // println "Wrote response($_code) for ${requestURI.path} size=${bytes.size()}"
            originalOutputStream().close()
            _context.close()
            _wasClosed = true
        } else {
            log.warn "Cannot write: Context already closed for req #$currentReqId ${requestURI.path} size=${bytes.size()}"

            EventManager.instance().publish(Events.FilterProcessing, [
                    stream   : this.class.name,
                    requestId: currentReqId,
                    message  : "Can't Write Bytes - already closed!"])
        }
    }// writeIt
    @Override
    String toString() {
        "#${id()} -> $requestMethod:$requestURI"
        return super.toString()
    }

    def id() {
        return attributes[GServ.contextAttributes.requestId]
    }

}//

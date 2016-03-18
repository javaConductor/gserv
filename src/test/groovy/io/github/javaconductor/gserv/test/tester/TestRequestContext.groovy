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

package io.github.javaconductor.gserv.test.tester

import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.Promise
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.factory.GServFactory
import io.github.javaconductor.gserv.requesthandler.AbstractRequestContext

/**
 * Context Wrapper used for unit testing.
 */
@Slf4j
class TestRequestContext extends AbstractRequestContext {
    OutputStream _responseBody
    def _wasClosed = false
    Closure _callBack

    def TestRequestContext(String method, Map headers, String path, byte[] data, Promise promise) {
        this(method, headers, path, data, promise, null)
    }

    def TestRequestContext(String method, Map headers, String path, byte[] data, Promise promise, Closure callBack) {
        super(new GServFactory().createGServConfig())
        this._callBack = ({ code, hdrs, bytesData ->
            promise << [statusCode: code, responseHeaders: hdrs, output: bytesData]
            if (callBack)
                callBack(code, hdrs, bytesData)
        });
        this.requestBody = new ByteArrayInputStream(data ?: new byte[0])
        this.responseBody = _responseBody = new ByteArrayOutputStream()
        setAttribute(GServ.contextAttributes.isWrapper, true)
        this.requestURI = new URI(path)
        this.requestMethod = method
        this.requestHeaders = headers as Map
    }

    @Override
    def close() {
        if (_wasClosed) {
            log.warn("RequestContext.close() called multiple times.")
            return
        }
        _wasClosed = true
        _callBack(responseCode, responseHeaders, _responseBody.toByteArray())
    }

    @Override
    def setStreams(InputStream is, OutputStream os) {
        this.requestBody = is
        this.responseBody = os
    }
    @Override
    void sendResponseHeaders(int responseCode, long size) {
        this.responseCode = responseCode
        /// really can't use the size - yet
    }

    @Override
    Object isClosed() {
        _wasClosed
    }


}

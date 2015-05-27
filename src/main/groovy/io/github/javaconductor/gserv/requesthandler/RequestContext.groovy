/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 - 2015 Lee Collins
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

package io.github.javaconductor.gserv.requesthandler

import io.github.javaconductor.gserv.configuration.GServConfig

import java.security.Principal

/**
 * Created by lcollins on 12/26/2014.
 */
interface RequestContext {

    Map<String, List> getRequestHeaders()

    String getRequestHeader(String header)

    void setRequestHeaders(Map<String, List> requestHeaders)

    Map<String, List> getResponseHeaders()

    String getResponseHeader(String header)

    void setResponseHeaders(Map<String, List> responseHeaders)

    void setResponseHeader(String header, String value)

    InputStream getRequestBody()

    void setRequestBody(InputStream requestBody)

    OutputStream getResponseBody()

    void setResponseBody(OutputStream responseBody)

    Map getAttributes()

    void setAttributes(Map attributes)

    String getRequestMethod()

    void setRequestMethod(String requestMethod)

    URI getRequestURI()

    void setRequestURI(URI requestURI)

    int getResponseCode()

    void setResponseCode(int responseCode)

    Principal getPrincipal()

    void setPrincipal(Principal principal)

    InetSocketAddress getLocalAddress()

    void setLocalAddress(InetSocketAddress localAddress)

    InetSocketAddress getRemoteAddress()

    void setRemoteAddress(InetSocketAddress remoteAddress)

    String getProtocol()

    void setProtocol(String protocol)

    Object getAttribute(String key)

    void setAttribute(String key, Object value)

    def close()

    def isClosed()

    def setStreams(InputStream is, OutputStream os)

    void sendResponseHeaders(int responseCode, long size)

    GServConfig config()

    def id()

    Map report()

/*
    String dump()
    Object nativeObject()
    */
}



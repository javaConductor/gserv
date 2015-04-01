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

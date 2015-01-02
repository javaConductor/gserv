package io.github.javaconductor.gserv.requesthandler

import java.security.Principal

/**
 * Created by lcollins on 12/26/2014.
 */
abstract class AbstractRequestContext implements RequestContext {

    Map<String,String> requestHeaders = [:]
    Map<String,String> responseHeaders = [:]
    InputStream requestBody
    OutputStream responseBody
    Map attributes = [:]
    String requestMethod
    URI requestURI
    int responseCode
    Principal principal;
    InetSocketAddress localAddress
    InetSocketAddress remoteAddress
    String protocol

    Map<String, String> getRequestHeaders() {
        return requestHeaders
    }

    void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders
    }

    Map<String, String> getResponseHeaders() {
        return responseHeaders
    }

    void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders
    }

    InputStream getRequestBody() {
        return requestBody
    }

    void setRequestBody(InputStream requestBody) {
        this.requestBody = requestBody
    }

    OutputStream getResponseBody() {
        return responseBody
    }

    void setResponseBody(OutputStream responseBody) {
        this.responseBody = responseBody
    }

    Map getAttributes() {
        return attributes
    }

    void setAttributes(Map attributes) {
        this.attributes = attributes
    }

    String getRequestMethod() {
        return requestMethod
    }

    void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod
    }

    URI getRequestURI() {
        return requestURI
    }

    void setRequestURI(URI requestURI) {
        this.requestURI = requestURI
    }

    int getResponseCode() {
        return responseCode
    }

    void setResponseCode(int responseCode) {
        this.responseCode = responseCode
    }

    Principal getPrincipal() {
        return principal
    }

    void setPrincipal(Principal principal) {
        this.principal = principal
    }

    InetSocketAddress getLocalAddress() {
        return localAddress
    }

    void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress
    }

    InetSocketAddress getRemoteAddress() {
        return remoteAddress
    }

    void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress
    }

    String getProtocol() {
        return protocol
    }

    void setProtocol(String protocol) {
        this.protocol = protocol
    }

    @Override
    Object getAttribute(String key) {
        return attributes[key]
    }

    @Override
    void setAttribute(String key, Object value) {
        attributes[key] = value
    }
    abstract void sendResponseHeaders( int responseCode, long size)
/*
    abstract def close()
    abstract String dump()
    abstract Object nativeObject();
    abstract  def setStreams(InputStream is, OutputStream os);
    */
    }

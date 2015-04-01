package io.github.javaconductor.gserv.requesthandler

import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.configuration.GServConfig

import java.security.Principal

/**
 * Created by lcollins on 12/26/2014.
 */
abstract class AbstractRequestContext implements RequestContext {

    Map<String, List> requestHeaders = [:]
    Map<String, List> responseHeaders = [:]
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
    GServConfig _config


    def AbstractRequestContext(GServConfig config) {
        _config = config
    }

    Map<String, List> getRequestHeaders() {
        return requestHeaders
    }

    void setRequestHeaders(Map<String, List> requestHeaders) {
        this.requestHeaders = requestHeaders
    }

    Map<String, List> getResponseHeaders() {
        return responseHeaders
    }

    String getResponseHeader(String header) {
        responseHeaders[header] ? responseHeaders[header][0] : null
    }

    void setResponseHeaders(Map<String, List> responseHeaders) {
        this.responseHeaders = responseHeaders
    }

    void setResponseHeader(String header, String value) {
        this.responseHeaders[header] = this.responseHeaders[header] ?: []
        this.responseHeaders[header] << value
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

    abstract void sendResponseHeaders(int responseCode, long size)

    def id() {
        return attributes[GServ.contextAttributes.requestId]
    }

    @Override
    Map report() {
        return [requestId: id(),
                path     : requestURI.path,
                query    : requestURI.query,
                method   : requestMethod,
                remoteIP : remoteAddress.address.toString(),
                closed   : isClosed()
        ]
    }

    GServConfig config() {
        this._config
    }

    @Override
    String toString() {
        "#${id()} -> $requestMethod:$requestURI"
//        return super.toString()
    }

}

package io.github.javaconductor.gserv.tester

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpContext
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpPrincipal

/**
 * Created by lcollins on 12/16/2014.
 */
class TestExchangeWrapper extends HttpExchange {
    Headers requestHeaders, responseHeaders
    URI uri
    int code
    OutputStream originalOutputStream, responseBody
    InputStream originalInputStream, requestBody
    boolean closed
    URI requestURI
    String requestMethod

    @Override
    URI getRequestURI() {
        return requestURI
    }

    @Override
    String getRequestMethod() {
        return requestMethod
    }

    @Override
    HttpContext getHttpContext() {
        return null
    }

    @Override
    void close() {

    }

    @Override
    void sendResponseHeaders(int i, long l) throws IOException {

    }

    @Override
    int getResponseCode() {
        return 0
    }

    @Override
    String getProtocol() {
        return null
    }

    @Override
    Object getAttribute(String s) {
        return null
    }

    @Override
    void setAttribute(String s, Object o) {

    }

    @Override
    void setStreams(InputStream inputStream, OutputStream outputStream) {

    }
    URI getUri() {
        return uri
    }

    void setUri(URI uri) {
        this.uri = uri
    }

    int getCode() {
        return code
    }

    void setCode(int code) {
        this.code = code
    }

    OutputStream getOriginalOutputStream() {
        return originalOutputStream
    }

    void setOriginalOutputStream(OutputStream originalOutputStream) {
        this.originalOutputStream = originalOutputStream
    }

    OutputStream getResponseBody() {
        return responseBody
    }

    void setResponseBody(OutputStream responseBody) {
        this.responseBody = responseBody
    }

    InputStream getOriginalInputStream() {
        return originalInputStream
    }

    void setOriginalInputStream(InputStream originalInputStream) {
        this.originalInputStream = originalInputStream
    }

    InputStream getRequestBody() {
        return requestBody
    }

    void setRequestBody(InputStream requestBody) {
        this.requestBody = requestBody
    }

    boolean getClosed() {
        return closed
    }

    void setClosed(boolean closed) {
        this.closed = closed
    }

    HttpPrincipal getPrincipal() {
        return principal
    }

    void setPrincipal(HttpPrincipal principal) {
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

    Headers getResponseHeaders() {
        return responseHeaders
    }

    void setResponseHeaders(Headers responseHeaders) {
        this.responseHeaders = responseHeaders
    }
    HttpPrincipal principal
    InetSocketAddress localAddress
    InetSocketAddress remoteAddress

    TestExchangeWrapper() {

    }
}

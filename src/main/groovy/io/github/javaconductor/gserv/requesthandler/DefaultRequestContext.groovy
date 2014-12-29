package io.github.javaconductor.gserv.requesthandler

import com.sun.net.httpserver.HttpExchange

/**
 * Created by lcollins on 12/26/2014.
 */
class DefaultRequestContext extends AbstractRequestContext {

    HttpExchange _exchange
    DefaultRequestContext(HttpExchange exchange){
        assert exchange
        this.requestBody = exchange.requestBody
        this.responseBody = exchange.responseBody
        this.requestHeaders = exchange.requestHeaders as Map
        this.responseHeaders = exchange.responseHeaders as Map
        this.requestURI = exchange.requestURI
        this.requestMethod = exchange.requestMethod
        this.localAddress = exchange.localAddress
        this.remoteAddress = exchange.remoteAddress
        this.principal = exchange.principal
        this.protocol = exchange.protocol
        _exchange = exchange
    }

    @Override
    void sendResponseHeaders(int responseCode, long size) {
        _exchange.sendResponseHeaders(responseCode,size)
    }

    @Override
    def setStreams(InputStream is, OutputStream os){
        requestBody = is
        responseBody = os
    }

    @Override
    def close() {
        _exchange.close()
    }

    @Override
    String dump() {
        ""
    }
    @Override
    Object nativeObject(){
        _exchange
    }

}

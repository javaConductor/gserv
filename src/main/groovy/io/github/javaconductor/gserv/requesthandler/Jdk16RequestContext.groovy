package io.github.javaconductor.gserv.requesthandler

import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.configuration.GServConfig

/**
 * Created by lcollins on 12/26/2014.
 */
@Log4j
class Jdk16RequestContext extends AbstractRequestContext {

    HttpExchange _exchange
    boolean _closed = false

    Jdk16RequestContext(GServConfig config, HttpExchange exchange) {
        super(config)
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

    def isClosed() {
        _closed
    }

    void sendResponseHeaders(int responseCode, long size) {
        if (!_closed) {
            _exchange.responseHeaders.putAll(responseHeaders)
            log.trace("Sending headers ($this) : ${_exchange.responseHeaders} ")
            _exchange.sendResponseHeaders(responseCode, size)
        }
        else {
            log.warn("sendResponseHeaders() called twice.")
        }
    }

    def setStreams(InputStream is, OutputStream os) {
        requestBody = is
        responseBody = os
    }

    @Override
    def close() {
        if (!_closed) {
            _exchange.close()
            _exchange.setAttribute(GServ.contextAttributes.requestContext, null)
        }
        _closed = true
    }

//    GServConfig config(){
//        _config
//    }

    String dump() {
        ""
    }

    Object nativeObject() {
        _exchange
    }

    @Override
    String toString() {
        "#${id()} -> $requestMethod:$requestURI"
        return super.toString()
    }

}

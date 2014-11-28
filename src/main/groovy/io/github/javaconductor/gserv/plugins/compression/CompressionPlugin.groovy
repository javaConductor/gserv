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

package io.github.javaconductor.gserv.plugins.compression

import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.ResourceAction
import io.github.javaconductor.gserv.ResourceActionFactory
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.filters.FilterOptions
import io.github.javaconductor.gserv.plugins.AbstractPlugin
import com.sun.net.httpserver.HttpExchange

import java.util.zip.DeflaterInputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Compression plugin
 */
@Log4j
class CompressionPlugin extends AbstractPlugin {
    @Override
    def init(Object options) {
        return null
    }

    @Override
    List<ResourceAction> filters() {
        createFilter("GET") //+ createFilter("PUT") + createFilter("POST")
    }

    /**
     * Filter Handler
     *
     * @param exchange
     * @return
     */
    def CompressionTypes = ["GZIP": "gzip", "DEFLATE": "deflate"]

    private def handleAfter(exchange, data) {
        def outEncodings = exchange.getRequestHeaders().get("Accept-Encoding")
        def outStream
        def outEncoding
        if (outEncodings)
            if (outEncodings[0]?.contains("gzip"))
                outEncoding = CompressionTypes.GZIP
            else if (outEncodings[0]?.contains("deflate"))
                outEncoding = CompressionTypes.DEFLATE
        def bytes = data
        /// if Should GZIP this
        if (outEncoding)
            if (shouldCompress(exchange.responseCode)) {
                bytes = compressBytes(bytes, outEncoding)
                /// add headers to real Exchange
                exchange.responseHeaders.add("Content-Encoding", outEncoding)
            }
        bytes
    }

    private def handleBefore(exchange) {
        def inputStream = exchange.requestBody

        /// wrap the input if needed
        def inEncoding = exchange.requestHeaders["Content-Encoding"]
        def instream
        if (inEncoding)
            switch (inEncoding) {
                case "gzip":
                    instream = new GZIPInputStream(inputStream)
                    break;

                case "deflate":
                    instream = new DeflaterInputStream(inputStream)
                    break;

                default:
                    break;
            }

        if (instream) {
            EventManager.instance().publish(Events.FilterProcessing, [
                    requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                    name     : "Compression-Before-Filter",
                    message  : "Processing Compression Filter - Replacing input stream."])
            exchange.setStreams(instream, exchange.responseBody)
        } else {
            EventManager.instance().publish(Events.FilterProcessing, [
                    requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                    name     : "Compression-Before-Filter",
                    message  : "Processing Compression Filter - NOT Replacing input stream."])
            //println("compressionPlugin: no compression here.")
        }
        exchange
    }

    private def createFilter(method) {
        EventManager.instance().publish(Events.FilterProcessing, [
                name   : "Compression-$method-Filter",
                message: "Creating Processing Compression Filter"])

        switch (method) {

            case "get":
            case "GET":
                [ResourceActionFactory.createAfterFilter("Compression-$method-After-Filter", method, '/**', [(FilterOptions.PassRouteParams): false, (FilterOptions.MatchedActionsOnly): true], 10) { e, data ->
                    data = handleAfter(e, data) ?: data
//                    println "CompressionPlugin: before GET doFilter()"
                    EventManager.instance().publish(Events.FilterProcessing, [
                            requestId: e.getAttribute(GServ.exchangeAttributes.requestId),
                            name     : "Compression-$method-After-Filter",
                            message  : "Filter is done! Compression Filter passing control down the chain. "])
                    data
                }]
                break;

            case "PUT":
            case "POST":
                [ResourceActionFactory.createBeforeFilter("Compression-$method-Before-Filter", method, '/**', [(FilterOptions.PassRouteParams): false, (FilterOptions.MatchedActionsOnly): true], 1) {
                    ->
                    HttpExchange e = handleBefore(exchange) ?: exchange
                    nextFilter(e)
                    log.trace "CompressionPlugin: before $method doFilter()"
                    e
                },
                 ResourceActionFactory.createAfterFilter("Compression-$method-After-Filter", method, '/**', [(FilterOptions.PassRouteParams): false, (FilterOptions.MatchedActionsOnly): true], 10) { exchange, data ->
                     handleAfter(exchange, data) ?: data
                 }]
                break;
        }
    }

    private byte[] compressBytes(bytes, contentEncoding) {
        def bostream = new ByteArrayOutputStream()
        def cmpStream = wrapOutputStreamWithCompression(bostream, contentEncoding)
        cmpStream.write(bytes)
        cmpStream.close()
        // return the compressed bytes
        def ret = bostream.toByteArray()
        EventManager.instance().publish(Events.FilterProcessing, [
                oldSize: bytes.size(),
                newSize: ret.size(),
                name   : "Compression-Filter"])
        ret
    }

    OutputStream wrapOutputStreamWithCompression(ostream, contentEncoding) {
        def os = ostream
        switch (contentEncoding) {
            case 'gzip':
                os = new GZIPOutputStream(ostream)
                break;
            case 'deflate':
                os = new DeflaterOutputStream(ostream)
                break;
        }
        os
    }

    private boolean shouldCompress(statusCode) {
        !([301, 305, 304, 500, 400..505, 302].contains(statusCode))
    }
}

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
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.cli.GServRunner
import io.github.javaconductor.gserv.factory.ResourceActionFactory
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.filters.FilterOptions
import io.github.javaconductor.gserv.plugins.AbstractPlugin
import io.github.javaconductor.gserv.requesthandler.RequestContext

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

    private def handleAfter(RequestContext context, data) {
        def reqId = context.id()
        def outEncodings = context.getRequestHeaders().get("Accept-Encoding")
        def outEncoding
        if (outEncodings)
            if (outEncodings[0]?.contains("gzip"))
                outEncoding = CompressionTypes.GZIP
            else if (outEncodings[0]?.contains("deflate"))
                outEncoding = CompressionTypes.DEFLATE
        byte[] bytes = data ?: new byte[0]
        /// if Should GZIP this
        if (outEncoding)
            if (shouldCompress(context.responseCode)) {
                def oldSz = bytes.length
                bytes = compressBytes(bytes, outEncoding)
                def newSz = bytes.length
                log.trace("Request #$reqId compressed [$outEncoding] $oldSz -> $newSz")
                /// add headers to real Exchange
                context.responseHeaders.put("Content-Encoding", [outEncoding])
            }
        bytes
    }

    private def handleBefore(context) {
        def inputStream = context.requestBody

        /// wrap the input if needed
        def inEncoding = context.requestHeaders["Content-Encoding"]
        def instream
        if (inEncoding) {
            inEncoding = inEncoding[0]
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
        }

        if (instream) {
            EventManager.instance().publish(Events.FilterProcessing, [
                    requestId: context.id(),
                    name     : "Compression-Before-Filter",
                    message  : "Processing Compression Filter - Replacing input stream."])
            context.setStreams(instream, context.responseBody)
        } else {
            EventManager.instance().publish(Events.FilterProcessing, [
                    requestId: context.id(),
                    name     : "Compression-Before-Filter",
                    message  : "Processing Compression Filter - NOT Replacing input stream."])
            //println("compressionPlugin: no compression here.")
        }
        context
    }

    private def createFilter(method) {
        EventManager.instance().publish(Events.FilterProcessing, [
                name   : "Compression-$method-Filter",
                message: "Creating Processing Compression Filter"])

        switch (method) {

            case "get":
            case "GET":
                [ResourceActionFactory.createAfterFilter("Compression-$method-After-Filter", method, '/**', [(FilterOptions.PassRouteParams): false, (FilterOptions.MatchedActionsOnly): true], 10) { context, data ->
                    data = handleAfter(context, data) ?: data
//                    println "CompressionPlugin: before GET doFilter()"
                    EventManager.instance().publish(Events.FilterProcessing, [
                            requestId: context.id(),
                            name     : "Compressioncontext-$method-After-Filter",
                            message  : "Filter is done! Compression Filter passing control down the chain. "])
                    data
                }]
                break;

            case "PUT":
            case "POST":
                [ResourceActionFactory.createBeforeFilter("Compression-$method-Before-Filter",
                        method, '/**',
                        [(FilterOptions.MatchedActionsOnly): true], 1) { requestContext, args ->
                    RequestContext ctxt = handleBefore(requestContext) ?: requestContext
                    //nextFilter(ctxt)
                    log.trace "CompressionPlugin: before $method doFilter()"
                    ctxt
                },
                 ResourceActionFactory.createAfterFilter("Compression-$method-After-Filter", method, '/**', [(FilterOptions.PassRouteParams): false, (FilterOptions.MatchedActionsOnly): true], 10) { requestContext, data ->
                     handleAfter(requestContext, data) ?: data
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

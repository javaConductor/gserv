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

package org.groovyrest.gserv.plugins.cors

import org.groovyrest.gserv.plugins.AbstractPlugin
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Log4j

class CORSMode {
    static final String SameHost = "SameHost"
    static final String AllowAll = "AllowAll"
    static final String WhiteList = "WhiteList"
    static final String BlackList = "BlackList"
}

/**
 *
 * Created by javaConductor on 1/13/14.
 *
 */
@Log4j
class CorsPlugin extends AbstractPlugin {

    def allowAllHandler = { CORSConfig corsConfig ->
//        println "CorsPlugin.allowAllHandler(): ${exchange.requestMethod}(${exchange.requestURI})"
        switch (exchange.requestMethod) {

            case "OPTIONS":
                // add header Access-Control-Allow-Origin, Access-Control-Allow-Methods, Access-Control-Allow-Headers, Access-Control-Max-Age
                exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
                exchange.responseHeaders.add("Access-Control-Allow-Methods", "OPTIONS,GET,PUT,POST,DELETE,HEAD")
                exchange.responseHeaders.add("Access-Control-Allow-Headers", "*")
                exchange.responseHeaders.add("Allow", "OPTIONS,GET,PUT,POST,DELETE,HEAD")

                def entry //= getHostEntry( exchange.requestURI.host )
                def maxAge = entry?.maxAge ?: (corsConfig?.maxAge ?: 3600)
                exchange.responseHeaders.add("Access-Control-Max-Age", "" + maxAge)

                /// send 200 with header
                def resp = new JsonBuilder([cors: [respones: 'OK']]).toPrettyString();

                exchange.sendResponseHeaders(200, resp.bytes.length)
                exchange.responseBody.write(resp.bytes);
                exchange.responseBody.close()
                exchange
                break;

            default:
                // look for origin header
                def origin = exchange.requestHeaders.get("Origin")
                // if found add the "Access-Control-Allow-Origin: *" header to response
                if (origin) {
//                    println "CorsPlugin filter: Allowing Origin: $origin"
                    exchange.responseHeaders.add("Access-Control-Allow-Methods", "OPTIONS,GET,PUT,POST,DELETE,HEAD")
                    exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
                } else {
                    //                  println "CorsPlugin filter: No Origin Header."
                }
//                println "CorsPlugin Filter: Calling chain"
                nextFilter()
                exchange
                break;
        }
    }

    def listHandler = { CORSConfig corsConfig ->
        log.trace "CorsPlugin.listHandler(): ${exchange.requestMethod} - ${exchange.requestURI} List: ${corsConfig.list}"
        switch (exchange.requestMethod) {
            case "OPTIONS":
                /// Check the List to see if this host has access according to the CORS Config
                // get the Origin
                String origin = exchange.requestHeaders.get("Origin")
                if (!origin) {
                    log.trace("CorsPlugin.listHandler():No ORIGIN header for OPTIONS method.")
                    nextFilter()
                    return
                }
                def host = originToHost(origin)
                log.trace("CorsPlugin.listHandler():ORIGIN header for OPTIONS method: $origin")
                log.trace("CorsPlugin.listHandler():HOST: $host")

                def accessReqMethod = exchange.requestHeaders.get("Access-Control-Request-Method")
                def accessReqHeaders = exchange.requestHeaders.get("Access-Control-Request-Headers")

                // if found add the "Access-Control-Allow-Origin: $origin" header to response
                def ok = corsConfig.hasAccess(host, accessReqMethod, accessReqHeaders)
                // def ok = checkListFn(corsConfig, origin, accessReqMethod, accessReqHeaders)
                log.trace "CorsPlugin.listHandler(): $origin can do $accessReqMethod = ${ok}"
                if (ok) {
                    def cfgForHost = corsConfig.findOriginConfig(host)
                    // send back the allow headers
                    exchange.responseHeaders.add("Access-Control-Allow-Origin", origin)
                    exchange.responseHeaders.add("Access-Control-Allow-Methods", cfgForHost.methods.join(","))
                    exchange.responseHeaders.add("Access-Control-Allow-Headers", accessReqHeaders)

                    def maxAge = cfgForHost?.maxAge ?: (corsConfig?.maxAge ?: 3600)
                    exchange.responseHeaders.add("Access-Control-Max-Age", maxAge)

                    /// send response
                    exchange.sendResponseHeaders(200, 0)
                    exchange.responseBody.close()

                } else { // not ok
                    /// send 403 Forbidden
                    exchange.sendResponseHeaders(403, 0)
                    exchange.responseBody.close()
                }
                exchange
                break;

            default:
                def statusCode = 403
                // look for origin header
                def originList = exchange.requestHeaders.get("Origin");
                if (!originList) {
                    log.trace("CorsPlugin.listHandler():No ORIGIN header for OPTIONS method.")
                    nextFilter()
                    return
                }
                def origin = originList[0];
                def host = originToHost(origin)
                log.trace("CorsPlugin.listHandler():ORIGIN header for OPTIONS method: $origin")
                log.trace("CorsPlugin.listHandler():HOST: $host")
                def ok = corsConfig.hasAccess(host, exchange.requestMethod)
                log.trace "CorsPlugin.listHandler(): $origin can do ${exchange.requestMethod} = ${ok}"
                if (ok) {
                    def cfgForHost = corsConfig.findHostConfig(host)
                    exchange.responseHeaders.add("Access-Control-Allow-Origin", (String) origin)
                    def maxAge = cfgForHost?.maxAge ?: (corsConfig?.maxAge ?: 3600)
                    exchange.responseHeaders.add("Access-Control-Max-Age", "" + maxAge)
                    log.trace("CorsPlugin.listHandler():Response HEADERS: ${exchange.responseHeaders}")
                    nextFilter()
                    return
                } else {
                    /// send 403 Forbidden
                    exchange.sendResponseHeaders(statusCode, 0)
                    exchange.responseBody.close()
                    return
                }
                exchange;
                break;
        }
    }

    /**
     * @return a closure - fn(chain)
     * the closure MUSt be a valid Before-Filter closure
     * @param corsConfig
     */
    def corsFilter(config) {
        def fn //The filter handler closure
        switch (config.mode) {
            case CORSMode.AllowAll:
                fn = allowAllHandler.curry(config)
                break;
            case CORSMode.BlackList:
                fn = listHandler.curry(config)
                break;
            case CORSMode.SameHost:
            case CORSMode.WhiteList:
                fn = listHandler.curry(config)
                break;

            default:
                throw new IllegalArgumentException("Invalid CORS config mode:" + config.mode.toString())
        }
        return fn
    }

    @Override
    def init(options) {
        // println "CorsPlugin: init($options)"
    }

/**
 * TODO
 * @param delegateType
 * @param delegateMetaClass
 * @return
 */
    @Override
    MetaClass decorateDelegate(String delegateType, MetaClass delegateMetaClass) {
        ///add some method to the HttpMethod delegate
        /// eg. markdown(string) -> HTML
        log.debug "CorsPlugin.decorateDelegate($delegateType, ${delegateMetaClass})"
        def ret = delegateMetaClass
        switch (delegateType) {
            case "http":
            case "https":
                def corsCfgFn = { uri, CORSConfig cfg ->
                    log.trace("CorsPlugin.decorateDelegate: cors(uri=$uri)")
                    try {
                        //    def before(name, url, method, options, order = 5, clozure) {
                        before("CORSPlugin-" + cfg.mode, uri, "*", [passRouteParams: false], 5, corsFilter(cfg))
                    } catch (Throwable e) {
                        e.printStackTrace(System.err)
                        log.error "CorsPlugin.decorateDelegate: Error calling filter function: ${e.message} ", e
                    }
                }

                def corsFileFn = { uri, String cfgFile ->
                    // open file:
                    def istresm = getFile(_staticRoots, cfgFile)
                    JsonSlurper js = new JsonSlurper()
                    def corsCfg = js.parse(new InputStreamReader(istresm))
                    corsCfgFn(uri, new CORSConfig(corsCfg))
                }

                def allowAllFn = { maxAge ->
                    return new CORSConfig(maxAge)
                }

                def whiteListFn = { maxAge, hostMap ->
                    return new CORSConfig(maxAge, CORSMode.WhiteList, hostMap)
                }

                def blackListFn = { maxAge, hostMap ->
                    return new CORSConfig(maxAge, CORSMode.BlackList, hostMap)
                }

                // cors() function
                ret.cors << corsCfgFn
                //TODO fix this  ret.cors << corsFileFn
                // add helper functions
                ret.allowAll << allowAllFn
                ret.whiteList << whiteListFn
                ret.blackList << blackListFn
                break
        }// switch
        ret
    }// function

    def originToHost(originHeader) {
        def ph = originHeader.split('//')
        def hAndP = (ph.size() == 1) ? ph[0] : ph[1];
        def h = hAndP.split(':')
        h[0]
    }//function

}

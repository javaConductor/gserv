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

import groovy.util.logging.Log4j

/**
 * Created by javaConductor on 1/13/14.
 */
@Log4j
class CORSConfig {
    def maxAge
    String mode
    def list = [:]

    def static exampleConfig = [
            maxAge: 3600,
            mode  : 'CORSMode.WhiteList',
            list  : [
                    "127.0.0.1"    : [
                            methods             : ["*"],
                            maxAge              : 14400,
                            customRequestHeaders: []
                    ],
                    "129.25.192.33": [
                            methods             : ["GET"],
                            maxAge              : 7200,
                            customRequestHeaders: ['X-Custom-Header']
                    ]
            ]
    ];

    def CORSConfig(corsConfig) {
        maxAge = corsConfig.maxAge
        mode = corsConfig.mode
        this.list = corsConfig?.list?.inject([:]) { hostCfgs, hcfg ->
            def host = hcfg.key
            def hostCfg = hcfg.value
            hostCfgs + [("$host".toString()): new HostConfig(host, hostCfg.methods, hostCfg.maxAge, hostCfg.customRequestHeaders ?: [])]
        }
    };

    def CORSConfig(int maxAge) {
        this.maxAge = maxAge
        mode = CORSMode.AllowAll
        list = [:]
    };

    def CORSConfig(int maxAge, String mode) {
        this(maxAge, mode, [:])
    }

    def CORSConfig(int maxAge, String mode, Map list) {
        this.maxAge = maxAge
        this.mode = mode
        this.list = list.inject([:]) { hostCfgs, hcfg ->
            def host = hcfg.key
            def hostCfg = hcfg.value
            hostCfgs + [("$host".toString()): new HostConfig(host,
                    hostCfg.methods,
                    hostCfg.maxAge ?: (maxAge ?: 3600),
                    hostCfg.customRequestHeaders ?: [])]
        }

    }

    def findHostConfig(String remoteHost) {
        remoteHost ? list[remoteHost] : null
    }

    def hasAccess(host, method, customReqHdrs) {
        def hcfg = findHostConfig(host);
        hcfg && hcfg.allowsCustomHeaders(customReqHdrs) && hasAccess(host, method)
    };

    def hasAccess(host, method) {
        switch (mode) {
            case (CORSMode.AllowAll):
                log.debug("CORSConfig(AllowAll): allows: $host")
                return true

            case (CORSMode.BlackList):
                log.debug("CORSConfig(BlackList): Looking for $host in $list")
                def hcfg = findHostConfig(host)
                def allow = (!hcfg) || !hcfg.hasMethod(method)
                log.debug("CORSConfig(BlackList): ${allow ? "allows" : "does NOT allow"} $host")
                return allow

            case (CORSMode.WhiteList):
                log.debug("CORSConfig(WhiteList): Looking for $host in $list")
                def hcfg = findHostConfig(host)
                def allow = hcfg?.hasMethod(method)
                log.debug("CORSConfig(WhiteList): ${allow ? "allows" : "does NOT allow"} $host")
                return allow;
        }// switch
    }// hasAccess
}

/**
 * Represents the configuration for ONE Host
 */
@Log4j
class HostConfig {
    String host
    List methods
    int maxAge
    List<String> customRequestHeaders

    def HostConfig(h, methodList, maxAge, customReqHdrs) {
        this.host = h
        methods = methodList ?: ["*"]
        this.maxAge = maxAge
        customRequestHeaders = customReqHdrs
        log.debug("HostConfig($h, ${methodList.join(',')}, $maxAge, ${customReqHdrs} )")
    }

    def allowsCustomHeaders(reqHeaders) {
        (!customRequestHeaders) || customRequestHeaders.intersect(reqHeaders).size() == reqHeaders.size()
    }

    def hasMethod(httpMethod) {
        return methods.contains("*") || methods.contains(httpMethod)
    }

    String toString() {
        "$host($maxAge, $methods, $customRequestHeaders)"
    }
}

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

package io.github.javaconductor.gserv.test

import io.github.javaconductor.gserv.plugins.cors.CORSConfig
import io.github.javaconductor.gserv.plugins.cors.CORSMode
import spock.lang.Specification

public class CorsSpec extends Specification {

    CORSConfig cfg;

    public void "test allowAllWithLocalHost"() {
        def cfg
        when:
        cfg = new CORSConfig(3600, CORSMode.AllowAll)

        then:
        cfg.hasAccess("localhost", "GET")
    }

    public void "should allow localhost using whitelist"() {
        cfg = new CORSConfig(3600, CORSMode.WhiteList, ["localhost": [maxAge: 3600, methods: ["*"]]
        ])
        assert cfg.hasAccess("localhost", "GET")
    }

    public void "should allow GET when * is in WhiteList"() {
        def cfg

        when:
        cfg = new CORSConfig(3600,
                CORSMode.WhiteList, [
                "127.0.0.1": [
                        maxAge : 3600,
                        methods: ["*"]
                ]
        ]
        );
        then:
        cfg.hasAccess("127.0.0.1", "GET")
    }


    public void "should allow GET when not in BlackList"() {
        def cfg

        when:
        cfg = new CORSConfig(3600,
                CORSMode.BlackList, [
                "127.0.0.1": [
                        maxAge : 3600,
                        methods: ["*"]
                ]])

        then:
        cfg.hasAccess("127.0.0.2", "GET")
    }

    public void "should NOT allow GET when in BlackList"() {
        def cfg

        when:
        cfg = new CORSConfig(3600,
                CORSMode.BlackList, [
                "127.0.0.1": [
                        maxAge : 3600,
                        methods: ["*"]
                ]])

        then:
        !cfg.hasAccess("127.0.0.1", "GET")
    }


    public void "should fail using WhiteList"() {
        def cfg

        when:
        cfg = new CORSConfig(3600,
                CORSMode.WhiteList, [
                "localhost.org": [
                        maxAge : 3600,
                        methods: ["*"]
                ]

        ])
        then:
        !cfg.hasAccess("localhost", "GET")
    }

    public void "should fail PUT using WhiteList"() {
        def cfg

        when:
        cfg = new CORSConfig(3600,
                CORSMode.WhiteList, [
                "localhost.org": [
                        maxAge : 3600,
                        methods: ["GET", "POST"]
                ]

        ])
        then:
        !cfg.hasAccess("localhost", "PUT")
    }

    public void "should allow PUT using WhiteList"() {
        def cfg

        when:
        cfg = new CORSConfig(3600,
                CORSMode.WhiteList, [
                "127.0.0.1": [
                        maxAge : 3600,
                        methods: ["GET", "PUT", "POST"]
                ]

        ])
        then:
        cfg.hasAccess("127.0.0.1", "PUT")
    }


    public void "should allow X-Other Header"() {
        def cfg

        when:
        cfg = new CORSConfig(3600,
                CORSMode.WhiteList, [
                "127.0.0.1": [
                        maxAge              : 3600,
                        methods             : ["GET", "PUT", "POST"],
                        customRequestHeaders: ['X-Other']
                ]

        ])
        then:
        cfg.hasAccess("127.0.0.1", "PUT", ['X-Other'])
    }

    public void "should not allow X-Same Header"() {
        def cfg

        when:
        cfg = new CORSConfig(3600,
                CORSMode.WhiteList, [
                "127.0.0.1": [
                        maxAge              : 3600,
                        methods             : ["GET", "PUT", "POST"],
                        customRequestHeaders: ['X-Other']
                ]

        ])
        then:
        !cfg.hasAccess("127.0.0.1", "PUT", ['X-Same'])
    }

    public void "should allow X-Same Header when none specified"() {
        def cfg

        when:
        cfg = new CORSConfig(3600,
                CORSMode.WhiteList, [
                "127.0.0.1": [
                        maxAge : 3600,
                        methods: ["GET", "PUT", "POST"]
                ]

        ])
        then:
        cfg.hasAccess("127.0.0.1", "PUT", ['X-Same'])
    }

}

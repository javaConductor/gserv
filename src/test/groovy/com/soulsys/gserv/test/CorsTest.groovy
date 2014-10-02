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

package com.soulsys.gserv.test

import com.soulsys.gserv.plugins.cors.CORSConfig
import com.soulsys.gserv.plugins.cors.CORSMode
import org.junit.Before
import org.junit.Test

public class CorsTest {

    CORSConfig cfg;

    @Before
    public final void init() {
        //     cfg = new CORSConfig(3600,CORSMode.AllowAll, [:])
    }

    @Test
    public final void testAllowAlWithLocalHost() {
        cfg = new CORSConfig(3600, CORSMode.AllowAll)
        assert cfg.hasAccess("localhost", "GET")
    }

    @Test
    public final void testWhiteListLocalhost() {
        cfg = new CORSConfig(3600, CORSMode.WhiteList, ["localhost": [maxAge: 3600, methods: ["*"]]
        ])
        assert cfg.hasAccess("localhost", "GET")
    }

    @Test
    public final void testWhiteList() {
        cfg = new CORSConfig(3600,
                CORSMode.WhiteList, [
                "127.0.0.1": [
                        maxAge : 3600,
                        methods: ["*"]
                ]
        ]
        );
        assert cfg.hasAccess("127.0.0.1", "GET")
    }

    @Test
    public final void testFailWhiteList() {
        cfg = new CORSConfig(3600,
                CORSMode.WhiteList, [
                "localhost.org": [
                        maxAge : 3600,
                        methods: ["*"]
                ]

        ])
        assert !cfg.hasAccess("localhost", "GET")
    }

}

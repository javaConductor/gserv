/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2014-2016 Lee Collins
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package test.integration

import com.github.restdriver.serverdriver.http.response.Response
import io.github.javaconductor.gserv.GServ
import org.junit.Test

import java.util.concurrent.TimeUnit

import static com.github.restdriver.serverdriver.Matchers.hasHeader
import static com.github.restdriver.serverdriver.Matchers.hasStatusCode
import static com.github.restdriver.serverdriver.RestServerDriver.*
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Created by javaConductor on 10/5/2014.
 */

class CorsSpec {

    def testResource = GServ.Resource('/test') {

        delegate.get("/") { ->
            write "text/plain", "CORS done!"
        }

    }

    def hostListWithLocalhost = [
            "localhost": [
                    methods             : ["GET", "PUT", "POST"],
                    maxAge              : 7200,
                    customRequestHeaders: ['X-Custom-Header']
            ]
    ]

    @Test
    public final void testCORSAllowAll() {
        def port = 51091
        def stopFn = new GServ().plugins {
            plugin("cors", [:])
        }.http {
            cors('/', allowAll(3600))
            resource testResource
        }.start(port)
        try {
            Response r = get("http://localhost:$port/test", withTimeout(5, TimeUnit.MINUTES),
                    header("Origin", 'http://localhost'))
            assertThat(r, hasStatusCode(200))
            assertThat(r, hasHeader("Access-Control-Allow-Origin", "*"))
        }
        finally {
            stopFn()
        }

    }

    @Test
    public final void testCORSWhiteList() {
        def port = 51031

        def stopFn = new GServ().plugins {
            plugin("cors", [:])
        }.http {
            cors('/', whiteList(3600, hostListWithLocalhost))
            resource testResource
        }.start(port)
        try {
            Response r = get("http://localhost:$port/test", withTimeout(5, TimeUnit.MINUTES),
                    header("Origin", 'http://localhost'))
            assertThat(r, hasStatusCode(200))
            assertThat(r, hasHeader("Access-Control-Allow-Origin", "http://localhost"))
        } finally {
            stopFn()
        }
    }

    @Test
    public final void testCORSBlackList() {
        def port = 51061

        def stopFn = new GServ().plugins {
            plugin("cors", [:])
        }.http {
            cors('/', blackList(3600, hostListWithLocalhost))
            resource testResource
        }.start(port)

        try {
            Response r = get("http://localhost:$port/test",
                    withTimeout(5, TimeUnit.MINUTES),
                    header("Origin", 'http://localhost'))
            assertThat(r, hasStatusCode(403))
        } finally {
            stopFn()
        }

    }


}

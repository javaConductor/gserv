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
import static com.github.restdriver.serverdriver.RestServerDriver.getOf
import static com.github.restdriver.serverdriver.RestServerDriver.withTimeout
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Created by javaConductor on 10/5/2014.
 */

class StatusPageSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    def res = GServ.Resource("/") {
        delegate.get("/") {
            if (requestHeader("Accept") == "text/plain") {
                write("text/plain", "Here is some plain text.")
            } else {
                responseHeader("Content-Type", "application/json")
//                contentType("application/json")
                writeJson([msg: "Here is some JSON."])
            }
        }

        delegate.get("/2") {
            if (requestHeader("Accept") == "text/plain") {
                write("text/plain", "Here is some plain text.")
                println responseHeader("Content-Type")
            } else
                writeJson([msg: "Here is some JSON."])
        }

        delegate.get('/3') {
            location("/2")
            write "text/plain", ""
        }

    }
    def instance = new GServ().http([:]) {
        resource res
        statusPage(true)/// the default is true
        /// if its false then there will be NO action defined
        /// if its true then  an Action will be created with path = $status_path
        //statusPath('/status.html') // default is status.html
    }

    @Test
    public final void testStatusPage() {
        def port = 54000
        def stopFn = instance.start(port)
        try {
            Response r = getOf("http://localhost:$port/status",
                    withTimeout(5, TimeUnit.MINUTES))

            assertThat(r, hasStatusCode(200))
            assertThat(r, hasHeader("Content-Type", "text/html"))
        } finally {
            stopFn()
        }
    }
}

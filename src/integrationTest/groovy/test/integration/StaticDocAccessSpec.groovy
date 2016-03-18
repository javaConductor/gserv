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

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.cli.GServRunner
import org.junit.Ignore
import org.junit.Test

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class StaticDocAccessSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Test
    public final void testSubFolderAccess() {
        def port = "11007"
        def dir = baseDir + "staticContentTest"
        def args = ["-p", port,
                    "-s", dir,
                    "-d", "index.html"
        ]
        // start server
        def stopFn = new GServRunner().start(args);
        Thread.sleep(1300)

        // request
        def http = new HTTPBuilder("http://localhost:$port/images/pictures.jpg")
        http.request(GET, TEXT) { req ->

            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                assert resp.status == 200
                //stop the server
                stopFn()
            }
            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                assert false, "Not found."
                println 'Not found'
                //stop the server
                stopFn()
            }
            response.'500' = { resp ->
                assert false, "Internal Error."
                println '500 Error'
                //stop the server
                stopFn()
            }
        }
    }

    /**
     * We need to useResourceDocs(true) and get the file from the classpath
     **/
    @Ignore
    public final void testSubFolderAccessInClasspath() {
        def port = "11006"
        def dir = baseDir + "staticContentTest"
        def args = ["-p", port,
                    "-s", dir,
                    "-i", dir + "/instance/StaticContentTest.groovy",
                    "-d", "index.html"
        ]

        /// start server
        def stopFn = new GServRunner().start(args);
        def cnt = 2
        ///make request
        def http = new HTTPBuilder("http://localhost:$port/images/pictures.jpg")
        http.request(GET, TEXT) { req ->

            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                assert resp.status == 200

                --cnt;
                //stop the server
                if (!cnt)
                    stopFn()
            }
            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                assert false, "Not found."
                println 'Not found'
                //stop the server
                --cnt;
                //stop the server
                if (!cnt)
                    stopFn()
            }
            response.'500' = { resp ->
                assert false, "Internal Error."
                println '500 Error'
                --cnt;
                //stop the server
                if (!cnt)
                    stopFn()
            }
        }

        ///make request
        http = new HTTPBuilder("http://localhost:$port/static/pictures.jpg")
        http.request(GET, TEXT) { req ->

            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                assert resp.status == 200

                --cnt;
                //stop the server
                if (!cnt)
                    stopFn()
            }
            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                assert false, "Not found."
                println 'Not found'
                --cnt;
                //stop the server
                if (!cnt)
                    stopFn()
            }
            response.'500' = { resp ->
                assert false, "Internal Error."
                println '500 Error'
                --cnt;
                //stop the server
                if (!cnt)
                    stopFn()
            }
        }
    }//test
}

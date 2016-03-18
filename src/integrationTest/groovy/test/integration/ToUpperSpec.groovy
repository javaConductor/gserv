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
import org.junit.Test

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class ToUpperSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Test
    public final void testToUpper() {

        def http = new HTTPBuilder('http://localhost:10010/upper/lowercaseword')
        def dir = baseDir + "toUpper"
        def args = ["-p", "10010",
                    "-r", dir + "/ToUpper.groovy"]
        def stopFn = new GServRunner().start(args);
        http.request(GET, TEXT) { req ->
            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                assert resp.status == 200
                resp.statusLine
                def text = reader.text;
                //stop the server
                stopFn()
                assert text == "LOWERCASEWORD"
            }
            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                println 'Not found'
            }
        }


    }

}

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
import org.spockframework.util.Assert

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class CliClasspathSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Ignore
    public final void testCliClasspath() {
        def port = "11001"
        def http = new HTTPBuilder("http://localhost:$port/math/add/34.34/45.45")
        def dir = baseDir + "cliClasspath"
        def args = ["-p", port,
                    "-r", dir + "/MathResource.groovy",
                    "-j", dir + "/CliMathService-1.0.jar"
        ]
        def stopFn = new GServRunner().start(args);

        http.request(GET, TEXT) { req ->

            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                stopFn()
                println "Cli returned: ${reader.text}"
                assert resp.status == 200
                //stop the server
            }
            response.failure = { resp ->
                //stop the server
                stopFn()
//                println "Cli returned: ${reader.text}"
                assert resp.status == 200
            }
        }
    }

    @Test
    public final void testCliClasspathResourceScriptFailure() {
        def port = "11002"
        def http = new HTTPBuilder("http://localhost:$port/math/add/434.434/454.454")
        def dir = baseDir + "cliClasspath"
        def args = ["-p", port,
                    "-r", dir + "/MathResource.groovy"
                    //          "-j", dir+"/CliMathService-1.0.jar"
        ]
        def stopFn
        try {
            stopFn = new GServRunner().start(args)
            stopFn();
//            assert  "Should throw exception.", false
            Assert.fail("Should NOT have gotten this far!!!")
        } catch (Exception ex) {
            if (stopFn)
                stopFn()
            assert ex.class.name.endsWith("ResourceScriptException")
        }
    }

    @Ignore
    public final void testCliClasspathInstanceScriptFailure() {
        def port = "11003"
        def http = new HTTPBuilder("http://localhost:$port/math/add/434.434/454.454")
        def dir = baseDir + "cliClasspath"
        def args = ["-p", port,
                    "-r", dir + "/MathInstance.groovy"
        ]
        def stopFn
        try {
            new GServRunner().start(args);
        } catch (Exception ex) {
            assert ex.class.name.endsWith("ResourceScriptException")
            //stopFn()
        }
    }
}

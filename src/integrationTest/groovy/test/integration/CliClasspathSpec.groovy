package test.integration

import groovyx.net.http.HTTPBuilder
import org.groovyrest.gserv.GServRunner
import org.junit.Test

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class CliClasspathSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Test
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
            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                stopFn()
                assert "Not found.", false

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
//                    "-j", dir+"/CliMathService-1.0.jar"
        ]

        try {
            new GServRunner().start(args);
            ///Assert.fail("Should NOT have gotten this far!!!")
        } catch (Exception ex) {
            assert ex.class.name.endsWith("ResourceScriptException")
        }
    }

    @Test
    public final void testCliClasspathInstanceScriptFailure() {
        def port = "11003"
        def http = new HTTPBuilder("http://localhost:$port/math/add/434.434/454.454")
        def dir = baseDir + "cliClasspath"
        def args = ["-p", port,
                    "-r", dir + "/MathInstance.groovy"
//                    "-j", dir+"/CliMathService-1.0.jar"
        ]

        try {
            new GServRunner().start(args);
        } catch (Exception ex) {
            assert ex.class.name.endsWith("InstanceScriptException")
        }
    }
}

package test.integration

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.cli.GServRunner
import org.junit.Test

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class DefaultResourceSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Test
    public final void testDefaultResource() {
        def port = "11004"
        def http = new HTTPBuilder("http://localhost:$port/")
        def dir = baseDir + "defaultResource"
        def f = new File(dir)
        // f.absolutePath
        def args = ["-p", port,
                    "-s", f.absolutePath,
                    "-d", "index.html"
        ]
        def stopFn = new GServRunner().start(args);

        http.request(GET, TEXT) { req ->

            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                println "Endpoint returned: ${reader.text}"
                assert resp.status == 200
                //stop the server
                stopFn()
            }
            response.failure = { resp ->
                assert false, "HTTP Error: Status: ${resp.statusLine}"
                println '500 Error'
                //stop the server
                stopFn()
            }
        }
    }

    @Test
    public final void testDefaultResourceOnAddress() {
        def port = "11009"
        def http = new HTTPBuilder("http://10.0.0.5:$port/")
        def dir = baseDir + "defaultResource"
        def args = [
                "-p", port,
                "-s", dir,
                "-a", "127.0.0.1",
                "-d", "index.html"
        ]
        def stopFn = new GServRunner().start(args);

        try {
            http.request(GET, TEXT) { req ->

                headers.'User-Agent' = 'Mozilla/5.0'
                response.success = { resp, Reader reader ->
                    println "Endpoint returned: ${reader.text}"
                    assert false, "should not return anything from this IP address";
                    //stop the server
                    stopFn()
                }
            }
        } catch (Exception e) {
            assert true, e.message
            stopFn()
        }
    }
}

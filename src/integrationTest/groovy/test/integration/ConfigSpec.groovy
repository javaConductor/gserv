package test.integration

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.GServRunner
import org.junit.Test

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class ConfigSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    /**
     *
     */
    @Test
    public final void testConfigInstance() {
        def port = "11111"
        def http = new HTTPBuilder("http://localhost:$port/math/add/34.34/45.45")
        def dir = baseDir + "configtest"
        def args = ["-c", dir + "/gserv.cfg.json"]
        def stopFn = new GServRunner().start(args);

        http.request(GET, TEXT) { req ->

            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                //stop the server
                stopFn()
                println "Cli returned: ${reader.text}"
                assert resp.status == 200

            }
            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                stopFn()
                assert "Not found.", false

            }
        }
    }

}

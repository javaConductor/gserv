package test.integration

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.GServRunner
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

package test.integration

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.GServRunner
import io.github.javaconductor.gserv.utils.Encoder
import org.junit.Test

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class BasicAuthHttpsSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Test
    public final void testToUpper() {

        def http = new HTTPBuilder('https://localhost:11000/')
        def dir = baseDir + "basicauthHttps"
        def args = ["-p", "11000",
                    "-i", dir + "/BasicAuthHttps.groovy"]
        def stopFn = new GServRunner().start(args);
        def testCnt = 2
        def uAndP = Encoder.base64("secret:thing".bytes)

        try {
            http.request(GET, TEXT) { req ->

                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'Authorization' = 'Basic ' + uAndP
                response.success = { resp, Reader reader ->
                    --testCnt
                    assert resp.status == 200
                    //stop the server
                    if (testCnt == 0)
                        stopFn()
                }
                // called only for a 404 (not found) status code:
                response.'403' = { resp ->
                    println 'Unauthorized'
                }
                // called only for a 404 (not found) status code:
                response.'404' = { resp ->
                    println 'Not found'
                }
            }
        } catch (Exception e) {
            if (e.message != 'peer not authenticated')
                assert false, "Should recognize the basic auth stuff."
        }

        try {
            http.request(GET, TEXT) { req ->
                headers.'User-Agent' = 'Mozilla/5.0'
                //headers.'Authorization' = 'Basic '+uAndP
                response.success = { resp, Reader reader ->
                    --testCnt
                    assert resp.status == 403
                    //stop the server
                    if (testCnt == 0)
                        stopFn()
                }
                // called only for a 404 (not found) status code:
                response.'404' = { resp ->
                    println 'Not found'
                }
                response.'403' = { resp ->
                    println 'Not found'
                }
            }
        } catch (Exception e) {
            assert (e.message == "Unauthorized" || e.message == "peer not authenticated")
            --testCnt
            if (testCnt == 0)
                stopFn()
        }
    }
}

package test.integration

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.GServRunner
import io.github.javaconductor.gserv.utils.Encoder
import org.junit.Ignore
import org.junit.Test

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class BasicAuthSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Ignore
    public final void testToUpper() {

        def http = new HTTPBuilder('http://localhost:11000/')
        def dir = baseDir + "basicauth"
        def args = ["-p", "11000",
                    "-i", dir + "/BasicAuth.groovy"]
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
                response.'404' = { resp ->
                    println 'Not found'
                }
            }
        } finally {
            --testCnt
            if (testCnt == 0)
                stopFn()
            //assert e.message == "Unauthorized"
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
        } finally {
//            assert e.message == "Unauthorized"
            --testCnt
            if (testCnt == 0)
                stopFn()
        }

    }

}

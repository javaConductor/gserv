package test.integration

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.GServRunner
import io.github.javaconductor.gserv.utils.Encoder
import org.junit.Ignore
import org.junit.Test
import org.junit.internal.builders.JUnit4Builder
import org.junit.runners.JUnit4

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */

class BasicAuthSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Test
    public final void testBasicAuthentication() {

        def http = new HTTPBuilder('http://localhost:51000/')
        def dir = baseDir + "basicauth"
        def args = ["-p", "51000",
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
                response.failure = { resp ->
                    --testCnt
                    
                    //stop the server
                    if (testCnt == 0)
                        stopFn()
                        assert "Failed!", false 
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
                response.failure = { resp ->
                    --testCnt
                    //stop the server
                    if (testCnt == 0)
                        stopFn()
                }
            }
        }
        catch (Throwable e){
            assert "${e.message}", false
        }
    }
}

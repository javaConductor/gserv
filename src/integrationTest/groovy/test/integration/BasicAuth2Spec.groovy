package test.integration

import com.github.restdriver.serverdriver.http.response.Response
import io.github.javaconductor.gserv.cli.GServRunner
import org.junit.Test

import java.util.concurrent.TimeUnit

import static com.github.restdriver.serverdriver.Matchers.hasStatusCode
import static com.github.restdriver.serverdriver.RestServerDriver.*
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Created by javaConductor on 10/5/2014.
 */
class BasicAuth2Spec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Test
    public final void testBasicAuthentication() {
        def port = 51002
        def dir = baseDir + "basicauth"
        def args = ["-p", "$port",
                    "-i", dir + "/BasicAuth.groovy"]
        def stopFn = new GServRunner().start(args)
        try {
            Response r = getOf("http://localhost:$port/", withBasicAuth('secret', 'thing'), withTimeout(5, TimeUnit.MINUTES))
            assertThat(r, hasStatusCode(200))
        } finally {
            stopFn()
        }
    }

    @Test
    public final void testBasicAuthenticationFailed() {
        def port = 51003
        def dir = baseDir + "basicauth"
        def args = ["-p", "$port",
                    "-i", dir + "/BasicAuth.groovy"]
        def stopFn = new GServRunner().start(args)
        Response r = getOf("http://localhost:$port/", withTimeout(5, TimeUnit.MINUTES))
        assertThat(r, hasStatusCode(401))
        stopFn()
    }
}

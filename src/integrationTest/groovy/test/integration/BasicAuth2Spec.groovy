package test.integration

import com.github.restdriver.serverdriver.http.response.Response
import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.cli.GServRunner
import org.junit.Test
import spock.lang.Ignore
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import java.util.concurrent.TimeUnit
import com.github.restdriver.serverdriver.matchers.*

import static com.github.restdriver.serverdriver.RestServerDriver.*
import static com.github.restdriver.serverdriver.Matchers.*
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */

class BasicAuth2Spec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Test
    public final void testBasicAuthentication() {
        def port = 51000
        def dir = baseDir + "basicauth"
        def args = ["-p", "$port",
                    "-i", dir + "/BasicAuth.groovy"]
        def stopFn = new GServRunner().start(args)
        Response r = getOf("http://localhost:$port/", withBasicAuth('secret', 'thing'), withTimeout(5, TimeUnit.MINUTES))
        assertThat(r, hasStatusCode(200))
        stopFn()
    }

    @Test
    public final void testBasicAuthenticationFailed() {
        def port = 51000
        def dir = baseDir + "basicauth"
        def args = ["-p", "$port",
                    "-i", dir + "/BasicAuth.groovy"]
        def stopFn = new GServRunner().start(args)
        Response r = getOf("http://localhost:$port/", withTimeout(5, TimeUnit.MINUTES))
        assertThat(r, hasStatusCode(401))
        stopFn()
    }
}

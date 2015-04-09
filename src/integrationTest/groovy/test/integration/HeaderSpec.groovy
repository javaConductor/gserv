package test.integration

import com.github.restdriver.serverdriver.http.response.Response
import io.github.javaconductor.gserv.cli.GServRunner
import io.github.javaconductor.gserv.GServ
import org.junit.Test

import java.util.concurrent.TimeUnit

import static com.github.restdriver.serverdriver.Matchers.hasHeader
import static com.github.restdriver.serverdriver.Matchers.hasStatusCode
import static com.github.restdriver.serverdriver.RestServerDriver.*
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Created by javaConductor on 10/5/2014.
 */

class HeaderSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    def res = GServ.Resource("/") {
        delegate.get("/") {
            if (requestHeader("Accept") == "text/plain") {
                write("text/plain", "Here is some plain text.")
            } else {
                responseHeader("Content-Type", "application/json")
//                contentType("application/json")
                writeJson([msg: "Here is some JSON."])
            }
        }

        delegate.get("/2") {
            if (requestHeader("Accept") == "text/plain") {
                write("text/plain", "Here is some plain text.")
                println responseHeader("Content-Type")
            } else
                writeJson([msg: "Here is some JSON."])
        }

    }
    def instance = new GServ().http([:]) {
        resource res
    }

    @Test
    public final void testAcceptHeader() {
        def port = 51000
        def stopFn = instance.start(port)
        Response r = getOf("http://localhost:$port/",
                header("Accept", "text/plain"),
                withTimeout(5, TimeUnit.MINUTES))

        assertThat(r, hasHeader("Content-Type", "text/plain"))
        assertThat(r, hasStatusCode(200))
        stopFn()
    }

    @Test
    public final void testAcceptHeader2() {
        def port = 51009
        def stopFn = instance.start(port)
        Response r = getOf("http://localhost:$port/",
                withTimeout(5, TimeUnit.MINUTES))

        assertThat(r, hasHeader("Content-Type", "application/json"))
        assertThat(r, hasStatusCode(200))
        stopFn()
    }

}

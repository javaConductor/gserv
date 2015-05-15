package test.integration

import com.github.restdriver.serverdriver.http.response.Response
import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.GServ
import org.junit.Test

import java.util.concurrent.TimeUnit

import static com.github.restdriver.serverdriver.Matchers.hasHeader
import static com.github.restdriver.serverdriver.Matchers.hasStatusCode
import static com.github.restdriver.serverdriver.RestServerDriver.*
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Created by javaConductor on 4/29/2015.
 */
@Log4j
class AcceptsSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    def res = GServ.Resource("/") {
        delegate.get("/") {
            if (delegate.accepts("text/plain")) {
                write("text/plain", "Here is some plain text.")
            } else {
                delegate.responseHeader("Content-Type", "application/json")
                writeJson([msg: "Here is some JSON."])
            }
        }
        delegate.get("/2") {
            if (delegate.accepts("text/plain","text/csv")) {
                write("text/plain", "Here is some plain text,")
            } else {
                delegate.responseHeader("Content-Type", "application/json")
                writeJson([msg: "Here is some JSON."])
            }
        }
    }
    def instance = new GServ().http([:]) {
        resource res
    }

    @Test
    public final void testAccepts() {
        def port = 51000
        def stopFn = instance.start(port)
        try {
            Response r = getOf("http://localhost:$port/",
                    header("Accept", "text/plain"),
                    withTimeout(5, TimeUnit.MINUTES))

            assertThat(r, hasStatusCode(200))
            assertThat(r, hasHeader("Content-Type", "text/plain"))
        } finally {
            stopFn()
        }
    }

    @Test
    public final void testAccepts2() {
        def port = 51009
        def stopFn = instance.start(port)
        try {
            Response r = getOf("http://localhost:$port",
                    withTimeout(5, TimeUnit.MINUTES))

            assertThat(r, hasStatusCode(200))
            assertThat(r, hasHeader("Content-Type", "application/json"))
        } finally {
            stopFn()
        }
    }

    @Test
    public final void testAcceptsMultiple() {
        def port = 51002
        def stopFn = instance.start(port)
        try {
            Response r = getOf("http://localhost:$port/2",
                    header("Accept", "text/csv"),
                    withTimeout(5, TimeUnit.MINUTES))

            assertThat(r, hasStatusCode(200))
            assertThat(r, hasHeader("Content-Type", "text/plain"))
        } finally {
            stopFn()
        }
    }
}

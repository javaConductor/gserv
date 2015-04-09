package test.integration

import com.github.restdriver.serverdriver.http.response.Response
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

class CustomMatcherSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    def res = GServ.Resource("/") {
        delegate.get("/"
                , onlyIfAccepts("text/plain")
        ) {
            write("text/plain", "Here is some plain text.")
        }

        delegate.get("/2"
                , onlyIfHeader("Accept", "text/plain")
        ) {
            write("text/plain", "Here is some plain text.")

        }
    }
    def instance = new GServ().http([:]) {
        resource res
    }

    @Test
    public final void testOnlyIfAccepts() {
        def port = 51099
        def stopFn = instance.start(port)
        try {
            Response r = getOf("http://localhost:$port/",
                    header("Accept", "text/plain"),
                    withTimeout(5, TimeUnit.MINUTES))

            assertThat(r, hasHeader("Content-Type", "text/plain"))
            assertThat(r, hasStatusCode(200))
        }
        finally {
            stopFn()
        }
        }


    @Test
    public final void testOnlyIfAcceptsFail() {
        def port = 51000
        def stopFn = instance.start(port)
        try {
            Response r = getOf("http://localhost:$port/",
                    // header("Accept", "text/plain"),
                    withTimeout(5, TimeUnit.MINUTES))
            // assertThat(r, hasHeader("Content-Type", "text/plain"))
            assertThat(r, hasStatusCode(404))
        } finally {
            stopFn()
        }
        }

    @Test
    public final void testOnlyIfHeader() {
        def port = 51000
        def stopFn = instance.start(port)
        try {
            Response r = getOf("http://localhost:$port/2",
                    header("Accept", "text/plain"),
                    withTimeout(5, TimeUnit.MINUTES))
            assertThat(r, hasHeader("Content-Type", "text/plain"))
            assertThat(r, hasStatusCode(200))
        } finally {
            stopFn()
        }
    }


    @Test
    public final void testOnlyIfHeaderFail() {
        def port = 51000
        def stopFn = instance.start(port)
        try {
            Response r = getOf("http://localhost:$port/2",
                    // header("Accept", "text/plain"),
                    withTimeout(5, TimeUnit.MINUTES))
            // assertThat(r, hasHeader("Content-Type", "text/plain"))
            assertThat(r, hasStatusCode(404))
        } finally {
            stopFn()
        }
    }
    }



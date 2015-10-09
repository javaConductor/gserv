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

class CorsSpec {

    def testResource = GServ.Resource('/test') {

        delegate.get("/") { ->
            write "text/plain", "CORS done!"
        }

    }

    def hostListWithLocalhost = [
            "localhost": [
                    methods             : ["GET", "PUT", "POST"],
                    maxAge              : 7200,
                    customRequestHeaders: ['X-Custom-Header']
            ]
    ]

    @Test
    public final void testCORSAllowAll() {
        def port = 51091
        def stopFn = new GServ().plugins {
            plugin("cors", [:])
        }.http {
            cors('/', allowAll(3600))
            resource testResource
        }.start(port)
        try {
            Response r = get("http://localhost:$port/test", withTimeout(5, TimeUnit.MINUTES),
                    header("Origin", 'http://localhost'))
            assertThat(r, hasStatusCode(200))
            assertThat(r, hasHeader("Access-Control-Allow-Origin", "*"))
        }
        finally {
            stopFn()
        }

    }

    @Test
    public final void testCORSWhiteList() {
        def port = 51031

        def stopFn = new GServ().plugins {
            plugin("cors", [:])
        }.http {
            cors('/', whiteList(3600, hostListWithLocalhost))
            resource testResource
        }.start(port)
        try {
            Response r = get("http://localhost:$port/test", withTimeout(5, TimeUnit.MINUTES),
                    header("Origin", 'http://localhost'))
            assertThat(r, hasStatusCode(200))
            assertThat(r, hasHeader("Access-Control-Allow-Origin", "http://localhost"))
        } finally {
            stopFn()
        }
    }

    @Test
    public final void testCORSBlackList() {
        def port = 51061

        def stopFn = new GServ().plugins {
            plugin("cors", [:])
        }.http {
            cors('/', blackList(3600, hostListWithLocalhost))
            resource testResource
        }.start(port)

        try {
            Response r = get("http://localhost:$port/test",
                    withTimeout(5, TimeUnit.MINUTES),
                    header("Origin", 'http://localhost'))
            assertThat(r, hasStatusCode(403))
        } finally {
            stopFn()
        }

    }


}

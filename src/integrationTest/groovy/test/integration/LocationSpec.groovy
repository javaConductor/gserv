package test.integration

import com.github.restdriver.serverdriver.http.response.Response
import io.github.javaconductor.gserv.GServ
import org.junit.Test

import java.util.concurrent.TimeUnit

import static com.github.restdriver.serverdriver.Matchers.hasHeader
import static com.github.restdriver.serverdriver.Matchers.hasStatusCode
import static com.github.restdriver.serverdriver.RestServerDriver.put
import static com.github.restdriver.serverdriver.RestServerDriver.withTimeout
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Created by javaConductor on 10/5/2014.
 */

class LocationSpec {
    def testResource = GServ.Resource('/test') {

        delegate.put("/") { String json ->
            location("/test/thing")
            write "text/plain", "done!"
        }
        delegate.get('/thing') {
            write "text/plain", "This is the thing."
        }

    }

    @Test
    public final void testLocation() {
        def port = 51001
        def stopFn = new GServ().http {
            resource testResource
        }.start(port)
        def data = ""
        Response r = put("http://localhost:$port/test", withTimeout(5, TimeUnit.MINUTES))
        assertThat(r, hasStatusCode(200))
        assertThat(r, hasHeader("Location", "/test/thing"))
        stopFn()
    }

}

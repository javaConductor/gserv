package test.integration

import com.github.restdriver.serverdriver.http.response.Response
import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.GServ
import org.junit.Test

import java.util.concurrent.TimeUnit

import static com.github.restdriver.serverdriver.Matchers.hasStatusCode
import static com.github.restdriver.serverdriver.RestServerDriver.getOf
import static com.github.restdriver.serverdriver.RestServerDriver.withTimeout
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Created by javaConductor on 4/29/2015.
 */
@Slf4j
class TemplateSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    def instance = new GServ().http([:]) {
        useResourceDocs(true)
        delegate.get("/") {
            template("text/html", "/testTemplate.html", [
                    title: "Template Test",
                    page : 3
            ])
        }
        delegate.get("/json") {
            template("application/json", "/testTemplate.json", [
                    title: "Template JSON Test",
                    page : 66
            ])
        }
    }

    @Test
    public final void testTemplateInResourceDocs() {
        def port = 51000
        def stopFn = instance.start(port)
        try {
            Response r = getOf("http://localhost:$port/",
                    withTimeout(5, TimeUnit.MINUTES))

            assertThat(r, hasStatusCode(200))
            assertThat("Template should have produced the text",
                    r.asText().contains("<title>Template Test</title>"))

        } finally {
            stopFn()
        }
    }

    @Test
    public final void testTemplateInResourceDocsJsp() {
        def port = 51001
        def stopFn = instance.start(port)
        try {
            Response r = getOf("http://localhost:$port/json",
                    withTimeout(5, TimeUnit.MINUTES))

            assertThat(r, hasStatusCode(200))
            assertThat(r.asJson().findValue("page").asText(), equalTo("66"))
            assertThat(r.asJson().findValue("page1").asText(), equalTo("66"))
            assertThat(r.asJson().findValue("page2").asText(), equalTo("66"))
            //     assertThat("Template should have produced the text",  r.asText().contains("<title>Template Test</title>"))

        } finally {
            stopFn()
        }
    }
}

/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2014-2016 Lee Collins
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

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

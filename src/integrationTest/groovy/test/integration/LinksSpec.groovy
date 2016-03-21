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
import groovy.json.JsonSlurper
import io.github.javaconductor.gserv.GServ
import org.junit.Test

import java.util.concurrent.TimeUnit

import static com.github.restdriver.serverdriver.Matchers.*
import static com.github.restdriver.serverdriver.RestServerDriver.*
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Created by javaConductor on 10/5/2014.
 */

class LinksSpec {
	def baseDir = "src/integrationTest/resources/test/integration/"

	def res = GServ.Resource("/thing") {

		delegate.get("/:id") { id ->
			writeJson([msg: "Here is some JSON."] + [links: links(id)])
		}

		delegate.put("/:id") { data, id ->
			println "Called PUT /thing/$id"
			location("/thing/$id")

			writeJson([ok: true] + [links: links(id)])
		}

		delegate.post("/:id") {
			writeJson([msg: "Here is some JSON."])
		}

		links { id ->
			[
					[
							href  : "/thing/$id",
							rel   : "self",
							method: "GET"
					],
					[
							href  : "/thing/$id",
							rel   : "edit",
							method: "PUT"
					],
					[
							href  : "/thing/$id",
							rel   : "remove",
							method: "DELETE"
					]
			]

		}

	}
	def instance = new GServ().http([:]) {
		resource res
	}

	@Test
	public final void testLinks() {
		def port = 59000
		def stopFn = instance.start(port)
		try {
			Response r = getOf("http://localhost:$port/thing/21",
					withTimeout(5, TimeUnit.MINUTES))

			assertThat(r, hasStatusCode(200))
			assertThat(r.asJson(), hasJsonPath("links"))
		} finally {
			stopFn()
		}
	}

	@Test
	public final void testLinksWithQry() {
		def port = 59000
		def stopFn = instance.start(port)
		try {
			Response r = getOf("http://localhost:$port/thing/21",
					withTimeout(5, TimeUnit.MINUTES))

			assertThat(r, hasStatusCode(200))
			assertThat(r.asJson(), hasJsonPath("links"))
		} finally {
			stopFn()
		}
	}

	@Test
	public final void testEditLinks() {
		def port = 59001
		def stopFn = instance.start(port)
		try {
			Response r = getOf("http://localhost:$port/thing/21",
					withTimeout(5, TimeUnit.MINUTES))

			assertThat(r, hasStatusCode(200))
			assertThat(r.asJson(), hasJsonPath("links"))

			def links = new JsonSlurper().parseText(new String(r.asBytes()))
			def theLink = links.links.find { lnk ->
				lnk.rel == "edit"
			}
			assertThat("This should be the 'PUT' method.", theLink.method == "PUT")
			//println(theLink)

		} finally {
			stopFn()
		}
	}

	@Test
	public final void testCanCallEditLinks() {
		def port = 59002
		def stopFn = instance.start(port)
		try {
			Response r = getOf("http://localhost:$port/thing/21",
					withTimeout(5, TimeUnit.MINUTES))

			assertThat(r, hasStatusCode(200))
			assertThat(r.asJson(), hasJsonPath("links"))

			def response = new JsonSlurper().parseText(new String(r.asBytes()))
			def theLink = response.links.find { lnk ->
				lnk.rel == "edit"
			}
			assertThat("This should be the 'PUT' method.", theLink.method == "PUT")
			println(theLink)

			Response r2 = putOf("${theLink.href}",
					withTimeout(30, TimeUnit.SECONDS))
			assertThat(r2, hasStatusCode(200))
			assertThat(r2, hasHeader("Location"))

		} finally {
			stopFn()
		}
	}

}

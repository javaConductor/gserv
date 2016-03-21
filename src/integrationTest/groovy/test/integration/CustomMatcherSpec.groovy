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

		delegate.put("/2"
				, onlyIfAccepts("Accept", "text/plain")
				, onlyIfContentType("text/plain")
		) { String json ->
			write("text/plain", json)
		}
		delegate.put("/3"
				, onlyIfAccepts("Accept", "text/plain")
				, onlyIfContentType("text/plain", "application/json")
		) { String json ->
			write("text/plain", json)
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
		def port = 51006
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
		def port = 51070
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
		def port = 51008
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

	@Test
	public final void testOnlyIfContentType() {
		def port = 51009
		def stopFn = instance.start(port)
		try {
			Response r = putOf("http://localhost:$port/2",
					body("Some plain text", "text/plain"),
					header("Accept", "text/plain"),
					withTimeout(5, TimeUnit.MINUTES))
			assertThat(r, hasHeader("Content-Type", "text/plain"))
			assertThat(r, hasStatusCode(200))
		} finally {
			stopFn()
		}
	}

//    @Test
//    public final void testOnlyIfContentTypeWith2() {
//        def port = 51010
//        def stopFn = instance.start(port)
//        try {
//            Response r = putOf("http://localhost:$port/3",
//                    body('{"msg": "Some JSON"}', "application/json"),
//                    header("Accept", "text/plain"),
//                    withTimeout(5, TimeUnit.MINUTES))
//            assertThat(r, hasHeader("Content-Type", "text/plain"))
//            assertThat(r, hasStatusCode(200))
//        } finally {
//            stopFn()
//        }
//    }

	@Test
	public final void testOnlyIfContentTypeWith2Fail() {
		def port = 51011
		def stopFn = instance.start(port)
		try {
			Response r = putOf("http://localhost:$port/3",
					body('Some JSON,', "text/csv"),
					header("Accept", "text/plain"),
					withTimeout(5, TimeUnit.MINUTES))
			assertThat(r, hasStatusCode(404))
		} finally {
			stopFn()
		}
	}
}

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
import io.github.javaconductor.gserv.cli.GServRunner
import org.junit.Test

import java.util.concurrent.TimeUnit

import static com.github.restdriver.serverdriver.Matchers.hasStatusCode
import static com.github.restdriver.serverdriver.RestServerDriver.*
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Created by javaConductor on 10/5/2014.
 */
class BasicAuth2Spec {
	def baseDir = "src/integrationTest/resources/test/integration/"

	@Test
	public final void testBasicAuthentication() {
		def port = 51002
		def dir = baseDir + "basicauth"
		def args = ["-p", "$port",
					"-i", dir + "/BasicAuth.groovy"]
		def stopFn = new GServRunner().start(args)
		try {
			Response r = getOf("http://localhost:$port/", withBasicAuth('secret', 'thing'), withTimeout(5, TimeUnit.MINUTES))
			assertThat(r, hasStatusCode(200))
		} finally {
			stopFn()
		}
	}

	@Test
	public final void testBasicAuthenticationFailed() {
		def port = 51003
		def dir = baseDir + "basicauth"
		def args = ["-p", "$port",
					"-i", dir + "/BasicAuth.groovy"]
		def stopFn = new GServRunner().start(args)
		Response r = getOf("http://localhost:$port/", withTimeout(5, TimeUnit.MINUTES))
		assertThat(r, hasStatusCode(401))
		stopFn()
	}
}

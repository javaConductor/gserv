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

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.cli.GServRunner
import org.junit.Test

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */

class BasicAuthSpec {
	def baseDir = "src/integrationTest/resources/test/integration/"

	@Test
	public final void testBasicAuthentication() {

		def http = new HTTPBuilder('http://localhost:51200/')
		def dir = baseDir + "basicauth"
		def args = ["-p", "51200",
					"-i", dir + "/BasicAuth.groovy"]
		def stopFn = new GServRunner().start(args);
		def testCnt = 2
		def uAndP = "secret:thing".bytes.encodeBase64()

		try {
			http.request(GET, TEXT) { req ->

				headers.'User-Agent' = 'Mozilla/5.0'
				headers.'Authorization' = 'Basic ' + uAndP
				response.success = { resp, Reader reader ->
					--testCnt
					assert resp.status == 200
					//stop the server
					if (testCnt == 0)
						stopFn()
				}
				response.failure = { resp ->
					--testCnt

					//stop the server
					if (testCnt == 0)
						stopFn()
					assert "Failed!", false
				}
			}
		} finally {
			--testCnt
			if (testCnt == 0)
				stopFn()
			//assert e.message == "Unauthorized"
		}

		try {
			http.request(GET, TEXT) { req ->
				headers.'User-Agent' = 'Mozilla/5.0'
				//headers.'Authorization' = 'Basic '+uAndP
				response.success = { resp, Reader reader ->
					--testCnt
					assert resp.status == 403
					//stop the server
					if (testCnt == 0)
						stopFn()
				}
				response.failure = { resp ->
					--testCnt
					//stop the server
					if (testCnt == 0)
						stopFn()
				}
			}
		}
		catch (Throwable e) {
			assert "${e.message}", false
		}
	}
}

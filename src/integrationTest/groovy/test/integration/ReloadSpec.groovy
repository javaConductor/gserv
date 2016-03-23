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

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.cli.GServRunner
import org.junit.Test
import java.nio.file.StandardCopyOption

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class ReloadSpec {
	def baseDir = "src/integrationTest/resources/test/integration/"

	@Test
	public final void testResourceReload() {
		def port = "11017"
		def dir = baseDir + "reloadTest"
		def fTarget = new File(dir, "MathInstance.groovy")
		def f1 = new File(dir, "MathInstance1.groovy")
		def f2 = new File(dir, "MathInstance2.groovy")
		def args = ["-p", port,
					"-s", dir,
					"-l",
					"-i", fTarget.absolutePath,
					"-d", "index.html"
		]
		// use the first instance file
		java.nio.file.Files.copy(f1.toPath(), fTarget.toPath(), StandardCopyOption.REPLACE_EXISTING)

		// start server
		def stopFn = new GServRunner().start(args);
		Thread.sleep(1300)
		def newText
		def p = getIdentity(port).then({ text ->
			assert (text.contains("MathInstance1"))
			// use the first instance file
			java.nio.file.Files.copy(f2.toPath(), fTarget.toPath(), StandardCopyOption.REPLACE_EXISTING )
			Thread.currentThread().sleep(4000);
			return getIdentity(port).then({ text2 ->
				return text2;
			}) ;
		}) ;
		newText=p.get()
		assert (newText.contains("MathInstance2"))
	}

	def getIdentity( port ){
		DataflowVariable p = new DataflowVariable()

		def http = new HTTPBuilder("http://localhost:$port/identity")
		http.request(GET, TEXT) { req ->

			headers.'User-Agent' = 'Mozilla/5.0'
			response.success = { resp, Reader reader ->
				assert resp.status == 200
				char[] ac = new char[100]
				reader.read(ac)
				def data = new String(ac);
				p << data
			}
			// called only for a 404 (not found) status code:
			response.'404' = { resp ->
				assert false, "Not found."
				println 'Not found'
				p <<  'Not found'
			}
			response.'500' = { resp ->
				assert false, "Internal Error."
				p <<  '500 Error'
			}
		}
		return p;
	}
}

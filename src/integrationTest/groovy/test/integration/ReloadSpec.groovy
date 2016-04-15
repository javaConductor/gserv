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

import java.nio.file.Files
import java.nio.file.StandardCopyOption

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class ReloadSpec {
	def baseDir = "src/integrationTest/resources/test/integration/"

	@Test
	public final void testInstanceReload() {
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
			// use the second instance file
			java.nio.file.Files.copy(f2.toPath(), fTarget.toPath(), StandardCopyOption.REPLACE_EXISTING)
			Thread.currentThread().sleep(4000);
			return getIdentity(port).then({ text2 ->
				return text2;
			});
		});
		newText = p.get()
		assert (newText.contains("MathInstance2"))
	}


	public final void testInstanceReload2() {
		def port = "11018"
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
			java.nio.file.Files.copy(f2.toPath(), fTarget.toPath(), StandardCopyOption.REPLACE_EXISTING)
			Thread.currentThread().sleep(4000);
			return getIdentity(port).then({ text2 ->
				return text2;
			});
		});
		newText = p.then( { text2 ->
			assert (text2.contains("MathInstance2"))
			// use the first instance file
			java.nio.file.Files.copy(f1.toPath(), fTarget.toPath(), StandardCopyOption.REPLACE_EXISTING)
			Thread.currentThread().sleep(4000);
			return getIdentity(port).then({ text3 ->
				return text3;
			});
		}).get();
		assert (newText.contains("MathInstance1"))
	}

	@Test
	public final void testInstanceReloadAndBack() {
		def port = "11019"
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
		println "Copying file to ${fTarget.absolutePath}"
		// use the first instance file
		java.nio.file.Files.copy(f1.toPath(), fTarget.toPath(), StandardCopyOption.REPLACE_EXISTING)

		// start server
		def stopFn = new GServRunner().start(args);
		Thread.sleep(1300)
		def newText
		def p = getIdentity(port).then({ text ->
			println "Text from endpoint1 ($text)"
			assert (text.contains("MathInstance1"))
			// use the second instance file
			println "Changing fTarget to MathInstance2"
			Files.copy(f2.toPath(), fTarget.toPath(), StandardCopyOption.REPLACE_EXISTING)
			println "Wait 4 seconds . . ."
			Thread.currentThread().sleep(4000);
			println "Hit Identity endpoint."
			return getIdentity(port).then({ text2 ->
				println "Getting text: $text2"
				return text2;
			}).then({ txt ->
				assert (txt.contains("MathInstance2"))
				// use the second instance file
				println "Changing fTarget to MathInstance1"
				java.nio.file.Files.copy(f1.toPath(), fTarget.toPath(), StandardCopyOption.REPLACE_EXISTING)
				println "Wait 4 seconds . . ."
				Thread.currentThread().sleep(4000);
				return getIdentity(port).then({ text1 ->
					println "Getting text: $text1"
					return text1;
				});
			});
			newText = p.get()
			assert (newText.contains("MathInstance1"))
		});
		def x = p.get();
	}

	def getIdentity(port) {
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
				p << 'Not found'
			}
			response.'500' = { resp ->
				assert false, "Internal Error."
				p << '500 Error'
			}
		}
		return p;
	}
}

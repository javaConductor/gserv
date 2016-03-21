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

package io.github.javaconductor.gserv.utils

import groovy.util.logging.Slf4j
import org.apache.commons.io.IOUtils

/**
 * Created by javaConductor on 1/13/14.
 * Code for manipulating static files
 */
@Slf4j
class StaticFileHandler {
	def fileFn(contentType, filePath) {
		file(contentType, filePath)
	}

	/**
	 * Creates a function that, when called, writes the content of file 'filename'
	 * to the outputStream and sets the contentType to 'mimeType'
	 *
	 * @param mimeType
	 * @param filename
	 * @return Closure
	 */
	def file(mimeType, filename) {

		if (!mimeType) {
			mimeType = URLConnection.guessContentTypeFromName(filename)
			log.trace("File: $filename said to have type $mimeType")
		}

		{ ->
//            EventManager.instance().publish(Events.ResourceProcessing, [
//                    requestId: exchange.getAttribute(GServ.contextAttributes.requestId),
//                    mimeType : mimeType,
//                    msg      : "Sending static file.",
//                    path     : "$filename"])
			/// search the staticRoots
			InputStream is = getFile(_staticRoots, filename)
			if (is) {
				def sz = is.available();
				requestContext.responseHeaders.put("Content-Type", [mimeType])
				requestContext.sendResponseHeaders(200, sz)
				IOUtils.copy(is, requestContext.responseBody)
			} else {
				def msg = "No such file: $filename"
				def ab = msg.getBytes()
				requestContext.sendResponseHeaders(404, ab.size())
				requestContext.responseBody.write(ab);
			}
			requestContext.responseBody.close()
			requestContext.close()
		}//fn
	}

/**
 *  Returns an inputStream to a file with path 'filePath'. Path is evaluated against
 *  the staticRoots, and possibly the Application's classpath resources
 *
 *
 * @param filePath
 * @param staticRoots
 * @param useResourceDocs
 * @return InputStream
 */
	InputStream resolveStaticResource(String filePath, List<String> staticRoots, boolean useResourceDocs) {
		/// if useResourceDocs then look in the resources
		(useResourceDocs) ? getFile(staticRoots, filePath) : getContentFile(staticRoots, filePath)
	}

	/**
	 * Get file from either src/main/resources/docs or from fileSystem
	 *
	 * @param staticRoots
	 * @param filePath
	 * @return inputStream to File
	 */
	def getFile(List<String> staticRoots, String filePath) {
		(getDoc(filePath)) ?: getContentFile(staticRoots, filePath)
	}

	/**
	 * Get documents relative to the /src/main/resources/docs folder
	 *
	 * @param filePath
	 * @return inputStream to File
	 */
	def getDoc(String filePath) {

		if (filePath.startsWith("/")) filePath = filePath.substring(1)
		//// check the default first (may change)
		URL u = Class.getResource("/docs/$filePath")
		u?.openStream()//content
	}

	/**
	 * Get file from the FileSystem
	 *
	 * @param staticRoots directories to search
	 * @param filePath String - The file to find
	 * @return inputStream to File
	 */
	InputStream getContentFile(List<String> staticRoots, String filePath) {

		if (filePath.startsWith("/")) filePath = filePath.substring(1)
		//// Maybe its in one of the other static roots (if any)

		def dir = staticRoots.find { sroot ->
			File f = new File("$sroot/$filePath")
			f.exists() && f.isFile()
		}
		(dir) ? new FileInputStream(new File("$dir/$filePath")) : null
	}

}

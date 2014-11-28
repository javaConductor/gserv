/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Lee Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.javaconductor.gserv.gserv.utils

import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import org.apache.commons.io.IOUtils

/**
 * Created by javaConductor on 1/13/14.
 * Code for manipulating static files
 */
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
        }

        { ->
            EventManager.instance().publish(Events.ResourceProcessing, [
                    requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                    mimeType : mimeType,
                    msg      : "Sending static file.",
                    path     : "$filename"])
            /// search the staticRoots
            InputStream is = getFile(_staticRoots, filename)
            if (is) {
                def sz = is.available();
                exchange.responseHeaders.add("Content-Type", mimeType)
                exchange.sendResponseHeaders(200, sz)
                IOUtils.copy(is, exchange.responseBody)
            } else {
                def msg = "No such file: $filename"
                def ab = msg.getBytes()
                exchange.sendResponseHeaders(404, ab.size())
                exchange.responseBody.write(ab);
            }
            exchange.responseBody.close()
        }
    }

/**
 *  Returns an inputStream to a file with path 'filePath'. Path is evaluated against
 *  the staticRoots, and possibly the Application's resources
 *
 *
 * @param filePath
 * @param staticRoots
 * @param useResourceDocs
 * @return InputStream
 */
    def resolveStaticResource(filePath, staticRoots, useResourceDocs) {
        /// if useResourceDocs then look in the resources
        (useResourceDocs) ? getFile(staticRoots, filePath) : getFsFile(staticRoots, filePath)
    }

    /**
     * Get file from either src/main/resources/docs or from fileSystem
     *
     * @param staticRoots
     * @param filePath
     * @return inputStream to File
     */
    def getFile(staticRoots, filePath) {
        (getDoc(filePath)) ?: getFsFile(staticRoots, filePath)
    }

    /**
     * Get documents relative to the /src/main/resources/docs folder
     *
     * @param filePath
     * @return inputStream to File
     */
    def getDoc(filePath) {

        if (filePath.startsWith("/")) filePath = filePath.substring(1)
        //// check the default first (may change)
        //URL u = ClassLoader.getSystemResource("/docs/$filePath")
        //URL u = Class.getClassLoader().getResource("/docs/$filePath")
        URL u = Class.getResource("/docs/$filePath")
        u?.content
    }

    /**
     * Get file from the FileSystem
     *
     * @param staticRoots directories to search
     * @param filePath The file to find
     * @return inputStream to File
     */
    def getFsFile(staticRoots, filePath) {

        if (filePath.startsWith("/")) filePath = filePath.substring(1)
        //// Maybe its in one of the other static roots (if any)

        def dir = staticRoots.find { sroot ->
            File f = new File("$sroot/$filePath")
            f.exists() && f.isFile()
        }
        (dir) ? new FileInputStream(new File("$dir/$filePath")) : null
    }

}

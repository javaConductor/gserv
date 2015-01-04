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

package io.github.javaconductor.gserv.delegates.functions

import groovy.text.Template
import groovy.text.TemplateEngine
import io.github.javaconductor.gserv.converters.InputStreamTypeConverter
import io.github.javaconductor.gserv.exceptions.TemplateException

/**
 *
 * @author javaConductor
 */

trait ResourceHandlerFn {

    //LinkBuilder linkBuilder = new LinkBuilder()
    /**
     *  Create URI for the named Route
     *  NOTE: Route must be  known by the LinkBuilder
     *
     * @param name
     * @param data
     * @return String
     */
    def link(name, data) {
        linkBuilder.link(name, data)
    }

    /**
     * Returns function that write the content of 'filePath' after setting
     * contentType to 'contentType'
     *
     * @param contentType
     * @param filePath
     * @return Closure
     */
    def fileFn(contentType, filePath) {
        file(contentType, filePath)
    }

    /**
     *  Using the current TemplateManager, interpolate a
     *  template with data and return the result.
     *
     * @param templatePath
     * @param data
     */
    void template(templatePath, data) {
        template(null, templatePath, data);
    }

    /**
     *  Using the current TemplateManager, interpolate a
     *  template with data and return the result.
     *  The ContentType of response will be set to 'contentType'
     *
     * @param contentType
     * @param templatePath
     * @param data
     */
    void template(contentType, templatePath, data) {
        InputStream is = getFile(templatePath)
        if (!contentType)
            contentType = URLConnection.guessContentTypeFromStream(istream)

        if (!is)
            throw new TemplateException(templatePath, data, "Template $templatePath not found.  Check static_root definitions.")
        def templateText = new DataInputStream(is).readLines().join("\n")

        def tmgr = value("tmgr")
        TemplateEngine tengine = tmgr.getTemplateEngine(value("templateEngineName"))
        if (!tengine)
            throw new TemplateException(templatePath, data, "No such template engine: ${value("templateEngineName")}")

        Template t = tengine.createTemplate(templateText)
        if (!t)
            throw new TemplateException(templatePath, data, "Template engine: ${value("templateEngineName")} failed to create template using [$templatePath]")
        Writable content = t.make(data)
        StringWriter sw = new StringWriter()
        content.writeTo(sw)
        write(contentType, sw.toString())
    }

    /**
     * Sets the Content-Type Http Header on response
     *
     * @param ctype
     */
    void contentType(contentType) {
        header("Content-Type", contentType)
    }

    /**
     * Writes a string and closes the output stream
     *
     * @param data The string
     *
     */
    void write(String data) {
        write(data.getBytes())
    }

    /**
     * Sets the contentType header, writes a byte array to the outputStream, and closes the output stream
     *
     * @param contentTyp
     * @param data
     */
    void write(String contentTyp, byte[] data) {
        header("Content-Type", contentTyp)
        write(data)
    }
    /**
     * Writes a byteArray and closes the output stream
     *
     * @param data The string
     *
     */
    void write(byte[] data) {
        requestContext.sendResponseHeaders(200, data.size() as long)
        requestContext.getResponseBody().write(data)
        requestContext.getResponseBody().close()
        requestContext.close()
    }

    /**
     * Sets the contentType header, writes a string to the outputStream, and closes the output stream
     *
     * @param contentTyp
     * @param data
     */
    void write(String contentTyp, String data) {
        write(contentTyp, data.getBytes())
    }

    /**
     * Sets the response header
     *
     * @param headerName
     * @param headerValue
     */
    void header(String headerName, String headerValue) {
        requestContext.getResponseHeaders().put(headerName, headerValue);
    }

    /**
     *  Formats a Map of data into a JSON string and writes the string to the
     *  outputStream. Sets contentType to application/json
     *
     * @param dataMap
     */
    void writeJson(Map dataMap) {
        def json = groovy.json.JsonOutput.toJson(dataMap)
        write("application/json", json.getBytes())
    }

    void writeJSON(Map dataMap) {
        writeJson(dataMap)
    }
    /**
     *  Formats a List of data into a JSON string and writes the string to the
     *  outputStream. Sets contentType to application/json
     *
     * @param dataList
     */
    void writeJson(List dataList) {
        def json = groovy.json.JsonOutput.toJson(dataList)
        write("application/json", json.getBytes())
    }

    void writeJSON(List dataList) {
        writeJson(dataList)
    }

    /**
     * Sets status to 'code', writes message to outputStream and closes stream.
     *
     * @param code
     * @param message
     */
    void error(int code, String message) {
        message = message ?: "Error!"
        //println "requestHandlerDelegate.error($code, $message)"
        requestContext.sendResponseHeaders(code, message.bytes.size() as long)
        requestContext.getResponseBody().write(message.bytes)
        requestContext.getResponseBody().close()
        requestContext.close()
    }

    /**
     * Sends a HTTP Redirect (302) w/ updated 'Location' header
     * @param url
     */
    void redirect(url) {
        def message = "Resource has moved to: $url"
        requestContext.getHeaders().add("Location", url)
        requestContext.sendResponseHeaders(302, message.bytes.size() as long)
        requestContext.getResponseBody().write(message)
        requestContext.getResponseBody().close()
        requestContext.close()
    }

    /**
     * Returns InputStream to file 'fname'
     *
     * @param fname
     * @return InputStream
     */
    def getFile(String fname) {
        getFile(value("staticRoots"), fname)
    }

    def to = (new InputStreamTypeConverter().converters)
}


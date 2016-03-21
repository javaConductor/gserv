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

package io.github.javaconductor.gserv.delegates.functions

import groovy.text.Template
import groovy.text.TemplateEngine
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.exceptions.TemplateException
import io.github.javaconductor.gserv.utils.LinkBuilder
import io.github.javaconductor.gserv.utils.StaticFileHandler
import org.apache.commons.io.IOUtils

import java.lang.reflect.Modifier

/**
 *
 * @author javaConductor
 */

trait ResourceHandlerFn {

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

	List links(Object... stuff) {
		// call the linkFn defined inside the Action
		def fn = ((ResourceAction) $this).linksFunction()
		def ret = fn ? fn.call(*stuff)
				: []
		LinkBuilder.expandLinksIfNeeded(requestContext, ret)
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
	void template(String templatePath, data) {
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
	void template(String contentType, String templatePath, data) {
		InputStream is = getFile(templatePath)
		if (!is)
			throw new TemplateException(templatePath, data, "Template $templatePath not found.  Check static_root definitions.")
		if (!contentType)
			contentType = URLConnection.guessContentTypeFromStream(istream)

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
		responseHeader("Content-Type", contentType)
	}

	/**
	 * Writes a string and closes the output stream
	 *
	 * @param data The string
	 *
	 */
	void write(String data) {
		write(data.bytes)
	}

	/**
	 * Sets the contentType header, writes a byte array to the outputStream, and closes the output stream
	 *
	 * @param contentTyp
	 * @param data
	 */
	void write(String contentTyp, byte[] data) {
		responseHeader("Content-Type", contentTyp)
		write(data)
	}

	/**
	 * Writes a byteArray and closes the output stream
	 *
	 * @param data The string
	 *
	 */
	void write(byte[] data) {
		if (!requestContext.isClosed()) {
			log.trace("write(): Writing data, headers: ${requestContext.responseHeaders} for req $requestContext  - ${requestContext.id()}  - ${requestContext.hashCode()} ")
			requestContext.sendResponseHeaders(200, data.size() as long)
			requestContext.getResponseBody().write(data)
			requestContext.getResponseBody().close()
			requestContext.close()
		} else {
			log.trace("write(): Writing data to closed context.")
		}
	}

	/**
	 * Writes a byteArray and closes the output stream
	 *
	 * @param data The string
	 *
	 */
	void writeFrom(InputStream is) {
		if (!requestContext.isClosed()) {
			int size = IOUtils.copy(is, requestContext.getResponseBody())
			requestContext.sendResponseHeaders(200, size)
			requestContext.getResponseBody().close()
			requestContext.close()
		}
	}

	/**
	 * Sets the contentType header, writes a string to the outputStream, and closes the output stream
	 *
	 * @param contentType
	 * @param data
	 */
	void write(String contentTyp, String data) {
		write(contentTyp, data.bytes)
	}

	void sendFile(File file) {
		sendFile(file, null)
	}
	/**
	 * Sets the contentType header, writes the content of file to OutputStream, and closes the output stream
	 *
	 * @param contentType
	 * @param file
	 */
	void sendFile(File file, String contentTyp) {
		if (requestContext.isClosed()) {
			return
		}
		if (!contentTyp) {
			contentTyp = URLConnection.guessContentTypeFromName(file.name)
		}

		EventManager.instance().publish(Events.ResourceProcessing, [
				requestId: requestContext.id(),
				mimeType : contentTyp,
				msg      : "Sending static file.",
				path     : "${file.absolutePath}"])
		/// search the staticRoots
		if (!file.exists()) {
			def msg = "No such file: ${file.absolutePath}"
			error(404, msg)
		} else {
			InputStream is = new FileInputStream(file);
			writeFrom(is)
		}
		requestContext.close()
		requestContext
	}

	/**
	 * Gets  request header
	 *
	 * @param headerName
	 * @return String
	 */
	String requestHeader(String headerName) {
		this.requestContext.getRequestHeader(headerName);
	}

	/**
	 * Gets request headers
	 *
	 * @param headerName
	 * @return List < String >
	 */
	List<String> requestHeaders(String headerName) {
		this.requestContext.getRequestHeaders(headerName);
	}

	/**
	 * Gets  response header
	 *
	 * @param headerName
	 * @return String
	 */
	String responseHeader(String headerName) {
		this.requestContext.getResponseHeader(headerName);
	}

	/**
	 * Gets response headers
	 *
	 * @param headerName
	 */
	List<String> responseHeaders(String headerName) {
		this.requestContext.getResponseHeader(headerName);
	}

	/**
	 * Sets the response header
	 *
	 * @param headerName
	 * @param headerValue
	 */
	void responseHeader(String headerName, String headerValue) {
		requestContext.setResponseHeader(headerName, headerValue);
	}

	/**
	 * Sets response headers
	 *
	 * @param headerName
	 * @param headerValues List of strings
	 */
	void responseHeaders(String headerName, List headerValues) {
		//log.trace("#${requestContext.id()} Set Header[$headerName] = $headerValues")
		this.requestContext.setResponseHeaders([(headerName): headerValues]);
	}

	/**
	 *
	 * @param mimeTypes
	 * @return true if mimeTypes intersects the list of values for the Accept request header
	 */
	boolean accepts(String... mimeTypes) {
		(mimeTypes as List).any { mimeType ->
			requestHeaders("Accept").any { mtype ->
				log.debug("accepts: Comparing ${mtype.toUpperCase()} to ${mimeType.toUpperCase()}")
				println("accepts: Comparing ${mtype.toUpperCase()} to ${mimeType.toUpperCase()}")
				mtype.toUpperCase() == mimeType.toUpperCase()
			}
		}
	}

	/**
	 * Set the
	 * @param uri
	 */
	void location(String uri) {
		responseHeaders("Location", [uri]);
	}

	Map asMap(Object thing) {
		(thing.class.declaredFields as List).findAll {
			!it.synthetic && (it.modifiers | Modifier.STATIC)
		}.collectEntries {
			thing."$it.name" ? [(it.name): thing."$it.name"] : [:]
		}
	}

	void writeJson(Object bean) {
		writeJson(asMap(bean))
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

	@Deprecated
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

	@Deprecated
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
		if (!requestContext.isClosed()) {
			message = message ?: "Error!"
			//println "requestHandlerDelegate.error($code, $message)"
			requestContext.sendResponseHeaders(code, message.bytes.size() as long)
			requestContext.getResponseBody().write(message.bytes)
			requestContext.getResponseBody().close()
			requestContext.close()
		}
	}

	void done(int code) {
		if (-1 != code)
			requestContext.responseCode = code
		requestContext.close()
	}

	void done() {
		done(-1)
	}

	/**
	 * Sends a HTTP Redirect (302) w/ updated 'Location' header
	 * @param url
	 */
	void redirect(url) {
		redirect(url, 302)
	}

	/**
	 * Sends a HTTP Redirect w/ updated 'Location' header
	 *
	 * @param url
	 * @param statusCode One of: 302,303,307
	 */
	void redirect(url, statusCode) {
		if (![302, 303, 307].contains(statusCode)) {
			throw IllegalArgumentException("Status code for HTTP redirect must be 302, 303, or 307.")
		}
		def message = "Resource has moved to: $url".toString()
		//RequestContext requestContext
		requestContext.setResponseHeader("Location", url)
		requestContext.sendResponseHeaders(statusCode, message.bytes.size() as long)
		requestContext.getResponseBody().write(message.bytes)
		requestContext.getResponseBody().close()
		requestContext.close()
	}

	/**
	 * Returns InputStream to file 'fname'
	 *
	 * @param fname
	 * @return InputStream
	 */
	StaticFileHandler fh = new StaticFileHandler()

	def getFile(String fname) {
		fh.getFile(value("staticRoots"), fname)
	}

	def to = (value("to")) //serverConfig.inputStreamTypeConverter.converters
}


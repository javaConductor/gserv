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

import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 *
 * @author javaConductor
 */
trait FilterFn {

	/**
	 *
	 * @param code
	 * @param message
	 */
	void error(int code, String message) {
		if (!requestContext.isClosed()) {
			message = message ?: "Error!"
			//println "requestHandlerDelegate.error($code, $message)"
			requestContext.sendResponseHeaders(code, message.bytes.size())
			requestContext.getResponseBody().write(message.bytes)
			requestContext.getResponseBody().close()
			requestContext.close()
		}
	}
	/**
	 * Sends a HTTP Redirect (302) w/ updated 'Location' header
	 * @param url
	 */
	void redirect(url) {
		if (!requestContext.isClosed()) {
			def message = "Resource has moved to: $url"
			requestContext.getHeaders().add("Location", url)
			requestContext.sendResponseHeaders(302, message.bytes.size())
			requestContext.getResponseBody().write(message)
			requestContext.getResponseBody().close()
			requestContext.close()
		}
	}

	def matchedAction(requestContext) {
		value("serverConfig").matchAction(requestContext)
	}

	def requestMatched(requestContext) {
		value("serverConfig").matchAction(requestContext) || resolveStaticResource(
				requestContext.requestURI.path,
				value("serverConfig").staticRoots,
				value("serverConfig").useResourceDocs())
	}

	def _nextFilterCalled = false

	def nextFilter() {
		nextFilter(requestContext)
	}

	def nextFilter(RequestContext e) {
		if (_nextFilterCalled)
			throw new IllegalStateException("nextFilter() has already been called.")
		_nextFilterCalled = true
	}
}


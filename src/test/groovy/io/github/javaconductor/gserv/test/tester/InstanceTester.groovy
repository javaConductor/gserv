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

package io.github.javaconductor.gserv.test.tester

import groovyx.gpars.dataflow.DataflowVariable
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.requesthandler.ActionRunner
import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 * Created by lcollins on 12/19/2014.
 */
class InstanceTester {
	GServConfig config
	int port
	def stopFn
	ActionRunner runner

	InstanceTester(GServConfig config) {
		this.config = config
		runner = new ActionRunner(config)
	}

	def stop() {
		if (stopFn) stopFn()
	}

	def run(String method, String path) {
		run(method, [:], path, "", null)
	}

	def run(String method, String path, Closure callback) {
		run(method, [:], path, "", callback)
	}

	def run(String method, Map requestHeaders, String path) {
		run(method, requestHeaders, path, "", null)
	}

	def run(String method, Map requestHeaders, String path, byte[] data) {
		run(method, requestHeaders, path, data, null)
	}

	def run(String method, Map requestHeaders, String path, Closure callback) {
		run(method, requestHeaders, path, "", callback)
	}

	def run(String method, Map requestHeaders, String path, byte[] data, Closure callback) {
		DataflowVariable promise = new DataflowVariable();
		def port = 11001
		if (path.startsWith('/')) path = path.substring(1)

		RequestContext context = new TestRequestContext(method, requestHeaders, path, data, promise, callback)
		ResourceAction action = config.matchAction(context)
		if (!action) {
			promise << [statusCode: 404, responseHeaders: [:], output: "NO such action".bytes]
			callback(404, [:], "NO such action".bytes)
			return promise
		}

		runner.process(context, action)
		promise
	}
}

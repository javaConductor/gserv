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

package io.github.javaconductor.gserv.delegates

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.delegates.functions.ResourceHandlerFn
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.requesthandler.RequestContext
import io.github.javaconductor.gserv.templating.TemplateManager
import io.github.javaconductor.gserv.utils.StaticFileHandler

/**
 * This is the delegate for any HTTP Method handler closure
 */
@Mixin(StaticFileHandler)
class HttpMethodDelegate extends DelegateFunctions
		implements ResourceHandlerFn {
	RequestContext requestContext
	def serverConfig
	ResourceAction $this
	def eventManager = EventManager.instance()

	def HttpMethodDelegate(RequestContext requestContext, ResourceAction action, GServConfig serverConfig) {
		value("tmgr", new TemplateManager());
		value("linkBuilder", serverConfig.linkBuilder());
		value("staticRoots", serverConfig.staticRoots());
		value('inputStreamTypeConverter', serverConfig.inputStreamTypeConverter);
		value('to', serverConfig.inputStreamTypeConverter.converters);
		value("templateEngineName", serverConfig.templateEngineName());
		// $this inside the closure will be the currently processing action
		to = value('to')
		$this = action
		log.trace("Created httpMethodDelegate : $requestContext  ${this.hashCode()}")
		this.requestContext = requestContext
		this.serverConfig = serverConfig
	}

	def actions() {
		value("actions")
	}

}

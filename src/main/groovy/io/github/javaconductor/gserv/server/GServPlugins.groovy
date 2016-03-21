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

package io.github.javaconductor.gserv.server

import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.delegates.DefaultDelegates
import io.github.javaconductor.gserv.delegates.DelegatesMgr
import io.github.javaconductor.gserv.plugins.IPlugin

/**
 *
 * Container for the application of plugins.
 *
 */
class GServPlugins {
	def plugins = []

	def add(IPlugin p) {
		plugins.add(p)
	}

	/**
	 * Apply the plugins to a ServerConfiguration
	 *
	 * @param serverConfig
	 * @return GServConfig
	 */
	def applyPlugins(GServConfig serverConfig) {
		def delegates = prepareAllDelegates(DefaultDelegates.delegates)
		serverConfig.delegateManager(new DelegatesMgr(delegates))
		/// for each plugin we add to the actions, filters, and staticRoots
		//TODO plugins MAY also contribute to the Type formatter (to)
		plugins.each {
			serverConfig.addActions(it.actions())
					.addFilters(it.filters())
					.addStaticRoots(it.staticRoots())
		}
		serverConfig.delegateTypeMap(delegates)
		serverConfig
	}

	/**
	 * Apply each plugin to each delegate
	 *
	 * @param delegates
	 * @return preparedDelegates
	 */
	def prepareAllDelegates(delegates) {
		delegates.each { kv ->
			def delegateType = kv.key
			def delegateExpando = kv.value
			plugins.each { plugin ->
				if (!plugin) {
					println "Skipping null plugin in list"
				} else {
					plugin.decorateDelegate(delegateType, delegateExpando)// get the side-effect
				}
			}
		}
		delegates
	}
}

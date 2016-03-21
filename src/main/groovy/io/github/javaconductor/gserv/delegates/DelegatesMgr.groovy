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
import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 * Manages Delegates for an application
 */
class DelegatesMgr {
	def delegates

	/**
	 * Creates the creation and manipulation of delegates for an App.
	 *
	 * @param delegates Map (delegateType: delegateMetaClass)
	 *                      Optional. Custom default delegates Map
	 *
	 */
	def DelegatesMgr(delegates = null) {
		this.delegates = delegates ?: DefaultDelegates.delegates.clone()
	}

	private def prepareDelegate(delegateType, delegates, args) {
		/// delegates is a map of delegates by type -> ExpandoMetaClass
		/// we invoke the constructor and return the newly created object
		if (!delegates[delegateType])
			throw new IllegalArgumentException("$delegateType is not a valid delegate type.")
		if (args)
			delegates[delegateType].invokeConstructor(*args)
		else
			delegates[delegateType].invokeConstructor()
	}

	/**
	 * Creates a delegate instance of the specified type.
	 *
	 * @param delegateType httpMethod|filter|http|resource
	 * @param args Options to pass to the init() function of the delegate
	 * @return Delegate
	 */
	def createDelegate(delegateType, args) {
		prepareDelegate(delegateType, this.delegates, args)
	}

	/**
	 * Create an HttpMethodDelegate
	 *
	 * @param httpExchange
	 * @param serverConfig
	 * @return
	 */
	def createHttpMethodDelegate(RequestContext requestContext, ResourceAction action, GServConfig serverConfig) {
		createDelegate(DelegateTypes.HttpMethod, [requestContext, action, serverConfig])
	}

	/**
	 *  Creates delegate for Server Configuration Closure
	 *
	 * @return HttpDelegate
	 */
	def createHttpDelegate() {
		createDelegate(DelegateTypes.Http, null)
	}

	/**
	 *  Creates delegate for Server Configuration Closure
	 *  This delegate is used for HTTPS configurations
	 *
	 * @return HttpDelegate
	 */
	def createHttpsDelegate() {
		createDelegate(DelegateTypes.Https, null)
	}
}

/**
 * Constants for ALL Delegate Types
 */
class DelegateTypes {
	final static public String Http = "http"
	final static public String Https = "https"
	final static public String HttpMethod = "httpMethod"
	final static public String Resource = "resource"
	final static public String Filter = "filter"

}

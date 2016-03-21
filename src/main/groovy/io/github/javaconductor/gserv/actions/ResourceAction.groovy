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

package io.github.javaconductor.gserv.actions

import io.github.javaconductor.gserv.pathmatching.custom.CustomActionMatcher

/**
 * Represents a URI/HttpMethod/Behavior Combination.  The encapsulation of a resource.
 */
class ResourceAction {

	private def _urlPatterns
	private def _queryPattern
	private def _handler, _method
	private def _options
	private List<CustomActionMatcher> _customMatchers = []
	String name

	String toString() {
		return "$_method(/" + _urlPatterns.join("/") + ")"
	}

	def ResourceAction(method, urlPatterns, queryPattern, clHandler) {
		this(method, urlPatterns, queryPattern, [:], clHandler)
	}

	def ResourceAction(name, method, urlPatterns, ActionPathQuery queryPattern, Map options, clHandler) {
		this(method, urlPatterns, queryPattern, options, clHandler)
		this.name = name
	}

	def ResourceAction(method, urlPatterns, ActionPathQuery queryPattern, Map options, clHandler) {
		_queryPattern = queryPattern
		_urlPatterns = urlPatterns
		_handler = clHandler
		_method = method
		_options = options
	}

	//returns clone of handler Closure
	def requestHandler() {
		_handler.clone()
	}

	def method() { _method }

	Map options() { _options }
	//returns PathElement representing const or var
	def path(idx) {
		(idx >= 0 && idx < _urlPatterns.size()) ? _urlPatterns[idx] : null
	}

	//returns number of elements in path
	def pathSize() {
		_urlPatterns.size()
	}

	//returns list of elements in path
	def pathElements() {
		(_urlPatterns as List).asImmutable()
	}

	//returns number of query values
	def queryPatternSize() {
		_queryPattern.size()
	}

	ActionPathQuery queryPattern() {
		_queryPattern
	}

	List<CustomActionMatcher> customMatchers() {
		_customMatchers
	}

	def customMatchers(List<CustomActionMatcher> matchers) {
		_customMatchers = matchers
	}

	def customMatcher(CustomActionMatcher matcher) {
		_customMatchers << matcher
	}

	Map<String, Closure> _linksFunctions = [:]

	@Deprecated
	def linksFunctions(String name = "default") {
		_linksFunctions[name]
	}

	Closure linksFunction(String name = "default") {
		_linksFunctions[name]
	}

	def addLinksFunction(String name, Closure c) {
		_linksFunctions[name] = c
	}

	def addLinksFunction(Closure c) {
		addLinksFunction("default", c)
	}

	@Override
	int hashCode() {
		return (method() + pathElements().join('/')).hashCode()
	}

	@Override
	boolean equals(Object obj) {
		hashCode() == obj.hashCode()
	}
}

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

package io.github.javaconductor.gserv.pathmatching

import io.github.javaconductor.gserv.actions.ActionPathElement
import io.github.javaconductor.gserv.actions.ActionPathQuery
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.pathmatching.types.IntegerType
import io.github.javaconductor.gserv.pathmatching.types.NumberType
import io.github.javaconductor.gserv.pathmatching.types.PathElementType
import io.github.javaconductor.gserv.pathmatching.types.RegExType

import static io.github.javaconductor.gserv.utils.TextUtils.stripBackTicks

/**
 * Misc PathMatchingUtils for Pattern matching
 */
class PathMatchingUtils {

	static def removeAction(List actionList, ResourceAction action) {
		actionList.findAll { a ->
			!actionsMatchEqual(a, action)
		}
	}

	static def actionsMatchEqual(ResourceAction a, ResourceAction b) {
		if (a.method() != b.method())
			return false;
		def matches = elementsMatchEqual(a.pathElements(), b.pathElements());
		if (!matches)
			return false;
		queriesMatchEqual(a.queryPattern(), b.queryPattern())
	}

	static def elementsMatchEqual(List<ActionPathElement> a, List<ActionPathElement> b) {
		if ((a.size() != b.size())) return false;
		if (a.size() == 0)
			return true;

		def matches = elementsMatchEqual(a.head(), b.head());
		return matches && elementsMatchEqual(a.tail(), b.tail());
	}

	static def elementsMatchEqual(ActionPathElement aElement, ActionPathElement bElement) {

		if (aElement.isVariable() != bElement.isVariable())
			return false;
		if (!aElement.isVariable() && (aElement.text() != bElement.text()))
			return false;

		return true;
	}

	static def queriesMatchEqual(ActionPathQuery aQry, ActionPathQuery bQry) {
		def aKeys = aQry.queryKeys()
		def bKeys = bQry.queryKeys()
		if (aKeys.size() != bKeys.size()) return false;

		for (int x = 0; x > aKeys.size(); ++x) {
			if (aKeys[x] != bKeys[x])
				return false;
		}
		return true;
	}

	/**
	 * Returns true if name is neither a data nor a matching pattern
	 *
	 * @param name
	 * @return
	 */
	static def isValuePattern(name) {
		!(isMatchingPattern(name) || isDataOnlyPattern(name))
	}

	/**
	 * Returns true if name is matching pattern with ‘:’
	 *
	 * @param name
	 * @return
	 */
	static def isMatchingPattern(name) {
		(name?.startsWith(':'))
	}

	/**
	 *  Returns true if name  will not be used for Route matching but is data-only.
	 *  Qry params with this designation will passed to the HTTP Method handler
	 *
	 * @param name
	 * @return
	 */
	static def isDataOnlyPattern(name) {
		//  should start with ‘?:'
		(name?.startsWith('?:'))
	}

	static def queryStringToMap(qry) {
		def m = [:]
		if (qry) {
			def queries = qry.split("&")
			queries.each { pair ->
				if (pair) {
					def kv = pair.split("=")
					m[(kv[0])] = (kv.size() > 1 ? kv[1] : true)
				}
			}
		}
		m
	}

	static def hasType(pathElement) {
		//:name:Number
		getType(pathElement) != null
	}

	static def getType(pathElement) {
		//:name:Number
		// only look at variables
		if (!pathElement.startsWith(':')) {
			return null;
		}
		def parts = pathElement.split(":")
		parts = parts.findAll { it }// remove nulls
		if (parts.size() < 2)
			return null
		else
			return createType(parts[1])
	}

	static PathElementType createType(elementType) {
		switch (elementType) {
			case "Number":
			case "Integer":
//            case "List":
				createKnownType(elementType)
				break;
			default:
				def regEx = stripBackTicks(elementType)
				createRegExType(regEx)
				break;
		}
	}

	static def numberType = new NumberType()
	static def integerType = new IntegerType()

	static PathElementType createKnownType(elementType) {

		switch (elementType) {
			case "Number":
				return numberType
			case "Integer":
				return integerType
			default:
				return null
		}

	}

	static def valueAsType(elementType, value) {
		return createType(elementType)?.toType(value) ?: value
	}

	static def createRegExType(regEx) {
		new RegExType(regEx)
	}

	static def extractElement(pathElement) {
		def parts = pathElement.split(':')
		":${parts[1]}"
	}
}

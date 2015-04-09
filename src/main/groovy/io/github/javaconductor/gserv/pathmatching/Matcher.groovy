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

package io.github.javaconductor.gserv.pathmatching

import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.factory.GServFactory
import io.github.javaconductor.gserv.requesthandler.AbstractRequestContext
import io.github.javaconductor.gserv.requesthandler.RequestContext
import org.omg.CORBA.Request

@Log4j
class Matcher {

    /**
     *
     * @param actionList
     * @param context
     * @return
     */
    ResourceAction matchAction(List<ResourceAction> actionList, RequestContext context) {
        def ret = actionList.find { action ->
            //println "Check route: $p"
            (action.method() == context.requestMethod && match(action, context))
        }
        return ret;
    }

    /**
     *
     * @param actionList
     * @param uri
     * @param method
     * @param headers
     * @return
     */
    ResourceAction matchAction(List<ResourceAction> actionList, URI uri, String method, Map headers) {
        def context = new GServFactory().createRequestContext(method, uri, headers)
        matchAction(actionList, context)
    }
/**
 *
 * @param context
 * @param action
 * @return
 */
//@CompileStatic
    boolean matchCustomMatcher(RequestContext context, ResourceAction action) {
        // it matches if there are no custom matchers
        if (action.customMatchers().isEmpty()) {
            return true
        }
        def ret = action.customMatchers().every { CustomActionMatcher cm ->
            println("Matcher.matchCustomMatcher(): ${context.responseHeaders} -> $action")
            cm.matches((RequestContext) context, (ResourceAction) action)
        }
        ret
    }

    /**
     *
     * @param action
     * @param uri
     * @return
     */
    boolean match(ResourceAction action, RequestContext context) {
        URI uri = context.requestURI
        def parts = uri.path.split("/")
        parts = parts.findAll { p -> p }
        def a = action.pathSize()
        def b = parts.size()
        if (a != b)
            return false;

        // empty == empty
        if (a == 0) {
            /// check the customMatchers
            def ret = matchCustomMatcher(context, action)
            println "Matcher matching:  ${context.responseHeaders} = ${ret}"
            return ret
        }
        def ans
        for (int i = 0; i != action.pathSize(); ++i) {
            ans = matchActionPathSegment(action.path(i), parts[i])
            if (!ans)
                return false;
        }

        return matchCustomMatcher(context, action) && matchActionQuery(
                action.queryPattern()?.queryMap() ?: [:],
                (action.queryPattern()?.queryMatchMap()?.keySet()?.toList() ?: []),
                PathMatchingUtils.queryStringToMap(uri.query));
    }

    /**
     *
     * @param actionPathPattern
     * @param uriValue
     * @return
     */
    def matchActionPathSegment(actionPathPattern, uriValue) {
        //returns true if the a part of the path matches the equivalent part of the uri
        //	it matches if pathPattern not var and same as uriValue  NOW
        //	it matches if pathPattern is regExpr and the uriValue matches
        //	it matches if pathPattern is var and the uriValue is not empty NOW
        //	else it returns false NOW
        def actionPathText = actionPathPattern.text()
        if ('*' == actionPathText)
            return true

        if (actionPathPattern.isVariable()) {
            /// check for type compatibility
            if (actionPathPattern.type()) {
                return actionPathPattern.type().validate(uriValue)
            }
            /// check for presence only
            return !(!uriValue)
        }
        /// does it match what the route says
        (uriValue == actionPathText)
    }
/**
 *
 * @param qryMap
 * @param queryKeys
 * @param requestQueryMap
 * @return
 */
    boolean matchActionQuery(Map qryMap, List<String> queryKeys, Map requestQueryMap) {
        queryKeys.every { key ->
            (
                    (PathMatchingUtils.isValuePattern(qryMap[key]))
                            /// compare it if its a value
                            ? requestQueryMap[key] == qryMap[key]
                            /// just verify the value exists
                            : requestQueryMap[key]
            )
        }
    }
}

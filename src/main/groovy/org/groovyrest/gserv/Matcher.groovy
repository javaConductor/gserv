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

package org.groovyrest.gserv

import com.sun.net.httpserver.HttpExchange


class Matcher {

    //returns the pattern that matches the uri
    def matchRoute(routeList, HttpExchange exchange) {
        def uri = exchange.getRequestURI()
        def method = exchange.requestMethod
        return matchRoute(routeList, uri, method)
    }

    def matchRoute(routeList, URI uri, String method) {
        //loop thru the routeList calling match(pattern,uri) where the method matches til one returns true then returning that pattern
        def ret = routeList.find { p ->
            //println "Check route: $p"
            (p.method() == method && match(p, uri))
        }
        //if (ret) println "Matched route: $ret"
        return ret;
    }

    //returns true if uri matches pattern
    def match(Route route, uri) {
        def parts = uri.path.split("/")
        parts = parts.findAll { p -> p }
        def a = route.pathSize()
        def b = parts.size()
        if (a != b)
            return false;

        // empty == empty
        if (a == 0)
            return true

        for (int i = 0; i != route.pathSize(); ++i) {
            def ans = matchRoutePathSegment(route.path(i), parts[i]) &&
                    matchRouteQuery(
                            route.queryPattern().queryMap(),
                            (route.queryPattern().queryMatchMap().keySet().toList()),
                            Utils.queryStringToMap(uri.query));
            if (!ans)
                return false;
        }
        return true;
    }

    def matchRoutePathSegment(routePathPattern, uriValue) {
        //returns true if the a part of the path matches the equivalent part of the uri
        //	it matches if pathPattern not var and same as uriValue  NOW
        //	it matches if pathPattern is regExpr and the uriValue matches
        //	it matches if pathPattern is var and the uriValue is not empty NOW
        //	else it returns false NOW
        def routeText = routePathPattern.text()
        if ('*' == routeText)
            return true
        (routePathPattern.isVariable()) ? !(!uriValue) : (uriValue == routeText)
    }

    boolean matchRouteQuery(Map qryMap, List<String> queryKeys, Map requestQueryMap) {
        queryKeys.every { key ->
            (
                    (Utils.isValuePattern(qryMap[key]))
                            /// compare it if its a value
                            ? requestQueryMap[key] == qryMap[key]
                            /// just verify the value exists
                            : requestQueryMap[key]
            )
        }
    }
}

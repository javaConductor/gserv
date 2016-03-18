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

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.filters.Filter
import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 * Matches urls against registered filters.
 */
class FilterMatcher extends Matcher {

    @Override
    ResourceAction matchAction(List<ResourceAction> filterList, RequestContext requestContext) {
        //loop thru the actionList calling match(pattern,uri) where the method matches til one returns true then returning that pattern
        def ret = filterList.find { p ->
            def rmethod = p.method()
            def methodOk = (rmethod == '*' || rmethod == requestContext.requestMethod)
            def b = (methodOk && match(p, requestContext.requestURI))
            b
        }
        return ret;
    }

    List<Filter> matchFilters(List<Filter> filterList, RequestContext requestContext) {
        //loop thru the actionList calling match(pattern,uri) where the method matches til one returns true then returning that pattern
        def ret = filterList.findAll { p ->
            def rmethod = p.method()
            def methodOk = (rmethod == '*' || rmethod == requestContext.requestMethod)
            def b = (methodOk && match(p, requestContext))
            b
        }
        return ret;
    }

    @Override
    ResourceAction matchAction(List<ResourceAction> filterList, URI uri, String method, Map headers) {

        //loop thru the actionList calling match(pattern,uri) where the method matches til one returns true then returning that pattern
        def ret = filterList.find { p ->
            def rmethod = p.method()
            def methodOk = (rmethod == '*' || rmethod == method)
            def b = (methodOk && match(p, uri))
            b
        }
        return ret;
    }

    /**
     *
     * @param filter
     * @param uri Request URI
     * @return true if uri matches pattern
     */
    @Override
    boolean match(ResourceAction filter, RequestContext context) {
        URI uri = context.requestURI
        def parts = uri.path.split("/")
        parts = parts.findAll { p -> p }
        def a = filter.pathSize()
        // filters w/ empty path matches all
        if (a == 0)
            return matchCustomMatcher(context, filter)
        for (int i = 0; i != filter.pathSize(); ++i) {
            if (filter.path(i).text() == '**')
                return matchCustomMatcher(context, filter)
            def ans = (filter.path(i).text() == '*') || matchActionPathSegment(filter.path(i), parts[i])
            if (!ans)
                return false;
        }
        return matchCustomMatcher(context, filter)
    }

}

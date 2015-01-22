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

package io.github.javaconductor.gserv.plugins.caching

import io.github.javaconductor.gserv.factory.ResourceActionFactory
import io.github.javaconductor.gserv.filters.FilterOptions
import io.github.javaconductor.gserv.plugins.AbstractPlugin

/**
 * Created by javaConductor on 4/23/2014.
 */
class CachingPlugin extends AbstractPlugin {
    def options

    @Override
    def init(Object options) {
        this.options = options ?: [:]
        return null
    }

    /**
     * This function adds Plugin-specific methods and variables to the various delegateTypes
     *
     * @param delegateType
     * @param delegateMetaClass
     * @return
     */
    @Override
    MetaClass decorateDelegate(String delegateType, MetaClass delegateMetaClass) {
        if (delegateType == "http" || delegateType == "https") {
            def weakFn = createWeakDelegateFunction()
            def strongFn = createStrongDelegateFunction()
            delegateMetaClass.weakETag << weakFn
            delegateMetaClass.strongETag << strongFn
        }
        delegateMetaClass
    }

    private def createWeakDelegateFunction() {

        return { path, etagFn ->

            def weakHandler = etagFn
            /// we must create a beforeFilter to create am ETag value from the request before the output is generated.

            def f = ResourceActionFactory.createBeforeFilter("CachingBeforeFilter",
                    "GET",
                    path,
                    [(FilterOptions.PassActionParams)  : false,
                     (FilterOptions.MatchedActionsOnly): true],
                    1) {
                ->

                def calcETag = weakHandler(requestContext)
                //check the hdr
                def etagValue = requestContext.requestHeaders["If-None-Match"]
                if (etagValue == calcETag) {
                    def msg = "Unchanged."
                    error(304, msg)
                } else {
                    // etag it
                    etagIt(requestContext, calcETag, options)
                    //exchange.responseHeaders["ETag"] = calcETag
                    nextFilter()
                }
                return requestContext
            }
            // add it to the config
            addFilter(f)
            requestContext
        }
    }/// method

    def etagIt(requestContext, etag, options) {
        requestContext.responseHeaders["Cache-Control"] = "public, max-age=3600;"
        requestContext.responseHeaders["ETag"] = etag
    }

    private def createStrongDelegateFunction() {

        return { path, etagFn ->
            def strongHandler = etagFn
            /// we must create a afterFilter to create an ETag value from the output once it is generated.
            def f = ResourceActionFactory.createAfterFilter("CachingAfterFilter", "GET", path, [(FilterOptions.PassActionParams): false, (FilterOptions.MatchedActionsOnly): true], 9
            ) { context, data ->
                //check the hdr
                def calcETag = strongHandler(context, data)
                def etagValue = context.requestHeaders["If-None-Match"]
                if (etagValue && etagValue == calcETag) {
                    def msg = "Unchanged."
                    error(304, msg)
                } else {
                    // etag it
                    etagIt(context, calcETag, options)
                }
                data
            }

            // add it tot the config
            addFilter(f)
        }
    }//method
}/// class

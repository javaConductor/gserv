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

package io.github.javaconductor.gserv.filters

import io.github.javaconductor.gserv.ResourceAction


/**
 * Represents a URI/HttpMethod/Behavior Combination
 * for a request filter.
 */
class Filter extends ResourceAction {
    def order = 5
    def filterType = FilterType.Normal

    def Filter(name, method, urlPatterns, qryPattern, opts, beforeClosure) {
        super(name, method, urlPatterns, qryPattern, opts, beforeClosure);
    }
}
/**
 * Supported Filter Types
 */
enum FilterType {
    After, Normal, Before
}

/**
 * Filter options. Specified when creating filters.
 */
class FilterOptions {
    /**
     *  Boolean:    if true, any path values corresponding to path variables will be passed to the Filter def closure
     *  Note: Only applies to 'Before' Filters
     *
     */
    static final String PassActionParams =  'passActionParams'

    @Deprecated
    static final String PassRouteParams =PassActionParams

/**
     *  Boolean:    if true, the filter will only be activated when it matches a route
     */
    static final String MatchedActionsOnly = 'matchedActionsOnly'
}

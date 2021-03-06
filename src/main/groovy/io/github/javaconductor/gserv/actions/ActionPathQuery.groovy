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

import io.github.javaconductor.gserv.pathmatching.PathMatchingUtils

/**
 * Represents the Query portion of a URI.  Needed when using the queryString as part of the pattern match.
 */
class ActionPathQuery {

    // which are the keys to the queryValues that are mapped

    private String _queryString = ""
    private List _matchQueryKeys = []/// list of the keys that have data
    private Map _queryMap = [:]/// Map
    private List _dataQueryKeys = []

    def ActionPathQuery(String qString) {
        if (!qString) {
            return
        }
        _queryString = URLDecoder.decode(qString, "UTF-8")
        def queries = _queryString.split("&")
        _matchQueryKeys = queries.inject([]) { acc, qry ->
            def kv = qry.split("=")
            /// only record the ones that are used for matching:
            ///     -   nonOptional Query Params
            ///         and
            ///     -   fixed Query Param values
            /// the rest are NOT used in matching
            (PathMatchingUtils.isMatchingPattern(kv[1]) || PathMatchingUtils.isValuePattern(kv[1])) ?
                    (acc + [kv[0]]) : acc
        }

        _dataQueryKeys = queries.inject(_matchQueryKeys) { acc, qry ->
            def kv = qry.split("=")
            /// only record the ones that are used for data mapping:
            ///     -   optional Query Params
            (PathMatchingUtils.isDataOnlyPattern(kv[1])) ? (acc + [kv[0]]) : acc
        }
        _queryMap = PathMatchingUtils.queryStringToMap(_queryString)
    }

    String text() {
        _queryString
    }

    String toString() {
        return text()
    }

    Map<String, String> queryMap() {
        _queryMap
    }

    Map<String, String> queryMatchMap() {
        queryKeys().inject([:]) { dataMap, dk -> dataMap + ["$dk": _queryMap[dk]] }
    }

    Map queryDataMap() {
        (queryKeys() + dataKeys()).inject([:]) { dataMap, dk -> dataMap + ["$dk": _queryMap[dk]] }
    }

    List queryKeys() {
        _matchQueryKeys
    }

    List dataKeys() {
        _dataQueryKeys
    }

}

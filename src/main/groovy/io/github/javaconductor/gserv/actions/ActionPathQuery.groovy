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

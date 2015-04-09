package io.github.javaconductor.gserv.pathmatching

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 * Created by lcollins on 4/5/2015.
 */
class HeaderMatcher implements CustomActionMatcher {
    List<String> _values = []
    String _header

    HeaderMatcher(String hdr, String... values) {
        _header = hdr
        _values = values as List
    }

    boolean matches(RequestContext context, ResourceAction action) {
        def requestHdrValues = context.requestHeaders[_header]
        if (!requestHdrValues)
            return _values.isEmpty()// return true if both are empty
        _values.intersect(requestHdrValues)
//
//        _values.any{ matchValue ->
//            requestHdrValues.any { requestValue ->
//                matchValue == value
//            }
//        }
    }
}

package io.github.javaconductor.gserv.pathmatching

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.requesthandler.RequestContext
import groovy.transform.CompileStatic

/**
 * Created by lcollins on 4/5/2015.
 */
class AcceptsMatcher implements CustomActionMatcher {
    List<String> _mimeTypes = []

    AcceptsMatcher(String... mimeTypes) {
        _mimeTypes = mimeTypes as List
        println "${_mimeTypes.getClass().toString()}"
    }

    @CompileStatic
    boolean matches(RequestContext context, ResourceAction action) {
        List<String> types = context.requestHeaders["Accept"] as List
        if (!context.requestHeaders["Accept"])
            return false;
//        def ret = _mimeTypes.any{ String t ->
//            types.any { String accept ->
//                t == accept
//            }
//        }
        def ret = !_mimeTypes.disjoint(types)
        return ret
    }
}

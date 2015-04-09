package io.github.javaconductor.gserv.pathmatching

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 * Created by lcollins on 4/5/2015.
 */
class ContentTypeMatcher implements CustomActionMatcher {
    List<String> _mimeTypes = []

    ContentTypeMatcher(String... mimeTypes) {
        _mimeTypes = mimeTypes as List
    }

    boolean matches(RequestContext context, ResourceAction action) {
        def types = context.requestHeaders["Content-Type"]
        if (!types)
            return false;
        _mimeTypes.any { t ->
            types[0] == t
        }
    }
}

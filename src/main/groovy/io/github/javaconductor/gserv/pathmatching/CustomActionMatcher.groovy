package io.github.javaconductor.gserv.pathmatching

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 * Created by lcollins on 4/5/2015.
 */
interface CustomActionMatcher {
    boolean matches(RequestContext context, ResourceAction action)
}

package io.github.javaconductor.gserv.pathmatching.custom

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.exceptions.HttpErrorException
import io.github.javaconductor.gserv.requesthandler.RequestContext

/**
 * Created by lcollins on 4/5/2015.
 */
interface CustomActionMatcher {
    boolean matches(RequestContext context, ResourceAction action)
}

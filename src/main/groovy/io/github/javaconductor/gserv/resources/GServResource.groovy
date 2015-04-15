package io.github.javaconductor.gserv.resources

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.pathmatching.PathMatchingUtils
import io.github.javaconductor.gserv.delegates.ResourceDelegate
import io.github.javaconductor.gserv.utils.LinkBuilder

/**
 * An HTTP/REST resource definition.
 * These resources may be added to a ServerInstance which, when deployed,
 * will publish all imported resources.
 *
 */
class GServResource {
    def actions
    def basePath
    def linkBuilder

    /**
     *
     *
     * @param path URL Prefix
     * @param actionList List of ResourceActions (URLPrefix/Method/Behavior combination)
     * @param linkBldr The LinkBuilder to use with this Resource
     *
     */
    def GServResource(path, actionList, linkBldr) {
        actions = actionList
        basePath = path
        linkBuilder = linkBldr
    }

/**
 *
 * @param path
 * @return
 */
    def GServResource(String path) {
        basePath = path
        actions = []
        linkBuilder = new LinkBuilder(path)

    }

    /**
     *
     * Defines a resource that may be added to a ServerInstance
     *
     * @param basePath URL pattern prefix
     * @param definitionClosure The definition of the resource. All methods of ResourceDelegate are available.
     *
     * @return gServResource
     */
    static def Resource(basePath, Closure definitionClosure) {
        //println "GServ.resource(): Creating resource [$basePath]"
        def dgt = new ResourceDelegate(basePath);
        definitionClosure.delegate = dgt
        definitionClosure.resolveStrategy = Closure.DELEGATE_FIRST
        definitionClosure()
        def actions = definitionClosure.delegate.actions()
        def linkBldr = definitionClosure.delegate.linkBuilder()
        actions.each { ResourceAction action ->
            linkBldr.linksFunctions().each { linkFn ->
                action.addLinksFunction(linkFn.name, linkFn.linksFunction)
            }
        }

        return new GServResource(basePath, actions, linkBldr)
    }

    static def Resource(basePath, ResourceObject target) {
        def addAction = { actionList, action ->
            actionList = PathMatchingUtils.removeAction(actionList, action)
            actionList << action
            actionList
        };

        def addActions = { actionList, List newActions ->
            newActions.inject(actionList) { acc, action ->
                addAction(acc, action)
            }

        };

        target.resourceDefinitions.each { definitionClosure ->

            def dgt = new ResourceDelegate(basePath);
            definitionClosure.delegate = dgt
            definitionClosure.resolveStrategy = Closure.DELEGATE_FIRST
            definitionClosure()
            target.actions = addActions(target.actions, definitionClosure.delegate.patterns())
            target.linkBuilder += definitionClosure.delegate.linkBuilder()
        }

        ///add the 'links' function to the
        target.actions.each { ResourceAction action ->
            target.linkBuilder.linksFunctions().each { linkFn ->
                action.addLinksFunction(linkFn.name, linkFn.linksFunction)
            }
        }

        target
    }

}

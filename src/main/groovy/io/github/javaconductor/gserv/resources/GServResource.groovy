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

package io.github.javaconductor.gserv.resources

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.delegates.DelegatesMgr
import io.github.javaconductor.gserv.delegates.ResourceDelegate
import io.github.javaconductor.gserv.pathmatching.PathMatchingUtils
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
            target.actions = addActions(target.actions, definitionClosure.delegate.actions())
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

    static def prepareDelegate(delegate) {
        new DelegatesMgr()
    }

}

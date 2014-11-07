package org.groovyrest.gserv

import org.groovyrest.gserv.delegates.ResourceDelegate

/**
 * An HTTP/REST resource definition.
 * These resources may be added to a ServerInstance which, when deployed,
 * will publish all imported resources.
 *
 */
class GServResource {
    def routes
    def basePath
    def linkBuilder

    /**
     *
     *
     * @param path URL Prefix
     * @param routeList List of Routes (URLPrefix/Method/Behavior combination)
     * @param linkBldr The LinkBuilder to use with this Resource
     *
     */
    def GServResource(path, routeList, linkBldr) {
        routes = routeList
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
        def patterns = definitionClosure.delegate.patterns()
        def linkBldr = definitionClosure.delegate.linkBuilder()
        return new GServResource(basePath, patterns, linkBldr)
    }

    static def Resource(basePath, ResourceObject target) {
        //println "GServ.resource(): Creating resource [$basePath]"
        def definitionClosure = target.resourceDefinition
        def dgt = new ResourceDelegate(basePath);
        definitionClosure.delegate = dgt
        definitionClosure.resolveStrategy = Closure.DELEGATE_FIRST
        definitionClosure()
        target.routes = definitionClosure.delegate.patterns()
        target.linkBuilder = definitionClosure.delegate.linkBuilder()
        target
    }

}

class ResourceObject extends GServResource {
    Closure resourceDefinition
    ResourceObject(String path, Closure resourceDef) {
        super(path)
        this.resourceDefinition = resourceDef;
    }
}

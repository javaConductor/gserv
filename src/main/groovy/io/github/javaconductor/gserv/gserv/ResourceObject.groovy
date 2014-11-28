package io.github.javaconductor.gserv.gserv

import io.github.javaconductor.gserv.gserv.GServResource

/**
 * Created by javaConductor on 11/16/2014.
 */
class ResourceObject extends GServResource {
    List<Closure> resourceDefinitions = []

    ResourceObject(String path) {
        super(path)
    }

    def resource(Closure resourceDef) {
        resourceDefinitions << resourceDef
    }

}

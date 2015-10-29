package io.github.javaconductor.gserv.test.tester

import io.github.javaconductor.gserv.factory.GServFactory
import io.github.javaconductor.gserv.resources.GServResource

/**
 * Created by lcollins on 12/30/2014.
 */
class ResourceTester extends InstanceTester {
    def ResourceTester(GServResource resource) {
        super(new GServFactory().createGServConfig(resource.actions))
    }
}

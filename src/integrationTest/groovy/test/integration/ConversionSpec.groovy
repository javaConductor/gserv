package test.integration

import groovy.json.JsonSlurper
import groovyx.gpars.dataflow.Promise
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.factory.GServFactory
import io.github.javaconductor.gserv.resources.GServResource
import io.github.javaconductor.gserv.server.GServInstance
import io.github.javaconductor.gserv.test.tester.InstanceTester
import io.github.javaconductor.gserv.test.tester.ResourceTester
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

/**
 * Created by lcollins on 12/29/2014.
 */

class ConversionSpec {

    GServResource res = GServ.Resource("/app") {

        get('/') {
            responseHeader("Content-Type", "text/plain")
            write "Hello"
        }

        put('/') { instream ->
            responseHeader("Content-Type", "text/plain")
            def data = to.type(domainType.class, instream)
            write "text/plain", """age = ${data.age}, name = ${data.name}"""
        }

        get('/error') {
            responseHeader("Content-Type", "text/plain")
            throw new RuntimeException("Testing exceptions handling in tests.")
//            write "Hello"
        }
    }

    class domainType {
        int age
        String name

        def domainType(int age, String name) {
            this.age = age
            this.name = name
        }
    }

    GServInstance instance = new GServ().http {
        conversion(domainType.class) { instream ->
            def obj = new JsonSlurper().parse(instream)
            new domainType(obj.age as int, obj.name)
        }
        resource res
    }

    @Test
    void testException() {

        def gServFactory = new GServFactory()

        InstanceTester t = new InstanceTester(instance.config())
        t.run("GET", [:], "/app/error", null) { int statusCode, Map responseHeaders, byte[] output ->
            assert statusCode == 500
            def outputStr = new String(output)
            assert outputStr == "Testing exceptions handling in tests."
        }// run
    }//TEST

    @Test
    void testPromiseSample() {

        GServInstance instance = new GServ().http {
            conversion(domainType.class) { instream ->
                def obj = new JsonSlurper().parse(instream)
                new domainType(obj.age as int, obj.name)
            }
            resource res
        }

        def cfg = instance.config()

        InstanceTester t = new InstanceTester(cfg)

//        def run(String method, Map requestHeaders, String path, byte[] data) {

        def data = """{ "age" : 75, "name": "Elder James" }""".bytes
        Promise p = t.run("PUT", [:], "/app", data)

        assertNotNull(p.get())
        def respData = p.get()
        assertEquals(200, respData.statusCode)
        assertEquals(new String(respData.output), "age = 75, name = Elder James")
    }

}

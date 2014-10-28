package org.groovyrest.gserv.test.integration

import groovyx.net.http.HTTPBuilder
import org.groovyrest.gserv.GServRunner
import org.junit.Test
import spock.lang.Ignore

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class DefaultResourceSpec {
    def baseDir = "src/test/resources/test/integration/"

    @Test
    public final void testDefaultResource() {
        def port = "11001"
        def http = new HTTPBuilder("http://localhost:$port/")
        def dir = baseDir + "defaultResource"
        def args = ["-p", port,
                    "-s", dir,
                    "-d", "index.html"
        ]
        def stopFn = new GServRunner().start(args);

        http.request(GET, TEXT) { req ->

            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                println "Endpoint returned: ${reader.text}"
                assert resp.status == 200
                //stop the server
                stopFn()
            }
            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                assert false, "Not found."
                println 'Not found'
                //stop the server
                stopFn()
            }
            response.'500' = { resp ->
                assert false, "Internal Error."
                println '500 Error'
                //stop the server
                stopFn()
            }
        }
    }
}

package com.soulsys.gserv.test.integration

import com.soulsys.gserv.GServRunner
import org.junit.Test
import spock.lang.*
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.TEXT
import org.junit.Before
import org.junit.Test

/**
 * Created by lcollins on 10/5/2014.
 */
class ToUpperSpec {
def baseDir = "src/test/resources/test/integration/"

    @Test
    public  final void testToUpper() {

        def http = new HTTPBuilder( 'http://localhost:10000/upper/lowercaseword' )
        def dir = baseDir + "toUpper"
        def args = [ "-p", "10000",
                     "-r", dir+"/ToUpper.groovy"]
        def stopFn = new GServRunner().start(args);
        http.request(GET,TEXT) { req ->
            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                assert resp.status == 200
                resp.statusLine
                def text = reader.text;
                //stop the server
                stopFn()
                assert text == "LOWERCASEWORD"
            }
            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                println 'Not found'
            }
        }



    }

}

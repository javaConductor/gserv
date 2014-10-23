package org.groovyrest.gserv.test.integration

import org.groovyrest.gserv.GServRunner
import org.groovyrest.gserv.Utils
import org.groovyrest.gserv.utils.Encoder
import groovyx.net.http.HTTPBuilder
import org.junit.Ignore
import org.junit.Test

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class DocsSpec {
    def baseDir = "src/main/resources/docs/"

    @Ignore
    public final void testToServeDocs() {

        def http = new HTTPBuilder('http://localhost:11000/')
        def dir = baseDir + "app"
        def args = ["-p", "11000", "-d", "index.html",
                    "-s", dir]
        def stopFn = new GServRunner().start(args);

        while (10 != System.in.read()) {
            Thread.sleep(2000)
        }

        stopFn();
    }
}

package io.github.javaconductor.gserv.tester

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.GServFactory
import io.github.javaconductor.gserv.GServInstance
import io.github.javaconductor.gserv.configuration.GServConfig

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by lcollins on 12/19/2014.
 */
class InstanceTester {

    int port
    def stopFn
    InstanceTester(GServConfig config, port=55555 ){

        this.port = port
        /// create instance
        GServInstance inst = new GServFactory().createHttpInstance(config)
        def stopFn = inst.start(port)
    }

    def stop(){
        if(stopFn) stopFn()
    }

    def run(String method, String path, Closure callback){
        def port = 11001
        if (path.startsWith('/')) path = path.substring(1)
        def http = new HTTPBuilder("http://localhost:$port/$path")

        http.request(method, TEXT) { req ->

            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                callback(resp.status, resp.headers, reader.text.bytes )
            }

            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                callback(resp.status, resp.headers, "".bytes )
            }
        }
    }
}

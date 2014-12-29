package io.github.javaconductor.gserv.tester

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.GServFactory
import io.github.javaconductor.gserv.GServInstance
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.requesthandler.ActionRunner

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by lcollins on 12/19/2014.
 */
class InstanceTester {
    GServConfig config
    int port
    def stopFn
    ActionRunner runner

    InstanceTester(GServConfig config) {
        this.config = config
        runner = new ActionRunner(config)
    }

    def stop(){
        if(stopFn) stopFn()
    }

    def run(String method, String path, Closure callback) {
    }

    def run(String method, String path, String data, Closure callback) {

    }

    def run(String method, String path, byte[] data, Closure callback) {
        def port = 11001
        if (path.startsWith('/')) path = path.substring(1)



        new

                runner.process()
    }
}

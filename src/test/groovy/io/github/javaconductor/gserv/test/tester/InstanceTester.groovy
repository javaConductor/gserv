package io.github.javaconductor.gserv.test.tester

import groovyx.gpars.dataflow.DataflowVariable
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.requesthandler.ActionRunner
import io.github.javaconductor.gserv.requesthandler.RequestContext

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

    def stop() {
        if (stopFn) stopFn()
    }

    def run(String method, String path) {
        run(method, [:], path, "", null)
    }

    def run(String method, String path, Closure callback) {
        run(method, [:], path, "", callback)
    }

    def run(String method, Map requestHeaders, String path) {
        run(method, requestHeaders, path, "", null)
    }

    def run(String method, Map requestHeaders, String path, byte[] data) {
        run(method, requestHeaders, path, data, null)
    }

    def run(String method, Map requestHeaders, String path, Closure callback) {
        run(method, requestHeaders, path, "", callback)
    }

    def run(String method, Map requestHeaders, String path, byte[] data, Closure callback) {
        DataflowVariable promise = new DataflowVariable();
        def port = 11001
        if (path.startsWith('/')) path = path.substring(1)

        RequestContext context = new TestRequestContext(method, requestHeaders, path, data, promise, callback)
        ResourceAction action = config.matchAction(context)
        if (!action) {
            promise << [statusCode: 404, responseHeaders: [:], output: "NO such action".bytes]
            callback(404, [:], "NO such action".bytes)
            return promise
        }

        runner.process(context, action)
        promise
    }
}

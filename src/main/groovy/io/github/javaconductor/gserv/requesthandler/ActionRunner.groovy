package io.github.javaconductor.gserv.requesthandler

import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.Utils
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.delegates.HttpMethodDelegate
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events

/**
 * Created by lcollins on 12/16/2014.
 */
@Log4j
class ActionRunner {
    def _cfg

    ActionRunner(GServConfig cfg) {
        _cfg = cfg
    }

    private def prepareDelegate(HttpExchange httpExchange, action) {
        _cfg.delegateMgr.createHttpMethodDelegate(httpExchange, action, _cfg)
    }

    private def prepareArguments(uri, istream, action) {
        def args = []
        def method = action.method()
        if (method == "PUT" || method == "POST") {
            // add the data before the other args
            // data is a byte[]
            args.add(istream)
        }

        def pathElements = uri.path.split("/").findAll { !(!it) }
        //// loop thru the Patterns getting the corresponding uri path elements
        for (int i = 0; i != action.pathSize(); ++i) {
            def p = action.path(i)
            def pathElement = pathElements[i]
            if (p.isVariable()) {
                /// if variable has type, use type to convert string value
                if (p.type() != null)
                    pathElement = p.type().toType(pathElement)
                args.add(pathElement)
            }
        }

        def qmap = Utils.queryStringToMap(uri.query)
        action.queryPattern().queryKeys().each { k ->
            args.add(qmap[k])
        }
        //println "AsyncHandler.prepareArguments(): $args"
        // log.debug "AsyncHandler.prepareArguments(): $args"
        args
    }

    private def prepareClosure(exchange, action) {
        def cl = action.requestHandler()//_handler
        HttpMethodDelegate dgt = prepareDelegate(exchange, action)
        cl.delegate = dgt
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl
    }

    def process(HttpExchange exchange, action) {
        Closure cl = prepareClosure(exchange, action)
        def args = prepareArguments(exchange.requestURI, exchange.requestBody, action)
//        println "AsyncHandler.process(): Calling errorHandlingWrapper w/ args: $args"
        ({ clozure, argList ->
            try {
                EventManager.instance().publish(Events.ResourceProcessing, [
                        requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                        uri      : exchange.requestURI.path,
                        msg      : "Resource Processing."])
                //println "AsyncHandler.process(): closureWrapper: Calling request handler w/ args: $argList"
                log.trace "ActionRunner: Running req#${exchange.getAttribute(GServ.exchangeAttributes.requestId)} ${exchange.requestURI.path}"
                clozure(*argList)
                log.trace "ActionRunner: req#${exchange.getAttribute(GServ.exchangeAttributes.requestId)} ${exchange.requestURI.path} - Finished."
            } catch (Throwable e) {
                EventManager.instance().publish(Events.ResourceProcessingError, [
                        requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                        uri      : exchange.requestURI.path, msg: e.message, e: e])
                cl.delegate.error(500, e.message)
                log.error "ActionRunner: Error Running req#${exchange.getAttribute(GServ.exchangeAttributes.requestId)} ${exchange.requestURI.path} : ${e.message}", e
                exchange
            }
            finally {
                EventManager.instance().publish(Events.ResourceProcessed, [
                        requestId: exchange.getAttribute(GServ.exchangeAttributes.requestId),
                        uri      : exchange.requestURI.path, msg: 'Resource processing done.'])
            }
        })(cl, args);

    }

}

package io.github.javaconductor.gserv.requesthandler

import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.pathmatching.PathMatchingUtils
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

    private def prepareDelegate(RequestContext context, action) {
        _cfg.delegateMgr.createHttpMethodDelegate(context, action, _cfg)
    }

    private def prepareArguments(URI uri, InputStream istream, ResourceAction action) {
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

        def qmap = PathMatchingUtils.queryStringToMap(uri.query)
        action.queryPattern().queryKeys().each { k ->
            args.add(qmap[k])
        }
        //println "AsyncHandler.prepareArguments(): $args"
        // log.debug "AsyncHandler.prepareArguments(): $args"
        args
    }

    private def prepareClosure(RequestContext context, ResourceAction action) {
        def cl = action.requestHandler()//_handler
        HttpMethodDelegate dgt = prepareDelegate(context, action)
        cl.delegate = dgt
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl
    }

    def process(RequestContext context, ResourceAction action) {

        def currentReqId = context.getAttribute(GServ.contextAttributes.requestId)
        //log.trace "ActionRunner.process() req#${currentReqId} requestContext has ${context.requestBody.bytes.size()} bytes to read."
        log.trace "ActionRunner.process() req#${currentReqId}."

        Closure cl = prepareClosure(context, action)
        def args = prepareArguments(context.requestURI, context.requestBody, action)
//        println "AsyncHandler.process(): Calling errorHandlingWrapper w/ args: $args"
        ({ clozure, argList ->
            try {
                EventManager.instance().publish(Events.ResourceProcessing, [
                        requestId: currentReqId,
                        uri      : context.requestURI.path,
                        msg      : "Resource Processing."])
                //println "AsyncHandler.process(): closureWrapper: Calling request handler w/ args: $argList"
                log.trace "ActionRunner: Running req#${currentReqId} ${context.requestURI.path}"
                clozure(*argList)
                log.trace "ActionRunner: req#${context.getAttribute(GServ.contextAttributes.requestId)} ${context.requestURI.path} - Finished."
            } catch (Throwable e) {
                EventManager.instance().publish(Events.ResourceProcessingError, [
                        requestId: context.getAttribute(GServ.contextAttributes.requestId),
                        uri      : context.requestURI.path, msg: e.message, e: e])
                if (!context.isClosed()) {
                    cl.delegate.error(500, e.message)
                }
                log.error "ActionRunner: Error Running req#${context.getAttribute(GServ.contextAttributes.requestId)} ${context.requestURI.path} : ${e.message}", e
                context
            }
            finally {
                EventManager.instance().publish(Events.ResourceProcessed, [
                        requestId: context.getAttribute(GServ.contextAttributes.requestId),
                        uri      : context.requestURI.path, msg: 'Resource processing done.'])
            }
        })(cl, args);

    }

}

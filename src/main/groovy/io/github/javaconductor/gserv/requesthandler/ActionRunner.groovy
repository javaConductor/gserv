/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 - 2015 Lee Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.javaconductor.gserv.requesthandler

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.converters.FormData
import io.github.javaconductor.gserv.delegates.HttpMethodDelegate
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.exceptions.ConversionException
import io.github.javaconductor.gserv.pathmatching.PathMatchingUtils

import java.util.concurrent.atomic.AtomicLong

/**
 * Created by lcollins on 12/16/2014.
 */
@Slf4j
class ActionRunner {
    GServConfig _cfg
    private AtomicLong reqId = new AtomicLong(0L)

    ActionRunner(GServConfig cfg) {
        _cfg = cfg
    }

    private def prepareDelegate(RequestContext context, action) {
        _cfg.delegateMgr.createHttpMethodDelegate(context, action, _cfg)
    }

    private def convertStreamToType(RequestContext context, InputStream instream, Class type) {
        if (type.equals(String.class)) {
            return _cfg.converter().to.text(instream)
        }

        if (type.equals(GPathResult.class)) {
            return _cfg.converter().to.xml(instream)
        }

        if (type.equals(FormData.class)) {
            return _cfg.converter().to.formData(instream, context)
        }

        if (type.equals((new byte[0]).class)) {
            return instream.bytes
        }

        _cfg.converter().to.type(type, instream)
    }

    private def prepareArguments(RequestContext context, ResourceAction action) {
        URI uri = context.requestURI;
        InputStream istream = context.requestBody
        def args = []
        def method = action.method()
        if (method == "PUT" || method == "POST") {
            ////lets see  the Type of the first arg
            Class class1stArg = action.requestHandler().parameterTypes[0]
            log.trace("PUT/POST firstArg type: ${class1stArg.name}")
            if (class1stArg && !class1stArg.name.contains("java.lang.Object")) {
                def value
                try {
                    value = convertStreamToType(context, istream, class1stArg)
                } catch (Exception e) {
                    log.trace("Cannot convert input to class: ${class1stArg.name}", e)
                    throw new ConversionException("Cannot convert input to class: ${class1stArg.name}")
                }
                args.add(value)
            } else {
                // add the data before the other args
                // data is a byte[]
                args.add(istream)
            }
        }//PUTorPOST

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
        args
    }

    private def prepareClosure(RequestContext context, ResourceAction action) {
        def cl = action.requestHandler()//_handler
        HttpMethodDelegate dgt = prepareDelegate(context, action)
        cl.delegate = dgt
        log.debug("$context setting delegate to ${dgt.hashCode()}")
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl
    }

    def process(RequestContext context, ResourceAction action) {
        def currentReqId = context.id()
        if (!currentReqId) {
            currentReqId = reqId.addAndGet(1L)
            context.setAttribute(GServ.contextAttributes.requestId, currentReqId)
        }

        //log.trace "ActionRunner.process() req#${currentReqId} requestContext has ${context.requestBody.bytes.size()} bytes to read."
        log.trace "ActionRunner.process() #$context."

        Closure cl = prepareClosure(context, action)
        def args = prepareArguments(context, action)
//        println "AsyncHandler.process(): Calling errorHandlingWrapper w/ args: $args"
        //cl.delegate.requestContext = context
        ({ clozure, argList ->
            try {
                EventManager.instance().publish(Events.ResourceProcessing, [
                        requestId: currentReqId,
                        uri      : context.requestURI.path,
                        msg      : "Resource Processing."])
                //println "AsyncHandler.process(): closureWrapper: Calling request handler w/ args: $argList"
                log.trace "ActionRunner: Running req#${currentReqId} $context  ${context.id()}  ${context.hashCode()}  ${context.requestURI.path} - delegate ${cl.delegate.hashCode()} : context: ${cl.delegate.requestContext}  "
                clozure(*argList)
                log.trace "ActionRunner: req#${currentReqId} ${context.requestURI.path} - Finished."
            } catch (ConversionException e) {
                EventManager.instance().publish(Events.ResourceProcessingError, [
                        requestId: currentReqId,
                        uri      : context.requestURI.path, msg: e.message, e: e])
                if (!context.isClosed()) {
                    cl.delegate.error(403, e.message)
                }
                log.error "ActionRunner: Error Running req#${currentReqId} ${context.requestURI.path} : ${e.message}", e
                context
            } catch (Throwable e) {
                EventManager.instance().publish(Events.ResourceProcessingError, [
                        requestId: currentReqId,
                        uri      : context.requestURI.path, msg: e.message, e: e])
                if (!context.isClosed()) {
                    cl.delegate.error(500, e.message)
                }
                log.error "ActionRunner: Error Running req#${currentReqId} ${context.requestURI.path} : ${e.message}", e
                context
            }
            finally {
//                EventManager.instance().publish(Events.ResourceProcessed, [
//                        requestId: currentReqId,
//                        uri      : context.requestURI.path, msg: 'Resource processing done.'])
            }
        })(cl, args);

    }

}

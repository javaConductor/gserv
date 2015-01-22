package io.github.javaconductor.gserv.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import groovy.util.logging.Log4j
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.ResizeablePool
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.factory.GServFactory
import io.github.javaconductor.gserv.filters.FilterOptions
import io.github.javaconductor.gserv.pathmatching.FilterMatcher
import io.github.javaconductor.gserv.requesthandler.AsyncHandler
import io.github.javaconductor.gserv.requesthandler.FilterRunner
import io.github.javaconductor.gserv.requesthandler.RequestContext
import io.github.javaconductor.gserv.utils.ActorPool

/**
 * This is the Handler that is called for each request by the Java 1.6 HttpServer
 *
 */
@Log4j
class GServHandler implements HttpHandler {
    private def _factory = new GServFactory();
    private def _actions
    private def _staticRoots
    private def _templateEngineName
    private def _dispatcher, _handler
    private GServConfig _cfg
    private def _nuHandler, _nuDispatcher
    Calendar cal = new GregorianCalendar();
    Long reqId = 1;
    FilterMatcher m = new FilterMatcher()
    FilterRunner filterRunner
    /**
     * Create a handler with a specific configuration
     *
     * @param cfg
     * @return
     */
    def GServHandler(GServConfig cfg) {
        _cfg = cfg;
        filterRunner = new FilterRunner(_cfg)
        this._actions = cfg.actions()
        this._staticRoots = cfg.staticRoots()
        this._templateEngineName = cfg.templateEngineName()

        _nuHandler = {
            new AsyncHandler(cfg)
        }
        _handler = _nuHandler()

        def actors = new ActorPool(10, 40, new DefaultPGroup(new ResizeablePool(false)), _nuHandler);
        _nuDispatcher = {
            _factory.createDispatcher(actors, _cfg);
        }
        _dispatcher = _nuDispatcher();
        _dispatcher.start()
    }

    static Long requestId = 0L
    /**
     * This method is called for each request
     * This is called after the Filters and done.
     *
     * @param httpExchange
     */
    void handle(HttpExchange httpExchange) {
        RequestContext context = new GServFactory().createRequestContext(httpExchange)
        synchronized (requestId) {
            context.setAttribute(GServ.contextAttributes.requestId, ++requestId)
            log.trace("ServerHandler.handle(${httpExchange.requestURI.path}) #$requestId")
        }

        log.trace("ServerHandler.handle(${httpExchange.requestURI.path})  #$requestId: Finding filters... ")
        context.setAttribute(GServ.contextAttributes.serverConfig, _cfg)
        boolean matched = _cfg.requestMatched(context)
        def filters = m.matchFilters(_cfg.filters(), context)
        filters = filters.findAll { theFilter ->
            (!theFilter.options()[FilterOptions.MatchedActionsOnly]) ||
                    (theFilter.options()[FilterOptions.MatchedActionsOnly] && matched)
        }

        log.trace("ServerHandler.handle(${httpExchange.requestURI.path})  #$requestId: Running filters -> $filters")
        context = filterRunner.runFilters(filters, context)
        def t = "${(context.isClosed() ? ' Context CLOSED!' : '')}"
        log.trace("ServerHandler.handle(${httpExchange.requestURI.path})  #$requestId: After filters $t ")

        if (context.isClosed())
            return;

        log.trace("ServerHandler.handle(${httpExchange.requestURI.path}) #$requestId: unharmed by filters. ")
//        log.debug("ServerHandler.handle(${httpExchange.requestURI.path}) instream: ${httpExchange.requestBody} outstream: ${httpExchange.responseBody}")
        def currentReqId = context.getAttribute(GServ.contextAttributes.requestId)
        try {
            EventManager.instance().publish(Events.RequestRecieved, [
                    requestId: currentReqId,
                    method   : context.requestMethod,
                    uri      : context.requestURI,
                    headers  : context.requestHeaders])
            def start = cal.getTimeInMillis();
            _handle(context)
            EventManager.instance().publish(Events.RequestDispatched, [
                    requestId: currentReqId,
                    method   : context.requestMethod,
                    uri      : context.requestURI,
                    headers  : context.requestHeaders])
        } catch (Throwable e) {
            def msg = "Error req #${currentReqId} ${e.message} "
            log.error(msg, e)
            EventManager.instance().publish(Events.RequestProcessingError, [
                    requestId: currentReqId,
                    error    : "${msg}",
                    method   : context.requestMethod,
                    uri      : context.requestURI,
                    headers  : context.requestHeaders])
            context.sendResponseHeaders(500, msg.bytes.size())
            context.responseBody.write(msg.bytes)
            context.responseBody.close()
            context.close()
        }
    }

    private void _handle(RequestContext context) {
        log.trace("ServerHandler._handle(${context})")
        //TODO be ready to stop/start the actor when it returns with IllegalState (actor cannot recv messages)
        try {
            _dispatcher << [requestContext: context]
        } catch (IllegalStateException e) {
            _dispatcher = _nuDispatcher()
            _handle(context)
        }
        log.trace("ServerHandler._handle(${context}) Sent to dispatcher.")

    }
}

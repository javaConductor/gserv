package io.github.javaconductor.gserv.requesthandler

import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.GServ
import io.github.javaconductor.gserv.pathmatching.PathMatchingUtils
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.delegates.FilterDelegate
import io.github.javaconductor.gserv.filters.Filter
import io.github.javaconductor.gserv.filters.FilterOptions
import io.github.javaconductor.gserv.filters.FilterType

/**
 * Created by lcollins on 1/11/2015.
 */
@Log4j
class FilterRunner {
    def _serverConfig

    def FilterRunner(GServConfig config) {
        _serverConfig = config
    }

    RequestContext runFilters(List<Filter> filters, RequestContext c) {

        RequestContext context = c
        for (Filter f : filters) {
            log.trace("FilterRunner() running $f on $context")
            context = runFilter(f, context) ?: context
            if (context.isClosed()) {
                log.trace("FilterRunner() filter $f closed context")
                return context
            }
            log.trace("FilterRunner() running $f on $context")
        }
        context
    }

    RequestContext runFilter(Filter theFilter, RequestContext requestContext) {

        switch (theFilter.filterType) {
            case FilterType.After:
                //////////////////////////////////////////////////////////////////////
                /// We will set the delegate for the closure before we put in the list
                //////////////////////////////////////////////////////////////////////

                def fn = theFilter.requestHandler()
                fn.delegate = prepareDelegate(theFilter, requestContext)

                /// add after closure to PostProcessList
                def ppList = requestContext.getAttribute(GServ.contextAttributes.postProcessList) ?: []
                log.trace "FilterRunner: Scheduling after filter ${theFilter.name} for req #${requestContext.getAttribute(GServ.contextAttributes.requestId)} "
                ppList << fn
                requestContext.setAttribute(GServ.contextAttributes.postProcessList, ppList)
                return requestContext

            case FilterType.Before:
            case FilterType.Normal:
                try {
                    // run the filter
                    // replace with new context if returned
                    requestContext = process(theFilter, requestContext) ?: requestContext
                } catch (Throwable ex) {
                    //TODO must handle this better -
                    log.trace("FilterRunner: $theFilter threw exception: ${ex.message}.", ex)
                    log.error("FilterRunner: $theFilter threw exception: ${ex.message}.")
                } finally {
                }
                return requestContext

        }//switch
    }

    RequestContext process(Filter theFilter, RequestContext context) {
        Closure cl = theFilter.requestHandler()//_handler
        def options = theFilter.options()
        FilterDelegate dgt = prepareDelegate(theFilter, context)
        def errorHandlingWrapper = { clozure, List argList ->
            try {
                return clozure(*argList)
            } catch (Throwable e) {
                log.error("FilterRunner error: ${e.message}", e)
                e.printStackTrace(System.err)
                dgt.error(500, e.message)
                context
            }
        }

        //TODO Should be a cleaner way to do this
        cl.delegate = dgt
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        /// Pass the route variables to the behavior if option is set
        /// Else there will be no args
        List args = options[FilterOptions.PassRouteParams] ? prepareArguments(context, theFilter) : [];
        /// Execute the behavior for this filter
        def ret = errorHandlingWrapper(cl, args)
        ret
    }

    private def prepareDelegate(filter, requestContext, templateEngineName = "") {
        new FilterDelegate(filter, requestContext, _serverConfig, templateEngineName)
    }

    /**
     * Set up the args to be sent to a FilterBehaviorClosure
     *
     * @param context
     * @param theFilter
     * @return
     */
    private def prepareArguments(context, theFilter) {
        def uri = context.requestURI
        def args = []
        def method = theFilter.method()
        if (method == "PUT" || method == "POST") {
            // add the data before the other args
            // data is a byte[]
            args.add(context.getRequestBody())
        }

        def pathElements = uri.path.split("/").findAll { !(!it) }
        //// loop thru the Patterns getting the corresponding uri path elements
        for (int i = 0; i != theFilter.pathSize(); ++i) {
            def p = theFilter.path(i)
            def pathElement = pathElements[i];
            if (p.isVariable()) {
                args.add(pathElement)
            }
        }

        def qmap = PathMatchingUtils.queryStringToMap(uri.query)
        theFilter.queryPattern().queryKeys().each { k ->
            args.add(qmap[k])
        }
        args
    }

}

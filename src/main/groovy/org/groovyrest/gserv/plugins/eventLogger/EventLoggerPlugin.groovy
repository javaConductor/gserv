/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Lee Collins
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

package org.groovyrest.gserv.plugins.eventLogger

import org.groovyrest.gserv.Route
import org.groovyrest.gserv.RouteFactory
import org.groovyrest.gserv.events.EventManager
import org.groovyrest.gserv.filters.FilterOptions
import org.groovyrest.gserv.plugins.AbstractPlugin
import org.groovyrest.gserv.utils.DateUtils
import org.groovyrest.gserv.utils.StaticFileHandler
import groovy.util.logging.Log

@Mixin(DateUtils)
/**
 * This is a GServ plugin that listens to ALL (or configured) topics.
 * Provides a path (usually log.html) that returns a page listing matching events.
 * Provides filter where path = Config.url || '/log'
 *
 *
 * Created by javaConductor on 2/5/14.
 */
@Log
class EventLoggerPlugin extends AbstractPlugin {
    def eventTopicList = [:]
    def eventList = []
    def logSinceUrl = "/log/since/:utc"
    def logLastUrl = "/log/last/:count"
    def logUrl = "/log"
    def logTemplate = "/gserv/views/eventLoggerPlugin/index.html"
    def logPageUrl = "/logs.html"
    def logSincePrefix = "/log/since/:utc"
    def logLastPrefix = "/log/last/:count"
    def _staticFileHandler = new StaticFileHandler();

    @Override
    def init(Object options) {

        //listen to ALL the messages
        //TODO def fn = options.logFileName
        //TODO def f = new File(fn)

        logUrl = options.url ?: "/log"
        logSincePrefix = "$logUrl/since"
        logLastPrefix = "$logUrl/last"
        logSinceUrl = "$logSincePrefix/:utc"
        logLastUrl = "$logLastPrefix/:count"

        EventManager.instance().subscribe('*') { t, d ->
//            println "$t => " + new JsonBuilder(d).toPrettyString()
            addEvent(t, d)
        }
    }

    def addEvent(topic, evtObject) {
        def o = [data: evtObject]
        o.topic = topic
        o.when = evtObject.when
        evtObject.when = null
        synchronized (eventList) {
            eventList << o
            if (eventList.size() > 1000)
                eventList = eventList.tail();
        }

    }

    @Override
    List<Route> filters() {
        def f = RouteFactory.createBeforeFilterURLPattern("EventLogFilter", '*', "/", [(FilterOptions.PassRouteParams): false]) {
            ->
/*
            if (exchange.requestURI.path.startsWith(logSincePrefix)) {
                Date d = formatUTC(exchange.requestURI.path.substring(11))
                def events = eventsSince(d)
                events = prepareEvents(events)
                writeJson(events)
            } else if (exchange.requestURI.path.startsWith(logLastPrefix)) {
                def evtCount = Integer.parseInt(exchange.requestURI.path.substring(10))
                def events = lastEvents(evtCount)
                events = prepareEvents(events)
                writeJson(events)
            } else*/
            if (exchange.requestURI.path.equals(logUrl)) {
                def events = lastEvents(1000)
                events = prepareEvents(events)
                writeJson(events)
            } else {
                nextFilter()
            }
            exchange
        }
        [f]
    }

    @Override
    List<Route> routes() {
        def r = RouteFactory.createURLPattern(
                'GET',
                "$logPageUrl",
                [useRouteVariables: false],
                _staticFileHandler.fileFn("text/html", logTemplate))
        [r]
    }

    def prepareEvents(evts) {
        synchronized (evts) {
            def events = evts
                    .sort { a, b -> a.when.after(b.when) ? 1 : (b.when.after(a.when) ? -1 : 0) }
                    .collect { event ->
                [when : event.when.format("yyyy/MM/dd-hh:mm:ss:S"),
                 topic: event.topic,
                 data : event.data
                ]
            }
        }
    }

    def eventsSince(events, evtDate) {
        events.findAll { it.when.after(evtDate) || it.when.equals(evtDate) }
    }

    def lastEvents(evtCount) {
        synchronized (eventList) {
            def sz = eventList.size()
            def elist
            if (sz <= evtCount)
                elist = eventList
            else {
                def idx = sz - evtCount
                elist = eventList.subList(idx, sz)
            }
            elist.sort { a, b -> a.when.after(b.when) ? 1 : -1 }
        }
    }

    @Override
    MetaClass decorateDelegate(String delegateType, MetaClass delegateMetaClass) {
        delegateMetaClass
    }
}

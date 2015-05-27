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

package io.github.javaconductor.gserv.status

import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.factory.ResourceActionFactory
import io.github.javaconductor.gserv.filters.Filter
import io.github.javaconductor.gserv.requesthandler.RequestContext

import java.text.NumberFormat

/**
 * Created by lcollins on 4/30/2015.
 */
@Log4j
class StatisticsMgr {

    GServConfig cfg

    def StatisticsMgr() {
        listenUp()
    }
    /**
     *
     * @return The Status Filter
     */
    Filter createStatusFilter(GServConfig cfg) {
        this.cfg = cfg
        def filter = ResourceActionFactory.createBeforeFilter("gServStatus", "GET", cfg.statusPath() ?: '/status', [:], 0) { RequestContext requestContext, args ->
            def requestId = requestContext.id()

            log.trace("statusFilter(#${requestId} context: $requestContext")
            def totalMemory = Runtime.runtime.totalMemory()
            def freeMemory = Runtime.runtime.freeMemory()
            def maxMemory = Runtime.runtime.maxMemory()
            //def reqCount = this.requestCount.get()

            def createActionStats = { action ->
                def actionStats = getActionStats(action)
                def start = "<tr><td colspan='2'><ul>"
                def end = "</ul></td></tr>"
                start + actionStats.keySet().collect { k ->
                    "<li>$k - ${actionStats[k]}</li>".toString()
                }.join(' ') + end
            }

            def actionRows = """
            <tr>
                <th colspan="2" style="text-align: center;" >Actions</th>
            </tr>""" +
                    cfg.actions().collect { action ->
                        """<tr><td colspan="2" >$action</td></tr>""" + createActionStats(action)
                    }.join('\n');

            def statsRows = """<tr>
            <th colspan='2' style="text-align: center;"  > Statistics </th>
            </tr>

                    """ +
                    getStats().keySet().collect { k ->
                        """
            <tr>
            <th>$k</th>
                        <td>${getStats()[k]}</td>
            </tr>
                """

                    }.join('\n')


            String page = """
<!DOCTYPE html>
<html>
<head lang="en">
    <meta charset="UTF-8">
    <title>${cfg.name()} Status Page </title>
    <link href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css" rel="stylesheet">
    <script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.4/js/bootstrap.min.js"></script>
</head>
<body>
<table class='table table-bordered' style="width: 800px ;margin-right: auto; margin-left: auto;">
    <thead>
    <tr>
        <th colspan="2" style="text-align: center;" >${cfg.name()} Status Page</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <th>Total Memory</th>
        <td>${NumberFormat.getNumberInstance(Locale.US).format(totalMemory)} bytes</td>
    </tr>

    <tr>
        <th>Free Memory</th>
        <td>${NumberFormat.getNumberInstance(Locale.US).format(freeMemory)} bytes</td>
    </tr>

    <tr>
        <th>Memory Used</th>
        <td>${NumberFormat.getNumberInstance(Locale.US).format(totalMemory - freeMemory)} bytes</td>
    </tr>

    <tr>
        <th>Max Memory</th>
        <td>${NumberFormat.getNumberInstance(Locale.US).format(maxMemory)} bytes</td>
    </tr>""" + statsRows + actionRows +
                    """</tbody>
</table>

</body>
</html>
"""


            write "text/html", page
            requestContext
        }
        filter
    }

    def listenUp() {
        def eventHandler = { topic, data ->
            handleEvent(topic, data)
        }

        EventManager.instance().subscribe(Events.RequestRecieved, eventHandler)
        EventManager.instance().subscribe(Events.ResourceProcessed, eventHandler)
        EventManager.instance().subscribe(Events.RequestProcessingError, eventHandler)
        EventManager.instance().subscribe(Events.FilterError, eventHandler)
    }

    def statRecorders = [
            new SuccessFailure(),
            new AvgMinMaxReqTime()
    ]

    def actionStatRecorders = [
            new ActionSuccessFailure()
         //   new ActionAvgMinMaxReqTime()
    ]

    def handleEvent(topic, data) {
        log.debug("Statistics: $topic => $data")
        statRecorders.each { statRec ->
            statRec.recordEvent(topic, data)
        }

        actionStatRecorders.each {ActionStatRecorder actionStatRec ->
            ResourceAction action = cfg.matchAction(data.requestContext)
            if(action)
                actionStatRec.recordEvent(action, topic, data)
        }

    }

    def getStats() {
        def theStats = [:]
        statRecorders.each { statRec ->
            theStats += statRec.reportStat()
        }
        theStats
    }

    def getActionStats(ResourceAction action) {
        def theStats = [:]
        actionStatRecorders.each { actionStatRec ->
            theStats += actionStatRec.reportStat(action)
        }
        theStats
    }

}

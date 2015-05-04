package io.github.javaconductor.gserv.status

import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.factory.ResourceActionFactory
import io.github.javaconductor.gserv.filters.Filter
import io.github.javaconductor.gserv.requesthandler.RequestContext

import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by lcollins on 4/30/2015.
 */
@Log4j
class StatisticsMgr {


    def StatisticsMgr() {
        listenUp()
    }
    /**
     *
     * @return The Status Filter
     */
    Filter createStatusFilter(GServConfig cfg) {
        def filter = ResourceActionFactory.createBeforeFilter("gServStatus", "GET", cfg.statusPath() ?: '/status', [:], 0) { RequestContext requestContext, args ->
            def requestId = requestContext.id()

            log.trace("statusFilter(#${requestId} context: $requestContext")
            def totalMemory = Runtime.runtime.totalMemory()
            def freeMemory = Runtime.runtime.freeMemory()
            def maxMemory = Runtime.runtime.maxMemory()
            //def reqCount = this.requestCount.get()

            def actionRows = """
            <tr>
                <th colspan="2" style="text-align: center;" >Actions</th>
            </tr>""" +
                    cfg.actions().collect { action ->
                        """
                <tr>
                    <td colspan="2" >$action</td>
                </tr>
                """
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

    def handleEvent(topic, data) {
        log.debug("Statistics: $topic => $data")
        statRecorders.each { statRec ->
            statRec.recordEvent(topic, data)
        }

    }

    def getStats() {
        def theStats = [:]
        statRecorders.each { statRec ->
            theStats += statRec.reportStat()
        }
        theStats
    }

}

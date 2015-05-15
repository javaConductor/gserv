package io.github.javaconductor.gserv.status

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.requesthandler.RequestContext

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by lcollins on 5/3/2015.
 */
class ActionSuccessFailure implements ActionStatRecorder {

    //Map statsByAction = [:]
    ConcurrentMap statsByAction = new ConcurrentHashMap()
    def runningReqs = [] // RegId

    Map createActionStats(ResourceAction action){
        if (!statsByAction[action]){
            statsByAction[action] = [
                    SuccessfulRequests: new AtomicLong(0),
                    FailedRequests    : new AtomicLong(0)
            ]
        }
        statsByAction[action]
    }

    @Override
    def recordEvent(ResourceAction action, String topic, Map eventData) {

        def actionStats = createActionStats(action)
        switch (topic) {

            case Events.RequestRecieved:
                /// add to number of requests

                ///TODO : add action stuff here !!!

                /// add to waiting list
                runningReqs << eventData.requestId
                break;

            case Events.RequestProcessingError:
                if (runningReqs.contains(eventData.requestId)) {
                    actionStats.FailedRequests.incrementAndGet()
                    runningReqs.remove(eventData.requestId)
                }
                break;

            case Events.ResourceProcessed://
                // find the request in the eventDataByRequestId
                if (runningReqs.contains(eventData.requestId)) {
                    actionStats.SuccessfulRequests.incrementAndGet()
                    runningReqs.remove(eventData.requestId)
                }
                break;
        }
    }

    @Override
    Map reportStat(ResourceAction action) {
        def actionStats = createActionStats(action)
        [
                ('Successful Requests') : actionStats.SuccessfulRequests.get(),
                ('Failed Requests')     : actionStats.FailedRequests.get()
        ]
    }
}

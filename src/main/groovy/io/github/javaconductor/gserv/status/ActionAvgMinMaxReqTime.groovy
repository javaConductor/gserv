package io.github.javaconductor.gserv.status

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.events.Events

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by lcollins on 5/3/2015.
 */
class ActionAvgMinMaxReqTime implements ActionStatRecorder {

    //Map statsByAction = [:]
    ConcurrentMap statsByAction = new ConcurrentHashMap()
    def runningReqs = [:] // RegId

    Map createActionStats(ResourceAction action){
        if (!statsByAction[action]){
            statsByAction[action] = [
                    MaxRequestTime: new AtomicLong(0),
                    MinRequestTime: new AtomicLong(0),
                    AvgRequestTime: new AtomicLong(0),
                    totalMilliseconds: new AtomicLong(0),
                    totalRequests: new AtomicLong(0)
            ]
        }
        statsByAction[action]
    }

    @Override
    def recordEvent(ResourceAction action, String topic, Map eventData) {

        def actionStats = createActionStats(action)
        switch (topic) {

            case Events.RequestRecieved:
                /// add to waiting list
                runningReqs[eventData.requestId] = eventData
                break;

            case Events.RequestProcessingError:
            case Events.ResourceProcessed://
                // find the request in the eventDataByRequestId
                Date startTime = runningReqs[eventData.requestId]
                Date endTime = eventData.when
                def totalTime = endTime.time - startTime.time

                /// set the Average
                def totalMilliseconds = actionStats.totalMilliseconds.addAndGet(totalTime)
                def totalRequests = actionStats.totalRequests.addAndGet(1)
                actionStats['AvgRequestTime'].set(totalMilliseconds / totalRequests as long)

                /// set Min
                synchronized (actionStats['MinRequestTime']) {
                    if (0 == actionStats['MinRequestTime'].get() || totalTime < actionStats['MinRequestTime'].get()) {
                        actionStats['MinRequestTime'].set(totalTime as long)
                    }
                }

                /// set Max
                synchronized (actionStats['MaxRequestTime']) {
                    if (totalTime > actionStats['MaxRequestTime'].get()) {
                        actionStats['MaxRequestTime'].set(totalTime as long)
                    }
                }
                break;
        }
    }

    @Override
    Map reportStat(ResourceAction action) {
        def actionStats = createActionStats(action)
        [
                ('Avg Request Time') : actionStats.AvgRequestTime.get(),
                ('Min Request Time') : actionStats.MinRequestTime.get(),
                ('Max Request Time') : actionStats.MaxRequestTime.get()
        ]
    }
}

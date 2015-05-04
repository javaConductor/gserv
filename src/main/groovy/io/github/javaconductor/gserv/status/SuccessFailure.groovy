package io.github.javaconductor.gserv.status

import io.github.javaconductor.gserv.events.Events

import java.util.concurrent.atomic.AtomicLong

/**
 * Created by lcollins on 5/3/2015.
 */
class SuccessFailure implements StatRecorder {

    def stats = [
            SuccessfulRequests: new AtomicLong(0),
            FailedRequests    : new AtomicLong(0)
    ]

    def runningReqs = [] // RegId

    @Override
    def recordEvent(String topic, Map eventData) {

        switch (topic) {

            case Events.RequestRecieved:
                /// add to number of requests

                /// add to waiting list
                runningReqs << eventData.requestId
                break;

            case Events.RequestProcessingError:
                if (runningReqs.contains(eventData.requestId)) {
                    stats.FailedRequests.incrementAndGet()
                    runningReqs.remove(eventData.requestId)
                }
                break;

            case Events.ResourceProcessed://
                // find the request in the eventDataByRequestId
                if (runningReqs.contains(eventData.requestId)) {
                    stats.SuccessfulRequests.incrementAndGet()
                    runningReqs.remove(eventData.requestId)
                }
                break;

        }

    }

    @Override
    Map reportStat() {
        [
                'Successful Requests': stats.SuccessfulRequests.get(),
                'Failed Requests'    : stats.FailedRequests.get()
        ]
    }

}

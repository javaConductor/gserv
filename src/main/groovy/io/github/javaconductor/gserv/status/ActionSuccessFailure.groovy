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

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.events.Events

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

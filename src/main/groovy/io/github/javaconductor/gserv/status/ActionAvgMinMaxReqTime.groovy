/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2014-2016 Lee Collins
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
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
class ActionAvgMinMaxReqTime implements ActionStatRecorder {

    //Map statsByAction = [:]
    ConcurrentMap statsByAction = new ConcurrentHashMap()
    def runningReqs = [:] // RegId

    Map createActionStats(ResourceAction action) {
        if (!statsByAction[action]) {
            statsByAction[action] = [
                    MaxRequestTime: new AtomicLong(0),
                    MinRequestTime: new AtomicLong(0),
                    AvgRequestTime: new AtomicLong(0),
                    totalMilliseconds: new AtomicLong(0),
                    totalRequests : new AtomicLong(0)
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
        actionStats ?
                [
                        ('Avg Request Time'): actionStats.AvgRequestTime.get(),
                        ('Min Request Time'): actionStats.MinRequestTime.get(),
                        ('Max Request Time'): actionStats.MaxRequestTime.get()
                ] : [:]
    }

    def reset() {
        statsByAction.clear()
    }
}

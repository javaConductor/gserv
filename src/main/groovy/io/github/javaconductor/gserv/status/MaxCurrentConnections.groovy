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

import io.github.javaconductor.gserv.events.Events

import java.util.concurrent.atomic.AtomicLong

/**
 * Created by lcollins on 5/3/2015.
 */
class MaxCurrentConnections implements StatRecorder {

    AtomicLong startedRequests = new AtomicLong(0)
    AtomicLong endedRequests = new AtomicLong(0)
    AtomicLong maxConcurrentRequests = new AtomicLong(0)
    Date maxConcurrentDate

    @Override
    def recordEvent(String topic, Map eventData) {

        switch (topic) {

            case Events.RequestRecieved:
                /// add to number of requests
                startedRequests.incrementAndGet()
                break

            case Events.RequestProcessingError:
            case Events.ResourceProcessed://
                endedRequests.incrementAndGet()
                break
        }
        calcMax()
    }

    synchronized def calcMax() {
        def concurrentRequests = Math.max(0, startedRequests.get() - endedRequests.get() - 1)
        if (concurrentRequests > maxConcurrentRequests.get()) {
            maxConcurrentRequests.set(concurrentRequests)
            maxConcurrentDate = new Date()
        }
    }

    @Override
    Map reportStat() {
        [
                'Max Concurrent Requests': maxConcurrentRequests.get(),
                'Max Concurrent Time'    : maxConcurrentDate ? maxConcurrentDate.format("yyyy-MM-dd hh:mm:ss") : "None"
        ]
    }

    def reset() {
        maxConcurrentRequests.set(0)
        maxConcurrentDate = null
    }


}

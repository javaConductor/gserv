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

	def reset() {
		stats.SuccessfulRequests.set(0)
		stats.FailedRequests.set(0)
	}

}

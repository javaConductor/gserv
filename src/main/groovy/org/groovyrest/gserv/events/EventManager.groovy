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

package org.groovyrest.gserv.events

import groovyx.gpars.GParsPool
import groovyx.gpars.actor.Actors

/**
 * Created by javaConductor on 1/17/14.
 */

class _subscription {
    def topic
    def evtHandler // evtHandler(topic, data)
}

class _broadcast {
    def topic
    def data
    def when = new Date()
}

/**
 * Manages the Pub/Sub subSystem
 */
class EventManager {
    private def EventManager() {
    }
    private static theInstance = new EventManager()

    static instance() {
        theInstance
    }
    private def _listeners = []

    /**
     * Subscribe to a topic
     *
     * @param topic The Topic
     * @param evtHandler Code to call once the event occurs. signature: evtHandler(topic, data)
     *
     */
    def subscribe(topic, evtHandler) {
        _act << new _subscription([topic: topic, evtHandler: evtHandler])
    }

    /**
     * Publish a message to a topic
     *
     * @param topic
     * @param data
     *
     */
    def publish(topic, data) {
        data.'when' = new Date()
        try {
            _act << new _broadcast([topic: topic, data: data])
        }
        catch (Throwable ex) {
            System.err.println("EventManager.publish. Exception: ${ex.message}")
            ex.printStackTrace(System.err)
        }
    }

    private def _tellIt(topic, data, List listeners) {
        def needToKnow = listeners.findAll {
            it.topic == topic || it.topic == '*'
        }
        GParsPool.withPool {
//            needToKnow.eachParallel {
            needToKnow.eachParallel {
                it.evtHandler(topic, data)
            }
        }
    }

    private def _act = Actors.actor {
        loop {
            react {
                switch (it) {
                    case _subscription:
                        _listeners.add(it)
                        break;

                    case _broadcast:
                        def topic = it.topic
                        def data = it.data
                        _tellIt(topic, data, _listeners)
                        break;
                }
            }
        }
    }
}

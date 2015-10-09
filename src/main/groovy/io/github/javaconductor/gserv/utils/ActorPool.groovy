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

package io.github.javaconductor.gserv.utils

import groovyx.gpars.actor.Actor
import groovyx.gpars.group.PGroup

/**
 * Created by javaConductor on 5/4/2014.
 */
class ActorPool {
    def actorList = []
    def nextIdx = 0;
    def nuHandlerActorFn
    def min
//    PGroup pGroup

    def ActorPool(int min, Closure nuHandlerActorFn) {
        this.nuHandlerActorFn = nuHandlerActorFn
        this.min = min
        initList(actorList, min)
    }

    private def initList(List actorList, int min) {
        if (actorList.size() < min) {
            def diff = min - actorList.size();
            diff.times {
                add(nuHandlerActorFn())
            }
        }
    }

    synchronized Actor next() {
        if (actorList.empty) {
            throw new IllegalStateException("No actors in pool.")
        }
        Actor ret = actorList[nextIdx]
        nextIdx = ((nextIdx + 1) >= actorList.size()) ? 0 : ++nextIdx;
        ret
    }

    synchronized def replaceActor(actor) {
        remove(actor)
        def nuActor = nuHandlerActorFn()
        add(nuActor)
    }

    synchronized def add(Actor actor) {
        actor.start()
        actorList.add(actor)
    }

    synchronized def remove(Actor actor) {
        actor.stop()
        actorList.remove(actor)
    }

    def start() {
        actorList.each {
            it.start()
        }
    }

}

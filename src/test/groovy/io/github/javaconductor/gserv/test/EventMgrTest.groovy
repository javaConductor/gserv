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

package io.github.javaconductor.gserv.test

import io.github.javaconductor.gserv.events.EventManager
import org.junit.Before
import org.junit.Test

/**
 *
 */
class EventMgrTest {
    EventManager em

    @Before
    public final void init() {
        em = EventManager.instance();
    }

    @Test
    public final void testEventMgr() {
        def _topic, _data
        em.subscribe("test_test") { topic, data ->
            _data = data
            _topic = topic
        }
        em.publish("test_test", [msg: 'test'])
        Thread.sleep(500)
        assert "test_test" == _topic
        assert "test" == _data.msg
    }

    @Test
    public final void testEventMgr2() {
        def _topic, _data
        em.subscribe("test_test") { topic, data ->
            _data = data
            _topic = topic
        }
        em.publish("test_test", [msg: 'test'])
        Thread.sleep(1000)
        assert "test_test" == _topic
        assert "test2" != _data.msg
    }
}

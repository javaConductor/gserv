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

package test.integration

import com.github.restdriver.serverdriver.http.response.Response
import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.cli.GServRunner
import org.junit.Test
import spock.lang.Ignore

import static org.junit.Assert.*

import java.util.concurrent.TimeUnit

import static com.github.restdriver.serverdriver.Matchers.hasStatusCode
import static com.github.restdriver.serverdriver.RestServerDriver.getOf
import static com.github.restdriver.serverdriver.RestServerDriver.withTimeout
import static groovyx.gpars.GParsPool.withPool
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Created by javaConductor on 10/5/2014.
 */
@Slf4j
class LongRunningProcessSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Test
    @Ignore
    public final void test_10_secondRequests() {
        def port = 51038
        def dir = baseDir + "longrun"
        def args = ["-p", "$port",
                    "-i", dir + "/LongRun.groovy"]
        def stopFn = new GServRunner().start(args)
        try {
            def now = new Date().time
            log.debug "LongRunningProcess @$now"
            withPool(3) {
                [1, 2, 3].collectParallel { idx ->

                    try {
                        log.debug("LongRunningProcess: Response $idx started.")
                        Response r = getOf("http://localhost:$port/", withTimeout(11, TimeUnit.SECONDS))
                        assertThat(r, hasStatusCode(200))
                        log.debug("LongRunningProcess: Response $idx returned in time.")
                        return r
                    } catch (Exception e) {
                        log.debug("LongRunningProcess: Response $idx did not return in time.")
                        assertFalse("LongRunningProcess: Response $idx did not return in time.", true)
                    }
                }
            }
            def after = new Date().time
            //log.debug("LongRunningProcess: Whole thing took too much time.")
            assertTrue((after - now) < (11 * 1000))
        } finally {
            stopFn()
        }
    }

    @Test
    @Ignore
    public final void test_many_10_secondRequests() {
        def port = 51039
        def dir = baseDir + "longrun"
        def args = ["-p", "$port", "-x", "1000",
                    "-i", dir + "/LongRun.groovy"]
        def stopFn = new GServRunner().start(args)
        try {
            def now = new Date().time
            log.debug "LongRunningProcess @$now"
            withPool(1000) {
                (1..1000).collectParallel { idx ->

                    try {
//                        Thread.currentThread().sleep(50)
                        log.debug("LongRunningProcess: Response $idx started.")
                        Response r = getOf("http://localhost:$port/", withTimeout(15, TimeUnit.SECONDS))
                        def tm = (new Date().time - now)
                        assertThat(r, hasStatusCode(200))
                        log.debug("LongRunningProcess: Response $idx returned in time : ${tm} msecs.")
                        return r
                    } catch (Exception e) {
                        def tm = (new Date().time - now)
//                        log.debug("LongRunningProcess: Response $idx did not return in time.")
                        log.error("LongRunningProcess", e)
                        assertFalse("LongRunningProcess: Response $idx did not return in time: ${tm} msecs.", true)
                    }
                }
            }
            def after = new Date().time
            log.debug("LongRunningProcess: Whole thing took ${(after - now)} msecs.")
            assertTrue("${(after - now)} msecs.", (after - now) < (50 * 1000))
        } finally {
            stopFn()
        }
    }
}

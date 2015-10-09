package test.integration

import com.github.restdriver.serverdriver.http.response.Response
import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.cli.GServRunner
import org.junit.Test
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
            log.debug("LongRunningProcess: Whole thing took too much time.")
            assertTrue((after - now) < (11 * 1000))
        } finally {
            stopFn()
        }
    }

    @Test
    public final void test_many_10_secondRequests() {
        def port = 51039
        def dir = baseDir + "longrun"
        def args = ["-p", "$port",
                    "-i", dir + "/LongRun.groovy"]
        def stopFn = new GServRunner().start(args)
        try {
            def now = new Date().time
            log.debug "LongRunningProcess @$now"
            withPool(1000) {
                (1..1000).collectParallel { idx ->

                    try {
                        Thread.currentThread().sleep(50)
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
                        assertFalse("LongRunningProcess: Response $idx did not return in time:  ${tm} msecs.", true)
                    }
                }
            }
            def after = new Date().time
//            log.debug("LongRunningProcess: Whole thing took too much time.")
            assertTrue("${(after - now)} msecs.", (after - now) < (50 * 1000))
        } finally {
            stopFn()
        }
    }
}

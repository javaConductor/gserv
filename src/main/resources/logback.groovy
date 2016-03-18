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

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.status.OnConsoleStatusListener

// For syntax, see http://logback.qos.ch/manual/groovy.html
// Logging detail levels: TRACK > DEBUG > INFO > WARN > ERROR

displayStatusOnConsole()
scan('5 minutes') // periodically scan for log configuration changes
setupAppenders()
setupLoggers()

def displayStatusOnConsole() {
    // According to the "logback" documentation, always a good idea to add an on console status listener
    statusListener OnConsoleStatusListener
}

def setupAppenders() {
    appender("FILE", RollingFileAppender) {
        // add a status message regarding the file property
        addInfo("Setting [file] property to [gserv.log]")
        file = "/opt/gserv/log/gserv.log"
        rollingPolicy(TimeBasedRollingPolicy) {
            fileNamePattern = "/opt/gserv/log/gserv.log.%d{yyyy-MM-dd}.%i"
            timeBasedFileNamingAndTriggeringPolicy(SizeAndTimeBasedFNATP) {
                maxFileSize = "100MB"
            }
        }
        encoder(PatternLayoutEncoder) {
            pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{66} - %msg%n"
        }
    }
    appender("CONSOLE", ConsoleAppender) {
        encoder(PatternLayoutEncoder) {
            pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{66} - %msg%n"
        }
    }
}

def setupLoggers() {

    logger("groovyx.net.http", WARN, ["FILE"])
    logger("org.apache.http", ERROR, ["FILE"])
    logger("io.github.javaconductor.gserv", DEBUG, ["FILE"]) // gServ
    root(DEBUG, ["CONSOLE", "FILE"])
}

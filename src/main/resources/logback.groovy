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

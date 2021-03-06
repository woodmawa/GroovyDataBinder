/**
 * Created by willw on 08/06/2017.
 */


import com.softwood.logging.logback.AnsiConsoleAppender
import com.softwood.logging.logback.ColourConverter
import com.softwood.logging.logback.WhitespaceThrowableProxyConverter
import com.softwood.utilities.BinderHelper

import java.nio.charset.Charset

import static ch.qos.logback.classic.Level.*
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.status.OnConsoleStatusListener
import ch.qos.logback.core.FileAppender

//displayStatusOnConsole()
scan('5 minutes')  // Scan for changes every 5 minutes.
setupAppenders()
setupLoggers()


def displayStatusOnConsole() {
    statusListener OnConsoleStatusListener
}

def setupAppenders() {

    //inform logback of existence of custom colour converter
    conversionRule("clr", ColourConverter)
    conversionRule("wex", WhitespaceThrowableProxyConverter)

    def consolePatternFormat = "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr([proc:%property{PID} - thread:%thread]){magenta} %clr(---){faint} %clr(%logger{39}){cyan} %clr(>){faint} %m%n%wex"

    appender('STDOUT', com.softwood.logging.logback.AnsiConsoleAppender) {
        //withJansi = true
        encoder(PatternLayoutEncoder) {
            charset = Charset.forName('UTF-8')
            pattern = consolePatternFormat
        }
    }

    //def filePatternFormat = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%property{PID} - %thread] %-5level %-12logger{12}:[.%M] > %msg%n%wex"
    /*  PID defined if springboot app
    if (!System.getProperty("PID")) {
        System.setProperty("PID", (new ApplicationPid()).toString())
    }*/

    /* appender("RollingFile-Appender", RollingFileAppender) {
        file = "${LOG_PATH}/rollingfile.log"
        rollingPolicy(TimeBasedRollingPolicy) {
            fileNamePattern = "${LOG_ARCHIVE}/rollingfile.log%d{yyyy-MM-dd}.log"
            maxHistory = 30
            totalSizeCap = "1KB"
        }
        encoder(PatternLayoutEncoder) {
            pattern = "%msg%n"
        }
    }
    */

    //def logfileDate = timestamp('yyyy-MM-dd') // Formatted current date.
    // hostname is a binding variable injected by Logback.
    //def filePatternFormat = "%d{HH:mm:ss.SSS} %-5level [${hostname}] %logger - %msg%n"
    //appender('logfile', FileAppender) {
    //    file = "simple.${logfileDate}.log"
    //    encoder(PatternLayoutEncoder) {
    //        pattern = filePatternFormat
    //    }
    //}


}

def setupLoggers() {
    //logger 'com.mrhaki.java.Simple', getLogLevel(), ['logfile']
    logger 'com.softwood.databinder.Gbinder', DEBUG
    root WARN, ['STDOUT']
}

def getLogLevel() {
    (isDevelopmentEnv() ? DEBUG : INFO)
}

def isDevelopmentEnv() {
    def env =  System.properties['app.env'] ?: 'DEV'
    env == 'DEV'
}


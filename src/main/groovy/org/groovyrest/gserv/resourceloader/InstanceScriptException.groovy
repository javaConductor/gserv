package org.groovyrest.gserv.resourceloader

/**
 * Created by javaConductor on 10/13/2014.
 */
class InstanceScriptException extends Exception {
    InstanceScriptException() {
    }

    InstanceScriptException(String s) {
        super(s)
    }

    InstanceScriptException(String s, Throwable throwable) {
        super(s, throwable)
    }

    InstanceScriptException(Throwable throwable) {
        super(throwable)
    }
}

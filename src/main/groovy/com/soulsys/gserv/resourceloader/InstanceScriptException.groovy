package com.soulsys.gserv.resourceloader

/**
 * Created by lcollins on 10/13/2014.
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

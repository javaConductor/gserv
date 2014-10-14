package com.soulsys.gserv.resourceloader

/**
 * Created by javaConductor on 10/13/2014.
 */
class ResourceScriptException extends Exception {
    ResourceScriptException() {
    }

    ResourceScriptException(String s) {
        super(s)
    }

    ResourceScriptException(String s, Throwable throwable) {
        super(s, throwable)
    }

    ResourceScriptException(Throwable throwable) {
        super(throwable)
    }
}

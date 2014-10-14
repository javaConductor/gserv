package com.soulsys.gserv.resourceloader

/**
 * Created by lcollins on 10/13/2014.
 */
class ScriptClasspathException extends Exception {
    ScriptClasspathException() {
    }

    ScriptClasspathException(String s) {
        super(s)
    }

    ScriptClasspathException(String s, Throwable throwable) {
        super(s, throwable)
    }

    ScriptClasspathException(Throwable throwable) {
        super(throwable)
    }
}

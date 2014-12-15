package io.github.javaconductor.gserv.installer
/**
 * Created by lcollins on 10/31/2014.
 */
class InstallationException extends Exception {
    InstallationException(String s) {
        super(s)
    }

    InstallationException(String s, Throwable throwable) {
        super(s, throwable)
    }
}

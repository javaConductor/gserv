package io.github.javaconductor.gserv.exceptions

/**
 * Created by lcollins on 5/4/2015.
 */
class HttpErrorException extends Exception{
    def httpStatusCode
    HttpErrorException(int httpStatusCode, String message) {
        super(message)
        this.httpStatusCode=httpStatusCode
    }
}

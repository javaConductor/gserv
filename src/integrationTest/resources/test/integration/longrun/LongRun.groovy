package test.integration.longrun
/**
 * Created by javaConductor on 9/28/2014.
 */
import io.github.javaconductor.gserv.GServ

def gserv = new GServ();

gserv.http((Map) [:]) { ->

    get('/') { ->
        Thread.currentThread().sleep(10 * 1000);
        println("${Thread.currentThread().name} - ${$this} - ${requestContext.id()} writing")
        write("text/plain", "Took 10 seconds!");
        println("${Thread.currentThread().name} - ${$this} - ${requestContext.id()} written")
    }

    get('/:howLongSecs') { int seconds ->

        Thread.currentThread().sleep(seconds * 1000);

        write("text/plain", "Took $seconds seconds!");
    }

}

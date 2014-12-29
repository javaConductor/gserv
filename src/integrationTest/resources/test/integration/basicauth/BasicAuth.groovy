package test.integration.basicauth
/**
 * Created by javaConductor on 9/28/2014.
 */
import io.github.javaconductor.gserv.GServ

def gserv = new GServ();

gserv.http((Map) [:]) { ->
/*
    def basicAuthentication(methods, path, realm, challengeFn)
 */
    basicAuthentication(['GET', "POST", "PUT"], '/*', "testRealm") { user, pswd ->
        def ok = (user == "secret" && pswd == "thing")
        println "u=$user p=$pswd : ${ok ? "OK" : "FAILED"}";
        ok
    }

    get ( '/') { ->
        write("text/plain", "You MUST be authorized!!");
    }

    get ('/hello') { ->
        write("text/plain", "You SHOULD HAVE BEEN authorized!!");
    }

}

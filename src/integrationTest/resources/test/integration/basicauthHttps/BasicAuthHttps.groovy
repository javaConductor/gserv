package test.integration.basicauthHttps
/**
 * Created by javaConductor on 9/28/2014.
 */
import org.groovyrest.gserv.GServ

def gserv = new GServ();
/*
 "https":{
        "keyManagerAlgorithm": "SunX509",
        "trustManagerAlgorithm": "SunX509",
        "keystoreFilePath": "/Users/lcollins/gserv.keystore",
        "keyStoreImplementation": "JKS",
        "password": "remoteip",
        "sslProtocol" : "TLS"
    },
 */
gserv.https((Map) [
        https: [
                keyManagerAlgorithm   : "SunX509",
                trustManagerAlgorithm : "SunX509",
                keystoreFilePath      : "/Users/lcollins/gserv.keystore",
                keyStoreImplementation: "JKS",
                password              : "remoteip",
                sslProtocol           : "TLS"
        ]
]) { ->
/*
    def basicAuthentication(methods, path, realm, challengeFn)
 */
    basicAuthentication(['GET', "POST", "PUT"], "/*", "testRealm") { user, pswd ->
        def ok = (user == "secret" && pswd == "thing")
        println "u=$user p=$pswd : ${ok ? "OK" : "FAILED"}";
        ok
    }

    get("/") { ->
        write("text/plain", "You MUST be authorized!!");
    }

    get("/hello") { ->
        write("text/plain", "You SHOULD HAVE BEEN authorized!!");
    }

}

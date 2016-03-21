/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2014-2016 Lee Collins
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package test.integration.basicauthHttps
/**
 * Created by javaConductor on 9/28/2014.
 */
import io.github.javaconductor.gserv.GServ

def gserv = new GServ();

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

	basicAuthentication(['GET', "POST", "PUT"], "/*", "testRealm") { user, pswd, requestContext ->
		def ok = (user == "secret" && pswd == "thing")
		println "u=$user p=$pswd : ${ok ? "OK" : "FAILED"}";
		ok
	}

	get "/", { ->
		write "text/plain", "You MUST be authorized!!"
	}

	get "/hello", { ->
		write "text/plain", "You SHOULD HAVE BEEN authorized!!"

	}

}

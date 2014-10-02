/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Lee Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.soulsys.gserv.testclient

import com.soulsys.gserv.GServ

/**
 * Created with IntelliJ IDEA.
 * User: lcollins
 * Date: 12/30/13
 * Time: 9:31 AM
 * To change this template use File | Settings | File Templates.
 */

class TestClient {

    public static void main(String[] args) {
        new TestClient();
    }

    def TestClient() {

        def gserv = new GServ()
        def bkRes = gserv.resource("/books") {

            get { ->
                write("Books Resource !!!!!")
            }

        }

        def movieRes = gserv.resource("/movies") {
            get("/film") { ->
                write("Movies Resource !!!!!")
            }
        }

        gserv.http {
            println "TestClient(): GServ init"
            useResourceDocs(true)

            filter("/hello", "GET") { chain ->
                println "Filter: ${exchange.requestURI}"
                nextFilter()
            }
            resource(bkRes)
            resource(movieRes)
            get("/hello", {
                ->
                println "Inside the handler for /hello"
                write("Hello World")

                def data = '<dog size="med" >name</dog>'
                def thing = to.xml(new ByteArrayInputStream(data.getBytes()))
                println(thing)
                // System.err.println("Error in handler: ${e.message}")
            });

            get("/letter/:name", { name
                ->
                println "Inside the handler for /letter/:name"
                template("letter.html", [name: name, dateString: "December 25, 2000"])
            });

            filter("/letter/:name", "GET") { chain, name ->

                println "Filter: ${exchange.requestURI}"
                if (name == "Lee") {
                    error(400, "Cannot send letter to Lee")
                } else
                    nextFilter()

            }

//            cors("/letter/:name", allowAll(3600))
            get("/:greeting/:person", { greeting, person
                ->
                println "Inside the handler for /:greeting/:person"
                contentType("text/plain")
                write("$greeting $person")
            })

            get("/", file("text/html", "index.html"))

            get("/hello_t", {
                ->
                println "Inside the handler for /hello_t"
                template("index.html", [:])
            })

            get("/error", {
                ->
                println "Inside the handler for /error"
                throw new IllegalStateException("Test Exception: Bad state")
                //template("letter.html", [name: "Jane Doe", dateString: "December 25, 2000"])
            })

        }.start(8080)
        println "TestClient(): GServ.http started!"
    }
}

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

package org.groovyrest.gserv.test

import org.groovyrest.gserv.GServ
import org.groovyrest.gserv.plugins.PluginMgr
import org.groovyrest.gserv.plugins.compression.CompressionPlugin
import org.groovyrest.gserv.plugins.cors.CorsPlugin
import groovy.json.JsonBuilder

/**
 * Created with IntelliJ IDEA.
 * User: lcollins
 * Date: 12/30/13
 * Time: 9:31 AM
 * To change this template use File | Settings | File Templates.
 */

class TestApp {

    public static void main(String[] args) {
        new TestApp();
    }

    def TestApp() {

        EventMgr.instance.subscribe('testAppEvent') { t, d ->
            println "$t => " + new JsonBuilder(d).toPrettyString()
        }
        EventMgr.instance.subscribe('*') { t, d ->
            println "$t => " + new JsonBuilder(d).toPrettyString()
        }
        def pluginMgr = PluginMgr.instance()
        pluginMgr.register("cors", CorsPlugin.class)
        pluginMgr.register("compression", CompressionPlugin.class)
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

        gserv.plugins {
            plugin("cors", [:])
            plugin("compression", [:])
        }.http {
            cors("/letter/:name", allowAll(3600))
            println "TestApp(): GServ init"
            eventManager.publish("testAppEvent", [msg: "TestApp(): GServ init"])

            static_root("/home/lcollins/Documents")
            useResourceDocs(true)
            filter("", "GET") { chain ->
                println "Filter: ${exchange.requestURI}"
                nextFilter()

            }
            resource(bkRes)
            resource(movieRes)
            get("/hello", {
                ->
                eventManager.publish("testAppEvent", [msg: "Inside the handler for /hello"])
                write("Hello World")
                eventManager.publish("testAppEvent", [link: "${link('letter', [name: 'David'])}"])
                def data = '<dog size="med" >name</dog>'
                def thing = to.xml(new ByteArrayInputStream(data.getBytes()))
                println(thing)
                // System.err.println("Error in handler: ${e.message}")
            })

            get("letter", "/letter/:name", { name
                ->
                eventManager.publish("testAppEvent", [msg: "Inside the handler for /letter/:name"])
                // println "Inside the handler for /letter/:name"
                template("letter.html", [name: name, dateString: "December 25, 2000"])
            })

            filter("/letter/:name", "GET") { chain, name ->
                eventManager.publish("testAppEvent", [Filter: "${exchange.requestURI}"])
                if (name == "Lee") {
                    error(400, "Cannot send letter to Lee")
                } else
                    nextFilter()
            }

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
            })

        }.start(8080)
    }


}

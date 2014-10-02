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

package com.soulsys.gserv.test

import com.soulsys.gserv.GServ
import org.junit.Before

public class Stress2Test {
    def gserv
    def http

    @Before
    public final void init() {
        gserv = new GServ()
        http = gserv.http {

            assert get
            assert put
            assert delete
            assert post

            get("/:word") { word ->
                writeJson([word: word, upperCase: word.toUpperCase(), lowerCase: word.toLowerCase()])
            }
        }
        Thread.start { http.start(10000) }
        Thread.sleep(200)
    }

//    @Test
    public final void testRootPath() {
        1..1000.times { n ->

            http.request(GET, JSON) {
                uri.port = 10000;
                uri.path = "/WordsToUpAndLow$n"
                // uri.query = [ v:'1.0', q: 'Calvin and Hobbes' ]

                headers.'User-Agent' = 'Mozilla/5.0 Ubuntu/8.10 Firefox/3.0.4'

                // response handler for a success response code:
                response.success = { resp, json ->
                    println resp.statusLine

                    // parse the JSON response object:
                    json.responseData.results.each {
                        println "  ${it.titleNoFormatting} : ${it.visibleUrl}"
                    }
                }

                // handler for any failure status code:
                response.failure = { resp ->
                    println "Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}"
                }
            }


        }

        assert pat.pathSize() == 1;
        assert !m.match(pat, new URI("http://acme.com/the_thing/2many"))
    }

}

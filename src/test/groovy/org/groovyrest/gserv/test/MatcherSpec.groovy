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

import org.groovyrest.gserv.Matcher
import org.groovyrest.gserv.Route
import org.groovyrest.gserv.RouteFactory
import org.junit.Before
import org.junit.Test
import spock.lang.Specification

public class MatcherSpec extends Specification {

    Matcher m;

    public void setup() {
        m = new Matcher();
    }

    public void "route should be empty"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/",
                { ->

                })

        then:
        pat.pathSize() == 0;
    }

    public void "path size should be 1"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/thing",
                { ->

                })
        then:
        pat.pathSize() == 1;
    }

    public void "simple path size should be 1"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:thing",
                { ->

                })
        then:
        pat.pathSize() == 1;
    }

    public void "path size should be 2"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing",
                { o, t ->

                })

        then:
        pat.pathSize() == 2;
    }

    public void "should have 1 query param"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=:query",
                { o, t, q ->
                })
        then:
        pat.pathSize() == 2;
        pat.queryPattern().queryKeys().size() == 1;
    }

    public void "should have 2 query param"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=:query&q2=:again",
                { o, t, q ->
                })
        then:
        pat.pathSize() == 2;
        pat.queryPattern().queryKeys().size() == 2;
    }

    public void "should match path"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:thing",
                { ->

                })
        then:
        pat.pathSize() == 1;
        m.match(pat, new URI("http://acme.com/the_thing"))
    }

    public void "should not match path"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:thing",
                { ->

                })
        then:
        !m.match(pat, new URI("http://acme.com/the_thing/2many"))
    }

    public void "should match query variable"() {
        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=:query",
                { o, t, q ->
                })
        then:
        m.match(pat, new URI("/first/second?q1=third"))
        pat.queryPattern().queryKeys().size() == 1;
    }

    public void "should fail because no qry params"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=:query",
                { o, t, q ->
                });

        then:
        !m.match(pat, new URI("/first/second"));
    }

    public void "should match static query value"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=query",
                { o, t, q ->
                });

        then:
        m.match(pat, new URI("/first/second?q1=query"));
    }


    public final void "should match static and variable query values"() {
        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=query&yo=:yo",
                { o, t, q ->
                })

        then:
        m.match(pat, new URI("/first/second?yo=true&q1=query"));
    };

    public void "should fail with wrong static query value"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=query",
                { o, t, q ->
                });

        then:
        !m.match(pat, new URI("/first/second?q1=notRight"));
    }

    public void "should fail query match but not path"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=query",
                { o, t, q ->
                });

        then:
        !m.match(pat, new URI("/first/second/third?q1=query"));
    }

    public void "should match path with type"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:thing:Number",
                { ->

                })
        then:
        pat.pathSize() == 1;
        m.match(pat, new URI("http://acme.com/009"))
    }

    public void "should not match path with type"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:thing:Number",
                { ->

                })
        then:
        pat.pathSize() == 1;
        !m.match(pat, new URI("http://acme.com/notnumber"))
    }

    public void "should NOT match int path with double value"() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:thing:Integer",
                { ->

                })
        then:
        pat.pathSize() == 1;
        !m.match(pat, new URI("http://acme.com/75.9"))
    }

    public void "should match regEx path."() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                URLEncoder.encode('/:thing:`\\d*`', "UTF-8"),
                { ->

                })
        then:
        pat.pathSize() == 1;
        m.match(pat, new URI("http://acme.com/75"))
    }

    public void "should not match regEx path."() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                URLEncoder.encode('/:thing:`\\d*`', "UTF-8"),
                { ->

                })
        then:
        pat.pathSize() == 1;
        !m.match(pat, new URI("http://acme.com/notnumber"))
    }

    public void "should match regEx path w space."() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                URLEncoder.encode('/:thing:`\\d_\\d*`', 'UTF-8'),
                { ->

                })
        then:
        pat.pathSize() == 1;
        m.match(pat, new URI("http://acme.com/7_5"))
    }

    public void "should not match regEx path too many spaces."() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                URLEncoder.encode('/:thing:`\\d\\s\\d*`', 'UTF-8'),
                { ->

                })
        then:
        pat.pathSize() == 1;
        !m.match(pat, new URI("http://acme.com/7%20%205"))
    }

    public void "should match regEx path 3 spaces."() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                URLEncoder.encode('/:thing:`\\d\\s\\s\\s\\d*`', 'UTF-8'),
                { ->

                })
        then:
        pat.pathSize() == 1;
        m.match(pat, new URI("http://acme.com/7%20%20%205"))
    }

    public void "should match SSN regEx."() {

        when:
        Route pat = RouteFactory.createURLPattern(
                "GET",
                URLEncoder.encode('/:thing:`\\d\\d\\d-\\d\\d-\\d\\d\\d\\d`', 'UTF-8'),
                { ssn ->

                })
        then:
        pat.pathSize() == 1;
        m.match(pat, new URI("http://acme.com/359-99-7447"))
    }

}

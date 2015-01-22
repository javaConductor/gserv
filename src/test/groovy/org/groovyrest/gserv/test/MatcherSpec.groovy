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

package io.github.javaconductor.gserv.test

import io.github.javaconductor.gserv.pathmatching.Matcher
import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.factory.ResourceActionFactory
import spock.lang.Specification

public class MatcherSpec extends Specification {

    Matcher m;

    public void setup() {
        m = new Matcher();
    }

    public void "route should be empty"() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                "/",
                { ->

                })

        then:
        pat.pathSize() == 0;
    }

    public void "path size should be 1"() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                "/thing",
                { ->

                })
        then:
        pat.pathSize() == 1;
    }

    public void "simple path size should be 1"() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                "/:thing",
                { ->

                })
        then:
        pat.pathSize() == 1;
    }

    public void "path size should be 2"() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                "/:other/:thing",
                { o, t ->

                })

        then:
        pat.pathSize() == 2;
    }

    public void "should have 1 query param"() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
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
        ResourceAction pat = ResourceActionFactory.createAction(
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
        ResourceAction pat = ResourceActionFactory.createAction(
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
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                "/:thing",
                { ->

                })
        then:
        !m.match(pat, new URI("http://acme.com/the_thing/2many"))
    }

    public void "should match query variable"() {
        when:
        ResourceAction pat = ResourceActionFactory.createAction(
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
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                "/:other/:thing?q1=:query",
                { o, t, q ->
                });

        then:
        !m.match(pat, new URI("/first/second"));
    }

    public void "should match static query value"() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                "/:other/:thing?q1=query",
                { o, t, q ->
                });

        then:
        m.match(pat, new URI("/first/second?q1=query"));
    }


    public final void "should match static and variable query values"() {
        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                "/:other/:thing?q1=query&yo=:yo",
                { o, t, q ->
                })

        then:
        m.match(pat, new URI("/first/second?yo=true&q1=query"));
    };

    public void "should fail with wrong static query value"() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                "/:other/:thing?q1=query",
                { o, t, q ->
                });

        then:
        !m.match(pat, new URI("/first/second?q1=notRight"));
    }

    public void "should fail query match but not path"() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                "/:other/:thing?q1=query",
                { o, t, q ->
                });

        then:
        !m.match(pat, new URI("/first/second/third?q1=query"));
    }

    public void "should match path with type"() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
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
        ResourceAction pat = ResourceActionFactory.createAction(
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
        ResourceAction pat = ResourceActionFactory.createAction(
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
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                ('/:thing:`\\d*`'),
                { ->

                })
        then:
        pat.pathSize() == 1;
        m.match(pat, new URI(URLDecoder.decode("http://acme.com/75", "UTF-8")))
    };

    public void "should not match regEx path."() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                ('/:thing:`\\d*`'),
                { ->

                })
        then:
        pat.pathSize() == 1;
        !m.match(pat, new URI("http://acme.com/notnumber"))
    }

    public void "should match regEx path w space."() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                ('/:thing:`\\d_\\d*`'),
                { ->

                })
        then:
        pat.pathSize() == 1;
        m.match(pat, new URI("http://acme.com/7_5"))
    }

    public void "should not match regEx path too many spaces."() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                ('/:thing:`\\d\\s\\d*`'),
                { ->

                })
        then:
        pat.pathSize() == 1;
        !m.match(pat, new URI("http://acme.com/7%20%205"))
    }

    public void "should match regEx path 3 spaces."() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                ('/:thing:`\\d\\s\\s\\s\\d*`'),
                { ->

                })
        then:
        pat.pathSize() == 1;
        m.match(pat, new URI("http://acme.com/7%20%20%205"))
    }

    public void "should match SSN regEx."() {

        when:
        ResourceAction pat = ResourceActionFactory.createAction(
                "GET",
                ('/:thing:`\\d\\d\\d-\\d\\d-\\d\\d\\d\\d`'),
                { ssn ->

                })
        then:
        pat.pathSize() == 1;
        m.match(pat, new URI("http://acme.com/359-99-7447"))
    }

}

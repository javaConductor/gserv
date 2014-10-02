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

import com.soulsys.gserv.Matcher
import com.soulsys.gserv.Route
import com.soulsys.gserv.RouteFactory
import org.junit.Before
import org.junit.Test

public class MatcherTest {

    Matcher m;

    @Before
    public final void init() {
        m = new Matcher();
    }

    @Test
    public final void testRootPath() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "http://x.y.com",
                {  ->

                })
        assert pat.pathSize() == 0;
    }

    @Test
    public final void testPath1() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "http://x.y.com/thing",
                {  ->

                })
        assert pat.pathSize() == 1;
    }

    @Test
    public final void testPathVar() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:thing",
                {  ->

                })
        assert pat.pathSize() == 1;
    }

    @Test
    public final void testPath2Var() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing",
                { o, t ->

                })
        assert pat.pathSize() == 2;
    }

    @Test
    public final void testPathVarWithQuery() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=:query",
                { o, t, q ->
                })
        assert pat.pathSize() == 2;
        assert pat.queryPattern().queryKeys().size() == 1;
    }

    @Test
    public final void testPathVarWith2Query() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=:query&q2=:again",
                { o, t, q ->
                })
        assert pat.pathSize() == 2;
        assert pat.queryPattern().queryKeys().size() == 2;
    }

    @Test
    public final void testPathVarMatch() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:thing",
                {  ->

                })
        assert pat.pathSize() == 1;
        assert m.match(pat, new URI("http://acme.com/the_thing"))
    }

    @Test
    public final void testPathVarMatchFail() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:thing",
                {  ->

                })
        assert pat.pathSize() == 1;
        assert !m.match(pat, new URI("http://acme.com/the_thing/2many"))
    }

    @Test
    public final void testMatchPathVarWithQuery() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=:query",
                { o, t, q ->
                })
        m.match(pat, new URI("/first/second?q1=third"))
        assert pat.pathSize() == 2;
        assert pat.queryPattern().queryKeys().size() == 1;
    }

    @Test
    public final void testFailMatchPathVarWithQuery() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=:query",
                { o, t, q ->
                });
        !m.match(pat, new URI("/first/second"));
    };

    @Test
    public final void testMatchPathVarWithQueryValue() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=query",
                { o, t, q ->
                });
        m.match(pat, new URI("/first/second?q1=query"));
    };

    @Test
    public final void testFailMatchPathVarWithQueryValue() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=query",
                { o, t, q ->
                });
        !m.match(pat, new URI("/first/second?q1=notRight"));
    };

    @Test
    public final void testFail2MatchPathVarWithQueryValue() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/:other/:thing?q1=query",
                { o, t, q ->
                });
        !m.match(pat, new URI("/first/second/third?q1=query"));
    };

}

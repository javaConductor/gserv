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

import org.groovyrest.gserv.FilterMatcher
import org.groovyrest.gserv.Matcher
import org.groovyrest.gserv.Route
import org.groovyrest.gserv.RouteFactory
import org.junit.Before
import org.junit.Test

public class FilterMatcherTest {

    Matcher m;

    @Before
    public final void setup() {
        m = new FilterMatcher();
    }

    @Test
    public void "testPathWildCardMatch"() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/*",
                { ->

                })
        assert m.match(pat, new URI("/anything_goes"))
    }


    @Test
    public final void testWildcardMatchesRoot() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/*",
                { ->

                })
        assert m.match(pat, new URI("/"))
    }

    @Test
    public final void testEmptyPathMatchesAll() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/**",
                { ->

                })
        assert m.match(pat, new URI("/yippy/yappy/yahooey"))
    }

    @Test
    public final void testSlashMatchesSlash() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/",
                { ->

                })
        assert m.match(pat, new URI("/"))
    }

    @Test
    public final void testSlashMatches() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/yippy/**",
                { ->

                })
        assert m.match(pat, new URI("/yippy/yappy/yahooey"))
    }

    @Test
    public final void testSlashMatches3() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/yippy/**",
                { ->

                })
        assert !m.match(pat, new URI("/yiippy/yappy/yahooey"))
    }

    @Test
    public final void testSlashMatches2() {
        Route pat = RouteFactory.createURLPattern(
                "GET",
                "/yippy/*/yahooey",
                { ->

                })
        assert m.match(pat, new URI("/yippy/yappy/yahooey"))
    }

}

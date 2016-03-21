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

package io.github.javaconductor.gserv.test

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.factory.ResourceActionFactory
import io.github.javaconductor.gserv.utils.LinkBuilder
import org.junit.Before
import org.junit.Test

public class LinkTest {

	LinkBuilder l

	@Before
	public final void init() {
		l = new LinkBuilder();
	}

	@Test
	public final void testLinkBuilder() {
		ResourceAction pat = ResourceActionFactory.createAction(
				"GET",
				"/books/:id",
				{ ->

				}
		)
		l.add("test1", pat)
		assert "/books/555" == l.link("test1", [id: 555])
	}

	@Test
	public final void testLinkBuilder2() {
		ResourceAction pat = ResourceActionFactory.createAction(
				"GET",
				"/books/:year/:rank",
				{ yr, rnk ->

				}
		)
		l.add("test2", pat)
		assert "/books/1970/10" == l.link("test2", [year: 1970, rank: 10])
	}

	@Test
	public final void testLinkBuilder3() {
		ResourceAction pat = ResourceActionFactory.createAction(
				"GET",
				"/books/:year/:rank/all",
				{ yr, rnk ->

				}
		)
		l.addLink("test2", pat)
		assert "/books/1970/10/all" == l.link("test2", [year: 1970, rank: 10])
	}

	@Test
	public final void testLinkBuilderWithQuery() {
		ResourceAction pat = ResourceActionFactory.createAction(
				"GET",
				"/books/:year/:rank/all?foo=:foo",
				{ yr, rnk, foo ->

				}
		)
		l.addLink("test2", pat)
		assert "/books/1970/10/all?foo=theQueryParam" == l.link("test2", [year: 1970, rank: 10, foo: "theQueryParam"])
	}

	@Test
	public final void testLinkBuilderFixHrefs() {
		def nuHref = LinkBuilder.expandLinkIfNeeded("http", "me.com", 8080, "/thing/21")
		assert nuHref.startsWith("http")
		assert nuHref.startsWith("http://me.com:8080")
	}

	@Test
	public final void testLinkBuilderFixHrefsWithQry() {
		def nuHref = LinkBuilder.expandLinkIfNeeded("http", "me.com", 8080, "/thing/21?page=5")
		assert nuHref.startsWith("http")
		assert nuHref.startsWith("http://me.com:8080")
		assert nuHref.contains("page=5")
	}

}

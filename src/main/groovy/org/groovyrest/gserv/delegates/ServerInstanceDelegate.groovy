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

package org.groovyrest.gserv.delegates

import org.groovyrest.gserv.delegates.functions.ResourceFunctions
import org.groovyrest.gserv.delegates.functions.ServerConfigFunctions
import org.groovyrest.gserv.events.EventManager
import org.groovyrest.gserv.utils.LinkBuilder
import org.groovyrest.gserv.utils.StaticFileHandler

/**
 * Delegate for Server Configuration Closure
 *
 */
@Mixin([StaticFileHandler, ServerConfigFunctions, ResourceFunctions])
class ServerInstanceDelegate {
    Map _properties = [:]

    def value(String key, Object value) { _properties.put(key, value) }

    def value(String key) { _properties[key] }

    def values() { _properties }
    def templateEngine = "default"
    def eventManager = EventManager.instance()
    def patterns = {

        value("routeList")

    }
    def filters = { value("filterList") }
    def staticRoots = { value("staticRoots") }
    def linkBuilder = { value("linkBuilder") }

    def ServerInstanceDelegate() {
        value("routeList", [])
        value("filterList", [])
        value("staticRoots", [])
        value("useResourceDocs", [])
        value("linkBuilder", new LinkBuilder())
    }
}

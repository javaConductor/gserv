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

import org.groovyrest.gserv.delegates.functions.ResourceHandlerFn
import org.groovyrest.gserv.events.EventManager
import org.groovyrest.gserv.templating.TemplateManager

/**
 * This is the delegate for any HTTP Method handler closure
 */
//@Mixin(ResourceHandlerFunctions)
class HttpMethodDelegate extends DelegateFunctions implements ResourceHandlerFn {
    def exchange
    def serverConfig
    def $this
    def eventManager = EventManager.instance()

    def HttpMethodDelegate(xchange, route, serverConfig) {
        value("tmgr", new TemplateManager());
        value("linkBuilder", serverConfig.linkBuilder());
        value("staticRoots", serverConfig.staticRoots());
        value("templateEngineName", serverConfig.templateEngineName());
        //value("to", new InputStreamTypeConverter().converters);
        // $this inside the closure will be the currently processing route
        $this = route
        exchange = xchange
        this.serverConfig = serverConfig
    }

}

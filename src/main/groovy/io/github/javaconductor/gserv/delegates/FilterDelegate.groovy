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

package io.github.javaconductor.gserv.delegates

import groovy.util.logging.Log4j
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.delegates.functions.FilterFn
import io.github.javaconductor.gserv.delegates.functions.ResourceFn
import io.github.javaconductor.gserv.delegates.functions.ResourceHandlerFn
import io.github.javaconductor.gserv.filters.Filter
import io.github.javaconductor.gserv.requesthandler.RequestContext
import io.github.javaconductor.gserv.templating.TemplateManager

/**
 *
 * The delegate for Filters
 *
 * User: javaConductor
 * Date: 1/7/14
 * Time: 2:19 AM

 */
@Log4j
class FilterDelegate extends DelegateFunctions implements ResourceHandlerFn, FilterFn, ResourceFn {
    def $this
    def requestContext

    def FilterDelegate(Filter filter, RequestContext requestContext,
                       GServConfig serverConfig, String templateEngineName) {
        assert requestContext
//        value("chain", chain);
        value("linkBuilder", serverConfig.linkBuilder());
        value("staticRoots", serverConfig.staticRoots());
        value("templateEngineName", serverConfig.templateEngineName());
        value('inputStreamTypeConverter', serverConfig.inputStreamTypeConverter);
        value("to", serverConfig.inputStreamTypeConverter.converters);
        value("serverConfig", serverConfig);
        value("tmgr", new TemplateManager());

        value('$this', filter);

        this.requestContext = requestContext
        $this = filter
    }
}

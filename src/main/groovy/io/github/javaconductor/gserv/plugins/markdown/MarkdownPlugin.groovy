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

package io.github.javaconductor.gserv.plugins.markdown

import io.github.javaconductor.gserv.actions.ResourceAction
import io.github.javaconductor.gserv.factory.ResourceActionFactory
import io.github.javaconductor.gserv.delegates.DelegateTypes
import io.github.javaconductor.gserv.filters.FilterOptions
import io.github.javaconductor.gserv.plugins.AbstractPlugin
import io.github.javaconductor.gserv.requesthandler.wrapper.ExchangeWrapper
import groovy.util.logging.Log4j
import org.apache.commons.io.IOUtils
import org.pegdown.PegDownProcessor

/**
 * Created by javaConductor on 5/13/2014.
 */
@Log4j
class MarkdownPlugin extends AbstractPlugin {
    def contentTypes = ["text/x-markdown", "application/markdown"]
    def fileExtensions = ["md", "markdown"]
    PegDownProcessor pegDownProcessor = new PegDownProcessor()
    boolean filterContentType = true

    @Override
    def init(options) {
//        filterContentType = options?.filterContentType
        return null
    }

    /**
     * Returns the list of filters deployed by this Plugin
     *
     * @return List < Filter >
     */
    @Override
    List<ResourceAction> filters() {
        [ResourceActionFactory.createAfterFilter("MarkdownPluginFilter", "GET", "/*", [(FilterOptions.MatchedActionsOnly): true]) { e, data ->
            handleAfter(e, data)
        }]
    }

    /**
     * This function adds Plugin-specific methods and variables to the various delegateTypes
     *
     * @param delegateType
     * @param delegateMetaClass
     * @return
     */
    @Override
    MetaClass decorateDelegate(String delegateType, MetaClass delegateMetaClass) {
        if (delegateType == DelegateTypes.HttpMethod) {
            delegateMetaClass.writeMarkdown << writeMarkdown
        }
        return super.decorateDelegate(delegateType, delegateMetaClass)
    }

    def writeMarkdown = { inStream ->
        def writer = new StringWriter();
        IOUtils.copy(inStream, writer)
        def bytes = pegDownProcessor.markdownToHtml(writer.buffer.toString()).bytes
        write(bytes)
    }

    private def handleAfter(ExchangeWrapper exchange, data) {
        /// if the name is known MD extension
        def convertMarkdown = isMarkdownFilePath(fileExtensions, exchange.requestURI.path)
        log.trace("Found MD file: $convertMarkdown")
        def bytes = (!convertMarkdown) ? data : pegDownProcessor.markdownToHtml(new String(data)).bytes
        if (convertMarkdown) {
            exchange.responseHeaders.set('Content-type', "text/html")
            exchange.responseHeaders.set('Content-length', "" + bytes.size())
        }
        bytes
    }

    def isMarkdownFilePath(fileExtensions, path) {
        !(!fileExtensions.find {
            path.endsWith('.' + it)
        });
    }

}

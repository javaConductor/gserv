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

package org.groovyrest.gserv.events

/**
 * Events for EventManager
 */
class Events {
    final def static ServerStarted = 'g$$/server/started'
    final def static RequestRecieved = 'g$$/request/recvd'
    final def static RequestMatchedFilter = 'g$$/request/matched/filter'
    final def static RequestNotMatchedFilter = 'g$$/request/notmatched/filter'
    final def static FilterProcessing = 'g$$/filter/processing'
    final def static FilterError = 'g$$/filter/error'
    final def static FilterComplete = 'g$$/filter/complete'
    final def static RequestNotMatchedDynamic = 'g$$/request/notmatched/dyn'
    final def static RequestMatchedDynamic = 'g$$/request/matched/dyn'
    final def static RequestMatchedStatic = 'g$$/request/matched/static'
    final def static RequestNotMatchedStatic = 'g$$/request/notmatched/static'
    final def static ResourceProcessing = 'g$$/resource/processing'
    final def static ResourceProcessed = 'g$$/resource/processed'
    final def static ResourceProcessingError = 'g$$/resource/processing/error'
    final def static RequestProcessingError = 'g$$/request/processing/error'
    final def static RequestDispatched = 'g$$/request/dispatched'
    final def static RequestProcessed = 'g$$/request/processed'

    final def static PluginRegistered = 'g$$/plugin/registered'
    final def static PluginLoaded = 'g$$/plugin/loaded'
}

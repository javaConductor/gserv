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

import io.github.javaconductor.gserv.*
@Grab('com.gmongo:gmongo:1.0')
import com.gmongo.GMongo
import io.github.javaconductor.gserv.plugins.PluginMgr
import io.github.javaconductor.gserv.plugins.caching.CachingPlugin
import io.github.javaconductor.gserv.plugins.compression.CompressionPlugin
import io.github.javaconductor.gserv.plugins.eventLogger.EventLoggerPlugin
import io.github.javaconductor.gserv.plugins.markdown.MarkdownPlugin
import io.github.javaconductor.gserv.utils.Encoder

def gmongo = new GMongo('localhost:27017')
def db = gmongo.getDB('gserv')

def pluginMgr = PluginMgr.instance()
pluginMgr.register("eventLogger", EventLoggerPlugin.class)
pluginMgr.register("compression", CompressionPlugin.class)
pluginMgr.register("caching", CachingPlugin.class)
pluginMgr.register("markdown", MarkdownPlugin.class)

def gserv = new GServ();
gserv.plugins {
    plugin("caching", [:])
    plugin("compression", [:])
}.https {

    strongETag("/*") { exch, data ->
        //MD5 it
        Encoder.md5WithBase64(data)
    }
    get("/") {
        write("Hello World")
    }

}//.config()

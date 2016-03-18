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

package io.github.javaconductor.gserv.plugins

import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.events.EventManager
import io.github.javaconductor.gserv.events.Events
import io.github.javaconductor.gserv.plugins.caching.CachingPlugin
import io.github.javaconductor.gserv.plugins.compression.CompressionPlugin
import io.github.javaconductor.gserv.plugins.cors.CorsPlugin

/**
 * Created by javaConductor on 1/24/14.
 */
@Slf4j
class PluginMgr {

    private static final INSTANCE = new PluginMgr()

    static PluginMgr instance() { INSTANCE }

    private def PluginMgr() {
        initDefaultPlugins()
    }

    def initDefaultPlugins() {
        // register('ldap', LdapPlugin.class)
        register('cors', CorsPlugin.class)
        register('caching', CachingPlugin.class)
        register('compression', CompressionPlugin.class)
    }

    def plugins = [:]

    def register(name, Class pluginClass) {

        if (plugins[name]) {
            log.warn("Plugin name: $name is being overridden with class: ${pluginClass.canonicalName}")
        }

        plugins[name] = pluginClass
        EventManager.instance().publish(Events.PluginRegistered, [name: name, pluginClass: pluginClass.toString()])
        this
    }

    IPlugin plugin(name, options) {
        def clazz = plugins[name]
        def ret
        if (clazz) {
            ret = clazz.newInstance()
            ret.init(options)
        }
        EventManager.instance().publish(Events.PluginLoaded, [name: name, options: options])
        ret
    }
}

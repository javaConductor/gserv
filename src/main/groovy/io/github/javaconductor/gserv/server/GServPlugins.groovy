package io.github.javaconductor.gserv.server

import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.delegates.DefaultDelegates
import io.github.javaconductor.gserv.delegates.DelegatesMgr
import io.github.javaconductor.gserv.plugins.IPlugin

/**
 *
 * Container for the application of plugins.
 *
 */
class GServPlugins {
    def plugins = []

    def add(IPlugin p) {
        plugins.add(p)
    }

    /**
     * Apply the plugins to a ServerConfiguration
     *
     * @param serverConfig
     * @return GServConfig
     */
    def applyPlugins(GServConfig serverConfig) {
        def delegates = prepareAllDelegates(DefaultDelegates.delegates)
        serverConfig.delegateManager(new DelegatesMgr(delegates))
        /// for each plugin we add to the actions, filters, and staticRoots
        //TODO plugins MAY also contribute to the Type formatter (to)
        plugins.each {
            serverConfig.addActions(it.actions())
                    .addFilters(it.filters())
                    .addStaticRoots(it.staticRoots())
        }
        serverConfig.delegateTypeMap(delegates)
        serverConfig
    }

    /**
     * Apply each plugin to each delegate
     *
     * @param delegates
     * @return preparedDelegates
     */
    def prepareAllDelegates(delegates) {
        delegates.each { kv ->
            def delegateType = kv.key
            def delegateExpando = kv.value
            plugins.each { plugin ->
                if (!plugin) {
                    println "Skipping null plugin in list"
                } else {
                    plugin.decorateDelegate(delegateType, delegateExpando)// get the side-effect
                }
            }
        }
        delegates
    }
}

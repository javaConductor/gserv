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

import io.github.javaconductor.gserv.actions.ResourceAction

/**
 * Created by javaConductor on 3/22/14.
 */
abstract class AbstractPlugin implements io.github.javaconductor.gserv.plugins.IPlugin {
    @Override
    abstract def init(Object options)

    /**
     * Returns the list of filters deployed by this Plugin
     *
     * @return List < Filter >
     */
    @Override
    List<ResourceAction> filters() {
        return []
    }

    /**
     * Returns the list of actions deployed by this Plugin
     *
     * @return List < Filter >
     */
    @Override
    List<ResourceAction> actions() {
        return []
    }

    /**
     * Returns the list of static roots used by this Plugin
     *
     * @return List < Filter >
     */
    @Override
    List<String> staticRoots() {
        return []
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
        return delegateMetaClass
    }
}

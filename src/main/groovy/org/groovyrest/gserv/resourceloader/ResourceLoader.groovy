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

package org.groovyrest.gserv.resourceloader

import org.groovyrest.gserv.GServ
import org.groovyrest.gserv.GServInstance
import org.groovyrest.gserv.configuration.GServConfig
import groovy.util.logging.Log4j
import org.codehaus.groovy.control.MultipleCompilationErrorsException

/**
 * Created by javaConductor on 8/14/2014.
 */
@Log4j
class ResourceLoader {
    static def resourceCache = [:]

    /**
     * Creates a list of Resources from a resourceScriptFile.
     *
     * @param instanceScriptFile
     * @return List < gServResource >
     */
    def loadResources(File resourceScriptFile, ClassLoader classLoader) {
        if (!(resourceScriptFile?.exists()))
            return [];
        GroovyShell groovyShell = new GroovyShell(classLoader)
        def resources
        try {
            resources = resourceCache[resourceScriptFile.absolutePath] ?: groovyShell.evaluate(resourceScriptFile)
            resourceCache[resourceScriptFile.absolutePath] = resources
        } catch (MultipleCompilationErrorsException ex) {
            log.error("Error compiling resource script file: ${resourceScriptFile.absolutePath} - rethrowing...", ex)
            throw new ResourceScriptException("Compilation error in resource script at ${resourceScriptFile.absolutePath}: ${ex.message}")
        } catch (Throwable ex) {
            log.error("Error evaluating resource script file: ${resourceScriptFile.absolutePath} - rethrowing...", ex)
            throw new ResourceScriptException("Error loading resource at ${resourceScriptFile.absolutePath}: ${ex.message}")
        }
        resources
    }

    /**
     * Creates a GServConfig from an instanceScriptFile.
     * @param instanceScriptFile
     * @return GServConfig
     */
    GServConfig loadInstance(File instanceScriptFile, ClassLoader classLoader) {
        if (!(instanceScriptFile?.exists()))
            return null;
        GroovyShell groovyShell = new GroovyShell(classLoader)
        GServInstance instance = groovyShell.evaluate(instanceScriptFile)
        instance ? instance.config() : null
    }
}

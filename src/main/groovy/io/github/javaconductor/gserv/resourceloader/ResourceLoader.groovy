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

package io.github.javaconductor.gserv.resourceloader

import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.configuration.GServConfig
import io.github.javaconductor.gserv.server.GServInstance
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * Created by javaConductor on 8/14/2014.
 */
@Slf4j
class ResourceLoader {
	static def resourceCache = [:]

	/**
	 * Creates a list of Resources from a resourceScriptFile.
	 *
	 * @param resourceScriptFile
	 * @param classPath
	 *
	 * @return List < gServResource >
	 */
	def loadResources(File resourceScriptFile, classPath) {
		assert true, "resourceScriptFile required"
		if (!resourceScriptFile.exists()) {
			throw new ResourceScriptException("Bad resource script file: $resourceScriptFile not found.")
		}

		classPath = classPath ?: []
		GroovyShell groovyShell = createGroovyShell(classPath)
		def resources
		try {
			resources = resourceCache[resourceScriptFile.absolutePath] ?: groovyShell.evaluate(resourceScriptFile)
			log.debug("Loaded ${resources.size()} resources from scripts: ${resourceScriptFile.absolutePath}")
			resourceCache[resourceScriptFile.absolutePath] = resources
		} catch (MultipleCompilationErrorsException ex) {
			//TODO report the line number of the syntax error
			//log.trace("Error compiling resource script file: ${resourceScriptFile.absolutePath} - rethrowing...", ex)
			log.warn("Error compiling resource script file: ${resourceScriptFile.absolutePath} " + ex.message)
			throw new ResourceScriptException("Compilation error in resource script at ${resourceScriptFile.absolutePath}: ${ex.message}")
		} catch (Throwable ex) {
			log.trace("Error evaluating resource script file: ${resourceScriptFile.absolutePath} - rethrowing...", ex)
			log.warn("Error evaluating resource script file: ${resourceScriptFile.absolutePath} " + ex.message)
			throw new ResourceScriptException("Error loading resource at ${resourceScriptFile.absolutePath}: ${ex.message}")
		}
		resources
	}

	/**
	 * Creates a GServConfig from an instanceScriptFile.
	 * @param instanceScriptFile
	 * @return GServConfig
	 */
	GServInstance loadInstance(File instanceScriptFile, classpath) {
		log.debug("Loading instance @ ${instanceScriptFile.absolutePath} . . . ")
		if (!(instanceScriptFile?.exists()))
			return null;
		GroovyShell groovyShell = createGroovyShell(classpath ?: [])
		GServInstance instance = groovyShell.evaluate(instanceScriptFile)
		log.debug("Instance @ ${instanceScriptFile.absolutePath} loaded.")
		instance
	}

	GServConfig loadInstanceConfig(File instanceScriptFile, classpath) {
		loadInstance(instanceScriptFile, classpath)?.config()
	}

	def createGroovyShell(classpath) {
		// Add imports for script.
		def importCustomizer = new ImportCustomizer()
		importCustomizer.addStaticStars 'io.github.javaconductor.gserv.GServ'
		importCustomizer.addImports 'io.github.javaconductor.gserv.GServ'

		def configuration = new CompilerConfiguration()
		configuration.classpathList = classpath
		configuration.addCompilationCustomizers(importCustomizer)

		// Create shell.
		new GroovyShell(configuration)
	}
}

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

package io.github.javaconductor.gserv.cli

import groovy.util.logging.Slf4j

/**
 * Created by lcollins on 3/21/2016.
 */
@Slf4j
class Reloader {
	List<Closure> stopFns
	def cliArgs
	CliContext ctxt
	def currentTimestamps = [:] // File:Date
	List<Closure> listeners = []
	Timer timer

	Reloader( CliContext ctxt) {
		this.ctxt = ctxt
		/// read the cliArgs to get the files ( Resources,Config )
		initTimestamps();
	}

	static Reloader createReloader( configFilename,instanceScript,resourceScripts){
		CliContext ctxt = new CliContext()
		if (configFilename) {
			ctxt.configFile = new File(configFilename)
		}
		if (instanceScript) {
			ctxt.instanceScript = new File(instanceScript)
		}
		if (resourceScripts) {
			ctxt.resourceScripts = resourceScripts.collect { new File(it) }
		}
		Reloader autoReloader = new Reloader( ctxt)
		print "Created autoReloader ($configFilename, $instanceScript, $resourceScripts)"
		autoReloader
	}

	def initTimestamps() {
		if (ctxt.configFile) currentTimestamps[ctxt.configFile] = ctxt.configFile.lastModified()
		if (ctxt.instanceScript) currentTimestamps[ctxt.instanceScript] = ctxt.instanceScript.lastModified()
		if (ctxt.resourceScripts) ctxt.resourceScripts.each { rFile ->
			currentTimestamps[rFile] = rFile.lastModified()
		}
	}

	def onFileChange(File f) {
		println '*' * (f.absolutePath.length() + 16)
		println "* Reloading ${f.absolutePath} . . ."
		println '*' * (f.absolutePath.length() + 16)
		stopFns.each { stopFn -> stopFn() }
		listeners.each { l -> l( f ) }
	}

	def checkTimestamps() {
		def changedFile = currentTimestamps.keySet().find { f ->
			f.lastModified() > currentTimestamps[f]
		}
		if (changedFile) {
			initTimestamps();
			return onFileChange(changedFile)
		}
	}

	def start() {
		TimerTask t = [run: { ->
			checkTimestamps()
		}] as TimerTask

		def delay = 1 * 1000
		def interval = 3 * 1000
		timer = new Timer("gserv-reload")
		log.debug("creating timer.")
		timer.scheduleAtFixedRate(t, delay, interval)
	}

	def stop() {
		log.debug("destroying timer.")
		timer?.cancel();
		timer = null;
	}

	def setStopFns(List<Closure> stopFns) {
		this.stopFns = stopFns
	}

	def listen(Closure c) {
		/// add closure to list
		listeners << c
	}


}

package com.soulsys.gserv.jmx

import groovy.time.Duration

/**
 * Created by lcollins on 10/7/2014.
 */
class GServJMX implements GServJMXMXBean {
    def GServJMX() {
        //      this.started = new Date()
    }

    def GServJMX(Date started, String status) {
        this.started = started
        this.status = status
    }

    String status() {
        return status
    }

    Date started() {
        return started;
    }

    String upTime() {
//        new Duration (0,0,0,new Date().getTime() - started().getTime()/1000, 0)
        ((new Date().getTime() - started().getTime()) / 1000) + " seconds"
    }

//    void setStatus(String status) {
//        this.status = status
//    }
//
//    Date getStarted() {
//        return started
//    }
//
//    void setStarted(Date started) {
//        this.started = started
//    }

    Date started = new Date()
    String status = "Starting ..."
}

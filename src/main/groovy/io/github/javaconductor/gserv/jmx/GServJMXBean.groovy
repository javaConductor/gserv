package io.github.javaconductor.gserv.jmx

import groovy.time.Duration

/**
 * Created by javaConductor on 10/7/2014.
 */
public interface GServJMXBean {
    String status
    long averageResponseTimeMsecs
    List<String> requestHistory = []
    int port
    boolean https = false
    List<String> errors = []
    int maxConcurrentConnections
    String gservVersion
    Date started

    Date started()

    String status();

    String upTime();
}

package com.soulsys.gserv.jmx

import groovy.time.Duration

/**
 * Created by lcollins on 10/7/2014.
 */
public interface GServJMXMXBean {
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

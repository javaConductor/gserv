package io.github.javaconductor.gserv.status

/**
 * Created by lcollins on 5/3/2015.
 */
interface StatRecorder {

    def recordEvent(String topic, Map eventData)

    Map reportStat() // Map [statDisplayName] : value
}

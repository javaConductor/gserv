package io.github.javaconductor.gserv.status

import io.github.javaconductor.gserv.actions.ResourceAction

/**
 * Created by lcollins on 5/3/2015.
 */
interface ActionStatRecorder {

    def recordEvent(ResourceAction action, String topic, Map eventData)

    Map reportStat(ResourceAction action) // Map [statDisplayName] : value
}

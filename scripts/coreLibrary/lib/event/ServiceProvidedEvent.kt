package coreLibrary.lib.event

import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.define.Script

@Suppress("unused")
class ServiceProvidedEvent<T : Any>(val service: T, val provider: Script) : Event {
    override val handler = Companion

    companion object : Event.Handler()
}
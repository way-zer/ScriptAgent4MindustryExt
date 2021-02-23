package coreLibrary.lib.event

import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.define.ISubScript
import coreLibrary.lib.util.ServiceInterface

class ServiceProvidedEvent<T : ServiceInterface>(val service: T, val provider: ISubScript) : Event {
    override val handler = Companion

    companion object : Event.Handler()
}
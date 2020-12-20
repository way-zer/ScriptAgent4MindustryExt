package coreLibrary.lib.event

import cf.wayzer.script_agent.Event
import cf.wayzer.script_agent.ISubScript
import coreLibrary.lib.util.ServiceInterface

class ServiceProvidedEvent<T : ServiceInterface>(val service: T, val provider: ISubScript) : Event {
    override val handler = Companion

    companion object : Event.Handler()
}
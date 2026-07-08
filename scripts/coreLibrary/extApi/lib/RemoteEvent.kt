package coreLib.extApi

import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.util.Services
import coreLibrary.lib.nullable
import java.io.Serializable

//@file:Depends("coreLibrary/extApi/remoteEventApi")
@Suppress("unused")//Api
abstract class RemoteEvent : Event, Serializable {
    private val handler0 get() = super.handler
    final override val handler: Event.Handler get() = error("You should use RemoteEvent.emit()")

    fun launchEmit() {
        service?.remoteEmit(this)
    }

    internal suspend fun onReceive() {
        handler0.handleAsync(this)
    }

    abstract class Handler : Event.Handler() {
        init {
            val eventCls = javaClass.enclosingClass
            service?.registerType(eventCls)
        }
    }

    interface Impl {
        fun remoteEmit(event: RemoteEvent)
        fun registerType(cls: Class<*>)
    }
    companion object {
        val service: Impl? by Services.get<Impl>().nullable
    }
}
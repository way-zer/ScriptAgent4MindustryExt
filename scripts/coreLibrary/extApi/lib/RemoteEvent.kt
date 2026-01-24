package coreLib.extApi

import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.define.SAExperimentalApi
import cf.wayzer.scriptAgent.util.Services
import coreLibrary.lib.all
import java.io.Serializable

@Suppress("unused")//Api
abstract class RemoteEvent : Event, Serializable {
    private val handler0 get() = super.handler
    final override val handler: Event.Handler get() = error("You should use RemoteEvent.emit()")

    fun launchEmit() {
        service.forEach {
            it.remoteEmit(this)
        }
    }

    internal suspend fun onReceive() {
        handler0.handleAsync(this)
    }

    abstract class Handler : Event.Handler() {
        init {
            val eventCls = javaClass.enclosingClass
            service.forEach {
                it.registerType(eventCls)
            }
        }
    }

    interface Impl {
        fun remoteEmit(event: RemoteEvent)
        fun registerType(cls: Class<*>)
    }
    @OptIn(SAExperimentalApi::class)
    companion object {
        val service: List<Impl> by Services.get<Impl>().all
    }
}
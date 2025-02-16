package coreLibrary.extApi

import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.contextScript
import java.io.Serializable
import java.lang.ref.WeakReference

@Suppress("unused")//Api
abstract class RemoteEvent : Event, Serializable {
    private val handler0 get() = super.handler
    final override val handler: Event.Handler get() = error("You should use RemoteEvent.emit()")

    fun launchEmit() {
        Impl.script.remoteEmit(this)
    }

    internal suspend fun onReceive() {
        handler0.handleAsync(this)
    }

    abstract class Handler : Event.Handler() {
        init {
            val eventCls = javaClass.enclosingClass
            Impl.classMap[eventCls.name] = WeakReference(eventCls)
        }
    }

    internal object Impl {
        val script = contextScript<RemoteEventApi>()
        val classMap = mutableMapOf<String, WeakReference<Class<*>>>()
    }
}
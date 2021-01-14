package coreMindustry.lib

import arc.Events
import arc.func.Cons
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import cf.wayzer.script_agent.ISubScript
import cf.wayzer.script_agent.events.ScriptDisableEvent
import cf.wayzer.script_agent.events.ScriptEnableEvent
import cf.wayzer.script_agent.getContextModule
import cf.wayzer.script_agent.listenTo
import cf.wayzer.script_agent.util.DSLBuilder
import coreMindustry.lib.Listener.Companion.listener

sealed class Listener<T : Any> : Cons<T> {
    abstract val script: ISubScript?
    abstract val handler: (T) -> Unit
    abstract fun register()
    abstract fun unregister()
    override fun get(p0: T) {
        try {
            if (script?.enabled != false) handler(p0)
        } catch (e: Exception) {
            Log.err("Error when handle event $this in ${script?.id ?: "Unknown"}", e)
        }
    }

    data class OnClass<T : Any>(
        override val script: ISubScript?,
        val cls: Class<T>,
        override val handler: (T) -> Unit
    ) : Listener<T>() {
        override fun register() {
            Events.on(cls, this)
        }

        override fun unregister() {
            Events.remove(cls, this)
        }
    }

    data class OnTrigger<T : Any>(
        override val script: ISubScript?,
        val v: T,
        override val handler: (T) -> Unit
    ) : Listener<T>() {
        override fun register() {
            map.get(v, ::Seq).add(this)
        }

        override fun unregister() {
            map[v]?.remove(this)
        }
    }

    companion object {
        private val key = DSLBuilder.DataKeyWithDefault("listener") { mutableListOf<Listener<*>>() }
        val ISubScript.listener by key

        @Suppress("UNCHECKED_CAST")
        private val map = Events::class.java.getDeclaredField("events").apply {
            isAccessible = true
        }.get(this) as ObjectMap<Any, Seq<Cons<*>>>

        init {
            Listener::class.java.getContextModule()!!.apply {
                listenTo<ScriptEnableEvent>(4) { //after
                    key.run { script.get() }?.forEach { it.register() }
                }
                listenTo<ScriptDisableEvent>(2) { //before
                    key.run { script.get() }?.forEach { it.unregister() }
                }
            }
        }
    }
}

inline fun <reified T : Any> ISubScript.listen(noinline handler: (T) -> Unit) {
    listener.add(Listener.OnClass(this, T::class.java, handler))
}

fun <T : Any> ISubScript.listen(v: T, handler: (T) -> Unit) {
    listener.add(Listener.OnTrigger(this, v, handler))
}
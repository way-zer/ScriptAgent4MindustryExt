package coreMindustry.lib

import arc.Events
import arc.func.Cons
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.events.ScriptDisableEvent
import cf.wayzer.scriptAgent.events.ScriptEnableEvent
import cf.wayzer.scriptAgent.getContextScript
import cf.wayzer.scriptAgent.listenTo
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreMindustry.lib.Listener.Companion.listener

sealed class Listener<T : Any> : Cons<T> {
    abstract val script: Script?
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
        override val script: Script?,
        val cls: Class<T>,
        override val handler: (T) -> Unit
    ) : Listener<T>() {
        override fun register() {
            Events.on(cls, this)
        }

        override fun unregister() {
            map[cls]?.remove(this)
        }
    }

    data class OnTrigger<T : Any>(
        override val script: Script?,
        val v: T,
        override val handler: (T) -> Unit
    ) : Listener<T>() {
        override fun register() {
            map.get(v) { Seq(Cons::class.java) }.add(this)
        }

        override fun unregister() {
            map[v]?.remove(this)
        }
    }

    companion object {
        private val key = DSLBuilder.DataKeyWithDefault("listener") { mutableListOf<Listener<*>>() }
        val Script.listener by key

        @Suppress("UNCHECKED_CAST")
        private val map = Events::class.java.getDeclaredField("events").apply {
            isAccessible = true
        }.get(this) as ObjectMap<Any, Seq<Cons<*>>>

        init {
            Listener::class.java.getContextScript().apply {
                listenTo<ScriptEnableEvent>(Event.Priority.After) {
                    key.run { script.get() }?.forEach { it.register() }
                }
                listenTo<ScriptDisableEvent>(Event.Priority.Before) {
                    key.run { script.get() }?.forEach { it.unregister() }
                }
            }
        }
    }
}

@ScriptDsl
inline fun <reified T : Any> Script.listen(noinline handler: (T) -> Unit) {
    listener.add(Listener.OnClass(this, T::class.java, handler))
}

@ScriptDsl
fun <T : Any> Script.listen(v: T, handler: (T) -> Unit) {
    listener.add(Listener.OnTrigger(this, v, handler))
}
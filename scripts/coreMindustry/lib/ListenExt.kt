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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class Listener<T : Any>(
    val script: Script?,
    private val key: Any,
    val insert: Boolean = false,
    val handler: (T) -> Unit
) : Cons<T> {
    fun register() {
        map.get(key) { Seq(Cons::class.java) }.let {
            if (insert) it.insert(0, this) else it.add(this)
        }
    }

    fun unregister() {
        map[key]?.remove(this)
    }

    override fun get(p0: T) {
        try {
            if (script?.enabled != false) handler(p0)
        } catch (e: Exception) {
            Log.err("Error when handle event $this in ${script?.id ?: "Unknown"}", e)
        }
    }

    @Deprecated("removed", level = DeprecationLevel.HIDDEN)
    class OnClass<T : Any>(
        script: Script?,
        cls: Class<T>,
        handler: (T) -> Unit
    ) : Listener<T>(script, cls, handler = handler)

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
                    if (!script.dslExists(key)) return@listenTo
                    withContext(Dispatchers.game) {
                        script.listener.forEach { it.register() }
                    }
                }
                listenTo<ScriptDisableEvent>(Event.Priority.Before) {
                    if (!script.dslExists(key)) return@listenTo
                    withContext(Dispatchers.game) {
                        script.listener.forEach { it.unregister() }
                    }
                }
            }
        }
    }
}

@Deprecated("hidden", level = DeprecationLevel.HIDDEN)
@ScriptDsl
fun <T : Any> Script.listen(v: T, handler: (T) -> Unit) {
    listener.add(Listener(this, v, handler = handler))
}

@ScriptDsl
inline fun <reified T : Any> Script.listen(insert: Boolean = false, noinline handler: (T) -> Unit) {
    listener.add(Listener(this, T::class.java, insert, handler))
}


@ScriptDsl
fun <T : Any> Script.listen(v: T, insert: Boolean = false, handler: (T) -> Unit) {
    listener.add(Listener(this, v, insert, handler))
}

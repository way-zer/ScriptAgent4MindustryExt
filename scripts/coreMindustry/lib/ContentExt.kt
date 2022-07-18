package coreMindustry.lib

import arc.func.Cons2
import arc.struct.ObjectMap
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.util.reflectDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mindustry.Vars
import mindustry.game.EventType
import mindustry.net.Administration
import mindustry.net.Net
import mindustry.net.NetConnection
import mindustry.net.Packet
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.properties.ReadOnlyProperty

val Net.serverListeners: ObjectMap<Class<*>, Cons2<NetConnection, *>> by reflectDelegate()

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Packet> getPacketHandle() =
    (Vars.net.serverListeners[T::class.java] as Cons2<NetConnection, T>?) ?: Cons2 { con: NetConnection, p: T ->
        p.handleServer(con)
    }

/**
 * @param handle return true to call old handler/origin
 */
@ScriptDsl
inline fun <reified T : Packet> Script.listenPacket2Server(crossinline handle: (NetConnection, T) -> Boolean) {
    onEnable {
        val old = getPacketHandle<T>()
        Vars.net.handleServer(T::class.java) { con, p ->
            if (handle(con, p))
                old.get(con, p)
        }
        onDisable {
            Vars.net.handleServer(T::class.java, old)
        }
    }
}

@ScriptDsl
inline fun <reified T : Packet> Script.listenPacket2ServerAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    crossinline handle: suspend (NetConnection, T) -> Boolean
) {
    onEnable {
        val old = getPacketHandle<T>()
        Vars.net.handleServer(T::class.java) { con, p ->
            launch(context) {
                if (handle(con, p))
                    withContext(Dispatchers.game) {
                        old.get(con, p)
                    }
            }
        }
        onDisable {
            Vars.net.handleServer(T::class.java, old)
        }
    }
}

@ScriptDsl
fun Script.registerActionFilter(handle: Administration.ActionFilter) {
    onEnable {
        Vars.netServer.admins.actionFilters.add(handle)
        onDisable {
            Vars.netServer.admins.actionFilters.remove(handle)
        }
    }
}

/**
 * Support for utilContentOverwrite
 * auto re[init] when [EventType.ContentInitEvent]
 */
@ScriptDsl
inline fun <T : Any> Script.useContents(crossinline init: () -> T): ReadOnlyProperty<Any?, T> {
    var v: T? = null
    if (Vars.content != null)
        v = init()
    listen<EventType.ContentInitEvent> {
        v = init()
    }
    return DSLBuilder.Companion.SimpleDelegate { v ?: error("No Vars.content") }
}
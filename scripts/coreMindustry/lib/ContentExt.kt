package coreMindustry.lib

import arc.func.Cons2
import arc.struct.ObjectMap
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
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
    crossinline handle: suspend (NetConnection, T) -> Boolean
) {
    onEnable {
        withContext(Dispatchers.game) {
            val old = getPacketHandle<T>()
            Vars.net.handleServer(T::class.java) { con, p ->
                this@listenPacket2ServerAsync.launch(Dispatchers.game) {
                    if (handle(con, p))
                        old.get(con, p)
                }
            }
            onDisable {
                withContext(Dispatchers.game) {
                    Vars.net.handleServer(T::class.java, old)
                }
            }
        }
    }
}

@ScriptDsl
fun Script.registerActionFilter(handle: Administration.ActionFilter) {
    onEnable {
        withContext(Dispatchers.game) {
            Vars.netServer.admins.actionFilters.add(handle)
        }
        onDisable {
            withContext(Dispatchers.game) {
                Vars.netServer.admins.actionFilters.remove(handle)
            }
        }
    }
}

/**
 * Support for utilContentOverwrite
 * auto re[init] when [EventType.ContentInitEvent]
 */
@ScriptDsl
@Deprecated("no use ContentsLoader", ReplaceWith("lazy{ init() }"), DeprecationLevel.HIDDEN)
inline fun <T : Any> Script.useContents(crossinline init: () -> T) = lazy { init() }
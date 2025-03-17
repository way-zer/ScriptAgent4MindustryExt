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

@ScriptDsl
inline fun Script.onEnableForGame(crossinline block: suspend () -> Unit) {
    onEnable {
        withContext(Dispatchers.game) {
            block()
        }
    }
}

@ScriptDsl
inline fun Script.onDisableForGame(crossinline block: suspend () -> Unit) {
    onDisable {
        withContext(Dispatchers.game) {
            block()
        }
    }
}


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
    onEnableForGame {
        val old = getPacketHandle<T>()
        Vars.net.handleServer(T::class.java) { con, p ->
            if (handle(con, p))
                old.get(con, p)
        }
        onDisableForGame {
            Vars.net.handleServer(T::class.java, old)
        }
    }
}

@ScriptDsl
inline fun <reified T : Packet> Script.listenPacket2ServerAsync(
    crossinline handle: suspend (NetConnection, T) -> Boolean
) {
    onEnableForGame {
        val old = getPacketHandle<T>()
        Vars.net.handleServer(T::class.java) { con, p ->
            this@listenPacket2ServerAsync.launch(Dispatchers.game) {
                if (handle(con, p))
                    old.get(con, p)
            }
        }
        onDisableForGame {
            Vars.net.handleServer(T::class.java, old)
        }
    }
}

@ScriptDsl
fun Script.registerActionFilter(handle: Administration.ActionFilter) {
    onEnableForGame {
        Vars.netServer.admins.actionFilters.add(handle)
        onDisableForGame {
            Vars.netServer.admins.actionFilters.remove(handle)
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
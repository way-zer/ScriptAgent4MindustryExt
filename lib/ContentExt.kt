package coreMindustry.lib

import arc.func.Cons2
import arc.struct.ObjectMap
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import mindustry.Vars
import mindustry.net.Administration
import mindustry.net.Net
import mindustry.net.NetConnection
import mindustry.net.Packet

/**
 * @param handle return true to call old handler/origin
 */
@ScriptDsl
inline fun <reified T : Packet> Script.listenPacket2Server(crossinline handle: (NetConnection, T) -> Boolean) {
    onEnable {
        val old = Net::class.java.getDeclaredField("serverListeners").run {
            isAccessible = true
            @Suppress("UNCHECKED_CAST")
            get(Vars.net) as ObjectMap<Class<*>, Cons2<NetConnection, T>>
        }.get(T::class.java) ?: Cons2 { con: NetConnection, p: T ->
            p.handleServer(con)
        }
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
fun Script.registerActionFilter(handle: Administration.ActionFilter) {
    onEnable {
        Vars.netServer.admins.actionFilters.add(handle)
        onDisable {
            Vars.netServer.admins.actionFilters.remove(handle)
        }
    }
}
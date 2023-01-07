package wayzer

import arc.util.Strings
import cf.wayzer.placehold.DynamicVar
import coreLibrary.DBApi
import coreLibrary.lib.util.loop
import mindustry.net.Administration
import mindustry.net.Packets
import mindustry.net.Packets.ConnectPacket
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.util.TransactionHelper
import wayzer.lib.event.ConnectAsyncEvent
import java.time.Duration
import java.util.*

name = "基础: 玩家数据"


registerVarForType<Player>().apply {
    registerChild("ext", "模块扩展数据", DynamicVar.obj { PlayerData[it.uuid()] })
    registerChild("profile", "统一账号信息(可能不存在)", DynamicVar.obj { PlayerData[it.uuid()].profile })
    registerChild(
        "prefix", "名字前缀,可通过prefix.xxx变量注册", DynamicVar.obj { resolveVar(it, "prefix.*.toString", "") })
    registerChild(
        "suffix", "名字后缀,可通过suffix.xxx变量注册", DynamicVar.obj { resolveVar(it, "suffix.*.toString", "") })
}

registerVarForType<Administration.PlayerInfo>().apply {
    registerChild("ext", "模块扩展数据", DynamicVar.obj { PlayerData[it.id] })
    registerChild("profile", "统一账号信息(可能不存在)", DynamicVar.obj { PlayerData[it.id].profile })
}

registerVarForType<PlayerData>().apply {
    registerChild("name", "名字", DynamicVar.obj { it.lastName })
    registerChild("uuid", "uuid", DynamicVar.obj { it.id.value })
    registerChild("firstJoin", "第一次进服", DynamicVar.obj { Date.from(it.firstTime) })
    registerChild("lastJoin", "最后在线", DynamicVar.obj { Date.from(it.lastTime) })
    registerChild("profile", "统一账号信息(可能不存在)", DynamicVar.obj { it.profile })
}

registerVarForType<PlayerProfile>().apply {
    registerChild("id", "绑定的账号ID(qq)", DynamicVar.obj { it.qq })
    registerChild("totalExp", "总经验", DynamicVar.obj { it.totalExp })
    registerChild("onlineTime", "总在线时间", DynamicVar.obj { Duration.ofSeconds(it.totalTime.toLong()) })
    registerChild("registerTime", "注册时间", DynamicVar.obj { Date.from(it.registerTime) })
    registerChild("lastTime", "账号最后登录时间", DynamicVar.obj { Date.from(it.lastTime) })
}

fun Player.updateName() {
    name = "[white]{player.prefix}[]{name}[white]{player.suffix}".with(
        "player" to this,
        "name" to PlayerData[uuid()].realName
    ).toString()
}

listenPacket2ServerAsync<ConnectPacket> { con, packet ->
    if (Groups.player.any { pp -> pp.uuid() == packet.uuid }) {
        con.kick(Packets.KickReason.idInUse)
        return@listenPacket2ServerAsync false
    }
    if (Strings.stripColors(packet.name).length > 24) {
        con.kick("Name is too long")
        return@listenPacket2ServerAsync false
    }
    val old = transaction { PlayerData.findById(packet.uuid) }
    val event = ConnectAsyncEvent(con, packet, old).apply {
        emitAsync {
            data =
                withContext(Dispatchers.IO) {
                    transaction {
                        PlayerData.findOrCreate(packet.uuid, con.address, packet.name).apply {
                            refresh(flush = true)
                            profile//warm up cache
                        }
                    }
                }
        }
    }
    if (event.cancelled) con.kick("[red]拒绝入服: ${event.reason}")
    !event.cancelled
}

listen<EventType.PlayerConnect> {
    val p = it.player
    val data = PlayerData[p.uuid()]
    data.realName = p.name
    p.updateName()
}

listen<EventType.PlayerJoin> {
    TransactionHelper.withAsyncFlush(this) {
        PlayerData[it.player.uuid()].onJoin(it.player)
    }
}

listen<EventType.PlayerLeave> {
    TransactionHelper.withAsyncFlush(this) {
        PlayerData[it.player.uuid()].onQuit(it.player)
    }
}

onEnable {
    launch(Dispatchers.game) {
        DBApi.DB.awaitInit()
        transaction {
            Groups.player.toList().forEach {
                PlayerData.findByIdWithTransaction(it.uuid())?.onJoin(it)
            }
        }
        loop(Dispatchers.IO) {
            delay(5000)
            val online = Groups.player.mapNotNull { PlayerData[it.uuid()].secureProfile(it) }
            transaction {
                online.forEach(PlayerProfile::loopCheck)
            }
        }
        loop(Dispatchers.game) {
            delay(5000)
            Groups.player.forEach { it.updateName() }
        }
    }
}
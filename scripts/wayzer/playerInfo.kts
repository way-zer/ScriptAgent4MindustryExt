package wayzer

import cf.wayzer.placehold.DynamicVar
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.net.Administration
import mindustry.net.Packets
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.util.*

name = "基础: 玩家数据"


registerVarForType<Player>().apply {
    registerChild("shortID", "uuid 3位前缀,可以展现给其他玩家", DynamicVar.obj { it.uuid.substring(0, 3) })
    registerChild("ext", "模块扩展数据", DynamicVar.obj { PlayerData.getOrNull(it.uuid) })
    registerChild("profile", "统一账号信息(可能不存在)", DynamicVar.obj { PlayerData.getOrNull(it.uuid)?.profile })
}

registerVarForType<Administration.PlayerInfo>().apply {
    registerChild("shortID", "uuid 3位前缀,可以展现给其他玩家", DynamicVar.obj { it.id.substring(0, 3) })
    registerChild("ext", "模块扩展数据", DynamicVar.obj { PlayerData.getOrNull(it.id) })
    registerChild("profile", "统一账号信息(可能不存在)", DynamicVar.obj { PlayerData.getOrNull(it.id)?.profile })
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

listen<EventType.PlayerConnect> {
    val p = it.player
    if (playerGroup.any { pp -> pp.uuid == p.uuid }) return@listen p.con.kick(Packets.KickReason.idInUse)
    @Suppress("EXPERIMENTAL_API_USAGE")
    transaction {
        PlayerData.findOrCreate(p)
    }
}

listen<EventType.PlayerLeave> { event ->
    @Suppress("EXPERIMENTAL_API_USAGE")
    transaction {
        PlayerData.getOrNull(event.player.uuid)?.apply {
            save()
            if (playerGroup.none { it != event.player && it.uuid == event.player.uuid })
                PlayerData.removeCache(event.player.uuid)
        }
    }
}
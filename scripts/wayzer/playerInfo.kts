package wayzer

import cf.wayzer.placehold.DynamicVar
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.net.Administration
import mindustry.net.Packets
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

name = "基础: 玩家数据"

registerVarForType<Duration>().apply {
    registerToString("参数设定单位(天,时,分,秒,d,h,m,s,默认m)", DynamicVar { obj, arg ->
        val unit = when (arg?.get(0)?.toLowerCase()) {
            'd','天' -> ChronoUnit.DAYS
            'h','小','时' -> ChronoUnit.HOURS
            'm','分' -> ChronoUnit.MINUTES
            's','秒' -> ChronoUnit.SECONDS
            else -> ChronoUnit.MINUTES
        }
        "%.2f%s".format((obj.seconds.toDouble() / unit.duration.seconds),arg?:"")
    })
}

registerVarForType<Player>().apply {
    registerChild("info", "PlayerInfo", DynamicVar { obj, _ -> netServer.admins.getInfoOptional(obj.uuid) })
    registerChild("shortID", "uuid 3位前缀,可以展现给其他玩家", DynamicVar { obj, _ -> obj.uuid.substring(0, 3) })
    registerChild("ext", "模块扩展数据", DynamicVar { obj, _ -> PlayerData.getOrNull(obj.uuid) })
    registerChild("profile", "统一账号信息(可能不存在)", DynamicVar { obj, _ -> PlayerData.getOrNull(obj.uuid)?.profile })
}

registerVarForType<Administration.PlayerInfo>().apply {
    registerChild("name", "名字", DynamicVar { obj, _ -> obj.lastName })
    registerChild("uuid", "uuid", DynamicVar { obj, _ -> obj.id })
    registerChild("shortID", "uuid 3位前缀,可以展现给其他玩家", DynamicVar { obj, _ -> obj.id.substring(0, 3) })
    registerChild("lastIP", "最后一次的登录IP", DynamicVar { obj, _ -> obj.lastIP })
    registerChild("lastBan", "最后一次被ban时间", DynamicVar { obj, _ -> obj.lastKicked.let(::Date) })
    registerChild("lastJoin", "最后一次加入时间", DynamicVar { obj, _ -> Date(obj.lastSyncTime) })
    registerChild("ext", "模块扩展数据", DynamicVar { obj, _ -> PlayerData.getOrNull(obj.id) })
    registerChild("profile", "统一账号信息(可能不存在)", DynamicVar { obj, _ -> PlayerData.getOrNull(obj.id)?.profile })
}

registerVarForType<PlayerData>().apply {
    registerChild("firstJoin", "第一次进服", DynamicVar { obj, _ -> Date.from(obj.firstTime) })
}

registerVarForType<PlayerProfile>().apply {
    registerChild("id", "绑定的账号ID(qq)", DynamicVar { obj, _ -> obj.qq })
    registerChild("onlineTime", "总在线时间", DynamicVar { obj, _ -> Duration.ofSeconds(obj.totalTime.toLong()) })
    registerChild("registerTime", "注册时间", DynamicVar { obj, _ -> Date.from(obj.registerTime) })
    registerChild("lastTime", "账号最后登录时间", DynamicVar { obj, _ -> Date.from(obj.lastTime) })
}

listen<EventType.PlayerConnect> {
    val p = it.player
    if (playerGroup.any { it.uuid == p.uuid }) return@listen p.con.kick(Packets.KickReason.idInUse)
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
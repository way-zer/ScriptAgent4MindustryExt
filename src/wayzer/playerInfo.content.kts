package wayzer

import cf.wayzer.placehold.DynamicVar
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.net.Administration
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.PlayerData
import java.time.Instant
import java.util.*

name = "基础: 玩家数据"

registerVarForType<Player>().apply {
    registerChild("info", "PlayerInfo", DynamicVar { obj, _ -> netServer.admins.getInfoOptional(obj.uuid) })
    registerChild("shortID", "uuid 3位前缀,可以展现给其他玩家", DynamicVar { obj, _ -> obj.uuid.substring(0, 3) })
    registerChild("profile", "统一账号信息(可能不存在)", DynamicVar { obj, _ -> PlayerData[obj].profile })
}

registerVarForType<Administration.PlayerInfo>().apply {
    registerChild("name", "名字", DynamicVar { obj, _ -> obj.lastName })
    registerChild("uuid", "uuid", DynamicVar { obj, _ -> obj.id })
    registerChild("shortID", "uuid 3位前缀,可以展现给其他玩家", DynamicVar { obj, _ -> obj.id.substring(0, 3) })
    registerChild("lastIP", "最后一次的登录IP", DynamicVar { obj, _ -> obj.lastIP })
    registerChild("lastBan", "最后一次被ban时间", DynamicVar { obj, _ -> obj.lastKicked.let(::Date) })
    registerChild("lastJoin", "最后一次加入时间", DynamicVar { obj, _ -> Date(obj.lastSyncTime) })
    registerChild("profile", "统一账号信息(可能不存在)", DynamicVar { obj, _ -> PlayerData[obj].profile })
}

//TODO PlayerData 和 PlaceProfile相关逻辑

listen<EventType.PlayerJoin> {
    val p = it.player
    transaction {
        PlayerData[p].apply {
            lastIp = p.con.address
            lastName = p.name
            lastTime = Instant.now()
        }
    }
}
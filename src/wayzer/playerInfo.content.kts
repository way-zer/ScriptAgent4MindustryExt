package wayzer

import cf.wayzer.placehold.DynamicVar
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.net.Administration
import java.util.*

name = "基础: 玩家数据"

registerVarForType<Player>().apply {
    registerChild("info", "PlayerInfo", DynamicVar { obj, _ -> netServer.admins.getInfoOptional(obj.uuid) })
    registerChild("shortID", "uuid 3位前缀,可以展现给其他玩家", DynamicVar { obj, _ -> obj.uuid.substring(0, 3) })
}

registerVarForType<Administration.PlayerInfo>().apply {
    registerChild("name", "名字", DynamicVar { obj, _ -> obj.lastName })
    registerChild("uuid", "uuid", DynamicVar { obj, _ -> obj.id })
    registerChild("shortID", "uuid 3位前缀,可以展现给其他玩家", DynamicVar { obj, _ -> obj.id.substring(0, 3) })
    registerChild("lastIP", "最后一次的登录IP", DynamicVar { obj, _ -> obj.lastIP })
    registerChild("lastBan", "最后一次被ban时间", DynamicVar { obj, _ -> obj.lastKicked.let(::Date) })
}

var DataStoreApi.DataEntity.firstJoin by dataStoreKey("firstJoin") { Date(0) }
var DataStoreApi.DataEntity.lastJoin by dataStoreKey("lastJoin") { Date(0) }
registerVarForType<Administration.PlayerInfo>().apply {
    registerChild("lastJoin", "第一次加入时间", DynamicVar { obj, _ -> playerData[obj.id].lastJoin })
    registerChild("firstJoin", "最后一次加入时间", DynamicVar { obj, _ -> playerData[obj.id].firstJoin })
}
listen<EventType.PlayerJoin> {
    playerData[it.player.uuid].apply {
        if (firstJoin.time == 0L) firstJoin = Date()
        lastJoin = Date()
    }
}
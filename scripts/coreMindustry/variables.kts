//WayZer 版权所有(请勿删除版权注解)
package coreMindustry

import arc.util.Time
import cf.wayzer.placehold.DynamicVar
import mindustry.core.Version
import mindustry.ctype.UnlockableContent
import mindustry.game.Team
import mindustry.gen.Unit
import mindustry.maps.Map
import mindustry.net.Administration
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.roundToLong

name = "基础: 全局变量"

//SystemVars
registerVar("tps", "服务器tps", DynamicVar {
    Core.graphics.framesPerSecond.coerceAtMost(255)
})
registerVar("heapUse", "内存占用(MB)", DynamicVar {
    Core.app.javaHeap / 1024 / 1024  //MB
})
//GameVars
registerVar("map", "当前游戏中的地图", DynamicVar {
    state.map
})
registerVarForType<Map>().apply {
    registerChild("name", "地图名") { it.name() }
    registerChild("desc", "地图介绍") { it.description() }
    registerChild("author", "地图作者") { it.author() }
    registerChild("width", "宽度") { it.width }
    registerChild("height", "高度") { it.height }
    registerChild("size", "即:宽度x高度") { "${it.width}x${it.height}" }
    registerChild("fileName", "地图文件名(不含扩展名)") { it.file?.nameWithoutExtension() }
}
registerVar("state.allUnit", "总单位数量", DynamicVar { Groups.unit.size() })
registerVar("state.allBan", "总禁封人数", DynamicVar { netServer.admins.banned.size })
registerVar("state.playerSize", "当前玩家数量", DynamicVar { Groups.player.size() })
registerVar("state.wave", "当前波数", DynamicVar { state.wave })
registerVar("state.enemies", "当前敌人数量", DynamicVar { state.enemies })
registerVar("state.gameMode", "地图游戏模式", DynamicVar { state.rules.mode() })
registerVar("state.startTime", "本局游戏开始时间", DynamicVar { startTime })
registerVar("state.gameTime", "本局游戏开始持续时间(不含回档)", DynamicVar {
    Duration.between(startTime, Instant.now()) - Duration.ofSeconds(pauseTime.roundToLong())
})
registerVar("state.mapTime", "当前地图的持续时间(含回档前时间)", DynamicVar {
    Duration.ofMillis((state.tick / 60 * 1000).toLong())
})
registerVar("game.version", "当前游戏版本", DynamicVar { Version.build })

//PlayerVars
registerVarForType<Player>().apply {
    registerChild("colorHandler", "颜色变量处理") { { color: String -> "[$color]" } }
    registerChild("name", "名字") { it.name }
    registerChild("uuid", "uuid") { it.uuid() }
    registerChild("ip", "当前ip") { it.con?.address }
    registerChild("team", "当前队伍") { it.team() }
    registerChild("unit", "获取玩家Unit") { it.unit() }
    registerChild("info", "PlayerInfo") { netServer.admins.getInfoOptional(it.uuid()) }
    registerToString("玩家名(name)") {
        resolveVarChild(it, "name")?.unwrap()?.toString()
    }
}
registerVarForType<Administration.PlayerInfo>().apply {
    registerChild("name", "名字(可能影响后文颜色)") { it.lastName }
    registerChild("uuid", "uuid") { it.id }
    registerChild("lastIP", "最后一次的登录IP") { it.lastIP }
    registerChild("lastBan", "最后一次被ban时间") { it.lastKicked.let(::Date) }
}

registerVar("team", "当前玩家的队伍", DynamicVar { VarToken("player.team").get() })
registerVarForType<Team>().apply {
    registerChild("name", "队伍名") { it.localized() }
    registerChild("color", "队伍颜色") { "[#${it.color}]" }
    registerChild("colorizeName", "彩色队伍名(影响后文颜色)") {
        resolveVarChild(it, "color")!!.unwrap()!!.toString() +
                resolveVarChild(it, "name")!!.unwrap()!!.toString()
    }
    registerToString("彩色队伍名(不影响后文颜色)") {
        resolveVarChild(it, "colorizeName")!!.unwrap()!!.toString() + "[]"
    }
}

//Unit
registerVarForType<UnlockableContent>().apply {
    registerChild("emoji", "图标") { it.emoji() }
    registerChild("name", "名字") { it.localizedName }
    registerToString("图标+名字") { it.emoji() + it.localizedName }
}
registerVarForType<Unit>().apply {
    registerChild("x", "坐标x") { it.tileX() }
    registerChild("y", "坐标y") { it.tileY() }
    registerChild("health", "当前血量") { it.health }
    registerChild("maxHealth", "最大血量") { it.maxHealth }
    registerChild("shield", "护盾值") { it.shield }
    registerChild("ammo", "弹药") { if (state.rules.unitAmmo) it.shield else resolveVarChild(it, "maxAmmo")?.unwrap() }
    registerChild("maxAmmo", "弹药容量") { it.type.ammoCapacity }
}

var startTime = Instant.now()!!
var pauseTime: Float = 0f //in seconds
listen<EventType.WorldLoadEvent> {
    startTime = Instant.now()
    pauseTime = 0f
}
listen(EventType.Trigger.update) {
    if (state.isPaused)
        pauseTime += Time.delta / 60
}
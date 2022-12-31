//WayZer 版权所有(请勿删除版权注解)
package coreMindustry

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

name = "基础: 全局变量"

//SystemVars
registerVar("tps", "服务器tps", DynamicVar.v {
    Core.graphics.framesPerSecond.coerceAtMost(255)
})
registerVar("heapUse", "内存占用(MB)", DynamicVar.v {
    Core.app.javaHeap / 1024 / 1024  //MB
})
//GameVars
registerVar("map", "当前游戏中的地图", DynamicVar.v {
    state.map
})
registerVarForType<Map>().apply {
    registerChild("name", "地图名", DynamicVar.obj { it.name() })
    registerChild("desc", "地图介绍", DynamicVar.obj { it.description() })
    registerChild("author", "地图作者", DynamicVar.obj { it.author() })
    registerChild("width", "宽度", DynamicVar.obj { it.width })
    registerChild("height", "高度", DynamicVar.obj { it.height })
    registerChild("size", "即:宽度x高度", DynamicVar.obj { "${it.width}x${it.height}" })
    registerChild("fileName", "地图文件名(不含扩展名)", DynamicVar.obj { it.file?.nameWithoutExtension() })
}
registerVar("state.allUnit", "总单位数量", DynamicVar.v { Groups.unit.size() })
registerVar("state.allBan", "总禁封人数", DynamicVar.v { netServer.admins.banned.size })
registerVar("state.playerSize", "当前玩家数量", DynamicVar.v { Groups.player.size() })
registerVar("state.wave", "当前波数", DynamicVar.v { state.wave })
registerVar("state.enemies", "当前敌人数量", DynamicVar.v { state.enemies })
registerVar("state.gameMode", "地图游戏模式", DynamicVar.v { state.rules.mode() })
registerVar("state.startTime", "本局游戏开始时间", DynamicVar.v { startTime })
registerVar("state.gameTime", "本局游戏开始持续时间", DynamicVar.v { Duration.between(startTime, Instant.now()) })
registerVar("game.version", "当前游戏版本", DynamicVar.v { Version.build })

//PlayerVars
registerVarForType<Player>().apply {
    registerChild("colorHandler", "颜色变量处理", DynamicVar.obj { { color: String -> "[$color]" } })
    registerChild("name", "名字", DynamicVar.obj { it.name })
    registerChild("uuid", "uuid", DynamicVar.obj { it.uuid() })
    registerChild("ip", "当前ip", DynamicVar.obj { it.con?.address })
    registerChild("team", "当前队伍", DynamicVar.obj { it.team() })
    registerChild("unit", "获取玩家Unit", DynamicVar.obj { it.unit() })
    registerChild("info", "PlayerInfo", DynamicVar.obj { netServer.admins.getInfoOptional(it.uuid()) })
    registerToString("玩家名(name)", DynamicVar.obj {
        resolveVar(it, "name")?.toString()
    })
}
registerVarForType<Administration.PlayerInfo>().apply {
    registerChild("name", "名字(可能影响后文颜色)", DynamicVar.obj { it.lastName })
    registerChild("uuid", "uuid", DynamicVar.obj { it.id })
    registerChild("lastIP", "最后一次的登录IP", DynamicVar.obj { it.lastIP })
    registerChild("lastBan", "最后一次被ban时间", DynamicVar.obj { it.lastKicked.let(::Date) })
}

registerVar("team", "当前玩家的队伍", DynamicVar.v { getVar("player.team") })
registerVarForType<Team>().apply {
    registerChild("name", "队伍名", DynamicVar.obj { it.localized() })
    registerChild("color", "队伍颜色", DynamicVar.obj { "[#${it.color}]" })
    registerChild("colorizeName", "彩色队伍名(影响后文颜色)", DynamicVar.obj {
        resolveVar(it, "color")!!.toString() + resolveVar(it, "name")!!.toString()
    })
    registerToString("彩色队伍名(不影响后文颜色)", DynamicVar.obj {
        resolveVar(it, "colorizeName")!!.toString() + "[]"
    })
}

//Unit
registerVarForType<UnlockableContent>().apply {
    registerChild("emoji", "图标", DynamicVar.obj { it.emoji() })
    registerChild("name", "名字", DynamicVar.obj { it.localizedName })
    registerToString("图标+名字", DynamicVar.obj { it.emoji() + it.localizedName })
}
registerVarForType<Unit>().apply {
    registerChild("x", "坐标x", DynamicVar.obj { it.tileX() })
    registerChild("y", "坐标y", DynamicVar.obj { it.tileY() })
    registerChild("health", "当前血量", DynamicVar.obj { it.health })
    registerChild("maxHealth", "最大血量", DynamicVar.obj { it.maxHealth })
    registerChild("shield", "护盾值", DynamicVar.obj { it.shield })
    registerChild("ammo", "弹药", DynamicVar.obj { if (state.rules.unitAmmo) it.shield else resolveVar(it, "maxAmmo") })
    registerChild("maxAmmo", "弹药容量", DynamicVar.obj { it.type.ammoCapacity })
}

var startTime = Instant.now()!!
listen<EventType.WorldLoadEvent> {
    startTime = Instant.now()
}
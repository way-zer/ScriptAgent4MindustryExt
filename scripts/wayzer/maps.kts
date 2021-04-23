@file:Import("@wayzer/maps.manager.kt", sourceFile = true)
@file:Import("@wayzer/maps.registry.kt", sourceFile = true)

package wayzer

import cf.wayzer.placehold.DynamicVar
import coreMindustry.lib.util.sendMenuPhone
import mindustry.game.Gamemode
import mindustry.gen.Groups
import mindustry.io.SaveIO
import java.time.Duration
import mindustry.maps.Map as MdtMap

name = "基础: 地图控制与管理"

val configEnableInternMaps by config.key(false, "是否开启原版内置地图")
val configTempSaveSlot by config.key(111, "临时缓存的存档格位")
val mapsPrePage by config.key(9, "/maps每页显示数")

MapRegistry.register(this, object : MapProvider() {
    override val supportFilter = baseFilter + "internal"
    override fun getMaps(filter: String): Collection<MapInfo> {
        maps.reload()
        val mapList = if (configEnableInternMaps) maps.all() else maps.customMaps()
        if (mapList.isEmpty) {
            logger.warning("服务器未安装自定义地图,自动使用自带地图")
            mapList.addAll(maps.all())
        }
        return mapList.sortedBy { it.file.lastModified() }
            .mapIndexed { i, map -> MapInfo(i + 1, map, bestMode(map)) }
            .filterWhen(!configEnableInternMaps && filter != "all") { it.map.custom }
            .filterByMode(filter)
    }

    private fun bestMode(map: mindustry.maps.Map): Gamemode {
        return when (map.file.name()[0]) {
            'A' -> Gamemode.attack
            'P' -> Gamemode.pvp
            'S' -> Gamemode.survival
            'C' -> Gamemode.sandbox
            'E' -> Gamemode.editor
            else -> Gamemode.survival
        }
    }
})

registerVarForType<MapInfo>().apply {
    registerChild("id", "在/maps中的id", DynamicVar.obj { it.id.toString().padStart(3, '0') })
    registerChild("mode", "地图设定模式", DynamicVar.obj { it.mode.name })
    registerChild("map", "Type@Map", DynamicVar.obj { it.map })
}

registerVarForType<MdtMap>().apply {
    registerChild("id", "在/maps中的id", DynamicVar.obj { MapRegistry.getId(it) })
    registerChild("mode", "地图设定模式", DynamicVar.obj { obj ->
        if (obj == state.map) state.rules.modeName ?: state.rules.mode().name
        else MapRegistry.findByMap(obj)?.mode?.name ?: "Unknown"
    })
}

command("maps", "列出服务器地图") {
    usage = "[page/filter] [page]"
    aliases = listOf("地图")
    body {
        val page = arg.lastOrNull()?.toIntOrNull()
        val filter = arg.getOrNull(0)?.toLowerCase()?.let { filter ->
            if (filter.matches(Regex("\\d+"))) return@let null
            if (filter !in MapRegistry.supportFilter.map { it.toLowerCase() }) {
                returnReply("[red]支持的选择器:[green]{list:, }".with("list" to MapRegistry.supportFilter))
            }
            filter
        } ?: "display"
        val maps = MapRegistry.getMaps(filter).sortedBy { it.id }
        sendMenuPhone("服务器地图 By WayZer", maps, page, mapsPrePage) { info ->
            "[red]{info.id}  [green]{info.map.name}[blue] | {info.mode}".with("info" to info)
        }
    }
}
onEnable {
    //hack to stop origin gameOver logic
    val control = Core.app.listeners.find { it.javaClass.simpleName == "ServerControl" }
    val field = control.javaClass.getDeclaredField("inExtraRound")
    field.apply {
        isAccessible = true
        setBoolean(control, true)
    }
}

val waitingTime by config.key(Duration.ofSeconds(10)!!, "游戏结束换图的等待时间")
val gameOverMsgType by config.key(MsgType.InfoMessage, "游戏结束消息是显示方式")
listen<EventType.GameOverEvent> { event ->
    ContentHelper.logToConsole(
        if (state.rules.pvp) "&lcGame over! Team &ly${event.winner.name}&lc is victorious with &ly${Groups.player.size()}&lc players online on map &ly${state.map.name()}&lc."
        else "&lcGame over! Reached wave &ly${state.wave}&lc with &ly${Groups.player.size()}&lc players online on map &ly${state.map.name()}&lc."
    )
    val map = MapRegistry.nextMapInfo(MapRegistry.findByMap(state.map))
    val winnerMsg: Any = if (state.rules.pvp) "[YELLOW] {team.colorizeName} 队胜利![]".with("team" to event.winner) else ""
    val msg = """
                | [SCARLET]游戏结束![]"
                | {winnerMsg}
                | 下一张地图为:[accent]{nextMap.name}[] By: [accent]{nextMap.author}[]
                | 下一场游戏将在 {waitTime} 秒后开始
            """.trimMargin().with("nextMap" to map.map, "winnerMsg" to winnerMsg, "waitTime" to waitingTime.seconds)
    broadcast(msg, gameOverMsgType, quite = true)
    ContentHelper.logToConsole("Next Map is ${map.map.name()}(ID:${map.id})")
    launch {
        val now = state.map
        delay(waitingTime.toMillis())
        if (state.map != now) return@launch//已经通过其他方式换图
        MapManager.loadMap(map)
    }
}
command("host", "管理指令: 换图") {
    usage = "[mapId]"
    permission = "wayzer.maps.host"
    body {
        launch(Dispatchers.game) {
            val map = if (arg.isEmpty()) MapRegistry.nextMapInfo(MapRegistry.findByMap(state.map))
            else arg[0].toIntOrNull()?.let { MapRegistry.findById(it, reply) }
                ?: return@launch reply("[red]请输入正确的地图ID".with())
            MapManager.loadMap(map)
            broadcast("[green]强制换图为{info.map.name},模式{info.mode}".with("info" to map))
        }
    }
}
command("load", "管理指令: 加载存档") {
    usage = "<slot>"
    permission = "wayzer.maps.load"
    body {
        val file = arg[0].let { saveDirectory.child("$it.$saveExtension") }
        if (!file.exists() || !SaveIO.isSaveValid(file))
            returnReply("[red]存档不存在或者损坏".with())
        MapManager.loadSave(file)
        broadcast("[green]强制加载存档{slot}".with("slot" to arg[0]))
    }
}

PermissionApi.registerDefault("wayzer.maps.host", "wayzer.maps.load", group = "@admin")
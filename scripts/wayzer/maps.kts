@file:Import("@wayzer/maps.manager.kt", sourceFile = true)
@file:Import("@wayzer/maps.registry.kt", sourceFile = true)

package wayzer

import arc.Events
import cf.wayzer.placehold.DynamicVar
import coreLibrary.lib.util.menu
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.io.SaveIO
import java.time.Duration
import mindustry.maps.Map as MdtMap

name = "基础: 地图控制与管理"

val configEnableInternMaps by config.key(false, "是否开启原版内置地图")
val mapsPrePage by config.key(9, "/maps每页显示数")

MapRegistry.register(this, object : MapProvider() {
    override val supportFilter = baseFilter + "internal"
    override fun getMaps(filter: String): Collection<MapInfo> {
        maps.reload()
        var enableIntern = configEnableInternMaps
        if (!enableIntern && maps.customMaps().isEmpty) {
            logger.warning("服务器未安装自定义地图,自动使用自带地图")
            enableIntern = true
        }
        val mapList = if (enableIntern) maps.all() else maps.customMaps()
        return mapList.sortedBy { it.file.lastModified() }
            .mapIndexed { i, map -> MapInfo(i + 1, map, bestMode(map)) }
            .filterWhen(!enableIntern && filter != "all") { it.map.custom }
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
    registerChild("id", "在/maps中的id(仅支持当前地图)", DynamicVar.obj {
        if (it == MapManager.current.map) MapManager.current.id
        else -1
    })
    registerChild("mode", "地图设定模式(仅支持当前地图)", DynamicVar.obj {
        if (it == MapManager.current.map) MapManager.current.mode.name
        else "UnSupport"
    })
}

command("maps", "列出服务器地图") {
    usage = "[page/filter] [page]"
    aliases = listOf("地图")
    body {
        val page = arg.lastOrNull()?.toIntOrNull() ?: 1
        val filter = arg.getOrNull(0)?.toLowerCase()?.let { filter ->
            if (filter.matches(Regex("\\d+"))) return@let null
            if (filter !in MapRegistry.supportFilter.map { it.toLowerCase() }) {
                returnReply("[red]支持的选择器:[green]{list:, }".with("list" to MapRegistry.supportFilter))
            }
            filter
        } ?: "display"
        val maps = MapRegistry.getMaps(filter).sortedBy { it.id }
        reply(menu("服务器地图 By WayZer", maps, page, mapsPrePage) { info ->
            "[red]{info.id}  [green]{info.map.name}[blue] | {info.mode}".with("info" to info)
        })
    }
}
onEnable {
    //hack to stop origin gameOver logic
    val control = Core.app.listeners.find { it.javaClass.simpleName == "ServerControl" }
    val field = control.javaClass.getDeclaredField("inGameOverWait")
    field.apply {
        isAccessible = true
        logger.info("inExtraRound:" + get(control))
        setBoolean(control, true)
    }
}

val waitingTime by config.key(Duration.ofSeconds(10)!!, "游戏结束换图的等待时间")
val gameOverMsgType by config.key(MsgType.InfoMessage, "游戏结束消息是显示方式")

class GameOverEvent(val winner: Team) : Event, Event.Cancellable {
    /**After cancell, there is no broadcast and changeMap */
    override var cancelled: Boolean = false
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}
listen<EventType.GameOverEvent> { event ->
    state.gameOver = true
    Call.updateGameOver(event.winner)

    ContentHelper.logToConsole(
        if (state.rules.pvp) "&lcGame over! Team &ly${event.winner.name}&lc is victorious with &ly${Groups.player.size()}&lc players online on map &ly${state.map.name()}&lc."
        else "&lcGame over! Reached wave &ly${state.wave}&lc with &ly${Groups.player.size()}&lc players online on map &ly${state.map.name()}&lc."
    )
    if (GameOverEvent(event.winner).emit().cancelled) return@listen
    val map = MapRegistry.nextMapInfo(MapManager.current)
    val winnerMsg: Any =
        if (state.rules.pvp) "[YELLOW] {team.colorizeName} 队胜利![]".with("team" to event.winner) else ""
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
        val map = if (arg.isEmpty()) MapRegistry.nextMapInfo(MapManager.current)
        else arg[0].toIntOrNull()?.let { MapRegistry.findById(it, reply) }
            ?: returnReply("[red]请输入正确的地图ID".with())
        MapManager.loadMap(map)
        broadcast("[green]强制换图为{info.map.name},模式{info.mode}".with("info" to map))
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

command("gameover", "管理指令: 结束游戏") {
    usage = "[winner]"
    permission = "wayzer.maps.gameover"
    body {
        val winner = arg.firstOrNull()?.let { Team.all.firstOrNull { t -> t.name == it } }
            ?: state.rules.waveTeam
        Events.fire(EventType.GameOverEvent(winner))
    }
}

PermissionApi.registerDefault("wayzer.maps.host", "wayzer.maps.load", group = "@admin")
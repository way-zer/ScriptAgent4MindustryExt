package wayzer

import arc.Events
import cf.wayzer.placehold.DynamicVar
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.io.SaveIO
import java.time.Duration
import mindustry.maps.Map as MdtMap

name = "基础: 地图控制与管理"

val configEnableInternMaps by config.key(false, "是否开启原版内置地图")
val nextSameMode by config.key(false, "自动换图是否选择相同模式地图,否则选择生存模式")

MapRegistry.register(this, object : MapProvider() {
    override suspend fun searchMaps(search: String?): Collection<MapInfo> {
        if (search == "@internal") return maps.defaultMaps()
            .mapIndexed { i, map -> MapInfo(i + 1, map, Gamemode.survival) }
        maps.reload()
        val mapList = (if (configEnableInternMaps) maps.all() else maps.customMaps())
            .sortedBy { it.file.lastModified() }
            .mapIndexed { i, map -> MapInfo(i + 1, map, bestMode(map)) }
        return when {
            search.isNullOrEmpty() -> mapList
            search == "survive" -> mapList.filter { it.mode == Gamemode.survival }
            search == "attack" -> mapList.filter { it.mode == Gamemode.attack }
            search == "pvp" -> mapList.filter { it.mode == Gamemode.pvp }
            else -> mapList.filter {
                it.map.name().contains(search, ignoreCase = true) || it.map.description()
                    .contains(search, ignoreCase = true)
            }
        }
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

registerVarForType<BaseMapInfo>().apply {
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
    /**After cancelled, there is no broadcast and changeMap */
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
    val now = state.map
    launch(Dispatchers.game) {
        if (GameOverEvent(event.winner).emitAsync().cancelled) return@launch
        val map = MapRegistry.nextMapInfo(
            MapManager.current,
            if (nextSameMode) MapManager.current.mode else Gamemode.survival
        )
        if (state.map != now) return@launch//已经通过其他方式换图
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
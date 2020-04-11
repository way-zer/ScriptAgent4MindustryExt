package wayzer

import arc.files.Fi
import mindustry.Vars
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.gen.Call
import mindustry.io.SaveIO
import mindustry.maps.Map
import java.time.Duration
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.ceil

name = "基础: 地图控制与管理"

val configEnableInternMaps by config.key(false, "是否开启原版内置地图")
val mapsDistinguishMode by config.key(false, "是否在/maps区分不同模式的地图")
val configTempSaveSlot by config.key(111, "临时缓存的存档格位")
val mapsPrePage by config.key(9, "/maps每页显示数")
registerVar("state.startTime", "本局游戏开始时间", Date())

@Suppress("PropertyName")
val MapManager = MapManager()

inner class MapManager : SharedData.IMapManager {
    override val maps: Array<Map>
        get() {
            Vars.maps.reload()
            @Suppress("ConstantConditionIf")
            return if (configEnableInternMaps) Vars.maps.all().items else Vars.maps.customMaps()!!.items
        }

    override fun loadMap(map: Map, mode: Gamemode) {
        resetAndLoad {
            world.loadMap(map, map.applyRules(mode))
            state.rules = world.map.applyRules(mode)
            logic.play()
        }
    }

    override fun loadSave(file: Fi) {
        resetAndLoad {
            SaveIO.load(file)
            logic.play()
        }
    }

    override fun nextMap(map: Map?, mode: Gamemode): Map {
        val maps = maps.toMutableList()
        maps.shuffle()
        val ret = maps.filter { bestMode(it) == mode }.firstOrNull { it != map } ?: maps[0]
        if (!SaveIO.isSaveValid(ret.file)) {
            ContentHelper.logToConsole("[yellow]invalid map ${ret.file.nameWithoutExtension()}, auto change")
            return nextMap(map, mode)
        }
        return ret
    }

    override fun bestMode(map: Map): Gamemode {
        return when (map.file.name()[0]) {
            'A' -> Gamemode.attack
            'P' -> Gamemode.pvp
            'S' -> Gamemode.survival
            'C' -> Gamemode.sandbox
            'E' -> Gamemode.editor
            else -> Gamemode.bestFit(map.rules())
        }
    }

    private fun resetAndLoad(callBack: () -> Unit) {
        Core.app.post {
            if (!net.server()) netServer.openServer()
            val players = playerGroup.all().toList()
            players.forEach { it.dead = true }
            callBack()
            Call.onWorldDataBegin()
            players.forEach {
                if (it.con == null) return@forEach
                it.reset()
                if (state.rules.pvp)
                    it.team = netServer.assignTeam(it, players)
                netServer.sendWorldData(it)
            }
            registerVar("state.startTime", "本局游戏开始时间", Date())
        }
    }

    override fun getSlot(id: Int): Fi? {
        val file = SaveIO.fileFor(id)
        if (!SaveIO.isSaveValid(file)) return null
        val voteFile = SaveIO.fileFor(configTempSaveSlot)
        if (voteFile.exists()) voteFile.delete()
        file.copyTo(voteFile)
        return voteFile
    }
}
SharedData.mapManager = MapManager

command("maps", "列出服务器地图", "[page/pvp/attack/all] [page]") { arg, p ->
    val mode: Gamemode? = arg.getOrNull(0).let {
        when {
            !mapsDistinguishMode -> null
            "pvp".equals(it, true) -> Gamemode.pvp
            "attack".equals(it, true) -> Gamemode.attack
            "all".equals(it, true) -> null
            else -> Gamemode.survival
        }
    }
    val page = arg.lastOrNull()?.toIntOrNull() ?: 1
    p.sendMessage("[yellow]默认只显示所有生存图,输入[green]/maps pvp[yellow]显示pvp图,[green]/maps attack[yellow]显示攻城图[green]/maps all[yellow]显示所有".i18n())
    val list = MapManager.maps.mapIndexed { index, map -> (index + 1) to map }
            .filter { mode == null || MapManager.bestMode(it.second) == mode }
            .page(page, mapsPrePage).map { (id, map) ->
                "[red]{id}[green]({map.width},{map.height})[]:[yellow]{map.fileName}[] | [blue]{map.name}\n"
                        .i18n("id" to "%2d".format(id), "map" to map)
            }
    p.sendMessage("""
            |[green]===[white] 服务器地图 [green]===
            |  [green]插件作者:[yellow]wayZer
            |{list}
            |[green]===[white] {page}/{totalPage} [green]===
        """.trimMargin().i18n("list" to list, "page" to page,
            "totalPage" to ceil(MapManager.maps.size / mapsPrePage.toDouble()).toInt())
}
onEnable {
    //hack to stop origin gameOver logic
    val control = Core.app.listeners.find { it.javaClass.simpleName == "SererControl" }
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
            if (state.rules.pvp) "&lcGame over! Team &ly${event.winner.name}&lc is victorious with &ly${playerGroup.size()}&lc players online on map &ly${world.map.name()}&lc."
            else "&lcGame over! Reached wave &ly${state.wave}&lc with &ly${playerGroup.size()}&lc players online on map &ly${Vars()}&lc."
    )
    val map = MapManager.nextMap(world.map)
    val winnerMsg: Any = if (state.rules.pvp) "[YELLOW] {team.colorizeName} 队胜利![]".i18n("team" to event.winner) else ""
    val msg = """
                | [SCARLET]游戏结束![]"
                | {winnerMsg}
                | 下一张地图为:[accent]{nextMap.name}[] By: [accent]{nextMap.author}[]
                | 下一场游戏将在 {waitTime} 秒后开始
            """.trimMargin().i18n("nextMap" to map, "winnerMsg" to winnerMsg, "waitTime" to waitingTime.seconds)
    broadcast(msg, gameOverMsgType, quite = true)
    ContentHelper.logToConsole("Next Map is ${map.name()}")
    SharedTimer.schedule(waitingTime.toMillis()) {
        MapManager.loadMap(map)
    }
}
command("host", "管理指令: 换图", "[mapId] [mode]") { arg, p ->
    if (p != null && !SharedData.admin.isAdmin(p))
        return@command p.sendMessage("[red]你没有权限执行该命令")
    val map = if (arg.isEmpty()) MapManager.nextMap(world.map) else
        arg[0].toIntOrNull()?.let { MapManager.maps.getOrNull(it - 1) }
                ?: return@command p.sendMessage("[red]请输入正确的地图ID".i18n())
    val mode = arg.getOrNull(1)?.let { name ->
        Gamemode.values().find { it.name == name } ?: return@command p.sendMessage("[red]请输入正确的模式".i18n())
    }
            ?: MapManager.bestMode(map)
    MapManager.loadMap(map, mode)
    broadcast("[green]强制换图为{map.name},模式{mode}".i18n("map" to map, "mode" to mode))
}
command("load", "管理指令: 加载存档", "<slot>") { arg, p ->
    if (p != null && !SharedData.admin.isAdmin(p))
        return@command p.sendMessage("[red]你没有权限执行该命令")
    val file = arg[0].let { saveDirectory.child("$it.$saveExtension") }
    if (!file.exists() || !SaveIO.isSaveValid(file))
        return@command p.sendMessage("[red]存档不存在或者损坏")
    MapManager.loadSave(file)
    broadcast("[green]强制加载存档{slot}".i18n("slot" to arg[0])
}
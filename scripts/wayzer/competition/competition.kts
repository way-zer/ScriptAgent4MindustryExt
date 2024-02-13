@file:Depends("wayzer/vote", "投票实现")
@file:Depends("wayzer/maps", "地图管理")
@file:Depends("wayzer/map/betterTeam")
@file:Depends("wayzer/competition/ext/patch")
@file:Depends("wayzer/ext/observer")
@file:Depends("wayzer/ext/voteMap", soft = true)

package wayzer.competition

import arc.util.Strings
import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.io.SaveIO
import wayzer.*
import wayzer.competition.ext.PatchManager
import mindustry.Vars.*

listenTo<GetNextMapEvent> {
    if (mapInfo !is MapInfo) return@listenTo
    mapInfo = (CompetitionService.nextMap ?: mapInfo as MapInfo).copy(mode = Gamemode.survival)
    CompetitionService.nextMap = null
    cancelled = true
}

listenTo<MapChangeEvent>(Event.Priority.After) {
    rules.pvp = CompetitionService.loading && info.mode == Gamemode.pvp//prevent rules in map
    if (!rules.pvp) {
        CompetitionService.gaming = false
    } else {
        CompetitionService.loading = false
    }
}

listen<EventType.PlayEvent> {
    if (!CompetitionService.gaming) {
        state.rules.apply {
            pvp = false
            waves = false
            canGameOver = false
            blockDamageMultiplier = 0f
            unitDamageMultiplier = 0f
            buildSpeedMultiplier = 0f
            modeName = "准备阶段"
            disableWorldProcessors = true
            enemyCoreBuildRadius = 0f
            polygonCoreProtection = false
            placeRangeCheck = false
            lighting = false
            fog = false
            staticFog = false
        }
    }
}

listen<EventType.CoreChangeEvent> {
    if (!CompetitionService.gaming) return@listen
    val team = it.core.team
    if (team == Team.derelict) return@listen
    if (team.data().cores.isEmpty) {
        broadcast(
            "{team.colorizeName}[]被{killer.colorizeName}[]淘汰！".with("team" to team, "killer" to it.core.lastDamage),
            quite = true
        )
    }
}
TeamControl.onLoad()
onEnable {
    CompetitionService.onEnable()
    ScriptManager.getScriptNullable("wayzer/ext/voteMap")?.let {
        ScriptManager.disableScript(it, "比赛系统接管")
        onDisable { ScriptManager.enableScript(it) }
        VoteEvent.VoteCommands += CommandInfo(this, "map", "投票换图") {
            aliases = listOf("换图")
            usage = "<mapId>"
            permission = "wayzer.vote.map"
            body {
                if (arg.isEmpty()) returnReply("[red]请输入地图序号".with())
                val map = arg[0].toIntOrNull()?.let { MapRegistry.findById(it, reply) } ?: returnReply("[red]地图序号错误,可以通过/maps查询".with())
                if (map.mode != Gamemode.pvp) returnReply("[red]pvp服仅支持PVP模式的地图".with())
                val desc = "下张地图([green]{nextMap.id}[]: [green]{nextMap.map.name}[yellow]|[green]{nextMap.mode}[])".with("nextMap" to map)
                val extdesc = "[white]地图作者: [lightgrey]${Strings.stripColors(map.map.author())}[][]\n" +
                        "[white]地图简介: [lightgrey]${Strings.truncate(Strings.stripColors(map.map.description()), 100, "...")}[][]"
                val event = VoteEvent(
                    script!!, player!!,
                    desc, extdesc,
                    true
                )
                if (event.awaitResult()) {
                    if (CompetitionService.gaming) {
                        CompetitionService.nextMap = map
                    } else {
                        broadcast("[yellow]异步加载地图中，请耐心等待".with())
                        if (withContext(Dispatchers.Default) { map.map.file.exists() && !SaveIO.isSaveValid(map.map.file) })
                            return@body broadcast("[red]换图失败,地图[yellow]{nextMap.name}[green](id: {nextMap.id})[red]已损坏".with("nextMap" to map.map))
                        MapManager.loadMap(map)
                        Core.app.post {
                            broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".with())
                        }
                    }
                }
            }
        }
        VoteEvent.VoteCommands.autoRemove(this)
    }
}
command("competition", "比赛管理指令") {
    permission = "competition.admin"
    body(commands)
}
val commands = Commands()
commands += CommandInfo(this, "start", "开始比赛") {
    body {
        if (CompetitionService.gaming) returnReply("[red]比赛正在进行中".with())
        CompetitionService.startGame()
    }
}
commands += CommandInfo(this, "lobby", "回到准备阶段") {
    body {
        CompetitionService.gaming = false
        MapManager.loadMap(MapManager.current.copy(mode = Gamemode.survival))
    }
}
commands += CommandInfo(this, "patch", "添加随机突变") {
    usage = "[突变名，不填为随机]"
    body {
        if (CompetitionService.loading || CompetitionService.gaming) returnReply("[red]只能在准备阶段添加".with())
        if (arg.isEmpty()) {
            PatchManager.randomOrNull(state.rules)?.let{
                CompetitionService.setPatch(it)
            } ?: player?.sendMessage("没有适用于当前地图的突变".with())
        } else {
            if (arg.size > 1) replyUsage()
            PatchManager.findOrNull(arg[0])?.let {
                CompetitionService.setPatch(it)
            } ?: player?.sendMessage("未找到该突变".with())
        }
    }
}

PermissionApi.registerDefault("competition.admin", group = "@admin")

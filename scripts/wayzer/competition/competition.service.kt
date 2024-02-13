package wayzer.competition

import cf.wayzer.placehold.PlaceHoldApi.with
import cf.wayzer.scriptAgent.contextScript
import cf.wayzer.scriptAgent.define.annotations.Savable
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.config
import coreLibrary.lib.util.loop
import coreMindustry.lib.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mindustry.game.Gamemode
import mindustry.gen.Call
import mindustry.gen.Groups
import wayzer.MapInfo
import wayzer.MapManager
import wayzer.VoteEvent
import wayzer.competition.ext.PatchManager
import mindustry.Vars.*
import wayzer.competition.ext.MapPatch

object CompetitionService {
    val script = contextScript<Competition>()
    val teams = contextScript<wayzer.map.BetterTeam>()
    val mapPatch = contextScript<wayzer.competition.ext.Patch>()

    private val config get() = script.config

    val startByAdmin by config.key(false, "是否必须由管理员开始")
    val selectTeam by config.key(true, "是否允许选队")
    val anonymous by config.key(false, "是否开启匿名模式")

    @Savable
    var loading = false
    @Savable
    var gaming = false
    @Savable
    var nextMap: MapInfo? = null

    fun updateHud() {
        if (loading || gaming) Call.hideHudText()
        else {
            val state = when {
                Groups.player.size() < 2 -> "[green]等待玩家中"
                startByAdmin -> "[red]人数已够，等待管理员开始"
                else -> "[green]使用 /vote start 投票开始"
            }
            Call.setHudTextReliable(
                """
                | [green]当前地图是{map.name}
                | [yellow]点击核心选择队伍
                | [yellow]观察者请选择灰队或ob
                | {state}
                |        {competition.teamState}
            """.trimMargin().with("state" to state).toString()
            )
        }
    }

    fun onEnable() = with(script) {
        loop(Dispatchers.game) {
            delay(1_000)
            updateHud()
        }
        VoteEvent.VoteCommands += CommandInfo(this, "start", "立即开始比赛") {
            aliases = listOf("开始")
            usage = ""
            body {
                if (gaming) returnReply("[red]游戏已经开始".with())
                if (startByAdmin) returnReply("[red]比赛模式需要由管理员开始".with())
                val event = VoteEvent(
                    script!!, player!!, this@CommandInfo.description,
                    canVote = { it.team() != teams.spectateTeam },
                )
                if (event.awaitResult()) {
                    startGame()
                }
                this.player?.let {
                    VoteEvent.coolDowns[it.uuid()] = System.currentTimeMillis()
                }
            }
        }
        VoteEvent.VoteCommands += CommandInfo(this, "randompatch", "添加随机突变") {
            body {
                if (loading || gaming) returnReply("[red]只能在准备阶段添加".with())
                val event = VoteEvent(
                    script!!, player!!, this@CommandInfo.description,
                    canVote = { it.team() != teams.spectateTeam },
                )
                if (event.awaitResult()) {
                    PatchManager.randomOrNull(state.rules)?.let{patch ->
                        mapPatch.patchesToLoad = setOf(patch)
                        val msg = """
                            | [green]本场游戏添加突变：
                            | [accent][gold]{mapPatch.name}[]
                            | [gold]{mapPatch.desc}[]
                        """.trimMargin().with("mapPatch" to patch)
                        broadcast(msg, quite=true)
                        broadcast(msg, MsgType.InfoMessage, quite=true)
                    } ?: player?.sendMessage("没有适用于当前地图的突变".with())
                }
            }
        }
        VoteEvent.VoteCommands.autoRemove(this)
    }
    fun onDisable() = with(script) {
        Call.hideHudText()
    }

    fun startGame() {
        if (loading || gaming) return
        loading = true
        TeamControl.beforeStart()
        MapManager.loadMap(MapManager.current.copy(mode = Gamemode.pvp))
        gaming = true
    }

    fun setPatch(patch: MapPatch) {
        mapPatch.patchesToLoad = setOf(patch)
        val msg = """
                    | [green]本场游戏添加突变：
                    | [accent][gold]{mapPatch.name}[]
                    | [gold]{mapPatch.desc}[]
                """.trimMargin().with("mapPatch" to patch)
        broadcast(msg, quite=true)
        broadcast(msg, MsgType.InfoMessage, quite=true)
    }
}
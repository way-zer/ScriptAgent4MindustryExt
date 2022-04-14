package wayzer.map

import arc.Events
import mindustry.core.NetServer
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.world.blocks.storage.CoreBlock
import wayzer.map.BetterTeam.AssignTeamEvent

name = "更好的队伍"

data class AssignTeamEvent(val player: Player, val group: Iterable<Player>, val oldTeam: Team?) : Event {
    var team: Team? = oldTeam
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

data class ChangeTeamEvent(val player: Player, var team: Team) : Event {
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

val spectateTeam = Team.all[255]!!
val allTeam: List<Team>
    get() = state.teams.active.asSequence().filter {
        it.hasCore() && (it.team != Team.derelict && it.team !in bannedTeam)
    }.map { it.team }.toList()

@Savable(false)
val teams = mutableMapOf<String, Team>()
customLoad(::teams, teams::putAll)
var bannedTeam = emptySet<Team>()
var keepTeamsOnce = false

onEnable {
    val backup = netServer.assigner
    netServer.assigner = NetServer.TeamAssigner { p, g ->
        ChangeTeamEvent(p, randomTeam(p, g)).emit().team.also {
            teams[p.uuid()] = it
        }
    }
    onDisable { netServer.assigner = backup }
    updateBannedTeam(true)
}
listen<EventType.PlayEvent> { updateBannedTeam(true) }
//custom gameover
listen<EventType.BlockDestroyEvent> { e ->
    if (state.gameOver || !state.rules.pvp) return@listen
    if (e.tile.block() is CoreBlock)
        launch(Dispatchers.gamePost) {
            if (state.gameOver) return@launch
            allTeam.singleOrNull()?.let {
                state.gameOver = true
                Events.fire(EventType.GameOverEvent(it))
            }
        }
}
listen<EventType.ResetEvent> {
    if (keepTeamsOnce) {
        keepTeamsOnce = false
        return@listen
    }
    teams.clear()
}

fun updateBannedTeam(force: Boolean = false) {
    if (force || bannedTeam.isEmpty())
        bannedTeam = state.rules.tags.get("@banTeam")?.split(',').orEmpty()
            .mapNotNull { Team.all.getOrNull(it.toIntOrNull() ?: -1) }.toSet()
    Groups.player.filter { it.team() in bannedTeam }.forEach {
        changeTeam(it)
        it.sendMessage("[yellow]因为原队伍被禁用,你已自动切换队伍".with(), MsgType.InfoMessage)
    }
}

/**
 * 1. 触发[AssignTeamEvent]
 * 2. 尝试使用[teams]队伍
 * 3. 从[allTeam]随机分配队伍
 */
fun randomTeam(player: Player, group: Iterable<Player> = Groups.player): Team {
    if (teams[player.uuid()]?.run { (this != spectateTeam && !data().hasCore()) || this in bannedTeam } == true)
        teams.remove(player.uuid())
    val fromEvent = AssignTeamEvent(player, group, teams[player.uuid()]).emit().team
    if (fromEvent != null) return fromEvent
    if (!state.rules.pvp) return state.rules.defaultTeam
    return allTeam.shuffled()
        .minByOrNull { group.count { p -> p.team() == it && player != p } }
        ?: state.rules.defaultTeam
}

fun changeTeam(p: Player, team: Team = randomTeam(p)) {
    val newTeam = ChangeTeamEvent(p, team).emit().team
    teams[p.uuid()] = newTeam
    p.clearUnit()
    p.team(newTeam)
    p.clearUnit()
}

export(::changeTeam)

command("ob", "切换为观察者") {
    type = CommandType.Client
    permission = "wayzer.ext.observer"
    body {
        if (player!!.team() == spectateTeam) {
            teams.remove(player!!.uuid())
            changeTeam(player!!)
            if (state.rules.enemyLights.not())
                Call.setRules(player!!.con, state.rules)
            broadcast(
                "[yellow]玩家[green]{player.name}[yellow]重新投胎到{player.team.colorizeName}"
                    .with("player" to player!!), quite = true
            )
        } else {
            changeTeam(player!!, spectateTeam)
            if (state.rules.enemyLights.not())
                Call.setRules(player!!.con, state.rules.copy().apply {
                    enemyLights = true
                })
            broadcast("[yellow]玩家[green]{player.name}[yellow]选择成为观察者".with("player" to player!!), quite = true)
            player!!.sendMessage("[green]再次输入指令可以重新投胎")
        }
    }
}

command("team", "管理指令: 修改自己或他人队伍(PVP模式)") {
    usage = "[队伍,不填列出] [玩家3位id,默认自己]"
    permission = "wayzer.ext.team.change"
    body {
        if (!state.rules.pvp) returnReply("[red]仅PVP模式可用".with())
        val team = arg.getOrNull(0)?.toIntOrNull()?.let { Team.get(it) } ?: let {
            val teams = Team.baseTeams
                .mapIndexed { i, t -> "{id}({team.colorizeName}[])".with("id" to i, "team" to t) }
            returnReply("[yellow]可用队伍: []{list}".with("list" to teams))
        }
        val player = arg.getOrNull(1)?.let {
            depends("wayzer/user/shortID")?.import<(String) -> String?>("getUUIDbyShort")?.invoke(it)
                ?.let { id -> Groups.player.find { it.uuid() == id } }
                ?: returnReply("[red]找不到玩家,请使用/list查询正确的3位id".with())
        } ?: player ?: returnReply("[red]请输入玩家ID".with())
        changeTeam(player, team)
        broadcast("[green]管理员更改了{player.name}[green]为{team.colorizeName}".with("player" to player, "team" to team))
    }
}

PermissionApi.registerDefault("wayzer.ext.observer")
PermissionApi.registerDefault("wayzer.ext.team.change", group = "@admin")
package wayzer.map

import arc.Events
import mindustry.core.NetServer
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.world.blocks.storage.CoreBlock

name = "更好的队伍"

data class AssignTeamEvent(val player: Player, val group: Iterable<Player>, var team: Team?) : Event {
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

val enableTeamLock by config.key(true, "PVP模式队伍锁定，单局不能更换队伍")
val spectateTeam = Team.all[255]!!
val allTeam: List<Team>
    get() = state.teams.active.items.filter {
        it.hasCore() && (it.team != Team.derelict && it.team !in bannedTeam)
    }.map { it.team }

@Savable(false)
val teams = mutableMapOf<String, Team>()
customLoad(::teams, teams::putAll)
var bannedTeam = emptySet<Team>()
var keepTeamsOnce = false

fun updateBannedTeam(force: Boolean = false) {
    if (force || bannedTeam.isEmpty())
        bannedTeam = state.rules.tags.get("@banTeam")?.split(',').orEmpty()
            .mapNotNull { Team.all.getOrNull(it.toIntOrNull() ?: -1) }.toSet()
    Groups.player.filter { it.team() in bannedTeam }.forEach {
        changeTeam(it)
        it.sendMessage("[yellow]因为原队伍被禁用,你已自动切换队伍".with(), MsgType.InfoMessage)
    }
}
onEnable {
    val backup = netServer.assigner
    netServer.assigner = object : NetServer.TeamAssigner {
        override fun assign(player: Player, p1: MutableIterable<Player>): Team {
            val event = AssignTeamEvent(player, p1, null).emit()
            event.team?.let {
                if (enableTeamLock) teams[player.uuid()] = it
                return it
            }
            if (!enableTeamLock) return backup!!.assign(player, p1)
            if (!state.rules.pvp) return state.rules.defaultTeam
            if (teams[player.uuid()]?.run { (this != spectateTeam && !active()) || this in bannedTeam } == true)
                teams.remove(player.uuid())
            return teams.getOrPut(player.uuid()) {
                //not use old,because it may assign to team without core
                randomTeam(player, p1)
            }
        }
    }
    onDisable {
        netServer.assigner = backup
    }
    updateBannedTeam(true)
}
listen<EventType.PlayEvent> { updateBannedTeam(true) }
//custom gameover
listen<EventType.BlockDestroyEvent> { e ->
    if (state.gameOver || !state.rules.pvp) return@listen
    if (e.tile.block() is CoreBlock) {
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

fun randomTeam(player: Player, group: Iterable<Player> = Groups.player): Team {
    return allTeam.shuffled()
        .minByOrNull { group.count { p -> p.team() == it && player != p } }
        ?: state.rules.defaultTeam
}

fun changeTeam(p: Player, team: Team? = null) {
    val newTeam = AssignTeamEvent(p, Groups.player, team).emit().team ?: randomTeam(p)
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
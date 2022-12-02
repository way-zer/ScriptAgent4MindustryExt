@file:Depends("wayzer/map/betterTeam")

package wayzer.ext

import arc.Events
import coreLibrary.lib.PermissionApi
import coreLibrary.lib.with
import coreMindustry.lib.*
import mindustry.Vars
import mindustry.core.NetServer
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.world.blocks.storage.CoreBlock
import wayzer.ext.Observer.AssignTeamEvent

data class AssignTeamEvent(val player: Player, val group: Iterable<Player>, val oldTeam: Team?) : Event,
    Event.Cancellable {
    var team: Team? = oldTeam
        set(value) {
            field = value
            cancelled = true
        }
    override var cancelled: Boolean = false
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

data class ChangeTeamEvent(val player: Player, var team: Team) : Event {
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

val spectateTeam = Team.all[255]!!
val allTeam: Set<Team>
    get() = Vars.state.teams.getActive().mapTo(mutableSetOf()) { it.team }.apply {
        remove(Team.derelict)
        removeIf { !it.data().hasCore() }
        removeAll(bannedTeam)
    }

@Savable(false)
val teams = mutableMapOf<String, Team>()
customLoad(::teams, teams::putAll)
var bannedTeam = emptySet<Team>()
var keepTeamsOnce = false

onEnable {
    val backup = Vars.netServer.assigner
    Vars.netServer.assigner = NetServer.TeamAssigner { p, g ->
        ChangeTeamEvent(p, randomTeam(p, g)).emit().team.also {
            teams[p.uuid()] = it
        }
    }
    onDisable { Vars.netServer.assigner = backup }
    updateBannedTeam(true)
}

fun updateBannedTeam(force: Boolean = false) {
    if (force || bannedTeam.isEmpty())
        bannedTeam = Vars.state.rules.tags.get("@banTeam")?.split(',').orEmpty()
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
    val allTeam = allTeam
    if (teams[player.uuid()]?.run { this != spectateTeam && this !in allTeam } == true)
        teams.remove(player.uuid())
    val fromEvent = AssignTeamEvent(player, group, teams[player.uuid()]).emit().team
    if (fromEvent != null) return fromEvent
    if (!Vars.state.rules.pvp) return Vars.state.rules.defaultTeam
    return allTeam.shuffled()
        .minByOrNull { group.count { p -> p.team() == it && player != p } }
        ?: Vars.state.rules.defaultTeam
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
            broadcast(
                "[yellow]玩家[green]{player.name}[yellow]重新投胎到{player.team.colorizeName}"
                    .with("player" to player!!), type = MsgType.InfoToast, quite = true
            )
        } else {
            changeTeam(player!!, spectateTeam)
            broadcast(
                "[yellow]玩家[green]{player.name}[yellow]选择成为观察者"
                    .with("player" to player!!), type = MsgType.InfoToast, quite = true
            )
            player!!.sendMessage("[green]再次输入指令可以重新投胎")
        }
    }
}

PermissionApi.registerDefault("wayzer.ext.observer")
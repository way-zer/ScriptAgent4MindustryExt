package wayzer.ext

import mindustry.core.NetServer
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.game.Team

name = "更好的队伍"

val enableTeamLock by config.key(true, "PVP模式队伍锁定，单局不能更换队伍")
val spectateTeam = Team.all[255]!!
val backup = netServer.assigner!!
onDisable {
    netServer.assigner = backup
}

val teams = mutableMapOf<String, Team>()
onEnable {
    netServer.assigner = object : NetServer.TeamAssigner {
        override fun assign(player: Player, p1: MutableIterable<Player>): Team {
            if(!enableTeamLock)return backup.assign(player, p1)
            if (!state.rules.pvp) return state.rules.defaultTeam
            if (teams[player.uuid]?.active() == false)
                teams.remove(player.uuid)
            return teams.getOrPut(player.uuid) {
                //not use old,because it may assign to team without core
                val teams = state.teams.active.filter { it.hasCore() }
                teams.shuffled()
                teams.minBy { p1.count { p -> p.team() == it.team && player != p } }!!.team
            }
        }
    }
}
listen<EventType.ResetEvent> {
    teams.clear()
}

command("ob", "切换为观察者", {
    type = CommandType.Client
    permission = "wayzer.ext.observer"
}) {
    if (player!!.team() == spectateTeam)
        return@command reply("[red]你已经是观察者了".with())
    broadcast("[yellow]玩家[green]{player.name}[yellow]选择成为观察者".with("player" to player!!), quite = true)
    player!!.run {
        teams[uuid] = spectateTeam
        team(spectateTeam)
        clearUnit()
    }
}

command("team", "管理指令: 修改自己或他人队伍(PVP模式)", {
    this.usage = "[队伍,不填列出] [玩家3位id,默认自己]"
    permission = "wayzer.ext.team.change"
}) {
    if (!state.rules.pvp) reply("[red]仅PVP模式可用".with())
    val team = arg.getOrNull(0)?.toIntOrNull()?.let { Team.get(it) } ?: let {
        val teams = Team.baseTeams.mapIndexed { i, t -> "{id}({team.colorizeName}[]) ".with("id" to i, "team" to t) }
        return@command reply("[yellow]可用队伍: []{list}".with("list" to teams))
    }
    val player = arg.getOrNull(1)?.let {
        playerGroup.find { p -> p.uuid.startsWith(it) } ?: return@command reply("[red]找不到玩家,请使用/list查询正确的3位id".with())
    } ?: player ?: return@command reply("[red]请输入玩家ID".with())
    teams[player.uuid] = team
    player.team(team)
    player.clearUnit()
    broadcast("[green]管理员更改了{player.name}[green]为{team.colorizeName}".with("player" to player, "team" to team))
}
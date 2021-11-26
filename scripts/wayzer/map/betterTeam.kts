package wayzer.map

import arc.Events
import arc.struct.Seq
import mindustry.core.NetServer
import mindustry.game.Team
import mindustry.game.Teams
import mindustry.gen.Groups
import mindustry.world.blocks.storage.CoreBlock

name = "更好的队伍"

val enableTeamLock by config.key(true, "PVP模式队伍锁定，单局不能更换队伍")
val spectateTeam = Team.all[255]!!
val allTeam: Seq<Teams.TeamData>
    get() = state.teams.active.filter {
        it.hasCore() && it.team !in arrayOf(
            Team.derelict,
            Team.crux
        )
    }
val backup = netServer?.assigner //Can be null when generate ktc
onDisable {
    netServer.assigner = backup
}

val teams = mutableMapOf<String, Team>()
onEnable {
    netServer.assigner = object : NetServer.TeamAssigner {
        override fun assign(player: Player, p1: MutableIterable<Player>): Team {
            if (!enableTeamLock) return backup!!.assign(player, p1)
            if (!state.rules.pvp) return state.rules.defaultTeam
            if (teams[player.uuid()]?.active() == false)
                teams.remove(player.uuid())
            return teams.getOrPut(player.uuid()) {
                //not use old,because it may assign to team without core
                allTeam.shuffled()
                    .minByOrNull { p1.count { p -> p.team() == it.team && player != p } }
                    ?.team ?: state.rules.defaultTeam
            }
        }
    }
}
//custom gameover
listen<EventType.BlockDestroyEvent> {
    if (state.gameOver || !state.rules.pvp) return@listen
    if (it.tile.block() is CoreBlock) {
        allTeam.singleOrNull()?.let {
            state.gameOver = true
            Events.fire(EventType.GameOverEvent(it.team))
        }
    }
}
listen<EventType.ResetEvent> {
    teams.clear()
}

fun changeTeam(p: Player, team: Team) {
    teams[p.uuid()] = team
    p.clearUnit()
    p.team(team)
    p.clearUnit()
}

export(::changeTeam)

command("ob", "切换为观察者") {
    type = CommandType.Client
    permission = "wayzer.ext.observer"
    body {
        if (player!!.team() == spectateTeam) {
            val team = netServer.assignTeam(player!!)
            changeTeam(player!!, team)
            if (state.rules.enemyLights.not())
                Call.setRules(player!!.con, state.rules)
            broadcast(
                "[yellow]玩家[green]{player.name}[yellow]重新投胎到{team.colorizeName}"
                    .with("player" to player!!, "team" to team), quite = true
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
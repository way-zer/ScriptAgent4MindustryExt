package wayzer.ext

import mindustry.core.NetServer
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.world.blocks.storage.CoreBlock
import mindustry.world.blocks.units.MechPad

name = "更好的队伍"

val enableTeamLock by config.key(true,"PVP模式队伍锁定，单局不能更换队伍")
val spectateTeam = Team.all()[255]!!
val backup = netServer.assigner!!
onDisable {
    netServer.assigner = backup
}

val teams = mutableMapOf<String, Team>()
onEnable {
    netServer.assigner = object : NetServer.TeamAssigner {
        override fun assign(p0: Player, p1: MutableIterable<Player>): Team {
            if(!enableTeamLock)return backup.assign(p0, p1)
            if (!state.rules.pvp) return state.rules.defaultTeam
            if (teams[player.uuid]?.active() == false)
                teams.remove(player.uuid)
            return teams.getOrPut(player.uuid) {
                //not use old,because it may assign to team without core
                val teams = state.teams.active.filter { it.hasCore() }
                teams.shuffled()
                teams.minBy { p1.count { p -> p.team == it.team && player != p } }!!.team
            }
        }
    }
}
listen<EventType.ResetEvent> {
    teams.clear()
}

command("ob", "切换为观察者", type = CommandType.Client) { _, p ->
    if (p!!.team == spectateTeam)
        return@command p.sendMessage("[red]你已经是观察者了".i18n())
    broadcast("[yellow]玩家[green]{player.name}[yellow]选择成为观察者".i18n("player" to p), quite = true)
    p.run {
        teams[uuid] = spectateTeam
        team = spectateTeam
        when (spawner) {
            is CoreBlock.CoreEntity -> Call.onUnitRespawn(spawner.tile, p)
            is MechPad.MechFactoryEntity -> Call.onMechFactoryDone(spawner.tile)
        }
        lastSpawner = null
        spawner = null
        Call.onPlayerDeath(this)
    }
}

command("team","管理指令: 修改自己或他人队伍(PVP模式)","[队伍,不填列出] [玩家3位id,默认自己]"){arg,p->
    if(p!=null&&!SharedData.admin.isAdmin(p))
        return@command p.sendMessage("[red]你没有权限使用该命令".i18n())
    if (!state.rules.pvp) p.sendMessage("[red]仅PVP模式可用".i18n())
    val team = arg.getOrNull(0)?.toIntOrNull()?.let { Team.get(it) } ?: let {
        val teams = Team.base().mapIndexed { i, t -> "{id}({team.colorizeName}[]) ".i18n("id" to i, "team" to t) }
        return@command p.sendMessage("[yellow]可用队伍: []{list}".i18n("list" to teams))
    }
    val player = arg.getOrNull(1)?.let {
        playerGroup.find { p -> p.uuid.startsWith(it) } ?: return@command p.sendMessage("[red]找不到玩家,请使用/list查询正确的3位id")
    } ?: p?:return@command p.sendMessage("[red]请输入玩家ID")
    teams[player.uuid] = team
    player.team = team
    Call.onPlayerDeath(player)
    broadcast("[green]管理员更改了{player.name}[green]为{team.colorizeName}".i18n("player" to player, "team" to team))
}
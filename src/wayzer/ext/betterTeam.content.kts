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
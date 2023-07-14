@file:Depends("wayzer/map/betterTeam")
@file:Depends("coreMindustry/menu")

package wayzer.ext

import cf.wayzer.placehold.DynamicVar
import coreMindustry.MenuBuilder
import mindustry.game.Team
import mindustry.gen.PlayerSpawnCallPacket
import mindustry.world.blocks.storage.CoreBlock

val teams = contextScript<wayzer.map.BetterTeam>()

@Savable(serializable = false)
val obTeam = mutableMapOf<Player, Team>()
customLoad(::obTeam) {
    obTeam.putAll(it.filterKeys { it.con != null })
}
fun getObTeam(player: Player): Team? = obTeam[player]?.takeIf { it != player.team() }
export(::getObTeam)
listen<EventType.ResetEvent> { obTeam.clear() }
listen<EventType.PlayerLeave> { obTeam.remove(it.player) }
listenPacket2Server<PlayerSpawnCallPacket> { con, _ -> con.player !in obTeam }
listen<EventType.TapEvent> {
    if (it.tile.build is CoreBlock.CoreBuild && it.player in obTeam) {
        val team = it.tile.team()
        if (obTeam[it.player] == team) return@listen
        obTeam[it.player] = team
        broadcast(
            "[yellow]玩家[green]{player.name}[yellow]正在观战{team}"
                .with("player" to it.player, "team" to team), type = MsgType.InfoToast, quite = true
        )
    }
}
registerVarForType<Player>().apply {
    registerChild("prefix.3obTeam", "观战队伍显示", DynamicVar.obj {
        getObTeam(it)?.let { team -> "[观战${team.coloredName()}]" }
    })
}

command("ob", "切换为观察者") {
    type = CommandType.Client
    permission = "wayzer.ext.observer"
    body {
        val player = player!!
        val team = arg.firstOrNull()?.toIntOrNull()
            ?.let { Team.all.getOrNull(it) }
            ?: MenuBuilder<Team> {
                title = "观战系统"
                msg = "By [gold]WayZer\n选择队伍观战"
                teams.allTeam.forEach {
                    option(it.coloredName()) { it }
                    newRow()
                }
                option("退出观战/重新投胎") { Team.derelict }
                newRow()
                option("关闭菜单") { Team.get(255) }
            }.sendTo(player)?.takeUnless { it.id == 255 }
            ?: return@body
        when (team) {
            Team.derelict -> {
                obTeam.remove(player)
                teams.changeTeam(player)
                broadcast(
                    "[yellow]玩家[green]{player.name}[yellow]重新投胎到{player.team.colorizeName}"
                        .with("player" to player), type = MsgType.InfoToast, quite = true
                )
            }

            else -> {
                obTeam[player] = team
                teams.changeTeam(player, teams.spectateTeam)
                broadcast(
                    "[yellow]玩家[green]{player.name}[yellow]正在观战{team}"
                        .with("player" to player, "team" to team), type = MsgType.InfoToast, quite = true
                )
                player.sendMessage("[green]再次输入指令可以重新投胎。点击核心可以快速切换观战队伍")
            }
        }
    }
}

PermissionApi.registerDefault("wayzer.ext.observer")

listen<EventType.WorldLoadEndEvent> {
    world.tiles.iterator().forEach {
        if (it.team().id == 255) it.setAir()
    }
}
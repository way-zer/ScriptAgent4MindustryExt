@file:Depends("wayzer/map/betterTeam", "获取可用队伍")

package wayzer.pvp

import mindustry.game.Team

val teams = contextScript<wayzer.map.BetterTeam>()

val score = IntArray(Team.all.size)
fun doCheck() {
    val existed = Groups.player.map { it.team() }.toSet()
    teams.allTeam.forEach { team ->
        val old = score[team.id]
        score[team.id] = when {
            team in existed -> 0
            old >= 60 -> 1.also {
                broadcast("[red][系统][yellow]队伍{team0}没有玩家,自动投降".with("team0" to team))
                team.cores().toArray().forEach {
                    it.lastDamage = Team.derelict
                    it.kill()
                }
            }

            else -> (old + 1).also {
                if (old == 0)
                    broadcast("[red][系统][yellow]队伍{team0}没有玩家,60s后自动投降".with("team0" to team))
            }
        }
    }
}

fun check() {
    if (!state.rules.pvp) return
    launch(Dispatchers.game) {
        delay(30000)
        while (true) {
            doCheck()
            delay(1000)
        }
    }
}

listen<EventType.WorldLoadEvent> { check() }
onEnable { check() }

listen<EventType.ResetEvent> {
    coroutineContext[Job]?.cancelChildren()
    score.fill(0)
}
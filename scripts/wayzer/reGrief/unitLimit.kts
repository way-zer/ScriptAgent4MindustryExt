package wayzer.reGrief

import arc.Events
import arc.util.Interval
import mindustry.gen.BuildingTetherc
import mindustry.gen.TimedKillc
import mindustry.gen.Unit
import kotlin.math.min

val unitToWarn by config.key(190, "开始警告的单位数")
val unitToKill by config.key(220, "单位数上限，禁止产生新的")

val interval = Interval(1)
listen<EventType.UnitUnloadEvent> { e ->
    if (e.unit.team == state.rules.waveTeam && state.rules.waves && state.rules.defaultTeam != state.rules.waveTeam)
        return@listen
    fun alert(text: PlaceHoldString) {
        if (interval[0, 2 * 60f]) {//2s cooldown
            broadcast(text, MsgType.InfoToast, 4f, true, Groups.player.filter { it.team() == e.unit.team })
        }
    }

    val count = e.unit.team.data().unitCount
    when {
        count >= unitToKill -> {
            launch(Dispatchers.gamePost) {
                val toKill = count - unitToKill
                val m = Groups.unit.filter { it.team == e.unit.team && it.maxHealth < 1000f && !it.isPlayer }
                    .filterNot { it is TimedKillc || it is BuildingTetherc }
                    .sortedBy { it.health }
                if (m.isNotEmpty())
                    alert("[red]警告: 单位过多,可能造成服务器卡顿,随机杀死低级单位".with("count" to count))
                repeat(min(toKill, m.size)) {
                    m[it].kill()
                }
                if (toKill > m.size) {
                    alert("[red]警告: 单位过多,可能造成服务器卡顿,已禁止生成".with("count" to count))
                    e.unit.kill()
                }
            }
        }

        count >= unitToWarn -> {
            alert("[yellow]警告: 建筑过多单位,可能造成服务器卡顿,当前: {count}".with("count" to count))
        }
    }
}

listen<EventType.UnitSpawnEvent> { e ->
    if (e.unit.team.data().unitCount > 5000 && !state.gameOver) {
        broadcast("[red]敌方单位超过5000,自动投降".with())
        state.gameOver = true
        Events.fire(EventType.GameOverEvent(state.rules.waveTeam))
        Core.app.post {
            Groups.unit.filter { it.team == state.rules.waveTeam }.forEach(Unit::kill)
        }
    }
}

var gameOverWave = -1
listen<EventType.ResetEvent> { gameOverWave = -1 }
fun checkNextWave() {
    val flySpawn = spawner.countFlyerSpawns()
    val groundSpawn = spawner.countGroundSpawns()
    val sum = state.rules.spawns.sum {
        it.getSpawned(state.wave) * if (it.type.flying) flySpawn else groundSpawn
    }
    if (sum >= 3000) {
        state.rules.spawns.clear()
        gameOverWave = state.wave
    }
}
listen<EventType.PlayEvent> { checkNextWave() }
listen<EventType.WaveEvent> {
    if (gameOverWave > 0 && state.wave > gameOverWave && !state.gameOver) {
        broadcast("[red]到达终结波,自动投降".with())
        state.gameOver = true
        Events.fire(EventType.GameOverEvent(state.rules.waveTeam))
        return@listen
    }
    checkNextWave()
}
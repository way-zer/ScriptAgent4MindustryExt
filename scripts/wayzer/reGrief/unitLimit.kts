package wayzer.reGrief

import arc.Events
import arc.util.Interval
import mindustry.gen.Groups
import kotlin.math.min

val unitToWarn by config.key(190, "开始警告的单位数")
val unitToKill by config.key(220, "单位数上限，禁止产生新的")

val interval = Interval(1)
listen<EventType.UnitUnloadEvent> { e ->
    if (e.unit.team == state.rules.waveTeam) {
        if (e.unit.team.data().unitCount > 5000 && !state.gameOver) {
            broadcast("[red]敌方单位超过5000,自动投降".with())
            state.gameOver = true
            Events.fire(EventType.GameOverEvent(state.rules.waveTeam))
        }
        return@listen
    }
    fun alert(text: PlaceHoldString) {
        if (interval[0, 2 * 60f]) {//2s cooldown
            broadcast(text, MsgType.InfoToast, 4f, true, Groups.player.filter { it.team() == e.unit.team })
        }
    }

    val count = e.unit.team.data().unitCount
    when {
        count >= unitToKill -> {
            launch(Dispatchers.game) {
                yield()
                val toKill = count - unitToKill
                val m = Groups.unit.filter { it.team == e.unit.team && it.maxHealth < 1000f && !it.isPlayer }
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
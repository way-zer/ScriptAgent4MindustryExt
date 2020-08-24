package wayzer.ext.reGrief

import arc.util.Interval
import mindustry.game.EventType
import mindustry.gen.Groups

val unitToWarn by config.key(190, "开始警告的单位数")
val unitToKill by config.key(220, "单位数上限，禁止产生新的")

val interval = Interval(1)
listen<EventType.UnitCreateEvent> { e ->
    if (e.unit.team == state.rules.waveTeam) return@listen
    fun alert(text: PlaceHoldString) {
        if (interval[0, 2 * 60f]) {//2s cooldown
            broadcast(text, MsgType.InfoToast, 4f, true, playerGroup.filter { it.team() == e.unit.team })
        }
    }

    val count = Groups.unit.count { it.team == e.unit.team }
    when {
        count >= unitToKill -> {
            alert("[red]警告: 建筑过多单位,可能造成服务器卡顿,已禁止生成".with("count" to count))
            e.unit.kill()
        }
        count >= unitToWarn -> {
            alert("[yellow]警告: 建筑过多单位,可能造成服务器卡顿,当前: {count}".with("count" to count))
        }
    }
}
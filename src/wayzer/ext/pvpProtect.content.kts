package wayzer.ext

import arc.Events
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.gen.Groups
import java.time.Duration
import kotlin.math.ceil

val time by config.key(600,"pvp保护时间(单位秒,小于等于0关闭)")

var job:Job?=null

listen<EventType.WorldLoadEvent> {
    job?.cancel()
    job = launch {
        delay(3_000)
        if(state.rules.mode() != Gamemode.pvp||time<=0)return@launch
        var leftTime = time.toLong()
        broadcast("[yellow]PVP保护时间,禁止在其他基地攻击(持续{time:分钟})".with("time" to Duration.ofSeconds(leftTime)),quite = true)
        while (leftTime>0){
            delay(60_000)
            leftTime-=60
            broadcast("[yellow]PVP保护时间还剩 {time}分钟".with("time" to ceil(leftTime/60f)),quite = true)
            if(leftTime<60){
                delay(leftTime*1000)
                broadcast("[yellow]PVP保护时间已结束, 全力进攻吧".with())
                return@launch
            }
        }
    }
}

//PVP Protect
var t = 0
Events.run(EventType.Trigger.update) {
    if (job?.isActive != true) return@run
    t = (t + 1) % 60
    if (t != 0) return@run //per 60ticks | 1 seconds
    Groups.unit.forEach {
        if (state.teams.closestEnemyCore(it.x, it.y, it.team)?.within(it, state.rules.enemyCoreBuildRadius) == true) {
            it.kill()
        }
    }
    playerGroup.forEach {
        if (it.shooting() && state.teams.closestEnemyCore(it.x, it.y, it.team())?.within(it, state.rules.enemyCoreBuildRadius) == true) {
            it.sendMessage("[red]PVP保护时间,禁止在其他基地攻击".with())
            it.clearUnit()
        }
    }
}
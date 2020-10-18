package wayzer.ext

import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.gen.Groups
import java.time.Duration
import kotlin.math.ceil

val time by config.key(600, "pvp保护时间(单位秒,小于等于0关闭)")

listen<EventType.WorldLoadEvent> {
    launch(Dispatchers.game) {
        delay(3_000)
        var leftTime = state.rules.tags.getInt("@pvpProtect", time)
        if (state.rules.mode() != Gamemode.pvp || time <= 0) return@launch
        broadcast("[yellow]PVP保护时间,禁止在其他基地攻击(持续{time:分钟})".with("time" to Duration.ofSeconds(leftTime.toLong())), quite = true)
        suspend fun checkAttack(time: Int) = repeat(time) {
            delay(1000)
            Groups.unit.forEach {
                if (state.teams.closestEnemyCore(it.x, it.y, it.team)?.within(it, state.rules.enemyCoreBuildRadius) == true) {
                    it.player?.sendMessage("[red]PVP保护时间,禁止在其他基地攻击".with())
                    it.kill()
                }
            }
        }
        while (leftTime > 0) {
            checkAttack(60)
            leftTime -= 60
            broadcast("[yellow]PVP保护时间还剩 {time}分钟".with("time" to ceil(leftTime / 60f)), quite = true)
            if (leftTime < 60) {
                checkAttack(leftTime)
                broadcast("[yellow]PVP保护时间已结束, 全力进攻吧".with())
                return@launch
            }
        }
    }
}

listen<EventType.ResetEvent> {
    coroutineContext[Job]?.cancelChildren()
}
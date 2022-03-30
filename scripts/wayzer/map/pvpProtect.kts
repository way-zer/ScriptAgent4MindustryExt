package wayzer.map

import coreLibrary.lib.util.loop
import mindustry.game.Gamemode
import mindustry.gen.Groups
import mindustry.gen.Unit
import java.time.Duration
import kotlin.math.ceil

val time by config.key(600, "pvp保护时间(单位秒,小于等于0关闭)")

val Unit.inEnemyArea: Boolean
    get() {
        val closestCore = state.teams.active
            .mapNotNull { it.cores.minByOrNull(this::dst2) }
            .minByOrNull(this::dst2) ?: return false
        return closestCore.team != team() && (state.rules.polygonCoreProtection || dst(closestCore) < state.rules.enemyCoreBuildRadius)
    }

listen<EventType.PlayEvent> {
    launch {
        var leftTime = state.rules.tags.getInt("@pvpProtect", time)
        if (state.rules.mode() != Gamemode.pvp || time <= 0) return@launch
        loop(Dispatchers.game) {
            delay(1000)
            Groups.unit.forEach {
                if (it.isShooting() && it.inEnemyArea) {
                    it.player?.sendMessage("[red]PVP保护时间,禁止在其他基地攻击".with())
                    it.kill()
                }
            }
        }
        broadcast(
            "[yellow]PVP保护时间,禁止在其他基地攻击(持续{time:分钟})".with("time" to Duration.ofSeconds(leftTime.toLong())),
            quite = true
        )
        repeat(leftTime / 60) {
            delay(60_000)
            leftTime -= 60
            broadcast("[yellow]PVP保护时间还剩 {time}分钟".with("time" to ceil(leftTime / 60f)), quite = true)
        }
        delay(leftTime * 1000L)
        broadcast("[yellow]PVP保护时间已结束, 全力进攻吧".with())
        cancel()
    }
}

listen<EventType.ResetEvent> {
    coroutineContext[Job]?.cancelChildren()
}
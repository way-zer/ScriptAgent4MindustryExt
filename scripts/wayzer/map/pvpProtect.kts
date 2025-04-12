package wayzer.map

import arc.math.geom.Geometry
import arc.math.geom.Point2
import mindustry.game.Gamemode
import mindustry.gen.Unit
import java.time.Duration
import kotlin.math.ceil

val time by config.key(600, "pvp保护时间(单位秒,小于等于0关闭)")

val Unit.inEnemyArea: Boolean
    get() {
        val closestCore = state.teams.active
            .mapNotNull { it.cores.minByOrNull(this::dst2) }
            .minByOrNull(this::dst2) ?: return false
        return closestCore.team != team() && (state.rules.polygonCoreProtection ||
                dst(closestCore) < state.rules.enemyCoreBuildRadius)
    }

listen<EventType.WorldLoadEvent> {
    var leftTime = state.rules.tags.getInt("@pvpProtect", time)
    if (state.rules.mode() != Gamemode.pvp || time <= 0) return@listen
    loop(Dispatchers.game) {
        delay(1000)
        Groups.unit.forEach {
            if (it.inEnemyArea) {
                it.player?.sendMessage("[red]PVP保护时间,禁止进入敌方区域".with())
                it.closestCore()?.run {
                    val valid = mutableListOf<Point2>()
                    Geometry.circle(tileX(), tileY(), world.width(), world.height(), 10) { x, y ->
                        if (it.canPass(x, y) && (!it.canDrown() || floorOn()?.isDeep == false))
                            valid.add(Point2(x, y))
                    }
                    val r = valid.randomOrNull() ?: return@run
                    it.x = r.x * tilesize.toFloat()
                    it.y = r.y * tilesize.toFloat()
                    it.snapInterpolation()
                }
                it.resetController()
                if (leftTime > 60)
                    it.apply(StatusEffects.unmoving, (leftTime - 60) * 60f)
//                    it.kill()
            }
        }
    }
    launch(Dispatchers.game) {
        broadcast(
            "[yellow]PVP保护时间,禁止在其他基地攻击(持续{time 分钟})".with(
                "time" to Duration.ofSeconds(leftTime.toLong())
            ),
            quite = true
        )
        repeat(leftTime / 60) {
            delay(60_000)
            leftTime -= 60
            broadcast("[yellow]PVP保护时间还剩 {time}分钟".with("time" to ceil(leftTime / 60f)), quite = true)
        }
        delay(leftTime * 1000L)
        broadcast("[yellow]PVP保护时间已结束, 全力进攻吧".with())
        thisScript.coroutineContext.cancelChildren()
    }
}

listen<EventType.ResetEvent> {
    coroutineContext.cancelChildren()
}
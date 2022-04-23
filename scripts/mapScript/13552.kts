package mapScript

import arc.graphics.Color
import arc.util.Align
import coreLibrary.lib.util.loop
import mindustry.content.Fx
import mindustry.content.StatusEffects
import mindustry.content.UnitTypes
import mindustry.game.Team
import kotlin.math.roundToInt
import kotlin.random.Random

/**@author Lucky Clover
含义：中间存在一个机关单位，每摧毁一次会立刻在地图的随机位置生成一个红队单位攻击玩家。是谓之：害群之马
纯娱乐向PVP模式
参考了RA2的某地图
 */
name = "害群之马模式"

fun spawnUnit() {
    UnitTypes.horizon
        .spawn(Team.derelict, world.unitWidth() / 2f, world.unitHeight() / 2f)
        .apply(StatusEffects.unmoving, 9999999f)    // 生成中间机关单位
}

onEnable {
    launch {
        delay(3000)
        spawnUnit()
    }
    loop {
        Call.infoPopup(
            """
            [cyan] 欢迎来到<[red]害群之马[cyan]>模式
            请自由探索本模式的机制！
            [red]千万不要打地图中间的灰队轰炸机""".trimIndent(), 10.05f,
            Align.topLeft, 350, 0, 0, 0
        )
        delay(10_000)
    }
}

listen<EventType.UnitDestroyEvent> { e ->
    if (e.unit.team == Team.derelict) {
        val rx = Random.nextFloat() * world.unitWidth()
        val ry = Random.nextFloat() * world.unitHeight()
        UnitTypes.quad.spawn(Team.crux, rx, ry).apply {
            apply(StatusEffects.slow, 999999f)
            health = state.wave * 10f
        }   //在地图的随机位置生成一个雷霆,但具有更低的血量(与游戏时间挂钩）

        Call.sendMessage("[red]警告！！！[orange]有玩家充当了害群之马，小心[acid][${(rx / 8).roundToInt()},${(ry / 8).roundToInt()}]")
        Call.effectReliable(Fx.launch, rx, ry, 0F, Color.red)
        launch(Dispatchers.game) {
            delay(1_000)
            spawnUnit()
        }
    }
}


package wayzer.reGrief

import arc.struct.IntSet
import arc.util.Interval
import arc.util.Time
import mindustry.core.NetServer
import mindustry.world.blocks.logic.LogicBlock
import mindustryX.events.SendPacketEvent

try {
    LogicBlock::class.java.getDeclaredField("running")
} catch (e: Exception) {
    error("本脚本依赖MindustryX v143.102 或更新版本")
}

//带滑动平均的速率统计
class RateKeeper(val timeWindow: Double = 5.0) {
    private var lastTime = Time.time
    var avgCount = 0.0
    fun count(count: Int = 1) {
        avgCount += count / timeWindow
        val delta = Time.time - lastTime
        if (delta > Time.toSeconds) {
            avgCount *= (1 - delta / timeWindow * Time.toSeconds).coerceAtLeast(0.0)
            lastTime = Time.time
        }
    }
}

val list = mutableListOf<Class<*>>()
val interval = Interval()
val packetRate = RateKeeper()
listen<SendPacketEvent> { event ->
    if (event.con == null && LogicBlock.running && state.isGame) {
        packetRate.count()
        list.add(event.packet::class.java)
        if (packetRate.avgCount > 2000) {
            if (state.rules.disableWorldProcessors) return@listen
            state.rules.disableWorldProcessors = true
            broadcast("[red]世界处理器发包严重，禁用世界处理器。请联系地图地图作者优化.".with())
        } else if (interval.get(30 * 60f)) {
            if (packetRate.avgCount > 1000) {
                val list = list.groupBy { it }
                    .map { it.key.simpleName to it.value.size }
                    .sortedByDescending { it.second }
                broadcast(
                    "[red]世界处理器发包超限, 未来将被禁用，请联系地图地图作者优化。当前值: {avg}/s\n{list|joinLines}".with(
                        "avg" to "%.2f".format(packetRate.avgCount), "list" to list
                    )
                )
            }
            list.clear()
        }
    }
}

val healthChanged = RateKeeper(3.0)
val NetServer.buildHealthChanged: IntSet by reflectDelegate()
listen(EventType.Trigger.update) {
    if (!state.isPlaying) return@listen
    healthChanged.count(netServer.buildHealthChanged.size)
    if (healthChanged.avgCount > 100_000) {
        if (state.rules.disableWorldProcessors) return@listen
        state.rules.disableWorldProcessors = true
        broadcast("[red]世界处理器发包严重，禁用世界处理器。请联系地图地图作者优化.".with())
        broadcast("BuildHealthChanged ${healthChanged.avgCount.toInt()}/s".asPlaceHoldString())
    }
}
command("debugLogicPacket", "查看逻辑发包速率") {
    permission = dotId
    body {
        val list = list.groupBy { it }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
        reply(
            """
            血量变化: ${healthChanged.avgCount.toInt()}/s
            当前发包速率: ${packetRate.avgCount.toInt()}/s
            ${list}
        """.trimIndent().asPlaceHoldString()
        )
    }
}
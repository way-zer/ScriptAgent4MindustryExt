package wayzer.user

import cf.wayzer.placehold.DynamicVar
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.world.Block
import java.time.Duration
import java.time.Instant
import java.util.*

data class StatisticsData(
        var playedTime: Int = 0,
        var idleTime: Int = 0,
        var buildScore: Float = 0f,
        var breakBlock: Int = 0
)

val statisticsData = mutableMapOf<String, StatisticsData>()
val Player.data get() = statisticsData.getOrPut(uuid) { StatisticsData() }
registerVarForType<StatisticsData>().apply {
    registerChild("playedTime", "本局在线时间", DynamicVar { obj, _ -> Duration.ofSeconds(obj.playedTime.toLong()) })
    registerChild("idleTime", "本局在线时间", DynamicVar { obj, _ -> Duration.ofSeconds(obj.idleTime.toLong()) })
    registerChild("buildScore", "建筑积分", DynamicVar { obj, p ->
        if (!p.isNullOrBlank()) p.format(obj.buildScore)
        else obj.buildScore
    })
    registerChild("breakBlock", "破坏方块数", DynamicVar { obj, _ -> obj.breakBlock })
}
onDisable {
    PlaceHoldString.bindTypes.remove(StatisticsData::class.java)//局部类，防止泄漏
}

val Player.isIdle get() = velocity().isZero && !isBuilding
val Block.buildScore: Float
    get() {
        //如果有更好的建筑积分规则，请修改此处
        return buildCost / 60f //建筑时间(单位秒)
    }

listen<EventType.ResetEvent> {
    statisticsData.clear()
}
onEnable {
    launch {
        while (true) {
            delay(1000)
            playerGroup.forEach {
                it.data.playedTime++
                if (it.isIdle)
                    it.data.idleTime++
            }
        }
    }
}
listen<EventType.BlockBuildEndEvent> {
    it.player?.data?.apply {
        if (it.breaking)
            breakBlock++
        else
            buildScore += it.tile.block().buildScore
    }
}

listen<EventType.GameOverEvent> {
    val startTime by PlaceHold.reference<Date>("state.startTime")
    val gameTime = Duration.between(startTime.toInstant(), Instant.now())
    var totalTime = 0
    val list = statisticsData.filterValues { it.playedTime > 60 }.toList().sortedByDescending {
        it.second.run { playedTime - 0.7 * idleTime + 0.4 * buildScore }
    }.map { (uuid, data) ->
        val player = netServer.admins.getInfo(uuid)
        totalTime += data.playedTime - data.idleTime
        "[white]{player.name}[white]({statistics.playedTime:分钟}/{statistics.idleTime:分钟}/{statistics.buildScore:%.1f}),".with(
                "player" to player, "statistics" to data
        )
    }
    broadcast("""
        [yellow]本局游戏时长: {gameTime:分钟}
        [yellow]有效总贡献时长: {totalTime:分钟}
        [yellow]贡献排行榜(时长/挂机/建筑): {list}
    """.trimIndent().with("gameTime" to gameTime, "totalTime" to Duration.ofSeconds(totalTime.toLong()), "list" to list))
}
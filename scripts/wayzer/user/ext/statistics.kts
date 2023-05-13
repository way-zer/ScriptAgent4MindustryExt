@file:Depends("wayzer/maps", "监测投票换图")
@file:Depends("wayzer/user/userService")

package wayzer.user.ext

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi
import coreLibrary.lib.util.loop
import mindustry.game.Team
import mindustry.world.Block
import wayzer.MapChangeEvent
import wayzer.user.UserService
import java.io.Serializable
import java.time.Duration
import kotlin.math.ceil
import kotlin.math.min

class GameoverStatisticsEvent(
    var data: List<StatisticsData>//sorted by score
) : Event {
    companion object : Event.Handler()
}

data class StatisticsData(
    val uuid: String,
    var name: String = "",
    var playedTime: Int = 0,
    var idleTime: Int = 0,
    var buildScore: Float = 0f,
    var breakBlock: Int = 0,
    @Transient var pvpTeam: Team = Team.sharded
) : Serializable {
    //set when gameover
    var win: Boolean = false
    var score: Double = 0.0
    var exp: Int = 0

    fun cal(winTeam: Team) {
        win = state.rules.pvp && pvpTeam == winTeam
        score = playedTime - 0.8 * idleTime +
                0.6 * min(buildScore, 0.75f * playedTime) +
                if (win) 600 * (1 - idleTime / playedTime) else 0
        exp = ceil((score * 15 / 3600).coerceAtMost(60.0)).toInt()//3600点积分为15,40封顶
    }
}

val Block.buildScore: Float
    get() {
        //如果有更好的建筑积分规则，请修改此处
        return buildCost / 60f //建筑时间(单位秒)
    }
val Player.active
    get() = depends("wayzer/user/ext/activeCheck")
        ?.import<(Player) -> Int>("inactiveTime")
        ?.let { it(this) < 5000 } ?: true

val userService = contextScript<UserService>()

//region Data
@Savable
val statisticsData = mutableMapOf<String, StatisticsData>()
val Player.data get() = statisticsData.getOrPut(uuid()) { StatisticsData(uuid()) }
customLoad(::statisticsData) { statisticsData += it }
listen<EventType.ResetEvent> { statisticsData.clear() }

registerVarForType<StatisticsData>().apply {
    registerChild("playedTime", "本局在线时间", DynamicVar.obj { Duration.ofSeconds(it.playedTime.toLong()) })
    registerChild("idleTime", "本局在线时间", DynamicVar.obj { Duration.ofSeconds(it.idleTime.toLong()) })
    registerChild("buildScore", "建筑积分") { _, obj, p ->
        if (!p.isNullOrBlank()) p.format(obj.buildScore)
        else obj.buildScore
    }
    registerChild("score", "综合得分", DynamicVar.obj { it.score })
    registerChild("breakBlock", "破坏方块数", DynamicVar.obj { it.breakBlock })
}
registerVarForType<Player>().apply {
    registerChild("statistics", "游戏统计数据", DynamicVar.obj { it.data })
}
onDisable {
    PlaceHoldApi.resetTypeBinder(StatisticsData::class.java)//局部类，防止泄漏
}
//endregion

listen<EventType.PlayerJoin> {
    it.player.data.pvpTeam = it.player.team()
}
listen<EventType.PlayEvent> {
    launch(Dispatchers.gamePost) {
        Groups.player.forEach { p ->
            p.data.pvpTeam = p.team()
        }
    }
}

onEnable {
    loop(Dispatchers.game) {
        delay(1000)
        Groups.player.forEach {
            it.data.name = it.info.lastName
            it.data.playedTime++
            if (it.dead() || !it.active)
                it.data.idleTime++
        }
    }
}
listen<EventType.BlockBuildEndEvent> {
    it.unit?.player?.data?.apply {
        if (it.breaking)
            breakBlock++
        else
            buildScore += it.tile.block().buildScore
    }
}

//region gameOver
listen<EventType.GameOverEvent> { event ->
    onGameOver(event.winner)
}

fun onGameOver(winner: Team) {
    val gameTime by PlaceHold.reference<Duration>("state.gameTime")
    if (state.rules.infiniteResources || state.rules.editor) {
        return broadcast(
            """
            [yellow]地图: [{map.id}]{map.name}[yellow]
            [yellow]总游戏时长: {state.mapTime:分钟}
            [yellow]本局游戏时长: {state.gameTime:分钟}
            [yellow]沙盒或编辑器模式,不计算贡献
        """.trimIndent().with()
        )
    }

    launch(Dispatchers.game) {
        val sortedData = statisticsData.values
            .filter { it.playedTime > 60 }
            .onEach { it.cal(winner) }
            .sortedByDescending { it.score }
            .let { GameoverStatisticsEvent(it).emitAsync().data }
        statisticsData.clear()

        val totalTime = sortedData.sumOf { it.playedTime - it.idleTime }
        val list = sortedData.map {
            "{pvpState}{name}[white]({statistics.playedTime:分钟}/{statistics.idleTime:分钟}/{statistics.buildScore:%.1f})".with(
                "name" to it.name, "statistics" to it, "pvpState" to if (it.win) "[green][胜][]" else ""
            )
        }
        broadcast(
            """
            [yellow]地图: [white][{map.id}]{map.name}
            [yellow]总游戏时长: [white]{state.mapTime:分钟}
            [yellow]本局游戏时长: [white]{state.gameTime:分钟}
            [yellow]有效总贡献时长: [white]{totalTime:分钟}
            [yellow]贡献排行榜(时长/挂机/建筑): [white]{list}
            """.trimIndent()
                .with("totalTime" to Duration.ofSeconds(totalTime.toLong()), "list" to list)
        )

        if (sortedData.isNotEmpty() && gameTime > Duration.ofMinutes(15)) withContext(Dispatchers.IO) {
            sortedData.groupBy { PlayerData.findByIdWithTransaction(it.uuid)?.profile }
                .forEach { (profile, data) ->
                    if (profile == null) return@forEach
                    val best = data.maxBy { it.score }
                    userService.updateExp(profile, best.exp, "游戏结算")
                }
        }
    }
}
listenTo<MapChangeEvent>(Event.Priority.Before) {
    if (statisticsData.none { it.value.playedTime > 60 }) return@listenTo
    onGameOver(Team.derelict)
}
//endregion
package wayzer.user

import cf.wayzer.placehold.DynamicVar
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.world.Block
import mindustry.world.blocks.distribution.Conveyor
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

data class StatisticsData(
        var playedTime: Int = 0,
        var idleTime: Int = 0,
        var buildScore: Float = 0f,
        var breakBlock: Int = 0,
        var lastActive: Long = 0,
        @Transient var pvpTeam: Team = Team.sharded
) : Serializable {
    val win get() = state.rules.pvp && pvpTeam == teamWin

    //加权比分
    val score get() = playedTime - 0.8 * idleTime + 0.6 * buildScore + if (win) 1200 * (1 - idleTime / playedTime) else 0

    //结算经验计算
    val exp get() = min(ceil(score * 15 / 3600).toInt(), 40)//3600点积分为15,40封顶

    companion object {
        lateinit var teamWin: Team
    }
}

val Block.buildScore: Float
    get() {
        //如果有更好的建筑积分规则，请修改此处
        return buildCost / 60f //建筑时间(单位秒)
    }
val Player.isIdle get() = (unit().vel.isZero(1e-9F) || (unit().onSolid() && tileOn()?.block() is Conveyor)) && !builder().isBuilding && !shooting() && textFadeTime < 0
val Player.active: Boolean
    get() {//是否挂机超过10秒
        if (!isIdle) data.lastActive = System.currentTimeMillis()
        return System.currentTimeMillis() - data.lastActive < 10_000
    }

fun active(p: Player) = p.active
export(::active)

@Savable
val statisticsData = mutableMapOf<String, StatisticsData>()
customLoad(::statisticsData) { statisticsData += it }
val Player.data get() = statisticsData.getOrPut(uuid ?: "UNKNOW") { StatisticsData() }
registerVarForType<StatisticsData>().apply {
    registerChild("playedTime", "本局在线时间", DynamicVar { obj, _ -> Duration.ofSeconds(obj.playedTime.toLong()) })
    registerChild("idleTime", "本局在线时间", DynamicVar { obj, _ -> Duration.ofSeconds(obj.idleTime.toLong()) })
    registerChild("buildScore", "建筑积分", DynamicVar { obj, p ->
        if (!p.isNullOrBlank()) p.format(obj.buildScore)
        else obj.buildScore
    })
    registerChild("breakBlock", "破坏方块数", DynamicVar { obj, _ -> obj.breakBlock })
}
registerVarForType<Player>().apply {
    registerChild("statistics", "游戏统计数据", DynamicVar { p, _ -> p.data })
}
onDisable {
    PlaceHoldString.bindTypes.remove(StatisticsData::class.java)//局部类，防止泄漏
}

listen<EventType.ResetEvent> {
    statisticsData.clear()
}
listen<EventType.PlayerJoin> {
    it.player.data.pvpTeam = it.player.team()
}

onEnable {
    launch {
        while (true) {
            delay(1000)
            playerGroup.forEach {
                it.data.playedTime++
                if (!it.active)
                    it.data.idleTime++
            }
        }
    }
}
listen<EventType.BlockBuildEndEvent> {
    it.unit.player?.data?.apply {
        if (it.breaking)
            breakBlock++
        else
            buildScore += it.tile.block().buildScore
    }
}

listen<EventType.GameOverEvent> { event ->
    val startTime by PlaceHold.reference<Date>("state.startTime")
    val gameTime = Duration.between(startTime.toInstant(), Instant.now())
    if (state.rules.mode() in arrayOf(Gamemode.editor, Gamemode.sandbox)) {
        broadcast("""
            [yellow]本局游戏时长: {gameTime:分钟}
            [yellow]沙盒或编辑器模式,不计算贡献
        """.trimIndent().with("gameTime" to gameTime))
    }

    StatisticsData.teamWin = if (state.rules.mode() != Gamemode.survival) event.winner else Team.sharded
    var totalTime = 0
    val sortedData = statisticsData.filterValues { it.playedTime > 60 }
            .mapKeys { netServer.admins.getInfo(it.key) }
            .toList()
            .sortedByDescending { it.second.score }
    val list = sortedData.map { (player, data) ->
        totalTime += data.playedTime - data.idleTime
        "[white]{pvpState}{player.name}[white]({statistics.playedTime:分钟}/{statistics.idleTime:分钟}/{statistics.buildScore:%.1f}),".with(
                "player" to player, "statistics" to data, "pvpState" to if (data.win) "[green][胜][]" else ""
        )
    }
    broadcast("""
        [yellow]本局游戏时长: {gameTime:分钟}
        [yellow]有效总贡献时长: {totalTime:分钟}
        [yellow]贡献排行榜(时长/挂机/建筑): {list}
    """.trimIndent().with("gameTime" to gameTime, "totalTime" to Duration.ofSeconds(totalTime.toLong()), "list" to list))

    if (sortedData.isNotEmpty() && depends("wayzer/user/expReward") != null && gameTime > Duration.ofMinutes(15)) {
        val updateExp = depends("wayzer/user/level")?.import<PlayerProfile.(Int) -> List<Player>>("updateExp")
        if (updateExp != null) {
            @OptIn(CacheEntity.NeedTransaction::class)
            transaction {
                val map = mutableMapOf<PlayerProfile, StatisticsData>()
                sortedData.groupBy { PlayerData.find(it.first)?.profile }.forEach { (key, value) ->
                    if (key == null || value.isEmpty()) return@forEach
                    map[key] = value.maxBy { it.second.score }!!.second
                }
                map.forEach { (profile, data) ->
                    profile.updateExp(data.exp).forEach {
                        it.sendMessage("[green]经验 +${data.exp}")
                    }
                    profile.save()
                }
            }
        }
    }
}